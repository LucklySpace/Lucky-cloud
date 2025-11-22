package com.xy.lucky.core.enums;

import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 群组加入状态
 * 0禁止申请，1需要审批，2允许自由加入
 */
@Getter
@NoArgsConstructor
public enum ImGroupJoinStatus {

    BAN(0, "禁止申请"),
    APPROVE(1, "需要审批"),
    FREE(2, "允许自由加入");

    private Integer code;
    private String desc;

    ImGroupJoinStatus(Integer index, String desc) {
        this.code = index;
        this.desc = desc;
    }

    public static ImGroupJoinStatus getByCode(Integer code) {
        for (ImGroupJoinStatus v : values()) {
            if (v.code.equals(code)) {
                return v;
            }
        }
        return null;
    }


}
