package com.xy.lucky.lbs.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xy.lucky.lbs.converter.RegionConverter;
import com.xy.lucky.lbs.domain.po.*;
import com.xy.lucky.lbs.domain.vo.AddressVo;
import com.xy.lucky.lbs.domain.vo.RegionVo;
import com.xy.lucky.lbs.mapper.*;
import com.xy.lucky.lbs.service.RegionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 行政区划服务实现类
 * 负责处理行政区划的查询、检索以及逆地理编码等业务逻辑
 *
 * @author lucky
 * @since 1.0.0
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class RegionServiceImpl implements RegionService {

    private final ProvinceMapper provinceMapper;
    private final CityMapper cityMapper;
    private final CountyMapper countyMapper;
    private final TownMapper townMapper;
    private final VillageMapper villageMapper;
    private final RegionConverter regionConverter;

    /**
     * 获取所有省份列表
     * 使用本地缓存，减少数据库访问
     *
     * @return 省份列表
     */
    @Override
    @Cacheable(value = "provinces", unless = "#result == null || #result.isEmpty()")
    public List<RegionVo> getProvinces() {
        log.info("查询所有省份数据");
        return regionConverter.toProvinceVOList(provinceMapper.selectList(null));
    }

    /**
     * 根据省份代码获取城市列表
     * 使用本地缓存
     *
     * @param provinceCode 省份代码
     * @return 城市列表
     */
    @Override
    @Cacheable(value = "cities", key = "#provinceCode", unless = "#result == null || #result.isEmpty()")
    public List<RegionVo> getCities(Long provinceCode) {
        return regionConverter.toCityVOList(
                cityMapper.selectList(new LambdaQueryWrapper<CityPo>().eq(CityPo::getProvinceCode, provinceCode))
        );
    }

    /**
     * 根据城市代码获取区县列表
     * 使用本地缓存
     *
     * @param cityCode 城市代码
     * @return 区县列表
     */
    @Override
    @Cacheable(value = "counties", key = "#cityCode", unless = "#result == null || #result.isEmpty()")
    public List<RegionVo> getCounties(Long cityCode) {
        return regionConverter.toCountyVOList(
                countyMapper.selectList(new LambdaQueryWrapper<CountyPo>().eq(CountyPo::getCityCode, cityCode))
        );
    }

    /**
     * 根据区县代码获取乡镇列表
     * 使用本地缓存
     *
     * @param countyCode 区县代码
     * @return 乡镇列表
     */
    @Override
    @Cacheable(value = "towns", key = "#countyCode", unless = "#result == null || #result.isEmpty()")
    public List<RegionVo> getTowns(Long countyCode) {
        return regionConverter.toTownVOList(
                townMapper.selectList(new LambdaQueryWrapper<TownPo>().eq(TownPo::getCountyCode, countyCode))
        );
    }

    /**
     * 根据乡镇代码获取村/社区列表
     * 数据量较大，视情况开启缓存，此处暂不缓存或短时缓存
     *
     * @param townCode 乡镇代码
     * @return 村/社区列表
     */
    @Override
    public List<RegionVo> getVillages(Long townCode) {
        return regionConverter.toVillageVOList(
                villageMapper.selectList(new LambdaQueryWrapper<VillagePo>().eq(VillagePo::getTownCode, townCode))
        );
    }

    /**
     * 查找最近的区县
     * 利用 PostGIS 空间查询能力
     *
     * @param lat 纬度
     * @param lng 经度
     * @return 最近的区县信息
     */
    @Override
    public RegionVo findNearestCounty(Double lat, Double lng) {
        CountyPo countyPo = countyMapper.findNearest(lng, lat);
        return countyPo != null ? regionConverter.toVo(countyPo) : null;
    }

    /**
     * 聚合模糊搜索行政区划（省、市、区县、乡镇）
     * 针对不同层级分别查询，限制返回数量以保证性能
     *
     * @param keyword 搜索关键词
     * @return 匹配的行政区划列表
     */
    @Override
    public List<RegionVo> searchRegions(String keyword) {
        List<RegionVo> results = new ArrayList<>();
        if (keyword == null || keyword.trim().isEmpty()) {
            return results;
        }

        // 1. 搜索省份
        List<ProvincePo> provinces = provinceMapper.selectList(
                new LambdaQueryWrapper<ProvincePo>()
                        .like(ProvincePo::getName, keyword)
                        .last("LIMIT 5")
        );
        provinces.forEach(p -> {
            RegionVo vo = regionConverter.toVo(p);
            vo.setLevel(1);
            vo.setFullAddress(p.getName());
            results.add(vo);
        });

        // 2. 搜索城市
        List<CityPo> cities = cityMapper.selectList(
                new LambdaQueryWrapper<CityPo>()
                        .like(CityPo::getName, keyword)
                        .last("LIMIT 10")
        );
        cities.forEach(c -> {
            RegionVo vo = regionConverter.toVo(c);
            vo.setLevel(2);
            results.add(vo);
        });

        // 3. 搜索区县
        List<CountyPo> counties = countyMapper.selectList(
                new LambdaQueryWrapper<CountyPo>()
                        .like(CountyPo::getName, keyword)
                        .last("LIMIT 10")
        );
        counties.forEach(c -> {
            RegionVo vo = regionConverter.toVo(c);
            vo.setLevel(3);
            results.add(vo);
        });

        // 4. 搜索乡镇 (限制数量，避免数据过多)
        List<TownPo> towns = townMapper.selectList(
                new LambdaQueryWrapper<TownPo>()
                        .like(TownPo::getName, keyword)
                        .last("LIMIT 10")
        );
        towns.forEach(t -> {
            RegionVo vo = regionConverter.toVo(t);
            vo.setLevel(4);
            results.add(vo);
        });

        return results;
    }

    /**
     * 逆地理编码：根据经纬度查询完整地址信息
     * 采用逐级查找策略：村 -> 镇 -> 区县
     *
     * @param lat 纬度
     * @param lng 经度
     * @return 完整地址信息
     */
    @Override
    public AddressVo reverseGeocoding(Double lat, Double lng) {
        // 1. 尝试查找最近的村/社区
        // PostGIS 索引支持高效的空间查询
        VillagePo village = villageMapper.findNearest(lng, lat);
        if (village != null) {
            // 这里可以增加距离阈值判断，例如距离超过 5km 则认为不在该村范围内
            // 目前假设最近的点即为所在位置
            return buildAddressFromVillage(village);
        }

        // 2. 如果没找到村（理论上全量数据覆盖下很少见），尝试查找镇
        TownPo town = townMapper.findNearest(lng, lat);
        if (town != null) {
            return buildAddressFromTown(town);
        }

        // 3. 如果没找到镇，尝试查找区县
        CountyPo county = countyMapper.findNearest(lng, lat);
        if (county != null) {
            return buildAddressFromCounty(county);
        }

        return null;
    }

    /**
     * 根据村级信息构建完整地址
     *
     * @param village 村级对象
     * @return 地址视图对象
     */
    private AddressVo buildAddressFromVillage(VillagePo village) {
        // 向上递归查询行政区划树
        TownPo town = townMapper.selectOne(new LambdaQueryWrapper<TownPo>().eq(TownPo::getCode, village.getTownCode()));
        CountyPo county = null;
        CityPo city = null;
        ProvincePo province = null;

        if (town != null) {
            county = countyMapper.selectOne(new LambdaQueryWrapper<CountyPo>().eq(CountyPo::getCode, town.getCountyCode()));
        }
        if (county != null) {
            city = cityMapper.selectOne(new LambdaQueryWrapper<CityPo>().eq(CityPo::getCode, county.getCityCode()));
        }
        if (city != null) {
            province = provinceMapper.selectOne(new LambdaQueryWrapper<ProvincePo>().eq(ProvincePo::getCode, city.getProvinceCode()));
        }

        return AddressVo.builder()
                .province(province != null ? province.getName() : "")
                .city(city != null ? city.getName() : "")
                .district(county != null ? county.getName() : "")
                .town(town != null ? town.getName() : "")
                .village(village.getName())
                .adCode(village.getCode())
                .fullAddress((province != null ? province.getName() : "") +
                        (city != null ? city.getName() : "") +
                        (county != null ? county.getName() : "") +
                        (town != null ? town.getName() : "") +
                        village.getName())
                .build();
    }

    /**
     * 根据乡镇信息构建完整地址
     *
     * @param town 乡镇对象
     * @return 地址视图对象
     */
    private AddressVo buildAddressFromTown(TownPo town) {
        CountyPo county = countyMapper.selectOne(new LambdaQueryWrapper<CountyPo>().eq(CountyPo::getCode, town.getCountyCode()));
        CityPo city = null;
        ProvincePo province = null;

        if (county != null) {
            city = cityMapper.selectOne(new LambdaQueryWrapper<CityPo>().eq(CityPo::getCode, county.getCityCode()));
        }
        if (city != null) {
            province = provinceMapper.selectOne(new LambdaQueryWrapper<ProvincePo>().eq(ProvincePo::getCode, city.getProvinceCode()));
        }

        return AddressVo.builder()
                .province(province != null ? province.getName() : "")
                .city(city != null ? city.getName() : "")
                .district(county != null ? county.getName() : "")
                .town(town.getName())
                .village("")
                .adCode(town.getCode())
                .fullAddress((province != null ? province.getName() : "") +
                        (city != null ? city.getName() : "") +
                        (county != null ? county.getName() : "") +
                        town.getName())
                .build();
    }

    /**
     * 根据区县信息构建完整地址
     *
     * @param county 区县对象
     * @return 地址视图对象
     */
    private AddressVo buildAddressFromCounty(CountyPo county) {
        CityPo city = cityMapper.selectOne(new LambdaQueryWrapper<CityPo>().eq(CityPo::getCode, county.getCityCode()));
        ProvincePo province = null;

        if (city != null) {
            province = provinceMapper.selectOne(new LambdaQueryWrapper<ProvincePo>().eq(ProvincePo::getCode, city.getProvinceCode()));
        }

        return AddressVo.builder()
                .province(province != null ? province.getName() : "")
                .city(city != null ? city.getName() : "")
                .district(county.getName())
                .town("")
                .village("")
                .adCode(county.getCode())
                .fullAddress((province != null ? province.getName() : "") +
                        (city != null ? city.getName() : "") +
                        county.getName())
                .build();
    }

}
