package com.xy.lucky.core.enums;

import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public enum IMuteScope {
    GLOBAL(1, "全局"),
    PRIVATE(2, "私聊"),
    GROUP_SINGLE(3, "群聊"),
    GROUP_ALL(4, "群聊全部");

    private static final Map<Integer, IMuteScope> CODE_MAP =
            Stream.of(IMuteScope.values()).collect(Collectors.toMap(IMuteScope::getCode, e -> e));

    private final Integer code;
    private final String desc;

    IMuteScope(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static IMuteScope fromCode(Integer code) {
        return CODE_MAP.get(code);
    }

    public Integer getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }
}
