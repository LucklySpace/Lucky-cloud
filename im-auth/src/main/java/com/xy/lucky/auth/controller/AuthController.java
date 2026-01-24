package com.xy.lucky.auth.controller;


import com.xy.lucky.auth.domain.AuthRefreshTokenResult;
import com.xy.lucky.auth.domain.LoginRequest;
import com.xy.lucky.auth.domain.LoginResult;
import com.xy.lucky.auth.domain.QRCodeResult;
import com.xy.lucky.auth.security.config.RSAKeyProperties;
import com.xy.lucky.auth.service.AuthService;
import com.xy.lucky.auth.utils.RequestContextUtil;
import com.xy.lucky.domain.vo.UserVo;
import com.xy.lucky.security.util.RSAUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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
@RequiredArgsConstructor
@RequestMapping({"/api/auth", "/api/{version}/auth"})
@Tag(name = "Auth", description = "统一认证接口")
public class AuthController {

    private final AuthService authService;
    private final RSAKeyProperties rsaKeyProperties;

    @PostMapping("/login")
    @Operation(summary = "统一登录", description = "支持表单(form)、短信(sms)、扫码(scan)等多种认证方式")
    public LoginResult login(@RequestBody LoginRequest loginRequest, HttpServletRequest request) {
        return authService.login(loginRequest, request);
    }

    @GetMapping("/qrcode")
    @Operation(summary = "获取认证二维码", description = "生成一个新的二维码凭证，返回渲染所需的 Base64 数据")
    public QRCodeResult generateQRCode(@Parameter(description = "二维码唯一标识", required = true)
                                       @RequestParam("qrCode") String qrCodeId) {
        return authService.generateQRCode(qrCodeId);
    }

    @PostMapping("/qrcode/scan")
    @Operation(summary = "二维码扫码授权", description = "移动端扫描并确认授权，绑定用户 ID 与二维码凭证")
    public QRCodeResult scanQRCode(@RequestBody @Parameter(description = "授权信息", required = true)
                                   Map<String, String> payload) {
        return authService.scanQRCode(payload);
    }

    @GetMapping("/qrcode/status")
    @Operation(summary = "轮询二维码状态", description = "桌面端/Web端轮询二维码状态，若已授权则返回临时登录凭证")
    public QRCodeResult getQRCodeStatus(@Parameter(description = "二维码唯一标识", required = true)
                                        @RequestParam("qrCode") String qrCodeId) {
        return authService.getQRCodeStatus(qrCodeId);
    }

    @GetMapping("/info")
    @Operation(summary = "获取当前用户信息", description = "根据用户 ID 查询详细资料")
    public UserVo info(@Parameter(description = "用户 ID", required = true)
                       @RequestParam("userId") String userId) {
        return authService.info(userId);
    }

    @GetMapping("/sms")
    @Operation(summary = "发送登录验证码", description = "校验手机号合法性并发送 6 位数字验证码")
    public Boolean sms(@Parameter(description = "手机号码", required = true)
                       @RequestParam("phone") String phone, HttpServletRequest request) {
        String clientIp = RequestContextUtil.resolveClientIp(request);
        String deviceId = RequestContextUtil.resolveDeviceId(request, clientIp);
        return authService.sendSms(phone, clientIp, deviceId);
    }

    @GetMapping("/publickey")
    @Operation(summary = "获取 RSA 公钥", description = "用于前端对敏感数据（如密码）进行非对称加密")
    public Map<String, String> getPublicKey() {
        return authService.getPublicKey();
    }

    @GetMapping("/refresh/token")
    @Operation(summary = "刷新令牌", description = "使用有效的 refreshToken 换取新的令牌对")
    public AuthRefreshTokenResult refreshToken(HttpServletRequest request) {
        return authService.refreshToken(request);
    }

    @GetMapping("/online")
    @Operation(summary = "在线状态检查", description = "判断指定用户当前是否处于在线状态")
    public Boolean isOnline(@Parameter(description = "用户 ID", required = true)
                            @RequestParam("userId") String userId) {
        return authService.isOnline(userId);
    }

    @GetMapping("/logout")
    @Operation(summary = "退出登录", description = "撤销当前会话的所有 Token 并清理相关缓存")
    public Boolean logout(HttpServletRequest request) {
        return authService.logout(request);
    }

    @PostMapping("/password")
    @Operation(summary = "密码测试加密", description = "（仅供调试）使用服务端公钥对密码原文进行加密")
    public String passwordEncode(@Parameter(description = "密码原文", required = true)
                                 @RequestParam("password") String password) throws Exception {
        return RSAUtil.encrypt(password, rsaKeyProperties.getPublicKeyStr());
    }
}
