package com.xy.lucky.lbs.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xy.lucky.lbs.domain.po.LocationHistoryPo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface LocationHistoryMapper extends BaseMapper<LocationHistoryPo> {

    @Select("SELECT *, ST_Distance(ST_SetSRID(ST_MakePoint(longitude, latitude), 4326)::geography, " +
            "ST_SetSRID(ST_MakePoint(#{lon}, #{lat}), 4326)::geography) as distance " +
            "FROM t_user_location_history " +
            "WHERE ST_DWithin(ST_SetSRID(ST_MakePoint(longitude, latitude), 4326)::geography, " +
            "ST_SetSRID(ST_MakePoint(#{lon}, #{lat}), 4326)::geography, #{radiusMeters}) " +
            "ORDER BY distance ASC, create_time DESC LIMIT #{limit}")
    List<LocationHistoryPo> searchNearbyHistory(@Param("lon") double lon,
                                                @Param("lat") double lat,
                                                @Param("radiusMeters") double radiusMeters,
                                                @Param("limit") int limit);
}
