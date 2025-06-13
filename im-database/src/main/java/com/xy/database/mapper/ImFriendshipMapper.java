package com.xy.database.mapper;


import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xy.domain.po.ImFriendshipPo;
import org.apache.ibatis.annotations.Mapper;

/**
 * @author dense
 * @description 针对表【im_friendship】的数据库操作Mapper
 */
@Mapper
public interface ImFriendshipMapper extends BaseMapper<ImFriendshipPo> {

    // List<FriendVo> selectFriendList(@Param("userId") String userId);
}




