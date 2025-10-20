package com.xy.database.mapper;


import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xy.domain.po.ImGroupMemberPo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * @author dense
 * @description 针对表【im_group_member】的数据库操作Mapper
 */
@Mapper
public interface ImGroupMemberMapper extends BaseMapper<ImGroupMemberPo> {

    List<String> selectNinePeopleAvatar(@Param("groupId") String groupId);
}
