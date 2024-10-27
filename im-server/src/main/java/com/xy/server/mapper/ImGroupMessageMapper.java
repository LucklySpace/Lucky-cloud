package com.xy.server.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xy.imcore.model.IMGroupMessageDto;
import com.xy.server.model.ImGroupMessage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * @author dense
 * @description 针对表【im_group_message】的数据库操作Mapper
 * @createDate 2024-03-28 23:00:15
 * @Entity com.xy.server.model.ImGroupMessage
 */
@Mapper
public interface ImGroupMessageMapper extends BaseMapper<ImGroupMessage> {

    List<ImGroupMessage> selectGroupMessageByGroupId(@Param("userId") String userId, @Param("groupId") String groupId, @Param("sequence") Long sequence);

    List<ImGroupMessage> selectGroupMessage(@Param("userId") String userId, @Param("sequence") Long sequence);

    ImGroupMessage selectLastGroupMessage(@Param("userId") String userId, @Param("groupId") String groupId);

}




