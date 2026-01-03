package com.xy.lucky.lbs.converter;

import com.xy.lucky.lbs.domain.po.*;
import com.xy.lucky.lbs.domain.vo.RegionVo;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface RegionConverter {

    @Mapping(source = "lat", target = "latitude")
    @Mapping(source = "lng", target = "longitude")
    @Mapping(target = "fullAddress", ignore = true)
    RegionVo toVo(ProvincePo provincePo);

    List<RegionVo> toProvinceVOList(List<ProvincePo> provincePos);

    @Mapping(source = "lat", target = "latitude")
    @Mapping(source = "lng", target = "longitude")
    @Mapping(target = "fullAddress", ignore = true)
    RegionVo toVo(CityPo cityPo);

    List<RegionVo> toCityVOList(List<CityPo> cities);

    @Mapping(source = "lat", target = "latitude")
    @Mapping(source = "lng", target = "longitude")
    @Mapping(target = "fullAddress", ignore = true)
    RegionVo toVo(CountyPo countyPo);

    List<RegionVo> toCountyVOList(List<CountyPo> counties);

    @Mapping(source = "lat", target = "latitude")
    @Mapping(source = "lng", target = "longitude")
    @Mapping(target = "fullAddress", ignore = true)
    RegionVo toVo(TownPo townPo);

    List<RegionVo> toTownVOList(List<TownPo> townPos);

    @Mapping(source = "lat", target = "latitude")
    @Mapping(source = "lng", target = "longitude")
    @Mapping(target = "fullAddress", ignore = true)
    RegionVo toVo(VillagePo villagePo);

    List<RegionVo> toVillageVOList(List<VillagePo> villagePos);
}