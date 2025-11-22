package com.xy.lucky.database.mapper;


import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xy.lucky.domain.po.ImGroupMessagePo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * @author dense
 * @description 针对表【im_group_message】的数据库操作Mapper
 */
@Mapper
public interface ImGroupMessageMapper extends BaseMapper<ImGroupMessagePo> {

    List<ImGroupMessagePo> selectGroupMessageByGroupId(@Param("userId") String userId, @Param("groupId") String groupId, @Param("sequence") Long sequence);

    List<ImGroupMessagePo> selectGroupMessage(@Param("userId") String userId, @Param("sequence") Long sequence);

    ImGroupMessagePo selectLastGroupMessage(@Param("userId") String userId, @Param("groupId") String groupId);

    Integer selectReadStatus(@Param("groupId") String groupId, @Param("toId") String toId, @Param("status") Integer code);

}




