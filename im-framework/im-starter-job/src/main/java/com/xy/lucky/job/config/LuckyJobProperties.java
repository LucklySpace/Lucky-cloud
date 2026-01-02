package com.xy.lucky.job.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 分布式任务配置属性
 *
 * @author lucky
 */
@Data
@ConfigurationProperties(prefix = "lucky.job")
public class LuckyJobProperties {

    /**
     * 是否启用
     */
    private boolean enabled = true;

    /**
     * 调度中心地址，多个地址用逗号分隔
     * 例如: http://127.0.0.1:8080/im-quartz
     */
    private String adminAddresses;

    /**
     * 执行器应用名称
     */
    private String appName;

    /**
     * 执行器IP，为空自动获取
     */
    private String ip;

    /**
     * 执行器端口，为空自动获取
     */
    private Integer port;

    /**
     * 访问令牌（用于权限校验）
     */
    private String accessToken;

    /**
     * 日志保留天数
     */
    private Integer logRetentionDays = 30;
}
