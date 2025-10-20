package com.xy.database.mapper;


import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xy.domain.po.ImFriendshipPo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ImFriendshipMapper extends BaseMapper<ImFriendshipPo> {

    List<ImFriendshipPo> selectFriendList(@Param("ownerId") String ownerId, @Param("sequence") Long sequence);
}




