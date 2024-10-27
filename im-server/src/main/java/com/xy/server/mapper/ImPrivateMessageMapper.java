package com.xy.server.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xy.imcore.model.IMSingleMessageDto;
import com.xy.server.model.ImPrivateMessage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * @author dense
 * @description 针对表【im_private_message】的数据库操作Mapper
 * @createDate 2024-03-28 23:00:15
 * @Entity com.xy.server.model.ImPrivateMessage
 */
@Mapper
public interface ImPrivateMessageMapper extends BaseMapper<ImPrivateMessage> {

    List<ImPrivateMessage> selectSingleMessageByToId(@Param("fromId") String fromId, @Param("toId") String toId, @Param("sequence") Long sequence);

    List<ImPrivateMessage> selectSingleMessage(@Param("userId") String userId, @Param("sequence") Long sequence);

    ImPrivateMessage selectLastSingleMessage(@Param("fromId") String fromId, @Param("toId") String toId);

    Integer selectReadStatus(@Param("fromId") String fromId, @Param("toId") String toId, @Param("status") Integer code);
}




