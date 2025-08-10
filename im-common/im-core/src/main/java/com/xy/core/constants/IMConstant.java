package com.xy.core.constants;


/**
 * IM 业务全局常量
 */
public final class IMConstant {

    private IMConstant() {
        // 私有构造，防止实例化
    }

    // ------------------------------------------------------------------------
    // Token 相关
    // ------------------------------------------------------------------------

    /** HTTP 请求头中的授权字段 */
    public static final String AUTH_TOKEN_HEADER      = "Authorization";

    /** 访问令牌参数名 */
    public static final String ACCESS_TOKEN_PARAM     = "accessToken";

    /** 刷新令牌参数名 */
    public static final String REFRESH_TOKEN_PARAM    = "refreshToken";

    /** Bearer 前缀 */
    public static final String BEARER_PREFIX          = "Bearer ";

    // ------------------------------------------------------------------------
    // 用户缓存、消息队列相关
    // ------------------------------------------------------------------------

    /** Redis 中存储在线用户信息的 key 前缀 */
    public static final String USER_CACHE_PREFIX      = "IM-USER-";

    /** RabbitMQ 交换机名称 */
    public static final String MQ_EXCHANGE_NAME       = "IM-SERVER";

    /** RabbitMQ 路由键前缀 */
    public static final String MQ_ROUTERKEY_PREFIX    = "IM-ROUTER-";

    /** Feign 内部调用标识 */
    public static final String INTERNAL_CALL_FLAG     = "Internal calls";

    // ------------------------------------------------------------------------
    // 登录
    // ------------------------------------------------------------------------

    /** 登录手机号前缀 */
    public static final String SMS_KEY_PREFIX = "IM-LOGIN-SMS-";

    /** 表单登录 authType 值 */
    public static final String AUTH_TYPE_FORM         = "form";

    /** 短信登录 authType 值 */
    public static final String AUTH_TYPE_SMS          = "sms";

    /** 二维码登录 authType 值 */
    public static final String AUTH_TYPE_QR           = "scan";

    // ------------------------------------------------------------------------
    // 二维码登录状态
    // ------------------------------------------------------------------------

    /** Redis key 前缀：登录二维码 */
    public static final String QRCODE_KEY_PREFIX      = "IM-LOGIN-QRCODE-";

    /** 二维码状态：待扫描 */
    public static final String QRCODE_PENDING         = "PENDING";

    /** 二维码状态：已扫描待授权 */
    public static final String QRCODE_SCANNED         = "SCANNED";

    /** 二维码状态：已授权 */
    public static final String QRCODE_AUTHORIZED      = "AUTHORIZED";

    /** 二维码状态：已过期 */
    public static final String QRCODE_EXPIRED         = "EXPIRED";

    /** 二维码状态：无效 */
    public static final String QRCODE_INVALID         = "INVALID";

    /** 二维码状态：发生错误 */
    public static final String QRCODE_ERROR           = "ERROR";

    // ------------------------------------------------------------------------
    // 通用响应状态
    // ------------------------------------------------------------------------

    /** 通用成功状态 */
    public static final String STATUS_SUCCESS         = "success";

    /** 通用失败状态 */
    public static final String STATUS_ERROR           = "error";


    // 用户名称
    public static final String IM_USER = "userId";
}


