package com.xy.lucky.auth.controller;


import com.xy.lucky.auth.domain.IMLoginRequest;
import com.xy.lucky.auth.domain.IMLoginResult;
import com.xy.lucky.auth.domain.IMQRCodeResult;
import com.xy.lucky.auth.security.config.RSAKeyProperties;
import com.xy.lucky.auth.service.AuthService;
import com.xy.lucky.auth.utils.RequestContextUtil;
import com.xy.lucky.domain.vo.UserVo;
import com.xy.lucky.security.util.RSAUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Map;


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
@RequestMapping({"/api/auth", "/api/{version}/auth"})
@Tag(name = "auth", description = "用户认证")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    private final RSAKeyProperties rsaKeyProperties;

    @PostMapping("/login")
    @Operation(summary = "用户登录", tags = {"auth"}, description = "请使用此接口进行用户登录")
    @Parameters({
            @Parameter(name = "loginRequest", description = "用户登录信息", required = true, in = ParameterIn.DEFAULT)
    })
    public IMLoginResult login(@RequestBody IMLoginRequest imLoginRequest, HttpServletRequest request) {
        return authService.login(imLoginRequest, request);
    }

    @GetMapping("/qrcode")
    @Operation(summary = "生成认证二维码", tags = {"auth"}, description = "请使用此接口生成认证二维码")
    @Parameters({
            @Parameter(name = "qrcode", description = "二维码内容字符串", required = true, in = ParameterIn.DEFAULT)
    })
    public IMQRCodeResult generateQRCode(
            @Parameter(description = "二维码凭证 ID", required = true)
            @RequestParam("qrCode") String qrCodeId) {
        return authService.generateQRCode(qrCodeId);
    }

    @PostMapping("/qrcode/scan")
    @Operation(summary = "处理二维码扫描", tags = {"auth"}, description = "请使用此接口处理二维码扫描")
    @Parameters({
            @Parameter(name = "map", description = "二维码信息", required = true, in = ParameterIn.DEFAULT)
    })
    public IMQRCodeResult scanQRCode(
            @RequestBody
            @Parameter(description = "扫码信息，需包含 qrCodeId 和 userId", required = true)
            Map<String, String> payload) {
        return authService.scanQRCode(payload);
    }

    @GetMapping("/qrcode/status")
    @Operation(summary = "检查二维码状态", tags = {"auth"}, description = "请使用此接口检查二维码状态")
    @Parameters({
            @Parameter(name = "qrcode", description = "二维码字符串", required = true, in = ParameterIn.DEFAULT)
    })
    public IMQRCodeResult getQRCodeStatus(
            @Parameter(description = "二维码凭证 ID", required = true)
            @RequestParam("qrCode") String qrCodeId) {
        return authService.getQRCodeStatus(qrCodeId);
    }

    @GetMapping("/info")
    @Operation(summary = "获取用户信息", tags = {"auth"}, description = "请使用此接口获取用户信息")
    @Parameters({
            @Parameter(name = "userId", description = "用户id", required = true, in = ParameterIn.DEFAULT)
    })
    public UserVo info(@RequestParam("userId") String userId) {
        return authService.info(userId);
    }

    @GetMapping(value = "/sms")
    @Operation(summary = "验证手机号码并发送验证码", tags = {"auth"}, description = "请使用此接口验证手机号码并发送验证码")
    @Parameters({
            @Parameter(name = "phone", description = "用户手机号码", required = true, in = ParameterIn.DEFAULT)
    })
    public String sms(@RequestParam("phone") String phone, HttpServletRequest request) {
        String clientIp = RequestContextUtil.resolveClientIp(request);
        String deviceId = RequestContextUtil.resolveDeviceId(request, clientIp);
        return authService.sendSms(phone, clientIp, deviceId);
    }

    /**
     * 获取公钥
     *
     * @return RSA公钥
     */
    @GetMapping(value = "/publickey")
    //@Cacheable(value = "publicKey")
    @Operation(summary = "获取RSA公钥", tags = {"auth"}, description = "请使用此接口获取RSA公钥")
    public Map<String, String> getPublicKey() {
        return authService.getPublicKey();
    }

    /**
     * token 刷新token
     *
     * @param request 请求
     */
    @Operation(summary = "token 刷新", tags = {"auth"}, description = "请使用此接口验证手机号码并发送验证码")
    @GetMapping(value = "/refresh/token")
    public Map<String, String> refreshToken(HttpServletRequest request) {
        return authService.refreshToken(request);
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
    public Boolean isOnline(@RequestParam("userId") String userId) {
        return authService.isOnline(userId);
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
    public Boolean logout(@RequestParam("userId") String userId) {
        return Boolean.TRUE;
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
    public String passwordEncode(@RequestParam("password") String password) throws Exception {
        return RSAUtil.encrypt(password, rsaKeyProperties.getPublicKeyStr());
    }
//    /**
//     * 密码加密
//     *
//     * @param password 密码
//     * @return 加密后的密文
//     * @throws Exception
//     */
//    @PostMapping("/password2")
//    @Operation(summary = "密码加密", tags = {"auth"}, description = "请使用此接口密码加密")
//    @Parameters({
//            @Parameter(name = "password", description = "密码原文", required = true, in = ParameterIn.DEFAULT)
//    })
//    public Mono<String> passwordEncode2(@RequestParam("password") String password, @RequestParam("publicKey") String publicKey) throws Exception {
//        return Mono.just(RSAUtil.encrypt(password, publicKey));
//    }
}
