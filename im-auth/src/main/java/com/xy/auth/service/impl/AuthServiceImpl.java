package com.xy.auth.service.impl;


import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateField;
import cn.hutool.extra.qrcode.QrCodeException;
import cn.hutool.extra.qrcode.QrCodeUtil;
import cn.hutool.extra.qrcode.QrConfig;
import com.alibaba.nacos.common.utils.JacksonUtils;
import com.fasterxml.jackson.core.type.TypeReference;
import com.xy.auth.api.database.user.ImUserFeign;
import com.xy.auth.domain.*;
import com.xy.auth.security.RSAKeyProperties;
import com.xy.auth.security.SecurityProperties;
import com.xy.auth.security.token.MobileAuthenticationToken;
import com.xy.auth.security.token.QrScanAuthenticationToken;
import com.xy.auth.security.token.UserAuthenticationToken;
import com.xy.auth.service.AuthService;
import com.xy.auth.utils.RedisCache;
import com.xy.core.constants.IMConstant;
import com.xy.core.constants.NacosInstanceMetadataConstants;
import com.xy.core.constants.ServiceNameConstants;
import com.xy.core.model.IMRegisterUser;
import com.xy.core.utils.JwtUtil;
import com.xy.domain.po.ImUserDataPo;
import com.xy.domain.vo.UserVo;
import com.xy.general.response.domain.Result;
import com.xy.general.response.domain.ResultCode;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.xy.core.constants.IMConstant.USER_CACHE_PREFIX;

/**
 * AuthServiceImpl 实现类，负责处理用户认证、二维码登录等逻辑。
 *
 * @author dense
 */
@Slf4j
@Service
public class AuthServiceImpl implements AuthService {

    @Resource
    private QrConfig qrConfig;
    @Resource
    private ImUserFeign imUserFeign;
    @Resource
    private RedisCache redisCache;
    @Resource
    private AuthenticationManager authenticationManager;
    @Resource
    private SecurityProperties securityProperties;
    @Resource
    private RSAKeyProperties rsaKeyProperties;
    @Resource
    private DiscoveryClient discoveryClient;

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
    public Result<?> login(IMLoginRequest req) {
        log.info("开始登录：authType={}, principal={}", req.getAuthType(), req.getPrincipal());
        Authentication auth;
        try {
            switch (req.getAuthType()) {
                case IMConstant.AUTH_TYPE_FORM:
                    // 表单登录
                    auth = authenticationManager.authenticate(
                            new UserAuthenticationToken(req.getPrincipal(), req.getCredentials()));
                    break;
                case IMConstant.AUTH_TYPE_SMS:
                    // 手机验证码登录
                    auth = authenticationManager.authenticate(
                            new MobileAuthenticationToken(req.getPrincipal(), req.getCredentials()));
                    break;
                case IMConstant.AUTH_TYPE_QR:
                    // 扫码登录
                    auth = authenticationManager.authenticate(
                            new QrScanAuthenticationToken(req.getPrincipal(), req.getCredentials()));
                    break;
                default:
                    log.warn("不支持的认证类型：{}", req.getAuthType());
                    return Result.failed(ResultCode.UNSUPPORTED_AUTHENTICATION_TYPE);
            }
        } catch (Exception ex) {
            log.error("认证失败：authType={}, principal={}", req.getAuthType(), req.getPrincipal(), ex);
            return Result.failed(ResultCode.AUTHENTICATION_FAILED);
        }
        // 生成用户认证信息
        IMLoginResult loginResult = generateAuthInfo(auth);

        String cacheStr = redisCache.get(USER_CACHE_PREFIX + loginResult.getUserId());
        IMRegisterUser imRegisterUser = null;
        if (StringUtils.hasLength(cacheStr)) {
            imRegisterUser = JacksonUtils.toObj(cacheStr, IMRegisterUser.class);
        }
        loginResult.setConnectEndpoints(fetchConnectServerEndpoints(imRegisterUser));

        log.info("登录成功：userId={}", loginResult.getUserId());

        return Result.success(loginResult);
    }

