package com.xy.lucky.rpc.api.quartz.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.io.Serializable;
import java.util.Arrays;

/**
 * 触发类型枚举
 *
 * @author Lucky
 */
@Getter
@AllArgsConstructor
@Schema(description = "触发类型")
public enum TriggerType implements BaseEnum, Serializable {

    @Schema(description = "本地")
    LOCAL(0, "本地"),

    @Schema(description = "远程")
    REMOTE(1, "远程");

    @JsonValue
    private final Integer code;
    private final String desc;

    @JsonCreator
    public static TriggerType of(Integer code) {
        return Arrays.stream(values())
                .filter(e -> e.code.equals(code))
                .findFirst()
                .orElse(null);
    }
}
