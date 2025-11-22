package com.xy.lucky.database.mapper;


import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xy.lucky.domain.po.ImGroupPo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ImGroupMapper extends BaseMapper<ImGroupPo> {

    List<ImGroupPo> selectGroupsByUserId(@Param("userId") String userId);
}




