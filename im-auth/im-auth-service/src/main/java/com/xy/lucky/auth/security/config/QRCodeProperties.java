package com.xy.lucky.auth.security.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 二维码配置类
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "security.qrcode")
public class QRCodeProperties {

    /**
     * 字符编码，默认为 UTF-8
     */
    private String charset = "UTF-8";

    /**
     * 错误纠正级别，默认为 H (30% 纠错能力)
     */
    private String errorCorrectionLevel = "H";

    /**
     * 二维码边距，默认为 1
     */
    private Integer margin = 1;

    /**
     * 默认宽度
     */
    private Integer defaultWidth = 300;

    /**
     * 默认高度
     */
    private Integer defaultHeight = 300;

    /**
     * 图片格式，默认为 png
     */
    private String format = "png";
    
    /**
     * 支持的图片格式列表
     */
    private String[] supportedFormats = {"png", "jpg", "jpeg", "gif", "bmp", "svg"};
    
    /**
     * 嵌入图片相关配置
     */
    private Logo logo = new Logo();

    /**
     * Logo图片配置
     */
    @Data
    public static class Logo {
        /**
         * Logo图片路径
         */
        private String path = "icon/logo.png";
        
        /**
         * Logo宽度
         */
        private Integer width = 80;
        
        /**
         * Logo高度
         */
        private Integer height = 80;
        
        /**
         * Logo透明度 (0.0 - 1.0)
         */
        private Float opacity = 1.0f;

    }
}