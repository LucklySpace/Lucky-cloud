package com.xy.server.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xy.server.model.ImFriendship;
import org.apache.ibatis.annotations.Mapper;

/**
 * @author dense
 * @description 针对表【im_friendship】的数据库操作Mapper
 * @createDate 2024-03-17 01:33:59
 * @Entity generator.domain.ImFriendship
 */
@Mapper
public interface ImFriendshipMapper extends BaseMapper<ImFriendship> {

    // List<FriendVo> selectFriendList(@Param("userId") String userId);
}




