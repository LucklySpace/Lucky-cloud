package com.xy.lucky.lbs.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xy.lucky.lbs.domain.po.VillagePo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface VillageMapper extends BaseMapper<VillagePo> {
    @Select("SELECT * FROM im_lbs_china_village ORDER BY " +
            "ST_Distance(ST_SetSRID(ST_MakePoint(lng, lat), 4326)::geography, " +
            "ST_SetSRID(ST_MakePoint(#{lng}, #{lat}), 4326)::geography) ASC LIMIT 1")
    VillagePo findNearest(@Param("lng") Double lng, @Param("lat") Double lat);
}
