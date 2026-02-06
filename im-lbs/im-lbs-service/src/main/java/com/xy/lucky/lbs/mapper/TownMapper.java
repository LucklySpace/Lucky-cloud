package com.xy.lucky.lbs.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xy.lucky.lbs.domain.po.TownPo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface TownMapper extends BaseMapper<TownPo> {
    @Select("SELECT * FROM im_lbs_china_town ORDER BY " +
            "ST_Distance(ST_SetSRID(ST_MakePoint(lng, lat), 4326)::geography, " +
            "ST_SetSRID(ST_MakePoint(#{lng}, #{lat}), 4326)::geography) ASC LIMIT 1")
    TownPo findNearest(@Param("lng") Double lng, @Param("lat") Double lat);
}
