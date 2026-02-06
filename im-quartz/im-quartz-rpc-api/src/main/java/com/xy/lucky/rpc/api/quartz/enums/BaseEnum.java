package com.xy.lucky.rpc.api.quartz.enums;

/**
 * 基础枚举接口
 *
 * @author Lucky
 */
public interface BaseEnum {
    /**
     * 获取枚举编码
     *
     * @return 编码
     */
    Integer getCode();

    /**
     * 获取枚举描述
     *
     * @return 描述
     */
    String getDesc();
}
