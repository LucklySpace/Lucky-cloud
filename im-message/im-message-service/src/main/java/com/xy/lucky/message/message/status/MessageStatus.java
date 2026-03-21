package com.xy.lucky.message.message.status;

import lombok.Data;
import lombok.Getter;

@Getter
public enum MessageStatus {

    PENDING(1,"待投递"),

    DELIVERED(2,"已投递"),

    FAILED(3,"投递失败");

    public int code;
    public String desc;

    MessageStatus(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}
