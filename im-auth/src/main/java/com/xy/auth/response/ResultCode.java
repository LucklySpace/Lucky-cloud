package com.xy.auth.response;


/**
 * http状态码枚举类
 */
public enum ResultCode {

    /* 默认 状态*/
    SUCCESS(200, "Success", "成功"),
    FAIL(-1, "Err", "失败"),


    DATABASE_OPERATION_FAILED(504, "数据库操作失败"),
    CONTINUE(100, "Continue", "请继续发送请求的剩余部分"),
    SWITCHING_PROTOCOLS(101, "Switching Protocols", "协议切换"),
    PROCESSING(102, "Processing", "请求将继续执行"),
    CHECKPOINT(103, "Checkpoint", "可以预加载"),
    OK(200, "OK", "请求已经成功处理"),
    CREATED(201, "Created", "请求已经成功处理，并创建了资源"),
    ACCEPTED(202, "Accepted", "请求已经接受，等待执行"),
    NON_AUTHORITATIVE_INFORMATION(203, "Non-Authoritative Information", "请求已经成功处理，但是信息不是原始的"),
    NO_CONTENT(204, "No Content", "请求已经成功处理，没有内容需要返回"),
    RESET_CONTENT(205, "Reset Content", "请求已经成功处理，请重置视图"),
    PARTIAL_CONTENT(206, "Partial Content", "部分Get请求已经成功处理"),
    MULTI_STATUS(207, "Multi-Status", "请求已经成功处理，将返回XML消息体"),
    ALREADY_REPORTED(208, "Already Reported", "请求已经成功处理，一个DAV的绑定成员被前一个请求枚举，并且没有被再一次包括"),
    IM_USED(226, "IM Used", "请求已经成功处理，将响应一个或者多个实例"),
    MULTIPLE_CHOICES(300, "Multiple Choices", "提供可供选择的回馈"),
    MOVED_PERMANENTLY(301, "Moved Permanently", "请求的资源已经永久转移"),
    FOUND(302, "Found", "请重新发送请求"),
    SEE_OTHER(303, "See Other", "请以Get方式请求另一个URI"),
    NOT_MODIFIED(304, "Not Modified", "资源未改变"),
    USE_PROXY(305, "Use Proxy", "请通过Location域中的代理进行访问"),
    TEMPORARY_REDIRECT(307, "Temporary Redirect", "请求的资源临时从不同的URI响应请求"),
    RESUME_INCOMPLETE(308, "Resume Incomplete", "请求的资源已经永久转移"),
    BAD_REQUEST(400, "Bad Request", "请求错误，请修正请求"),
    UNAUTHORIZED(401, "Unauthorized", "没有被授权或者授权已经失效"),
    PAYMENT_REQUIRED(402, "Payment Required", "预留状态"),
    FORBIDDEN(403, "Forbidden", "请求被理解，但是拒绝执行"),
    NOT_FOUND(404, "Not Found", "资源未找到"),
    METHOD_NOT_ALLOWED(405, "Method Not Allowed", "请求方法不允许被执行"),
    NOT_ACCEPTABLE(406, "Not Acceptable", "请求的资源不满足请求者要求"),
    PROXY_AUTHENTICATION_REQUIRED(407, "Proxy Authentication Required", "请通过代理进行身份验证"),
    REQUEST_TIMEOUT(408, "Request Timeout", "请求超时"),
    CONFLICT(409, "Conflict", "请求冲突"),
    GONE(410, "Gone", "请求的资源不可用"),
    LENGTH_REQUIRED(411, "Length Required", "Content-Length未定义"),
    PRECONDITION_FAILED(412, "Precondition Failed", "不满足请求的先决条件"),
    REQUEST_ENTITY_TOO_LARGE(413, "Request Entity Too Large", "请求发送的实体太大"),
    REQUEST_URI_TOO_LONG(414, "Request-URI Too Long", "请求的URI超长"),
    UNSUPPORTED_MEDIA_TYPE(415, "Unsupported Media Type", "请求发送的实体类型不受支持"),
    REQUESTED_RANGE_NOT_SATISFIABLE(416, "Requested range not satisfiable", "Range指定的范围与当前资源可用范围不一致"),
    EXPECTATION_FAILED(417, "Expectation Failed", "请求头Expect中指定的预期内容无法被服务器满足"),
    UNPROCESSABLE_ENTITY(422, "Unprocessable Entity", "请求格式正确，但是由于含有语义错误，无法响应"),
    LOCKED(423, "Locked", "当前资源被锁定"),
    FAILED_DEPENDENCY(424, "Failed Dependency", "由于之前的请求发生错误，导致当前请求失败"),
    UPGRADE_REQUIRED(426, "Upgrade Required", "客户端需要切换到TLS1.0"),
    PRECONDITION_REQUIRED(428, "Precondition Required", "请求需要提供前置条件"),
    TOO_MANY_REQUESTS(429, "Too Many Requests", "请求过多"),
    REQUEST_HEADER_FIELDS_TOO_LARGE(431, "Request Header Fields Too Large", "请求头超大，拒绝请求"),
    INTERNAL_SERVER_ERROR(500, "Internal Server Error", "服务器内部错误"),
    NOT_IMPLEMENTED(501, "Not Implemented", "服务器不支持当前请求的部分功能"),
    BAD_GATEWAY(502, "Bad Gateway", "响应无效"),
    SERVICE_UNAVAILABLE(503, "Service Unavailable", "服务器维护或者过载，拒绝服务"),
    GATEWAY_TIMEOUT(504, "Gateway Timeout", "上游服务器超时"),
    HTTP_VERSION_NOT_SUPPORTED(505, "HTTP Version not supported", "不支持的HTTP版本"),
    VARIANT_ALSO_NEGOTIATES(506, "Variant Also Negotiates", "服务器内部配置错误"),
    INSUFFICIENT_STORAGE(507, "Insufficient Storage", "服务器无法完成存储请求所需的内容"),
    LOOP_DETECTED(508, "Loop Detected", "服务器处理请求时发现死循环"),
    BANDWIDTH_LIMIT_EXCEEDED(509, "Bandwidth Limit Exceeded", "服务器达到带宽限制"),
    NOT_EXTENDED(510, "Not Extended", "获取资源所需的策略没有被满足"),
    NETWORK_AUTHENTICATION_REQUIRED(511, "Network Authentication Required", "需要进行网络授权"),


