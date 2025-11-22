package com.xy.lucky.file.util;


import lombok.Data;

@Data
public class ResponseResult<T> {
    private int code;
    private String enMessage;
    private String zhMessage;
    private T data;

    public ResponseResult() {
    }

    public ResponseResult(int code, String enMessage, String zhMessage) {
        this.code = code;
        this.enMessage = enMessage;
        this.zhMessage = zhMessage;
    }


    /**
     * 成功
     */
    public static <T> ResponseResult<T> success() {
        ResponseResult<T> result = new ResponseResult<T>();
        result.setCode(ResultCode.SUCCESS.getCode());
        result.setEnMessage(ResultCode.SUCCESS.getEnMessage());
        result.setZhMessage(ResultCode.SUCCESS.getZhMessage());
        return result;
    }


    /**
     * 成功
     */
    public static <T> ResponseResult success(T data) {
        ResponseResult<T> result = new ResponseResult<T>();
        result.setCode(ResultCode.SUCCESS.getCode());
        result.setEnMessage(ResultCode.SUCCESS.getEnMessage());
        result.setZhMessage(ResultCode.SUCCESS.getZhMessage());
        result.setData(data);
        return result;
    }


    /**
     * 失败
     */
    public static <T> ResponseResult<T> error() {
        ResponseResult<T> result = new ResponseResult<T>();
        result.setCode(ResultCode.FAIL.getCode());
        result.setEnMessage(ResultCode.FAIL.getEnMessage());
        result.setZhMessage(ResultCode.FAIL.getZhMessage());
        return result;
    }

    /**
     * 失败
     */
    public static <T> ResponseResult<T> error(T data) {
        ResponseResult<T> result = new ResponseResult<T>();
        result.setCode(ResultCode.FAIL.getCode());
        result.setEnMessage(ResultCode.FAIL.getEnMessage());
        result.setZhMessage(ResultCode.FAIL.getZhMessage());
        result.setData(data);
        return result;
    }


    /**
     * @param data 数据
     * @param <T>
     * @return
     */
    public static <T> ResponseResult uploading(T data) {
        ResponseResult<T> result = new ResponseResult<T>();
        result.setCode(ResultCode.UPLOADING.getCode());
        result.setEnMessage(ResultCode.UPLOADING.getEnMessage());
        result.setZhMessage(ResultCode.UPLOADING.getZhMessage());
        result.setData(data);
        return result;
    }

    /**
     * 成功
     */
    public static <T> ResponseResult success(int code, String enMessage, String zhMessage) {
        return new ResponseResult(code, enMessage, zhMessage);
    }


    /**
     * 失败
     */
    public static <T> ResponseResult error(int code, String enMessage, String zhMessage) {
        return new ResponseResult(code, enMessage, zhMessage);
    }


    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getEnMessage() {
        return enMessage;
    }

    public void setEnMessage(String enMessage) {
        this.enMessage = enMessage;
    }

    public String getZhMessage() {
        return zhMessage;
    }

    public void setZhMessage(String zhMessage) {
        this.zhMessage = zhMessage;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

//    public static ResponseResult SUCCESS = new ResponseResult<>(200,"成功");
//    public static ResponseResult INTEVER_ERROR = new ResponseResult<>(500,"服务器错误");
//    public static ResponseResult NOT_FOUND = new ResponseResult<>(404,"未找到");

}