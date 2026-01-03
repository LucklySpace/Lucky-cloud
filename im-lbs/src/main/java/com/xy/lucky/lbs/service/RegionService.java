package com.xy.lucky.lbs.service;

import com.xy.lucky.lbs.domain.vo.AddressVo;
import com.xy.lucky.lbs.domain.vo.RegionVo;

import java.util.List;

/**
 * 行政区划服务接口
 */
public interface RegionService {

    /**
     * 获取所有省份
     *
     * @return 省份列表
     */
    List<RegionVo> getProvinces();

    /**
     * 根据省份代码获取城市列表
     *
     * @param provinceCode 省份代码
     * @return 城市列表
     */
    List<RegionVo> getCities(Long provinceCode);

    /**
     * 根据城市代码获取区县列表
     *
     * @param cityCode 城市代码
     * @return 区县列表
     */
    List<RegionVo> getCounties(Long cityCode);

    /**
     * 根据区县代码获取乡镇列表
     *
     * @param countyCode 区县代码
     * @return 乡镇列表
     */
    List<RegionVo> getTowns(Long countyCode);

    /**
     * 根据乡镇代码获取村/社区列表
     *
     * @param townCode 乡镇代码
     * @return 村/社区列表
     */
    List<RegionVo> getVillages(Long townCode);

    /**
     * 根据经纬度查找最近的区县
     *
     * @param lat 纬度
     * @param lng 经度
     * @return 最近的区县信息
     */
    RegionVo findNearestCounty(Double lat, Double lng);

    /**
     * 聚合模糊搜索行政区划（省、市、区县、乡镇）
     *
     * @param keyword 搜索关键词
     * @return 匹配的行政区划列表
     */
    List<RegionVo> searchRegions(String keyword);

    /**
     * 逆地理编码：根据经纬度查询完整地址信息
     *
     * @param lat 纬度
     * @param lng 经度
     * @return 完整地址信息
     */
    AddressVo reverseGeocoding(Double lat, Double lng);
}
