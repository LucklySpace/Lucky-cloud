package com.xy.auth.controller;


import cn.hutool.core.date.DateField;
import com.xy.auth.annotations.count.TakeCount;
import com.xy.auth.domain.LoginRequest;
import com.xy.auth.security.RSAKeyProperties;
import com.xy.auth.security.token.MobileAuthenticationToken;
import com.xy.auth.security.token.QrScanAuthenticationToken;
import com.xy.auth.service.ImUserService;
import com.xy.auth.service.QrCodeService;
import com.xy.auth.service.SmsService;
import com.xy.auth.utils.RSAUtil;
import com.xy.auth.utils.RedisUtil;
import com.xy.domain.vo.UserVo;
import com.xy.imcore.utils.JwtUtil;
import com.xy.response.domain.Result;
import com.xy.response.domain.ResultCode;
import com.xy.security.sign.annotation.Signature;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.xy.auth.constant.QrcodeConstant.*;


/**
 * 二维码 登录流程
 * 1. 桌面端或web端请求 生成二维码
 * 2. 桌面端展示 并轮训状态
 * 3. 移动端扫码授权,更改二维码状态,传递用户名
 * 4. 授权成功后给二维码生成临时密码
 * 5. 桌面端使用qrcode 和临时密码登录
 */
@Slf4j
@RestController
@RequestMapping("/api/{version}/auth")
@Tag(name = "auth", description = "用户认证")
public class AuthController {

    //rsa密匙类
    @Resource
    private RSAKeyProperties rsaKeyProp;

    @Resource
    private SmsService smsService;

    @Resource
    private ImUserService imUserService;

    @Resource
    private QrCodeService codeService;

    @Resource
    private RedisUtil redisUtil;

    @Resource
    private AuthenticationManager authenticationManager;