    /* 用户异常 */
    //提供的凭据（用户名、密码等）无效
    INVALID_CREDENTIALS(1001, "Invalid Credentials", "账号或密码错误"),
    //：账户被禁用，无法进行登录。
    ACCOUNT_DISABLED(1002, "Account Disabled", "账户已禁用"),
    //账户已被锁定，无法进行登录。
    ACCOUNT_LOCKED(1003, "Account Locked", "账户已锁定"),
    // 账户已过期，无法进行登录。
    ACCOUNT_EXPIRED(1004, "Account Expired", "账户已过期"),
    //提供的凭据已过期，无法进行登录。
    CREDENTIALS_EXPIRED(1005, "Credentials Expired", "凭据已过期"),
    //身份验证失败，无法进行登录
    AUTHENTICATION_FAILED(1006, "Authentication Failed", "身份验证失败"),
    //验证码错误，无法进行登录
    CAPTCHA_ERROR(1007, "Captcha Error", "验证码错误"),
    //Token为空，无法进行验证
    TOKEN_IS_NULL(1008, "Token is null", "Token为空"),
    //Token无效，
    TOKEN_IS_INVALID(1009, "Token is invalid", "Token无效"),

    EXCESSIVE_LOGIN_FAILURES(1010, "Excessive Login Failures", "登录失败次数过多"),

    ACCOUNT_NOT_FOUND(1011, "Account Not Found", "账户未找到"),

    SMS_ERROR(1012, "SMS Error", "短信发送失败，请稍后再试"),

    ACCOUNT_ALREADY_EXIST(1013, "Account already exist", "账号已存在"),

    QRCODE_IS_INVALID(1014, "QRCode is invalid", "二维码已失效"),

    /* 业务错误 */
    NO_PERMISSION(2001, "no permission", "没有权限");;


    private int code;
    private String en;
    private String zh;

    ResultCode(int code, String en, String zh) {
        this.code = code;
        this.en = en;
        this.zh = zh;
    }

    ResultCode(int code, String en) {
        this.code = code;
        this.en = en;
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getEn() {
        return en;
    }

    public void setEn(String en) {
        this.en = en;
    }

    public String getZh() {
        return zh;
    }

    public void setZh(String zh) {
        this.zh = zh;
    }
}
