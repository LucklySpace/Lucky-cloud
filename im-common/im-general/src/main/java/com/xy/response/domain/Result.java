package com.xy.response.domain;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * 通用响应包装类，用于统一 API 接口返回格式。
 *
 * @param <T> 响应数据类型
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL) // 排除 null 字段
public class Result<T> {

    /**
     * 状态码，例如 0 表示成功，其他表示错误
     */
    private Integer code;

    /**
     * 提示信息（支持国际化 key，也可以是明文）
     */
    private String message;

    /**
     * UTC 时间戳，记录响应生成时间（自动赋值）
     */
    @Builder.Default
    private Long timestamp = Instant.now().toEpochMilli();

    /**
     * 响应数据
     */
    private T data;

    // =================== 成功响应 ===================

    /**
     * @param code    状态码
     * @param message 消息
     * @param data    数据
     */
    public Result(Integer code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
        // 默认时间
        this.timestamp = Instant.now().toEpochMilli();
    }

    /**
     * 成功无数据
     */
    public static <T> Result<T> success() {
        return success(null);
    }

    /**
     * 成功，带数据
     */
    public static <T> Result<T> success(T data) {
        return success(ResultCode.SUCCESS.getMessage(), data);
    }

    /**
     * 成功，自定义消息 + 数据
     */
    public static <T> Result<T> success(String message, T data) {
        return new Result<>(ResultCode.SUCCESS.getCode(), message, data);
    }

    // =================== 失败响应 ===================

    /**
     * 成功，自定义 IResult（用于国际化场景）
     */
    public static <T> Result<T> success(IResult result, T data) {
        return new Result<>(result.getCode(), result.getMessage(), data);
    }

    /**
     * 失败，默认错误
     */
    public static Result<?> failed() {
        return failed(ResultCode.FAIL);
    }

    /**
     * 失败，自定义消息
     */
    public static Result<?> failed(String message) {
        return new Result<>(ResultCode.FAIL.getCode(), message, null);
    }

    /**
     * 失败，自定义错误结构（如业务码+国际化key）
     */
    public static Result<?> failed(IResult errorResult) {
        return new Result<>(errorResult.getCode(), errorResult.getMessage(), null);
    }

    /**
     * 失败，自定义错误结构 + 数据
     */
    public static <T> Result<T> failed(IResult errorResult, T data) {
        return new Result<>(errorResult.getCode(), errorResult.getMessage(), data);
    }

    /**
     * 失败，自定义状态码 + 消息
     */
    public static Result<?> failed(Integer code, String message) {
        return new Result<>(code, message, null);
    }

    // =================== 实例化辅助 ===================

    /**
     * 失败，自定义状态码 + 消息 + 数据
     */
    public static <T> Result<T> failed(Integer code, String message, T data) {
        return new Result<>(code, message, data);
    }

    /**
     * 自定义响应结构构造器（静态方式）
     */
    public static <T> Result<T> instance(Integer code, String message, T data) {
        return new Result<>(code, message, data);
    }
}