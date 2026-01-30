package com.xy.lucky.core.enums;

import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 审批状态枚举
 * <p>
 * 用于好友请求、群组邀请等场景的审批状态
 */
@Getter
@NoArgsConstructor
public enum IMApproveStatus {

    PENDING(0, "待处理"),
    APPROVED(1, "已同意"),
    REJECTED(2, "已拒绝");

    private Integer code;
    private String desc;

    IMApproveStatus(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    /**
     * 根据 code 获取枚举
     *
     * @param code 状态码
     * @return 对应枚举，未找到返回 null
     */
    public static IMApproveStatus getByCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (IMApproveStatus status : values()) {
            if (status.code.equals(code)) {
                return status;
            }
        }
        return null;
    }

    /**
     * 判断是否为待处理状态
     */
    public boolean isPending() {
        return this == PENDING;
    }

    /**
     * 判断是否为已同意状态
     */
    public boolean isApproved() {
        return this == APPROVED;
    }

    /**
     * 判断是否为已拒绝状态
     */
    public boolean isRejected() {
        return this == REJECTED;
    }
}

