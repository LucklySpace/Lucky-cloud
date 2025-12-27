package com.xy.lucky.general.response.domain;

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
     * 状态码，例如 200 表示成功，其他表示错误
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


    /**
     * 全参构造函数
     *
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

    // =================== 成功响应 ===================

    /**
     * 成功无数据
     *
     * @param <T> 数据类型
     * @return Result实例
     */
    public static <T> Result<T> success() {
        return new Result<>(ResultCode.SUCCESS.getCode(), ResultCode.SUCCESS.getMessage(), null);
    }

    /**
     * 成功，带数据
     *
     * @param data 数据
     * @param <T>  数据类型
     * @return Result实例
     */
    public static <T> Result<T> success(T data) {
        return success(ResultCode.SUCCESS.getMessage(), data);
    }

    /**
     * 成功，自定义消息 + 数据
     *
     * @param data    数据
     * @param message 消息
     * @param <T>     数据类型
     * @return Result实例
     */
    public static <T> Result<T> success(String message, T data) {
        return new Result<>(ResultCode.SUCCESS.getCode(), message, data);
    }

    /**
     * 成功，自定义 IResult（用于国际化场景）
     *
     * @param result 结果码
     * @param data   数据
     * @param <T>    数据类型
     * @return Result实例
     */
    public static <T> Result<T> success(IResult result, T data) {
        return new Result<>(result.getCode(), result.getMessage(), data);
    }

    /**
     * 成功，自定义 IResult（用于国际化场景）
     *
     * @param result 结果码
     * @param <T>    数据类型
     * @return Result实例
     */
    public static <T> Result<T> success(IResult result) {
        return new Result<>(result.getCode(), result.getMessage(), null);
    }

    // =================== 失败响应 ===================

    /**
     * 失败，默认错误
     *
     * @return Result实例
     */
    public static Result<?> failed() {
        return failed(ResultCode.FAIL);
    }

    /**
     * 失败，自定义消息
     *
     * @param message 消息
     * @return Result实例
     */
    public static Result<?> failed(String message) {
        return new Result<>(ResultCode.FAIL.getCode(), message, null);
    }

    /**
     * 失败，自定义错误结构（如业务码+国际化key）
     *
     * @param errorResult 错误结果
     * @return Result实例
     */
    public static Result<?> failed(IResult errorResult) {
        return new Result<>(errorResult.getCode(), errorResult.getMessage(), null);
    }

    /**
     * 失败，自定义错误结构 + 数据
     *
     * @param errorResult 错误结果
     * @param data        数据
     * @param <T>         数据类型
     * @return Result实例
     */
    public static <T> Result<T> failed(IResult errorResult, T data) {
        return new Result<>(errorResult.getCode(), errorResult.getMessage(), data);
    }

    /**
     * 失败，自定义状态码 + 消息
     *
     * @param code    状态码
     * @param message 消息
     * @return Result实例
     */
    public static Result<?> failed(Integer code, String message) {
        return new Result<>(code, message, null);
    }

    /**
     * 失败，自定义状态码 + 消息 + 数据
     *
     * @param code    状态码
     * @param message 消息
     * @param data    数据
     * @param <T>     数据类型
     * @return Result实例
     */
    public static <T> Result<T> failed(Integer code, String message, T data) {
        return new Result<>(code, message, data);
    }

    // =================== 实例化辅助 ===================

    /**
     * 自定义响应结构构造器（静态方式）
     *
     * @param code    状态码
     * @param message 消息
     * @param data    数据
     * @param <T>     数据类型
     * @return Result实例
     */
    public static <T> Result<T> instance(Integer code, String message, T data) {
        return new Result<>(code, message, data);
    }

    /**
     * 创建一个只有状态码和消息的Result实例
     *
     * @param code    状态码
     * @param message 消息
     * @param <T>     数据类型
     * @return Result实例
     */
    public static <T> Result<T> of(Integer code, String message) {
        return new Result<>(code, message, null);
    }

    /**
     * 创建一个包含状态码、消息和数据的Result实例
     *
     * @param code    状态码
     * @param message 消息
     * @param data    数据
     * @param <T>     数据类型
     * @return Result实例
     */
    public static <T> Result<T> of(Integer code, String message, T data) {
        return new Result<>(code, message, data);
    }
}