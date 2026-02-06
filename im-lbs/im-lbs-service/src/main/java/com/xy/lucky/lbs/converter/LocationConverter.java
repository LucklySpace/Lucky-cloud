package com.xy.lucky.lbs.converter;

import com.xy.lucky.lbs.domain.dto.LocationUpdateDto;
import com.xy.lucky.lbs.domain.po.LocationHistoryPo;
import com.xy.lucky.lbs.domain.vo.LocationVo;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface LocationConverter {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createTime", ignore = true)
    @Mapping(target = "geohash", ignore = true)
    @Mapping(target = "userId", ignore = true)
    LocationHistoryPo toPo(LocationUpdateDto dto);

    @Mapping(target = "distance", ignore = true)
    LocationVo toVo(LocationHistoryPo entity);
}
