package com.xy.server.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xy.server.domain.po.ImGroupPo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * @author dense
 * @description 针对表【im_group】的数据库操作Mapper
 * @createDate 2024-03-17 01:34:00
 * @Entity generator.domain.ImGroup
 */
@Mapper
public interface ImGroupMapper extends BaseMapper<ImGroupPo> {

    List<String> selectNinePeople(@Param("groupId") String groupId);
}




