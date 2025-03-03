package com.xy.auth.config;

import cn.hutool.extra.qrcode.QrConfig;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.awt.*;

@Configuration
@ConfigurationProperties(prefix = "qrcode")
public class QRConfig {

    private int width = 300;  // 默认宽度
    private int height = 300; // 默认高度
    private String errorCorrectionLevel = "L"; // 容错级别

    @Bean
    public QrConfig qrConfig() {
        //初始宽度和高度
        QrConfig qrConfig = new QrConfig(width, height);

        //设置边距，即二维码和边框的距离
        qrConfig.setMargin(2);
        //设置二维码的纠错级别
        qrConfig.setErrorCorrection(ErrorCorrectionLevel.valueOf(errorCorrectionLevel));
        //设置前景色
        qrConfig.setForeColor(Color.BLACK);
        //设置背景色
        qrConfig.setBackColor(Color.WHITE);

        return qrConfig;
    }

}