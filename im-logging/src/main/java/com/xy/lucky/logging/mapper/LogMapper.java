package com.xy.lucky.logging.mapper;

import com.xy.lucky.logging.domain.po.LogPo;
import com.xy.lucky.logging.domain.vo.LogRecordVo;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface LogMapper {

    @Mapping(target = "tags", ignore = true)
    @Mapping(target = "context", ignore = true)
    LogPo toPo(LogRecordVo record);

    @Mapping(target = "tags", ignore = true)
    @Mapping(target = "context", ignore = true)
    @Mapping(source = "ts", target = "timestamp")
    LogRecordVo toVo(LogPo po);

}
