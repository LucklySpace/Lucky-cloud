package com.xy.lucky.rpc.api.quartz.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.io.Serializable;
import java.util.Arrays;

/**
 * 并发策略枚举
 *
 * @author Lucky
 */
@Getter
@AllArgsConstructor
@Schema(description = "并发策略")
public enum ConcurrencyStrategy implements BaseEnum, Serializable {

    @Schema(description = "串行")
    SERIAL(0, "串行"),

    @Schema(description = "并行")
    PARALLEL(1, "并行");

    @JsonValue
    private final Integer code;
    private final String desc;

    @JsonCreator
    public static ConcurrencyStrategy of(Integer code) {
        return Arrays.stream(values())
                .filter(e -> e.code.equals(code))
                .findFirst()
                .orElse(null);
    }
}
