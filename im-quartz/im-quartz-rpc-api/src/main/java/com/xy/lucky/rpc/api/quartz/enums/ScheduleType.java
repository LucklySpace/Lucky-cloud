package com.xy.lucky.rpc.api.quartz.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.io.Serializable;
import java.util.Arrays;

/**
 * 调度类型枚举
 *
 * @author Lucky
 */
@Getter
@AllArgsConstructor
@Schema(description = "调度类型")
public enum ScheduleType implements BaseEnum, Serializable {

    @Schema(description = "Cron表达式")
    CRON(0, "Cron表达式"),

    @Schema(description = "固定频率")
    FIXED_RATE(1, "固定频率"),

    @Schema(description = "固定延迟")
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
