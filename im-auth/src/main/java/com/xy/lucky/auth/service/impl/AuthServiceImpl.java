package com.xy.lucky.auth.service.impl;


import com.alibaba.nacos.common.utils.JacksonUtils;
import com.fasterxml.jackson.core.type.TypeReference;
import com.xy.lucky.auth.domain.*;
import com.xy.lucky.auth.domain.vo.UserVo;
import com.xy.lucky.auth.security.config.RSAKeyProperties;
import com.xy.lucky.auth.security.domain.AuthRequestContext;
import com.xy.lucky.auth.security.token.MobileAuthenticationToken;
import com.xy.lucky.auth.security.token.QrScanAuthenticationToken;
import com.xy.lucky.auth.security.token.UserAuthenticationToken;
import com.xy.lucky.auth.service.AuthService;
import com.xy.lucky.auth.service.AuthTokenService;
import com.xy.lucky.auth.service.SmsService;
import com.xy.lucky.auth.utils.QRCodeUtil;
import com.xy.lucky.auth.utils.RedisCache;
import com.xy.lucky.auth.utils.RequestContextUtil;
import com.xy.lucky.core.constants.IMConstant;
import com.xy.lucky.core.constants.NacosMetadataConstants;
import com.xy.lucky.core.constants.ServiceNameConstants;
import com.xy.lucky.dubbo.web.api.database.user.ImUserDataDubboService;
import com.xy.lucky.dubbo.web.api.database.user.ImUserDubboService;
import com.xy.lucky.general.response.domain.ResultCode;
import com.xy.lucky.security.exception.AuthenticationFailException;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.beans.BeanUtils;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
public class AuthServiceImpl implements AuthService {

    @DubboReference
    private ImUserDubboService imUserDubboService;

    @DubboReference
    private ImUserDataDubboService imUserDataDubboService;

    @Resource
    private RedisCache redisCache;
    @Resource
    private AuthenticationManager authenticationManager;
    @Resource
    private AuthTokenService authTokenService;
    @Resource
    private RSAKeyProperties iMRSAKeyProperties;
    @Resource
    private DiscoveryClient discoveryClient;
    @Resource
    private SmsService smsService;

    // --------------------------------------------------
    // 1. 统一登录接口
    // --------------------------------------------------

    /**
     * 根据不同 authType 执行用户名密码、短信或扫码登录。
     */
    @Override
    public LoginResult login(LoginRequest req, HttpServletRequest request) {
        log.info("用户登录请求：authType={}, principal={}", req.getAuthType(), req.getPrincipal());

        // 1. 核心认证逻辑
        Authentication auth = authenticate(req, request);

        // 2. 签发会话令牌
        String clientIp = RequestContextUtil.resolveClientIp(request);
        String deviceId = RequestContextUtil.resolveDeviceId(request, clientIp);
        LoginResult loginResult = generateAuthInfo(auth, deviceId, clientIp);

        // 3. 异步获取可用的 IM 服务节点（用于长连接引导）
        try {
            List<ConnectEndpointMetadata> endpoints = fetchConnectServerEndpoints();
            loginResult.setConnectEndpoints(endpoints);
        } catch (Exception ex) {
            log.error("获取连接端点失败: {}", ex.getMessage());
        }

        return loginResult;
    }

    /**
     * 内部认证分发逻辑
     */
    private Authentication authenticate(LoginRequest req, HttpServletRequest request) {
        try {
            return switch (req.getAuthType()) {
                case IMConstant.AUTH_TYPE_FORM ->
                        authenticationManager.authenticate(new UserAuthenticationToken(req.getPrincipal(), req.getCredentials()));
                case IMConstant.AUTH_TYPE_SMS -> {
                    String clientIp = RequestContextUtil.resolveClientIp(request);
                    String deviceId = RequestContextUtil.resolveDeviceId(request, clientIp);
                    MobileAuthenticationToken token = new MobileAuthenticationToken(req.getPrincipal(), req.getCredentials());
                    token.setDetails(new AuthRequestContext(clientIp, deviceId));
                    yield authenticationManager.authenticate(token);
                }
                case IMConstant.AUTH_TYPE_QR ->
                        authenticationManager.authenticate(new QrScanAuthenticationToken(req.getPrincipal(), req.getCredentials()));
                default -> throw new AuthenticationFailException(ResultCode.UNSUPPORTED_AUTHENTICATION_TYPE);
            };
        } catch (Exception ex) {
            log.error("身份认证失败 [{}]: {}", req.getAuthType(), ex.getMessage());
            throw new AuthenticationFailException(ResultCode.AUTHENTICATION_FAILED);
        }
    }

