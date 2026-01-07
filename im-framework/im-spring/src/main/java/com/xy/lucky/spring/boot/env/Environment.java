package com.xy.lucky.spring.boot.env;

/**
 * Environment 接口 - 代表应用运行时的环境配置
 * <p>
 * 设计参考 Spring Boot 的 Environment，提供：
 * - 多 Profile 支持
 * - 属性获取
 * - 占位符解析
 */
public interface Environment extends PropertyResolver {

    /**
     * 获取当前激活的 profiles
     *
     * @return 激活的 profile 数组
     */
    String[] getActiveProfiles();

    /**
     * 获取默认的 profiles（当没有激活任何 profile 时使用）
     *
     * @return 默认 profile 数组
     */
    String[] getDefaultProfiles();

    /**
     * 判断指定的 profile 是否被接受（激活或默认）
     *
     * @param profile profile 名称
     * @return 是否被接受
     */
    boolean acceptsProfiles(String... profile);
}

