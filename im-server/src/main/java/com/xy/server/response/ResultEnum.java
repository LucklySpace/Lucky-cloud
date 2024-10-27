package com.xy.server.response;

/**
 * 常用结果的枚举
 */
public enum ResultEnum implements IResult {
    ERROR(100, "失败"),
    SUCCESS(200, "成功"),
    VALIDATE_FAILED(400, "参数错误"),
    COMMON_FAILED(500, "系统错误"),
    FORBIDDEN(2004, "没有权限访问资源"),

    PASSWD_ERROR(1000, "用户名或密码错误"),

    USER_EMPTY(1000, "用户不存在");

    private Integer code;
    private String message;

    //省略get、set方法和构造方法

    ResultEnum(Integer code, String message) {
        this.code = code;
        this.message = message;
    }

    @Override
    public Integer getCode() {
        return code;
    }

    public void setCode(Integer code) {
        this.code = code;
    }

    @Override
    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

}

