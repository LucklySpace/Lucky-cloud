package com.xy.lucky.quartz.mapper;

import com.xy.lucky.quartz.domain.po.TaskInfoPo;
import com.xy.lucky.quartz.domain.po.TaskLogPo;
import com.xy.lucky.quartz.domain.vo.TaskInfoVo;
import com.xy.lucky.quartz.domain.vo.TaskLogVo;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.List;

@Mapper(componentModel = "spring")
public interface TaskMapper {

    TaskInfoVo toVo(TaskInfoPo taskInfoPo);

    TaskInfoPo toEntity(TaskInfoVo taskInfoVo);

    List<TaskInfoVo> toTaskInfoVoList(List<TaskInfoPo> taskInfoPoList);

    TaskLogVo toVo(TaskLogPo taskLogPo);

    List<TaskLogVo> toTaskLogVoList(List<TaskLogPo> taskLogPoList);
}
