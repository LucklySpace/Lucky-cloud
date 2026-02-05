package com.xy.lucky.database.web.mapper;


import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xy.lucky.domain.po.ImChatPo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ImChatMapper extends BaseMapper<ImChatPo> {


    List<ImChatPo> getChatList(@Param("ownerId") String ownerId, @Param("sequence") Long sequence);
}




