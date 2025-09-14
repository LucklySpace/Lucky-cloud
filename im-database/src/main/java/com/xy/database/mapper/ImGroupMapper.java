package com.xy.database.mapper;


import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xy.domain.po.ImGroupPo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * @author dense
 * @description 针对表【im_group】的数据库操作Mapper
 */
@Mapper
public interface ImGroupMapper extends BaseMapper<ImGroupPo> {

    List<String> selectNinePeople(@Param("groupId") String groupId);

    List<ImGroupPo> selectGroupsByUserId(@Param("userId") String userId);
}




