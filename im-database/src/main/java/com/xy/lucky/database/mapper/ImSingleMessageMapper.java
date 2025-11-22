package com.xy.lucky.database.mapper;


import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xy.lucky.domain.po.ImSingleMessagePo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * @author dense
 * @description 针对表【im_single_message】的数据库操作Mapper
 */
@Mapper
public interface ImSingleMessageMapper extends BaseMapper<ImSingleMessagePo> {

    List<ImSingleMessagePo> selectSingleMessageByToId(@Param("fromId") String fromId, @Param("toId") String toId, @Param("sequence") Long sequence);

    List<ImSingleMessagePo> selectSingleMessage(@Param("userId") String userId, @Param("sequence") Long sequence);

    ImSingleMessagePo selectLastSingleMessage(@Param("fromId") String fromId, @Param("toId") String toId);

    Integer selectReadStatus(@Param("fromId") String fromId, @Param("toId") String toId, @Param("status") Integer code);
}