    /**
     * 获取用户连接服务端信息
     * <p>
     * 策略：从不同的 broker 分组中选择实例，打乱顺序后返回最多 3 个端点，实现负载均衡。
     */
    private List<ConnectEndpointMetadata> fetchConnectServerEndpoints() {
        List<ServiceInstance> instances = discoveryClient.getInstances(ServiceNameConstants.SVC_IM_CONNECT);
        if (CollectionUtils.isEmpty(instances)) {
            return Collections.emptyList();
        }

        // 按 brokerId 分组，避免同一物理机/容器过度集中
        return instances.stream()
                .collect(Collectors.groupingBy(instance ->
                        instance.getMetadata().getOrDefault(NacosMetadataConstants.BROKER_ID, instance.getHost())))
                .values()
                .stream()
                // 每个 Broker 组内随机抽取一个实例
                .map(group -> group.get(ThreadLocalRandom.current().nextInt(group.size())))
                // 打乱 Broker 间的顺序
                .collect(Collectors.collectingAndThen(Collectors.toList(), list -> {
                    Collections.shuffle(list);
                    return list.stream();
                }))
                .limit(3)
                .map(this::buildIMConnectEndpointMetadata)
                .toList();
    }

    /**
     * 构建连接元数据。
     *
     * @param instance 服务实例
     * @return 连接元数据
     */
    private ConnectEndpointMetadata buildIMConnectEndpointMetadata(ServiceInstance instance) {
        Map<String, String> instanceMetadata = instance.getMetadata();
        return ConnectEndpointMetadata.builder()
                .region(instanceMetadata.get(NacosMetadataConstants.REGION))
                .priority(Integer.parseInt(instanceMetadata.get(NacosMetadataConstants.PRIORITY)))
                .wsPath(instanceMetadata.get(NacosMetadataConstants.WS_PATH))
                .endpoint(instance.getHost() + ":" + instance.getPort())
                .protocols(JacksonUtils.toObj(instanceMetadata.get(NacosMetadataConstants.PROTOCOLS), new TypeReference<>() {
                }))
                .createdAt(System.currentTimeMillis() / 1000L)
                .build();
    }

    // --------------------------------------------------
    // 2. 用户信息与在线状态
    // --------------------------------------------------

    /**
     * 查询用户基本信息。
     *
     * @param userId 用户 ID
     * @return 用户视图对象
     */
    @Override
    public UserVo info(String userId) {
        log.debug("获取用户信息：userId={}", userId);
        return Optional.ofNullable(imUserDataDubboService.queryOne(userId))
                .map(data -> {
                    UserVo vo = new UserVo();
                    BeanUtils.copyProperties(data, vo);
                    return vo;
                }).orElseGet(UserVo::new);
    }

    @Override
    public Boolean isOnline(String userId) {
        boolean online = redisCache.hasKey(IMConstant.USER_CACHE_PREFIX + userId);
        log.debug("用户在线检查：userId={}, online={}", userId, online);
        return online;
    }

    /**
     * 刷新访问令牌逻辑
     */
    @Override
    public AuthRefreshTokenResult refreshToken(HttpServletRequest request) {
        String refreshHeader = request.getHeader("X-Refresh-Token");
        String refreshParam = request.getParameter(IMConstant.REFRESH_TOKEN_PARAM);
        String refreshToken = authTokenService.resolveRefreshToken(refreshHeader, refreshParam).orElse(null);
        if (!StringUtils.hasText(refreshToken)) {
            log.warn("刷新 Token 失败：请求中未包含 refresh token");
            throw new AuthenticationFailException(ResultCode.TOKEN_IS_NULL);
        }

        String clientIp = RequestContextUtil.resolveClientIp(request);
        String deviceId = RequestContextUtil.resolveDeviceId(request, clientIp);
        String userAgent = request.getHeader("User-Agent");

        try {
            AuthTokenPair pair = authTokenService.refreshTokens(refreshToken, clientIp, deviceId);
            if (pair == null) {
                throw new AuthenticationFailException(ResultCode.TOKEN_IS_INVALID);
            }
            log.info("Token 刷新成功");

            return AuthRefreshTokenResult.builder().accessToken(pair.getAccessToken()).build();
        } catch (Exception e) {
            log.error("Token 刷新失败: {}", e.getMessage());
            throw new AuthenticationFailException(ResultCode.AUTHENTICATION_FAILED);
        }
    }

    @Override
    public Map<String, String> getPublicKey() {
        log.debug("获取 RSA 公钥");
        return Map.of("publicKey", iMRSAKeyProperties.getPublicKeyStr());
    }

    /**
     * 生成登录认证二维码
     */
    @Override
    public QRCodeResult generateQRCode(String qrCodeId) {
        log.info("生成登录二维码：qrCodeId={}", qrCodeId);
        String redisKey = IMConstant.QRCODE_KEY_PREFIX + qrCodeId;
        String image = createCodeToBase64(redisKey);

        QRCode qr = QRCode.builder()
                .code(qrCodeId)
                .status(IMConstant.QRCODE_PENDING)
                .createdAt(System.currentTimeMillis())
                .build();

        // 有效期 3 分钟
        redisCache.set(redisKey, qr, 3, TimeUnit.MINUTES);
        log.info("二维码生成成功：qrCodeId={}", qrCodeId);
        return new QRCodeResult()
                .setCode(qrCodeId)
                .setStatus(IMConstant.QRCODE_PENDING)
                .setImageBase64(image)
                .setExpireAt(System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(3));
    }

