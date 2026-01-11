package com.xy.lucky.core.enums;

import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 群成员角色枚举
 */
@Getter
@NoArgsConstructor
public enum IMemberStatus {

    GROUP_OWNER(0, "群主"),
    ADMIN(1, "管理员"),
    NORMAL(2, "普通成员"),
    MUTED(3, "禁言成员"),
    REMOVED(4, "已移除成员");

    private Integer code;
    private String desc;

    IMemberStatus(Integer status, String desc) {
        this.desc = desc;
        this.code = status;
    }

    public static IMemberStatus getByCode(Integer code) {
        for (IMemberStatus v : values()) {
            if (v.code.equals(code)) {
                return v;
            }
        }
        return null;
    }

}
