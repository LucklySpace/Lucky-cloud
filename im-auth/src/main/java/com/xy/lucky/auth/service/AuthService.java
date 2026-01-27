package com.xy.lucky.auth.service;


import com.xy.lucky.auth.domain.AuthRefreshTokenResult;
import com.xy.lucky.auth.domain.LoginRequest;
import com.xy.lucky.auth.domain.LoginResult;
import com.xy.lucky.auth.domain.QRCodeResult;
import com.xy.lucky.auth.domain.vo.UserVo;
import jakarta.servlet.http.HttpServletRequest;

import java.util.Map;

/**
 * 统一身份认证服务接口
 *
 * @author dense
 */
public interface AuthService {

    /**
     * 执行统一登录（支持表单、短信、扫码等多种方式�?     *
     * @param loginRequest 登录请求参数
     * @param request      原始请求
     * @return 登录结果（包�?Token 和连接端点）
     */
    LoginResult login(LoginRequest loginRequest, HttpServletRequest request);

    /**
     * 获取指定用户的详细信�?     *
     * @param userId 用户唯一标识
     * @return 用户视图对象
     */
    UserVo info(String userId);

    /**
     * 检查用户当前是否在线（是否存在有效的长连接或会话）
     *
     * @param userId 用户唯一标识
     * @return true 表示在线
     */
    Boolean isOnline(String userId);

    /**
     * 使用刷新令牌获取新的访问令牌
     *
     * @param request 包含刷新令牌的请�?     * @return 新的令牌�?     */
    AuthRefreshTokenResult refreshToken(HttpServletRequest request);

    /**
     * 生成一个新的认证二维码
     *
     * @param qrCodeId 二维码标�?     * @return 二维码渲染信�?     */
    QRCodeResult generateQRCode(String qrCodeId);

    /**
     * 手机端扫描二维码并授�?     *
     * @param payload 扫码授权负载（包�?qrCodeId �?userId�?     * @return 处理结果
     */
    QRCodeResult scanQRCode(Map<String, String> payload);

    /**
     * 轮询获取二维码的最新状�?     *
     * @param qrCodeId 二维码标�?     * @return 二维码当前状态（如已授权、已过期等）
     */
    QRCodeResult getQRCodeStatus(String qrCodeId);

    /**
     * 获取用于数据加密�?RSA 公钥
     *
     * @return 公钥字符�?Map
     */
    Map<String, String> getPublicKey();

    /**
     * 发送短信验证码，并进行频率限制
     *
     * @param phone    手机�?     * @param clientIp 客户�?IP
     * @param deviceId 设备 ID
     * @return 响应结果
     */
    Boolean sendSms(String phone, String clientIp, String deviceId);

    /**
     * 退出登录，撤销当前会话的所有令�?     *
     * @param request 当前请求
     * @return true 表示退出成�?     */
    Boolean logout(HttpServletRequest request);
}
