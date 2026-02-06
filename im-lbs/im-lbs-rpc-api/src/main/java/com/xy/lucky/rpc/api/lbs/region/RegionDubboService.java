package com.xy.lucky.rpc.api.lbs.region;

import com.xy.lucky.rpc.api.lbs.vo.AddressVo;
import com.xy.lucky.rpc.api.lbs.vo.RegionVo;

import java.util.List;

/**
 * 行政区划服务Dubbo接口
 */
public interface RegionDubboService {
    List<RegionVo> getProvinces();

    List<RegionVo> getCities(Long provinceCode);

    List<RegionVo> getCounties(Long cityCode);

    List<RegionVo> getTowns(Long countyCode);

    List<RegionVo> getVillages(Long townCode);

    RegionVo findNearestCounty(Double lat, Double lng);

    List<RegionVo> searchRegions(String keyword);

    AddressVo reverseGeocoding(Double lat, Double lng);
}