    /**
     * 获取用户连接服务端信息
     * @param imRegisterUser
     * @return
     */
    private List<IMConnectEndpointMetadata> fetchConnectServerEndpoints(IMRegisterUser imRegisterUser) {
        boolean isConnectedServer = imRegisterUser != null && StringUtils.hasLength(imRegisterUser.getBrokerId());
        List<ServiceInstance> instances = discoveryClient.getInstances(ServiceNameConstants.SVC_IM_CONNECT);
        if (CollUtil.isEmpty(instances)) {
            return Collections.emptyList();
        }

        Map<String, List<ServiceInstance>> brokerGroups = instances.stream()
                .collect(Collectors.groupingBy(instance ->
                        instance.getMetadata().getOrDefault(NacosInstanceMetadataConstants.BROKER_ID, instance.getHost())));

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

    private IMConnectEndpointMetadata buildIMConnectEndpointMetadata(ServiceInstance instance) {
        Map<String, String> instanceMetadata = instance.getMetadata();
        return IMConnectEndpointMetadata.builder()
                .region(instanceMetadata.get(NacosInstanceMetadataConstants.REGION))
                .priority(Integer.parseInt(instanceMetadata.get(NacosInstanceMetadataConstants.PRIORITY)))
                .endpoint(instance.getHost() + ":" + instance.getPort())
                .protocols(JacksonUtils.toObj(instanceMetadata.get(NacosInstanceMetadataConstants.PROTOCOLS), new TypeReference<>() {}))
                .createdAt(Long.parseLong(instanceMetadata.get(NacosInstanceMetadataConstants.CREATED_AT)))
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
    public Result<?> info(String userId) {

        log.debug("获取用户信息：userId={}", userId);

        // 获取用户信息
        ImUserDataPo data = imUserFeign.getOneUserData(userId);

        UserVo vo = new UserVo();

        if (Objects.nonNull(data)) {

            BeanUtils.copyProperties(data, vo);
        }

        return Result.success(vo);
    }

    /**
     * 检查用户是否在线（Redis 中有对应 key 即在线）。
     *
     * @param userId 用户 ID
     * @return Mono 布尔值
     */
    @Override
    public Result<?> isOnline(String userId) {

        boolean online = redisCache.hasKey(IMConstant.USER_CACHE_PREFIX + userId);

        log.debug("用户在线检查：userId={}, online={}", userId, online);

        return Result.success(online);
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
    public Result<?> refreshToken(HttpServletRequest request) {

        String oldToken = extractToken(request);

        if (!StringUtils.hasText(oldToken)) {

            log.warn("未检测到旧 token");

            return Result.failed(ResultCode.TOKEN_IS_NULL);
        }

        String newToken = JwtUtil.refreshToken(oldToken, securityProperties.getExpiration(), DateField.HOUR);

        log.info("Token 刷新成功");

        return Result.success(Map.of("token", newToken));
    }

    // --------------------------------------------------
    // 4. RSA 公钥获取
    // --------------------------------------------------

    /**
     * 返回当前 RSA 公钥字符串
     */
    @Override
    public Result<?> getPublicKey() {
        String pubKey = rsaKeyProperties.getPublicKeyStr();

        log.debug("获取 RSA 公钥");

        return Result.success(Map.of("publicKey", pubKey));
    }

    // --------------------------------------------------
    // 5. 二维码登录流程
    // --------------------------------------------------

    /**
     * 生成二维码：返回凭证 code、base64 图片及到期时间。
     */
    @Override
    public Result<?> generateQRCode(String qrCodeId) {

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
        return Result.success(new IMQRCodeResult()
                .setCode(qrCodeId)
                .setStatus(IMConstant.QRCODE_PENDING)
                .setImageBase64(image)
                .setExpireAt(System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(3)));
    }

    /**
     * 扫码接口：更新状态为已扫描，30 秒后过期。
     */
    @Override
    public Result<?> scanQRCode(Map<String, String> payload) {

        String qrCodeId = payload.get("qrCode");

        String userId = payload.get("userId");

        log.info("扫码通知：qrCodeId={}, userId={}", qrCodeId, userId);

        String redisKey = IMConstant.QRCODE_KEY_PREFIX + qrCodeId;

        // 1. 检查 Redis 中是否存在该二维码
        if (!redisCache.hasKey(redisKey)) {

            log.warn("二维码无效或已过期：qrCodeId={}", qrCodeId);
            // 已失效
            return Result.success(new IMQRCodeResult()
                    .setCode(qrCodeId)
                    .setStatus(IMConstant.QRCODE_EXPIRED));
        }

        // 2. 获取并更新二维码状态
        IMQRCode qr = redisCache.get(redisKey);
        qr.setStatus(IMConstant.QRCODE_AUTHORIZED)
                .setUserId(userId)
                .setScannedAt(System.currentTimeMillis());

        // 3. 写回 Redis，设置 30 秒后过期
        redisCache.set(redisKey, qr, 30, TimeUnit.SECONDS);

        // 4. 返回统一响应
        return Result.success(new IMQRCodeResult()
                .setCode(qrCodeId)
                .setStatus(IMConstant.QRCODE_AUTHORIZED)
                .setExpireAt(System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(30)));
    }

    /**
     * 状态查询：若已扫描则生成临时密码并标记已授权，20 秒后过期。
     */
    @Override
    public Result<?> getQRCodeStatus(String qrCodeId) {

        log.debug("查询二维码状态：qrCodeId={}", qrCodeId);

        String redisKey = IMConstant.QRCODE_KEY_PREFIX + qrCodeId;

        IMQRCode qr = redisCache.get(redisKey);

        // 如果二维码不存在或已过期
        if (Objects.isNull(qr)) {
            log.warn("二维码不存在或已过期：qrCodeId={}", qrCodeId);

            return Result.success(new IMQRCodeResult()
                    .setCode(qrCodeId)
                    .setStatus(IMConstant.QRCODE_EXPIRED));
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
            return Result.success(new IMQRCodeResult()
                    .setCode(qrCodeId)
                    .setStatus(IMConstant.QRCODE_AUTHORIZED)
                    .setExpireAt(System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(20))
                    .setExtra(Map.of("password", tempPwd)));
        }

        // 其他状态直接返回
        return Result.success(new IMQRCodeResult()
                .setCode(qrCodeId)
                .setStatus(qr.getStatus()));
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
        String token = JwtUtil.createToken(userId, securityProperties.getExpiration(), DateField.HOUR);

        log.debug("生成 JWT Token：userId={}", userId);

        return new IMLoginResult()
                .setUserId(userId)
                .setAccessToken(token)
                .setExpiration(securityProperties.getExpiration());
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
            return QrCodeUtil.generateAsBase64(content, qrConfig, QrCodeUtil.QR_TYPE_SVG);
        } catch (Exception e) {
            log.error("二维码生成失败：content={}", content, e);
            return null;
        }
    }

    /**
     * 输出二维码到 HTTP 响应流
     */
    public void createCodeToStream(String content, HttpServletResponse resp) {
        try {
            QrCodeUtil.generate(content, qrConfig, "png", resp.getOutputStream());
        } catch (IOException | QrCodeException e) {
            log.error("二维码写入流失败：content={}", content, e);
        }
    }

    @Override
    public Result<?> sendSms(String phone) {
        // TODO: 实现短信发送逻辑
        return null;
    }
}