    /**
     * 统一登录接口，根据 authType 选择认证方式
     *
     * @param loginRequest 登录信息
     * @return 返回登录结果
     */
    @PostMapping("/login")
    @TakeCount
    @Operation(summary = "用户登录", tags = {"auth"}, description = "请使用此接口进行用户登录")
    @Parameters({
            @Parameter(name = "loginRequest", description = "用户登录信息", required = true, in = ParameterIn.DEFAULT)
    })
    @Signature(verify = false, sign = true)
    public Mono<Result<?>> login(@RequestBody LoginRequest loginRequest) {
        Locale locale = LocaleContextHolder.getLocale();
        Authentication authentication;
        try {
            switch (loginRequest.getAuthType()) {
                case "form":
                    // 用户名密码认证
                    authentication = authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(loginRequest.getPrincipal(), loginRequest.getCredentials()));
                    break;
                case "sms":
                    // 手机验证码认证
                    authentication = authenticationManager.authenticate(new MobileAuthenticationToken(loginRequest.getPrincipal(), loginRequest.getCredentials()));
                    break;
                case "scan":
                    // 二维码认证
                    authentication = authenticationManager.authenticate(new QrScanAuthenticationToken(loginRequest.getPrincipal(), loginRequest.getCredentials()));
                    break;
                default:
                    // 不支持的认证类型
                    return Mono.just(Result.failed(ResultCode.UNSUPPORTED_AUTHENTICATION_TYPE));
            }
        } catch (Exception e) {
            // 身份认证失败
            log.error("Authentication failed", e);
            return Mono.just(Result.failed(ResultCode.AUTHENTICATION_FAILED));
        }
        return Mono.just(Result.success(generateTokenByUsername(authentication)));
    }


    /**
     * 生成用于认证的二维码。
     *
     * @param qrcode 要编码到二维码中的随机字符串
     * @return 包含base64编码的二维码图片的Map
     */
    @GetMapping("/qrcode")
    @Operation(summary = "生成认证二维码", tags = {"auth"}, description = "请使用此接口生成认证二维码")
    @Parameters({
            @Parameter(name = "qrcode", description = "二维码内容字符串", required = true, in = ParameterIn.DEFAULT)
    })
    public Mono<Map<String, String>> qrcode(@RequestParam("qrcode") String qrcode) {

        String code = QRCODE_PREFIX + qrcode;

        // 生成 base64 图片二维码
        String codeToBase64 = codeService.createCodeToBase64(code);

        // 将二维码状态信息存储为一个Map 设置3分钟二维码有效期，状态为“待扫描”
        redisUtil.set(code, Map.of("status", QRCODE_PENDING, "createdAt", System.currentTimeMillis()), 3, TimeUnit.MINUTES);

        return Mono.just(Map.of("qrcode", codeToBase64));
    }

    /**
     * 处理二维码扫描过程。
     *
     * @param map 包含二维码和用户信息的Map
     * @return 包含扫描结果的ResponseEntity
     */
    @PostMapping("/qrcode/scan")
    @Operation(summary = "处理二维码扫描", tags = {"auth"}, description = "请使用此接口处理二维码扫描")
    @Parameters({
            @Parameter(name = "map", description = "二维码信息", required = true, in = ParameterIn.DEFAULT)
    })
    public ResponseEntity<Map<String, Object>> handleScan(@RequestBody Map<String, Object> map) {

        String qrcode = (String) map.get("qrcode");
        String userId = (String) map.get("userId");

        String redisKey = QRCODE_PREFIX + qrcode;

        Map<String, Object> response = new HashMap<>();

        if (!redisUtil.hasKey(redisKey)) {

            response.put("status", "error");

            response.put("message", "Invalid or expired QR code.");

            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }

        // Update QR code status information
        Map<String, Object> qrCodeInfo = redisUtil.get(redisKey);

        qrCodeInfo.put("status", QRCODE_SCANNED);

        qrCodeInfo.put("userId", userId);

        qrCodeInfo.put("scannedAt", System.currentTimeMillis());

        // Set the updated status to scanned with an expiration time of 30 seconds
        redisUtil.set(redisKey, qrCodeInfo, 30, TimeUnit.SECONDS);

        response.put("status", "success");
        response.put("message", "QR code scanned successfully.");
        return ResponseEntity.ok(response);
    }

    /**
     * 检查二维码的登录状态。
     *
     * @param qrcode 要检查的二维码字符串
     * @return 包含二维码当前状态的ResponseEntity
     */
    @GetMapping("/qrcode/status")
    @Operation(summary = "检查二维码状态", tags = {"auth"}, description = "请使用此接口检查二维码状态")
    @Parameters({
            @Parameter(name = "qrcode", description = "二维码字符串", required = true, in = ParameterIn.DEFAULT)
    })
    public ResponseEntity<?> checkLoginStatus(@RequestParam("qrcode") String qrcode) {

        String redisKey = QRCODE_PREFIX + qrcode;

        Map<String, Object> qrCodeInfo = redisUtil.get(redisKey);

        if (qrCodeInfo == null) {
            return ResponseEntity.ok(QRCODE_EXPIRED);

        } else if (QRCODE_SCANNED.equals(qrCodeInfo.get("status"))) {
            // 更新状态为已登录，过期时间为20秒
            qrCodeInfo.put("status", "LOGGED_IN");

            // 生成临时密码，让前端使用临时密码登录
            qrCodeInfo.put("password", String.valueOf((int) ((Math.random() * 9 + 1) * 100000)));

            qrCodeInfo.put("loggedInAt", System.currentTimeMillis());

            redisUtil.set(redisKey, qrCodeInfo, 20, TimeUnit.SECONDS);

            return ResponseEntity.ok(qrCodeInfo);
        } else {
            return ResponseEntity.ok(qrCodeInfo);
        }
    }


    /**
     * 获取用户信息。
     *
     * @param userId 用户ID
     * @return 包含用户信息的UserVo对象
     */
    @GetMapping("/info")
    @Operation(summary = "获取用户信息", tags = {"auth"}, description = "请使用此接口获取用户信息")
    @Parameters({
            @Parameter(name = "userId", description = "用户id", required = true, in = ParameterIn.DEFAULT)
    })
    public UserVo info(@RequestParam("userId") String userId) {
        return imUserService.info(userId);
    }


    /**
     * 验证手机号码并发送验证码。
     *
     * @param phone 要验证并发送验证码的手机号码
     * @return 发送结果的字符串响应
     * @throws Exception 如果发送过程中出现错误
     */
    @GetMapping(value = "/sms")
    @Operation(summary = "验证手机号码并发送验证码", tags = {"auth"}, description = "请使用此接口验证手机号码并发送验证码")
    @Parameters({
            @Parameter(name = "phone", description = "用户手机号码", required = true, in = ParameterIn.DEFAULT)
    })
    public Mono<String> sms(@RequestParam("phone") String phone) throws Exception {
        return Mono.just(smsService.sendMessage(phone));
    }

    /**
     * 获取公钥
     *
     * @return
     */
    @GetMapping(value = "/publickey")
    @Cacheable(value = "publicKey")
    @Operation(summary = "获取RSA公钥", tags = {"auth"}, description = "请使用此接口获取RSA公钥")
    public Mono<Map<String, String>> getLoginPublicKey() {
        return Mono.just(Map.of("publicKey", rsaKeyProp.getPublicKeyStr()));
    }

    /**
     * 密码加密
     *
     * @param password 密码
     * @return 加密后的密文
     * @throws Exception
     */
    @PostMapping("/password")
    @Operation(summary = "密码加密", tags = {"auth"}, description = "请使用此接口密码加密")
    @Parameters({
            @Parameter(name = "password", description = "密码原文", required = true, in = ParameterIn.DEFAULT)
    })
    public Mono<String> passwordEncode(@RequestParam("password") String password) throws Exception {
        return Mono.just(RSAUtil.encrypt(password, rsaKeyProp.getPublicKeyStr()));
    }

    /**
     * 密码加密
     *
     * @param password 密码
     * @return 加密后的密文
     * @throws Exception
     */
    @PostMapping("/password2")
    @Operation(summary = "密码加密", tags = {"auth"}, description = "请使用此接口密码加密")
    @Parameters({
            @Parameter(name = "password", description = "密码原文", required = true, in = ParameterIn.DEFAULT)
    })
    public Mono<String> passwordEncode2(@RequestParam("password") String password,@RequestParam("publicKey") String publicKey) throws Exception {
        return Mono.just(RSAUtil.encrypt(password, publicKey));
    }


    /**
     * 用户是否在线
     *
     * @param userId
     * @return
     */
    @GetMapping("/online")
    @Operation(summary = "用户是否在线", tags = {"auth"}, description = "请使用此接口判断用户是否在线")
    @Parameters({
            @Parameter(name = "userId", description = "用户id", required = true, in = ParameterIn.DEFAULT)
    })
    public Mono<Boolean> isOnline(@RequestParam("userId") String userId) {
        return Mono.just(imUserService.isOnline(userId));
    }


    /**
     * 用户退出登录
     *
     * @return
     */
    @GetMapping("/logout")
    @Operation(summary = "退出登录", tags = {"auth"}, description = "请使用此接口退出登录")
    @Parameters({
            @Parameter(name = "userId", description = "用户id", required = true, in = ParameterIn.DEFAULT)
    })
    public Result logout(@RequestParam("userId") String userId) {
        return Result.success();
    }


    public Map<String, String> generateTokenByUsername(Authentication authenticate) {

        // 获取userId
        String userId = authenticate.getPrincipal().toString();

        //生成token
        return Map.of("token", JwtUtil.createToken(userId, 24, DateField.HOUR), "userId", userId);
    }
}
