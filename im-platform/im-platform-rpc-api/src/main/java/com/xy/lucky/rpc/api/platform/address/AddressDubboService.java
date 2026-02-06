package com.xy.lucky.rpc.api.platform.address;

import com.xy.lucky.rpc.api.platform.vo.AreaVo;

/**
 * 地址服务 Dubbo 接口
 *
 * @author Lucky Platform
 * @since 1.0.0
 */
public interface AddressDubboService {

    /**
     * 根据 IP 查询地区信息
     *
     * @param ip IPv4 地址
     * @return 地区信息
     */
    AreaVo getAreaByIp(String ip);

    /**
     * 根据 IP 查询地区编号
     *
     * @param ip IPv4 地址
     * @return 地区编号
     */
    Integer getAreaIdByIp(String ip);

    /**
     * 根据地区编号查询节点
     *
     * @param id 地区编号
     * @return 地区信息
     */
    AreaVo getAreaById(Integer id);

    /**
     * 解析区域路径
     *
     * @param path 路径字符串（如：河南省/郑州市/金水区）
     * @return 地区信息
     */
    AreaVo parseArea(String path);

    /**
     * 格式化地区编号为路径字符串
     *
     * @param id 地区编号
     * @return 格式化路径（如：上海/上海市/静安区）
     */
    String formatArea(Integer id);
}