    /**
     * 手机端扫码并确认授权
     */
    @Override
    public QRCodeResult scanQRCode(Map<String, String> payload) {
        String qrCodeId = payload.get("qrCode");
        String userId = payload.get("userId");
        log.info("扫码授权请求：qrCodeId={}, userId={}", qrCodeId, userId);

        String redisKey = IMConstant.QRCODE_KEY_PREFIX + qrCodeId;
        QRCode qr = redisCache.get(redisKey);

        if (qr == null) {
            log.warn("二维码不存在：qrCodeId={}", qrCodeId);
            return new QRCodeResult().setCode(qrCodeId).setStatus(IMConstant.QRCODE_EXPIRED);
        }

        // 状态流转：PENDING -> AUTHORIZED
        qr.setStatus(IMConstant.QRCODE_AUTHORIZED)
                .setUserId(userId)
                .setScannedAt(System.currentTimeMillis());

        // 授权后有效期缩短，需尽快完成登录
        redisCache.set(redisKey, qr, 30, TimeUnit.SECONDS);

        log.info("二维码授权成功：qrCodeId={}, userId={}", qrCodeId, userId);
        return new QRCodeResult()
                .setCode(qrCodeId)
                .setStatus(IMConstant.QRCODE_AUTHORIZED)
                .setExpireAt(System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(30));
    }

    /**
     * 轮询获取二维码状态及登录凭证
     */
    @Override
    public QRCodeResult getQRCodeStatus(String qrCodeId) {
        String redisKey = IMConstant.QRCODE_KEY_PREFIX + qrCodeId;
        QRCode qr = redisCache.get(redisKey);

        if (qr == null) {
            return new QRCodeResult().setCode(qrCodeId).setStatus(IMConstant.QRCODE_EXPIRED);
        }

        // 若已授权，生成临时登录凭证
        if (IMConstant.QRCODE_AUTHORIZED.equals(qr.getStatus())) {
            String tempPwd = String.format("%06d", ThreadLocalRandom.current().nextInt(1000000));
            qr.setPassword(tempPwd).setLoggedInAt(System.currentTimeMillis());
            redisCache.set(redisKey, qr, 20, TimeUnit.SECONDS);

            log.info("二维码登录凭证已签发：qrCodeId={}", qrCodeId);

            return new QRCodeResult()
                    .setCode(qrCodeId)
                    .setStatus(IMConstant.QRCODE_AUTHORIZED)
                    .setExpireAt(System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(20))
                    .setExtra(Map.of("password", tempPwd));
        }

        return new QRCodeResult().setCode(qrCodeId).setStatus(qr.getStatus());
    }

    private LoginResult generateAuthInfo(Authentication auth, String deviceId, String clientIp) {
        String userId = auth.getPrincipal().toString();
        AuthTokenPair pair = authTokenService.issueTokens(userId, deviceId, clientIp);
        log.debug("生成认证信息：userId={}, accessExpiresIn={}", userId, pair.getAccessExpiresIn());

        return new LoginResult()
                .setUserId(userId)
                .setAccessToken(pair.getAccessToken())
                .setRefreshToken(pair.getRefreshToken())
                .setAccessExpiration((int) pair.getAccessExpiresIn())
                .setRefreshExpiration((int) pair.getRefreshExpiresIn());
    }

    /**
     * 会话注销逻辑
     */
    @Override
    public Boolean logout(HttpServletRequest request) {
        String accessHeader = request.getHeader(IMConstant.AUTH_TOKEN_HEADER);
        String accessParam = request.getParameter(IMConstant.ACCESS_TOKEN_PARAM);
        String refreshHeader = request.getHeader("X-Refresh-Token");
        String refreshParam = request.getParameter(IMConstant.REFRESH_TOKEN_PARAM);

        String accessToken = authTokenService.resolveAccessToken(accessHeader, accessParam).orElse(null);
        String refreshToken = authTokenService.resolveRefreshToken(refreshHeader, refreshParam).orElse(null);

        if (!StringUtils.hasText(accessToken) && !StringUtils.hasText(refreshToken)) {
            return Boolean.FALSE;
        }
        authTokenService.revokeTokens(accessToken, refreshToken);
        log.info("用户成功退出登录");
        return Boolean.TRUE;
    }

    /**
     * 生成 Base64 二维码
     */
    private String createCodeToBase64(String content) {
        try {
            return  QRCodeUtil.generateQRCodeBase64(content, "png");
        } catch (Exception e) {
            log.error("二维码生成失败：content={}", content, e);
            return null;
        }
    }

    @Override
    public Boolean sendSms(String phone, String clientIp, String deviceId) {
        try {
            return smsService.sendMessage(phone, clientIp, deviceId);
        } catch (Exception e) {
            throw new AuthenticationFailException(ResultCode.SMS_ERROR);
        }
    }
}
