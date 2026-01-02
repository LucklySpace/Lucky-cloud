package com.xy.lucky.quartz.domain.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

@Getter
@AllArgsConstructor
public enum ScheduleType implements BaseEnum {
    CRON(0, "Cron表达式"),
    FIXED_RATE(1, "固定频率"),
    FIXED_DELAY(2, "固定延迟");

    @JsonValue
    private final Integer code;
    private final String desc;

    @JsonCreator
    public static ScheduleType of(Integer code) {
        return Arrays.stream(values())
                .filter(e -> e.code.equals(code))
                .findFirst()
                .orElse(null);
    }
}
