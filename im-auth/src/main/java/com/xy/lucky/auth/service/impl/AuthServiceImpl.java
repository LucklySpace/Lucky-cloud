package com.xy.lucky.auth.service.impl;


import com.alibaba.nacos.common.utils.JacksonUtils;
import com.fasterxml.jackson.core.type.TypeReference;
import com.xy.lucky.auth.domain.*;
import com.xy.lucky.auth.security.config.RSAKeyProperties;
import com.xy.lucky.auth.security.domain.AuthRequestContext;
import com.xy.lucky.auth.security.token.MobileAuthenticationToken;
import com.xy.lucky.auth.security.token.QrScanAuthenticationToken;
import com.xy.lucky.auth.security.token.UserAuthenticationToken;
import com.xy.lucky.auth.service.AuthService;
import com.xy.lucky.auth.service.SmsService;
import com.xy.lucky.auth.utils.QRCodeUtil;
import com.xy.lucky.auth.utils.RedisCache;
import com.xy.lucky.auth.utils.RequestContextUtil;
import com.xy.lucky.core.constants.IMConstant;
import com.xy.lucky.core.constants.NacosMetadataConstants;
import com.xy.lucky.core.constants.ServiceNameConstants;
import com.xy.lucky.core.model.IMRegisterUser;
import com.xy.lucky.core.utils.JwtUtil;
import com.xy.lucky.domain.vo.UserVo;
import com.xy.lucky.dubbo.web.api.database.user.ImUserDataDubboService;
import com.xy.lucky.dubbo.web.api.database.user.ImUserDubboService;
import com.xy.lucky.general.response.domain.ResultCode;
import com.xy.lucky.security.SecurityAuthProperties;
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

