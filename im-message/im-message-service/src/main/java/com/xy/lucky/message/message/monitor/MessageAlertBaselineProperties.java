package com.xy.lucky.message.message.monitor;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "message.alert.baseline")
public class MessageAlertBaselineProperties {

    private int minConnectionCount = 1;
    private int maxConsumerBacklog = 2000;
    private double minSuccessRate = 0.99;
    private double maxRetryRate = 0.05;
    private double maxP99Ms = 300;
}
