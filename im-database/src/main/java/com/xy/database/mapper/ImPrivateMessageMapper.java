package com.xy.database.mapper;


import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xy.domain.po.ImPrivateMessagePo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * @author dense
 * @description 针对表【im_private_message】的数据库操作Mapper
 */
@Mapper
public interface ImPrivateMessageMapper extends BaseMapper<ImPrivateMessagePo> {

    List<ImPrivateMessagePo> selectSingleMessageByToId(@Param("fromId") String fromId, @Param("toId") String toId, @Param("sequence") Long sequence);

    List<ImPrivateMessagePo> selectSingleMessage(@Param("userId") String userId, @Param("sequence") Long sequence);

    ImPrivateMessagePo selectLastSingleMessage(@Param("fromId") String fromId, @Param("toId") String toId);

    Integer selectReadStatus(@Param("fromId") String fromId, @Param("toId") String toId, @Param("status") Integer code);
}




