package com.xy.connect.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 消息事件传递类
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class MessageEvent {

    // 消息内容
    private String body;
}
