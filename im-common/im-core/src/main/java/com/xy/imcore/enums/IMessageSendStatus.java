package com.xy.imcore.enums;

public enum IMessageSendStatus {

    SUCCESS(0, "成功"),
    FAILED(1, "失败"),
    SENDING(2, "发送中"),
    OTHER(3, "其它异常");

    private Integer code;

    private String status;

    IMessageSendStatus(Integer index, String status) {
        this.code = index;
        this.status = status;
    }

    public Integer getCode() {
        return code;
    }

    public void setCode(Integer code) {
        this.code = code;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
