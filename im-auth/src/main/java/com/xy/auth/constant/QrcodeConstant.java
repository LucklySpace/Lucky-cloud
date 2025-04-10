package com.xy.auth.constant;

public class QrcodeConstant {

    // 登录二维码前缀
    public static final String QRCODE_PREFIX = "IM-LOGIN-QRCODE-";

    // 待扫描
    public static final String QRCODE_PENDING = "PENDING";
    // 已扫描待授权
    public static final String QRCODE_SCANNED = "SCANNED";
    // 已过期
    public static final String QRCODE_EXPIRED = "EXPIRED";
    // 无效二维码
    public static final String QRCODE_INVALID = "INVALID";
    // 已授权
    public static final String QRCODE_AUTHORIZED = "AUTHORIZED";
    // 错误状态
    public static final String QRCODE_ERROR = "ERROR";

}