import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * AuthServiceImpl 实现类，负责处理用户认证、二维码登录等逻辑。
 *
 * @author dense
 */
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
    private SecurityAuthProperties iMSecurityAuthProperties;
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
     *
     * @param req 登录请求
     * @return 包含 token、userId、过期时间的 Result
     */
    @Override
    public IMLoginResult login(IMLoginRequest req, HttpServletRequest request) {
        log.info("用户登录请求：authType={}, principal={}", req.getAuthType(), req.getPrincipal());

        // 1. 认证
        Authentication auth = authenticate(req, request);

        // 2. 生成或获取认证信息 (Token 等)
        IMLoginResult loginResult = generateAuthInfo(auth);

        // 3. 获取可用的连接端点 (网关/Broker 地址)
        try {
            List<IMConnectEndpointMetadata> endpoints = fetchConnectServerEndpoints();
            if (CollectionUtils.isEmpty(endpoints)) {
                log.warn("未发现可用的 IM 连接节点，用户可能无法即时通信: userId={}", loginResult.getUserId());
            }
            loginResult.setConnectEndpoints(endpoints);
        } catch (Exception ex) {
            log.error("获取连接端点失败: {}", ex.getMessage());
            // 容错处理：即使获取端点失败，登录本身是成功的，允许返回，由客户端重试或稍后获取
            loginResult.setConnectEndpoints(Collections.emptyList());
        }

        log.info("用户登录成功：userId={}", loginResult.getUserId());
        return loginResult;
    }

    private Authentication authenticate(IMLoginRequest req, HttpServletRequest request) {
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
                default -> {
                    log.error("不支持的认证类型: {}", req.getAuthType());
                    throw new AuthenticationFailException(ResultCode.UNSUPPORTED_AUTHENTICATION_TYPE);
                }
            };
        } catch (Exception ex) {
            log.error("认证过程异常 [{}]: {}", req.getAuthType(), ex.getMessage());
            throw new AuthenticationFailException(ResultCode.AUTHENTICATION_FAILED);
        }
    }

    /**
     * 获取用户连接服务端信息
     * <p>
     * 策略：从不同的 broker 分组中选择实例，打乱顺序后返回最多 3 个端点，实现负载均衡。
     */
    private List<IMConnectEndpointMetadata> fetchConnectServerEndpoints() {
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
    private IMConnectEndpointMetadata buildIMConnectEndpointMetadata(ServiceInstance instance) {
        Map<String, String> instanceMetadata = instance.getMetadata();
        return IMConnectEndpointMetadata.builder()
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

    @Override
    public Map<String, String> refreshToken(HttpServletRequest request) {
        String oldToken = extractToken(request);
        if (!StringUtils.hasText(oldToken)) {
            log.warn("刷新 Token 失败：请求中未包含有效 Token");
            throw new AuthenticationFailException(ResultCode.TOKEN_IS_NULL);
        }

        try {
            String newToken = JwtUtil.refreshToken(oldToken, iMSecurityAuthProperties.getExpiration(), ChronoUnit.HOURS);
            log.info("Token 刷新成功");
            return Map.of("token", newToken);
        } catch (Exception e) {
            log.error("Token 刷新异常: {}", e.getMessage());
            throw new AuthenticationFailException(ResultCode.AUTHENTICATION_FAILED);
        }
    }

    @Override
    public Map<String, String> getPublicKey() {
        log.debug("获取 RSA 公钥");
        return Map.of("publicKey", iMRSAKeyProperties.getPublicKeyStr());
    }

    @Override
    public IMQRCodeResult generateQRCode(String qrCodeId) {
        log.info("生成二维码：qrCodeId={}", qrCodeId);
        String redisKey = IMConstant.QRCODE_KEY_PREFIX + qrCodeId;
        String image = createCodeToBase64(redisKey);

        IMQRCode qr = IMQRCode.builder()
                .code(qrCodeId)
                .status(IMConstant.QRCODE_PENDING)
                .createdAt(System.currentTimeMillis())
                .build();

        // 二维码有效期 3 分钟
        redisCache.set(redisKey, qr, 3, TimeUnit.MINUTES);

        return new IMQRCodeResult()
                .setCode(qrCodeId)
                .setStatus(IMConstant.QRCODE_PENDING)
                .setImageBase64(image)
                .setExpireAt(System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(3));
    }

    /**
     * 手机端确认授权：更新状态为已授权，并绑定用户 ID
     */
    @Override
    public IMQRCodeResult scanQRCode(Map<String, String> payload) {
        String qrCodeId = payload.get("qrCode");
        String userId = payload.get("userId");
        log.info("扫码授权：qrCodeId={}, userId={}", qrCodeId, userId);

        String redisKey = IMConstant.QRCODE_KEY_PREFIX + qrCodeId;
        IMQRCode qr = redisCache.get(redisKey);

        if (qr == null) {
            log.warn("二维码授权失败：二维码无效或已过期, qrCodeId={}", qrCodeId);
            return new IMQRCodeResult().setCode(qrCodeId).setStatus(IMConstant.QRCODE_EXPIRED);
        }

        // 更新为已授权状态，并记录授权人
        qr.setStatus(IMConstant.QRCODE_AUTHORIZED)
                .setUserId(userId)
                .setScannedAt(System.currentTimeMillis());

        // 授权后有效期缩短至 30 秒，需尽快完成登录轮询
        redisCache.set(redisKey, qr, 30, TimeUnit.SECONDS);

        return new IMQRCodeResult()
                .setCode(qrCodeId)
                .setStatus(IMConstant.QRCODE_AUTHORIZED)
                .setExpireAt(System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(30));
    }

    /**
     * 网页端轮询：获取二维码状态，若已授权则生成登录凭证
     */
    @Override
    public IMQRCodeResult getQRCodeStatus(String qrCodeId) {
        String redisKey = IMConstant.QRCODE_KEY_PREFIX + qrCodeId;
        IMQRCode qr = redisCache.get(redisKey);

        if (qr == null) {
            return new IMQRCodeResult().setCode(qrCodeId).setStatus(IMConstant.QRCODE_EXPIRED);
        }

        // 如果手机端已授权，生成一次性登录凭证（临时密码）
        if (IMConstant.QRCODE_AUTHORIZED.equals(qr.getStatus())) {
            // 生成 6 位随机数作为临时凭证
            String tempPwd = String.format("%06d", ThreadLocalRandom.current().nextInt(1000000));

            qr.setPassword(tempPwd).setLoggedInAt(System.currentTimeMillis());
            // 凭证 20 秒内有效
            redisCache.set(redisKey, qr, 20, TimeUnit.SECONDS);

            log.info("二维码登录凭证已生成：qrCodeId={}", qrCodeId);
            return new IMQRCodeResult()
                    .setCode(qrCodeId)
                    .setStatus(IMConstant.QRCODE_AUTHORIZED)
                    .setExpireAt(System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(20))
                    .setExtra(Map.of("password", tempPwd));
        }

        return new IMQRCodeResult().setCode(qrCodeId).setStatus(qr.getStatus());
    }

    private IMLoginResult generateAuthInfo(Authentication auth) {
        String userId = auth.getPrincipal().toString();
        Integer expiration = iMSecurityAuthProperties.getExpiration();

        // 优先从缓存获取已有的有效 Token（支持多端复用同一 Token 逻辑）
        IMRegisterUser userInfo = redisCache.get(IMConstant.USER_CACHE_PREFIX + userId);
        String token = (userInfo != null && StringUtils.hasText(userInfo.getToken()))
                ? userInfo.getToken()
                : JwtUtil.createToken(userId, expiration, ChronoUnit.HOURS);

        log.debug("生成/获取认证信息：userId={}, tokenSource={}", userId, (userInfo != null ? "cache" : "new"));

        return new IMLoginResult()
                .setUserId(userId)
                .setAccessToken(token)
                .setExpiration(expiration);
    }

    private String extractToken(HttpServletRequest req) {
        return Optional.ofNullable(req.getHeader(IMConstant.AUTH_TOKEN_HEADER))
                .filter(StringUtils::hasText)
                .map(h -> h.replaceFirst(IMConstant.BEARER_PREFIX, "").trim())
                .orElseGet(() -> Optional.ofNullable(req.getParameter(IMConstant.ACCESS_TOKEN_PARAM))
                        .filter(StringUtils::hasText)
                        .map(p -> p.replaceFirst(IMConstant.BEARER_PREFIX, "").trim())
                        .orElse(null));
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
    public String sendSms(String phone, String clientIp, String deviceId) {
        try {
            return smsService.sendMessage(phone, clientIp, deviceId);
        } catch (Exception e) {
            throw new AuthenticationFailException(ResultCode.SMS_ERROR);
        }
    }
}



