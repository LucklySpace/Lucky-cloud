package com.xy.auth.constant;

public class Qrcode {

    public static final String QRCODE_PREFIX = "IM-QRCODE-";

    // 各种状态常量
    public static final String QRCODE_PENDING =  "PENDING"; // 待扫描
    public static final String QRCODE_SCANNED =  "SCANNED"; // 已扫描
    public static final String QRCODE_EXPIRED =  "EXPIRED"; // 已过期
    public static final String QRCODE_INVALID =  "INVALID"; // 无效二维码
    public static final String QRCODE_AUTHORIZED =  "AUTHORIZED"; // 已授权
    public static final String QRCODE_ERROR =  "ERROR"; // 错误状态

}