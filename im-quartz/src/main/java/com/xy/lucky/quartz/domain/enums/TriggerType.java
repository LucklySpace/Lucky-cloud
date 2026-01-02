package com.xy.lucky.quartz.domain.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

@Getter
@AllArgsConstructor
public enum TriggerType implements BaseEnum {
    LOCAL(0, "本地"),
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
