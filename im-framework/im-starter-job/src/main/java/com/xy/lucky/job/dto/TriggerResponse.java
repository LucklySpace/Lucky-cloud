package com.xy.lucky.job.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TriggerResponse implements Serializable {
    public static final int SUCCESS_CODE = 200;
    public static final int FAIL_CODE = 500;
    private static final long serialVersionUID = 1L;
    private int code;
    private String msg;
    private Object content;

    public static TriggerResponse success() {
        return new TriggerResponse(SUCCESS_CODE, "success", null);
    }

    public static TriggerResponse success(Object content) {
        return new TriggerResponse(SUCCESS_CODE, "success", content);
    }

    public static TriggerResponse fail(String msg) {
        return new TriggerResponse(FAIL_CODE, msg, null);
    }
}
