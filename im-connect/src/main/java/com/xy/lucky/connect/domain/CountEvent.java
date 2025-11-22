package com.xy.lucky.connect.domain;

import io.netty.channel.Channel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 计数事件传递类
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class CountEvent {

    // 加
    public static final int INCREMENT = 1;

    // 减
    public static final int DECREMENT = 0;

    private Channel ctx;

    /**
     * 1 为加  0 为减
     */
    private Integer flag;

}
