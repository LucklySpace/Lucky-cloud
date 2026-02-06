package com.xy.lucky.rpc.api.quartz.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.io.Serializable;
import java.util.Arrays;

/**
 * 任务状态枚举
 *
 * @author Lucky
 */
@Getter
@AllArgsConstructor
@Schema(description = "任务状态")
public enum TaskStatus implements BaseEnum, Serializable {

    @Schema(description = "停止")
    STOPPED(0, "停止"),

    @Schema(description = "运行中")
    RUNNING(1, "运行中"),

    @Schema(description = "暂停")
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
