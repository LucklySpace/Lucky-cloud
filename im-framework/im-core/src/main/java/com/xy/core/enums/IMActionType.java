package com.xy.core.enums;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public enum IMActionType {


    RECALL(1, "撤回"),
    EDIT(2, "编辑"),
    DELETE(3, "删除"),
    REPLY(4, "回复"),
    FORWARD(5, "转发"),
    AT(6, "@"),
    MUTE(7, "禁言"),
    UNMUTE(8, "取消禁言"),
    KICK(9, "踢出群"),
    INVITE(10, "邀请"),
    SET_GROUP_OWNER(11, "设置群主"),
    SET_GROUP_JOIN_STATUS(12, "设置群组加入状态"),
    SET_GROUP_JOIN_APPROVE_STATUS(13, "设置群组加入审批状态"),
    SET_GROUP_TYPE(14, "设置群组类型"),


    ;
    private Integer code;
    private String desc;

    IMActionType(Integer status, String desc) {
        this.desc = desc;
        this.code = status;
    }


    public static IMActionType getByCode(Integer code) {
        for (IMActionType v : values()) {
            if (v.code.equals(code)) {
                return v;
            }
        }
        return null;
    }


}
