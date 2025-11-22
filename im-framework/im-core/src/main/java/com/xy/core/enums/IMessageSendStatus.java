package com.xy.core.enums;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public enum IMessageSendStatus {

    SUCCESS(0, "成功"),
    FAILED(1, "失败"),
    SENDING(2, "发送中"),
    OTHER(3, "其它异常");

    private Integer code;

    private String desc;

    IMessageSendStatus(Integer index, String status) {
        this.code = index;
        this.desc = status;
    }

}
