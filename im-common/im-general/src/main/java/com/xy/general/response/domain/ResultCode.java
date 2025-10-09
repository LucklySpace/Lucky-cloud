package com.xy.general.response.domain;


import com.xy.general.response.service.I18nService;
import lombok.Getter;

/**
 * 通用响应状态码枚举，支持国际化
 */
@Getter
public enum ResultCode implements IResult {

    // ========== 通用 ==========
    SUCCESS(200, "result.success"),                // 请求成功
    FAIL(-1, "result.fail"),                       // 请求失败

    // ========== HTTP 状态 ==========
    BAD_REQUEST(400, "http.bad_request"),          // 错误的请求，通常是参数不合法
    UNAUTHORIZED(401, "http.unauthorized"),        // 未授权或Token已过期
    FORBIDDEN(403, "http.forbidden"),              // 没有访问权限
    NOT_FOUND(404, "http.not_found"),              // 请求资源不存在
    INTERNAL_SERVER_ERROR(500, "http.internal_server_error"), // 服务器内部错误
    SERVICE_UNAVAILABLE(503, "http.service_unavailable"),     // 服务不可用，可能是维护中或负载过高

    // ========== 参数相关 ==========

    // ========== 用户相关 ==========
    INVALID_CREDENTIALS(1001, "user.invalid_credentials"), // 用户名或密码错误
    ACCOUNT_DISABLED(1002, "user.account_disabled"),        // 账户已被禁用
    ACCOUNT_LOCKED(1003, "user.account_locked"),            // 账户已被锁定
    ACCOUNT_EXPIRED(1004, "user.account_expired"),          // 账户已过期
    CREDENTIALS_EXPIRED(1005, "user.credentials_expired"),  // 登录凭证已过期
    AUTHENTICATION_FAILED(1006, "user.authentication_failed"), // 身份验证失败
    CAPTCHA_ERROR(1007, "user.captcha_error"),              // 验证码错误
    TOKEN_IS_NULL(1008, "user.token_is_null"),              // Token为空
    TOKEN_IS_INVALID(1009, "user.token_is_invalid"),        // Token无效
    EXCESSIVE_LOGIN_FAILURES(1010, "user.excessive_login_failures"), // 登录失败次数过多
    ACCOUNT_NOT_FOUND(1011, "user.account_not_found"),      // 账户未找到
    SMS_ERROR(1012, "user.sms_error"),                      // 短信发送失败
    ACCOUNT_ALREADY_EXIST(1013, "user.account_already_exist"), // 账户已存在
    QRCODE_IS_INVALID(1014, "user.qrcode_is_invalid"),      // 二维码无效或过期
    UNSUPPORTED_AUTHENTICATION_TYPE(1015, "user.unsupported_authentication_type"), // 不支持的认证方式
    VALIDATION_INCOMPLETE(1016, "user.validation_incomplete"), // 验证信息不完整
    USER_OFFLINE(1017, "user.offline"),                     // 用户当前不在线

    // ========== 权限/业务 ==========
    NO_PERMISSION(2001, "biz.no_permission"),               // 没有权限访问该资源或操作

    // ========== 服务相关 ==========
    SERVICE_EXCEPTION(3000, "service.exception"),           // 服务异常
    REQUEST_DATA_TOO_LARGE(3001, "request.data_too_large"), // 请求数据过大，被拒绝处理
    TOO_MANY_REQUESTS(3002, "request.too_many")  // 请求次数过多

    ;

    // 响应码
    private final int code;

    // 国际化消息键
    private final String messageKey;

    ResultCode(int code, String messageKey) {
        this.code = code;
        this.messageKey = messageKey;
    }

    @Override
    public Integer getCode() {
        return code;
    }

    @Override
    public String getMessage() {
        // 从 Spring 容器中获取 MessageSource，并根据 Locale 获取消息
        return I18nService.getMessage(messageKey);
    }
}
