package com.xy.lucky.quartz.domain.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

@Getter
@AllArgsConstructor
public enum TaskStatus implements BaseEnum {
    STOPPED(0, "停止"),
    RUNNING(1, "运行中"),
    PAUSED(2, "暂停");

    @JsonValue
    private final Integer code;
    private final String desc;

    @JsonCreator
    public static TaskStatus of(Integer code) {
        return Arrays.stream(values())
                .filter(e -> e.code.equals(code))
                .findFirst()
                .orElse(null);
    }
}
