package com.xy.lucky.core.enums;

import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 群组类型枚举
 * <p>
 * 用于区分不同类型的群组
 */
@Getter
@NoArgsConstructor
public enum IMGroupType {

    PUBLIC(0, "公开群"),
    PRIVATE(1, "私有群");

    private Integer code;
    private String desc;

    IMGroupType(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    /**
     * 根据 code 获取枚举
     *
     * @param code 类型码
     * @return 对应枚举，未找到返回 null
     */
    public static IMGroupType getByCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (IMGroupType type : values()) {
            if (type.code.equals(code)) {
                return type;
            }
        }
        return null;
    }

    /**
     * 判断是否为公开群
     */
    public boolean isPublic() {
        return this == PUBLIC;
    }

    /**
     * 判断是否为私有群
     */
    public boolean isPrivate() {
        return this == PRIVATE;
    }
}

