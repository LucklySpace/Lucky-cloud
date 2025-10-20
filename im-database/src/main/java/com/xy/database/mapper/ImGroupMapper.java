package com.xy.database.mapper;


import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xy.domain.po.ImGroupPo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ImGroupMapper extends BaseMapper<ImGroupPo> {

    List<ImGroupPo> selectGroupsByUserId(@Param("userId") String userId);
}




