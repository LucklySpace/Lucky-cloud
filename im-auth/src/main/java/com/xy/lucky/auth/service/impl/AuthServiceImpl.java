package com.xy.lucky.auth.service.impl;


import com.alibaba.nacos.common.utils.JacksonUtils;
import com.fasterxml.jackson.core.type.TypeReference;
import com.xy.lucky.auth.domain.*;
import com.xy.lucky.auth.security.token.MobileAuthenticationToken;
import com.xy.lucky.auth.security.token.QrScanAuthenticationToken;
import com.xy.lucky.auth.security.token.UserAuthenticationToken;
import com.xy.lucky.auth.service.AuthService;
import com.xy.lucky.auth.service.SmsService;
import com.xy.lucky.auth.utils.QRCodeUtil;
import com.xy.lucky.auth.utils.RedisCache;
import com.xy.lucky.core.constants.IMConstant;
import com.xy.lucky.core.constants.NacosMetadataConstants;
import com.xy.lucky.core.constants.ServiceNameConstants;
import com.xy.lucky.core.model.IMRegisterUser;
import com.xy.lucky.core.utils.JwtUtil;
import com.xy.lucky.domain.po.ImUserDataPo;
import com.xy.lucky.domain.vo.UserVo;
import com.xy.lucky.dubbo.web.api.database.user.ImUserDataDubboService;
import com.xy.lucky.dubbo.web.api.database.user.ImUserDubboService;
import com.xy.lucky.general.response.domain.ResultCode;
import com.xy.lucky.security.RSAKeyProperties;
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
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.xy.lucky.core.constants.IMConstant.USER_CACHE_PREFIX;

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
    public IMLoginResult login(IMLoginRequest req) {
        log.info("开始登录：authType={}, principal={}", req.getAuthType(), req.getPrincipal());
        Authentication auth;
        try {
            auth = switch (req.getAuthType()) {
                case IMConstant.AUTH_TYPE_FORM ->
                    // 表单登录
                        authenticationManager.authenticate(
                                new UserAuthenticationToken(req.getPrincipal(), req.getCredentials()));
                case IMConstant.AUTH_TYPE_SMS ->
                    // 手机验证码登录
                        authenticationManager.authenticate(
                                new MobileAuthenticationToken(req.getPrincipal(), req.getCredentials()));
                case IMConstant.AUTH_TYPE_QR ->
                    // 扫码登录
                        authenticationManager.authenticate(
                                new QrScanAuthenticationToken(req.getPrincipal(), req.getCredentials()));
                default -> {
                    log.warn("不支持的认证类型：{}", req.getAuthType());
                    throw new AuthenticationFailException(ResultCode.UNSUPPORTED_AUTHENTICATION_TYPE);
                }
            };
        } catch (Exception ex) {
            log.error("认证失败：authType={}, principal={}", req.getAuthType(), req.getPrincipal(), ex);
            throw new AuthenticationFailException(ResultCode.AUTHENTICATION_FAILED);
        }
        // 生成用户认证信息
        IMLoginResult loginResult = generateAuthInfo(auth);

        IMRegisterUser imRegisterUser = redisCache.get(USER_CACHE_PREFIX + loginResult.getUserId());

        loginResult.setConnectEndpoints(fetchConnectServerEndpoints(imRegisterUser));

        log.info("登录成功：userId={}", loginResult.getUserId());

        return loginResult;
    }

    /**
     * 获取用户连接服务端信息
     *
     * @param imRegisterUser
     * @return
     */
    private List<IMConnectEndpointMetadata> fetchConnectServerEndpoints(IMRegisterUser imRegisterUser) {
        boolean isConnectedServer = imRegisterUser != null && StringUtils.hasLength(imRegisterUser.getBrokerId());
        List<ServiceInstance> instances = discoveryClient.getInstances(ServiceNameConstants.SVC_IM_CONNECT);
        if (CollectionUtils.isEmpty(instances)) {
            return Collections.emptyList();
        }

        Map<String, List<ServiceInstance>> brokerGroups = instances.stream()
                .collect(Collectors.groupingBy(instance ->
                        instance.getMetadata().getOrDefault(NacosMetadataConstants.BROKER_ID, instance.getHost())));

        if (isConnectedServer) {
            return brokerGroups
                    .get(imRegisterUser.getBrokerId())
                    .stream()
                    .map(this::buildIMConnectEndpointMetadata)
                    .collect(Collectors.toList());
        }

        List<IMConnectEndpointMetadata> result = new ArrayList<>();
        List<String> brokerKeys = new ArrayList<>(brokerGroups.keySet());
        Random random = new Random();

        for (int i = 0; i < 3; i++) {
            String brokerKey = brokerKeys.get(i % brokerKeys.size());
            List<ServiceInstance> brokerInstances = brokerGroups.get(brokerKey);

            ServiceInstance selectedInstance = brokerInstances.get(
                    random.nextInt(brokerInstances.size()));

            IMConnectEndpointMetadata endpointMetadata = buildIMConnectEndpointMetadata(selectedInstance);
            result.add(endpointMetadata);
        }

        return result;
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
                .protocols(JacksonUtils.toObj(instanceMetadata.get(NacosMetadataConstants.PROTOCOLS), new TypeReference<List<String>>() {
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

        // 获取用户信息
        ImUserDataPo data = imUserDataDubboService.queryOne(userId);

        UserVo vo = new UserVo();

        if (Objects.nonNull(data)) {

            BeanUtils.copyProperties(data, vo);
        }

        return vo;
    }

    /**
     * 检查用户是否在线（Redis 中有对应 key 即在线）。
     *
     * @param userId 用户 ID
     * @return Mono 布尔值
     */
    @Override
    public Boolean isOnline(String userId) {

        boolean online = redisCache.hasKey(IMConstant.USER_CACHE_PREFIX + userId);

        log.debug("用户在线检查：userId={}, online={}", userId, online);

        return online;
    }

    // --------------------------------------------------
    // 3. Token 刷新
    // --------------------------------------------------

    /**
     * 刷新 JWT Token。
     *
     * @param request HttpServletRequest
     * @return Mono<Result < 新 token>>
     */
    @Override
    public Map<String, String> refreshToken(HttpServletRequest request) {

        String oldToken = extractToken(request);

        if (!StringUtils.hasText(oldToken)) {

            log.warn("未检测到旧 token");

            throw new AuthenticationFailException(ResultCode.TOKEN_IS_NULL);
        }

        String newToken = JwtUtil.refreshToken(oldToken, iMSecurityAuthProperties.getExpiration(), ChronoUnit.HOURS);

        log.info("Token 刷新成功");

        return Map.of("token", newToken);
    }

    // --------------------------------------------------
    // 4. RSA 公钥获取
    // --------------------------------------------------

    /**
     * 返回当前 RSA 公钥字符串
     */
    @Override
    public Map<String, String> getPublicKey() {
        String pubKey = iMRSAKeyProperties.getPublicKeyStr();

        log.debug("获取 RSA 公钥");

        return Map.of("publicKey", pubKey);
    }

    // --------------------------------------------------
    // 5. 二维码登录流程
    // --------------------------------------------------

    /**
     * 生成二维码：返回凭证 code、base64 图片及到期时间。
     */
    @Override
    public IMQRCodeResult generateQRCode(String qrCodeId) {

        log.info("生成二维码：qrCodeId={}", qrCodeId);

        // 1. 拼装完整的 Redis Key
        String redisKey = IMConstant.QRCODE_KEY_PREFIX + qrCodeId;

        // 2. 生成 base64 格式的二维码图片
        String image = createCodeToBase64(redisKey);

        // 3. 构建二维码状态实体，初始状态为待扫描
        IMQRCode qr = IMQRCode.builder()
                .code(qrCodeId)
                .status(IMConstant.QRCODE_PENDING)
                .createdAt(System.currentTimeMillis())
                .build();

        // 4. 将二维码状态存入 Redis，3 分钟后过期
        redisCache.set(redisKey, qr, 3, TimeUnit.MINUTES);

        // 5. 返回统一结果
        return new IMQRCodeResult()
                .setCode(qrCodeId)
                .setStatus(IMConstant.QRCODE_PENDING)
                .setImageBase64(image)
                .setExpireAt(System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(3));
    }

    /**
     * 扫码接口：更新状态为已扫描，30 秒后过期。
     */
    @Override
    public IMQRCodeResult scanQRCode(Map<String, String> payload) {

        String qrCodeId = payload.get("qrCode");

        String userId = payload.get("userId");

        log.info("扫码通知：qrCodeId={}, userId={}", qrCodeId, userId);

        String redisKey = IMConstant.QRCODE_KEY_PREFIX + qrCodeId;

        // 1. 检查 Redis 中是否存在该二维码
        if (!redisCache.hasKey(redisKey)) {

            log.warn("二维码无效或已过期：qrCodeId={}", qrCodeId);
            // 已失效
            return new IMQRCodeResult()
                    .setCode(qrCodeId)
                    .setStatus(IMConstant.QRCODE_EXPIRED);
        }

        // 2. 获取并更新二维码状态
        IMQRCode qr = redisCache.get(redisKey);
        qr.setStatus(IMConstant.QRCODE_AUTHORIZED)
                .setUserId(userId)
                .setScannedAt(System.currentTimeMillis());

        // 3. 写回 Redis，设置 30 秒后过期
        redisCache.set(redisKey, qr, 30, TimeUnit.SECONDS);

        // 4. 返回统一响应
        return new IMQRCodeResult()
                .setCode(qrCodeId)
                .setStatus(IMConstant.QRCODE_AUTHORIZED)
                .setExpireAt(System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(30));
    }

    /**
     * 状态查询：若已扫描则生成临时密码并标记已授权，20 秒后过期。
     */
    @Override
    public IMQRCodeResult getQRCodeStatus(String qrCodeId) {

        log.debug("查询二维码状态：qrCodeId={}", qrCodeId);

        String redisKey = IMConstant.QRCODE_KEY_PREFIX + qrCodeId;

        IMQRCode qr = redisCache.get(redisKey);

        // 如果二维码不存在或已过期
        if (Objects.isNull(qr)) {
            log.warn("二维码不存在或已过期：qrCodeId={}", qrCodeId);

            return new IMQRCodeResult()
                    .setCode(qrCodeId)
                    .setStatus(IMConstant.QRCODE_EXPIRED);
        }

        // 如果已扫描且确认登录，生成临时密码并标记为已授权
        if (IMConstant.QRCODE_AUTHORIZED.equals(qr.getStatus())) {

            // 生成临时密码
            String tempPwd = String.valueOf((int) ((Math.random() * 9 + 1) * 100000));

            qr.setStatus(IMConstant.QRCODE_AUTHORIZED)
                    .setPassword(tempPwd)
                    .setLoggedInAt(System.currentTimeMillis());

            // 更新 Redis，20 秒后过期
            redisCache.set(redisKey, qr, 20, TimeUnit.SECONDS);

            log.info("二维码授权：qrCodeId={}, password={}", qrCodeId, tempPwd);

            // 返回包含临时密码的响应
            return new IMQRCodeResult()
                    .setCode(qrCodeId)
                    .setStatus(IMConstant.QRCODE_AUTHORIZED)
                    .setExpireAt(System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(20))
                    .setExtra(Map.of("password", tempPwd));
        }

        // 其他状态直接返回
        return new IMQRCodeResult()
                .setCode(qrCodeId)
                .setStatus(qr.getStatus());
    }

    // --------------------------------------------------
    // 6. 辅助方法
    // --------------------------------------------------

    /**
     * 生成登录用 JWT Token 并封装返回结果
     */
    private IMLoginResult generateAuthInfo(Authentication auth) {

        String userId = auth.getPrincipal().toString();

        // 生成token
        String token = JwtUtil.createToken(userId, iMSecurityAuthProperties.getExpiration(), ChronoUnit.HOURS);

        log.debug("生成 JWT Token：userId={}", userId);

        return new IMLoginResult()
                .setUserId(userId)
                .setAccessToken(token)
                .setExpiration(iMSecurityAuthProperties.getExpiration());
    }

    /**
     * 从请求头或参数中提取 Bearer Token
     */
    private String extractToken(HttpServletRequest req) {

        String header = req.getHeader(IMConstant.AUTH_TOKEN_HEADER);

        if (StringUtils.hasText(header)) {
            return header.replaceFirst(IMConstant.BEARER_PREFIX, "").trim();
        }

        String param = req.getParameter(IMConstant.ACCESS_TOKEN_PARAM);

        if (StringUtils.hasText(param)) {
            return param.replaceFirst(IMConstant.BEARER_PREFIX, "").trim();
        }

        return null;
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
    public String sendSms(String phone) {
        try {
            return smsService.sendMessage(phone);
        } catch (Exception e) {
            throw new AuthenticationFailException(ResultCode.SMS_ERROR);
        }
    }
}



