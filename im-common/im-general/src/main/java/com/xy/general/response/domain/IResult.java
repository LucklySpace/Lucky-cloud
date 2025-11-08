package com.xy.general.response.domain;

/**
 * 响应结果接口，定义通用的响应结果规范
 * <p>
 * 该接口定义了响应结果需要实现的基本方法，包括状态码和消息
 * 实现该接口的类可以用于统一API响应格式
 */
public interface IResult {

    /**
     * 获取响应状态码
     * <p>
     * 状态码应该遵循HTTP状态码规范，例如：
     * - 200: 成功
     * - 4xx: 客户端错误
     * - 5xx: 服务器错误
     * - 自定义业务状态码通常使用4位数字表示
     *
     * @return 响应状态码
     */
    Integer getCode();

    /**
     * 获取响应消息
     *
     * 消息可以是：
     * 1. 直接的文本消息
     * 2. 国际化消息键（通过I18nService解析）
     *
     * @return 响应消息
     */
    String getMessage();
}