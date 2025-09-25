package com.xy.core.enums;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public enum IMessageReadStatus {

    UNREAD(0, "未读"),
    ALREADY_READ(1, "已读"),
    RECALL(2, "已撤回");

    private Integer code;

    private String desc;

    IMessageReadStatus(Integer index, String desc) {
        this.code = index;
        this.desc = desc;
    }

    public static IMessageReadStatus fromCode(Integer code) {
        for (IMessageReadStatus typeEnum : values()) {
            if (typeEnum.code.equals(code)) {
                return typeEnum;
            }
        }
        return null;
    }

}
