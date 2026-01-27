package com.xy.lucky.server.domain.mapper;

import com.xy.lucky.domain.po.ImFriendshipRequestPo;
import com.xy.lucky.server.domain.dto.FriendRequestDto;
import com.xy.lucky.server.domain.vo.FriendshipRequestVo;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * 好友请求相关实体映射
 */
@Mapper
public interface FriendRequestBeanMapper {

    FriendRequestBeanMapper INSTANCE = Mappers.getMapper(FriendRequestBeanMapper.class);

    /**
     * ImFriendshipRequestPo -> FriendshipRequestVo
     */
    @Mappings({
            @Mapping(source = "id", target = "id"),
            @Mapping(source = "fromId", target = "fromId"),
            @Mapping(source = "toId", target = "toId"),
            @Mapping(source = "message", target = "message"),
            @Mapping(source = "createTime", target = "createTime"),
            @Mapping(source = "approveStatus", target = "approveStatus")
    })
    FriendshipRequestVo toFriendshipRequestVo(ImFriendshipRequestPo imFriendshipRequestPo);

    /**
     * FriendRequestDto -> ImFriendshipRequestPo
     */
    ImFriendshipRequestPo toImFriendshipRequestPo(FriendRequestDto friendRequestDto);

    /**
     * ImFriendshipRequestPo -> FriendRequestDto
     */
    FriendRequestDto toFriendRequestDto(ImFriendshipRequestPo imFriendshipRequestPo);

    /**
     * List<ImFriendshipRequestPo> -> List<FriendshipRequestVo>
     */
    List<FriendshipRequestVo> toFriendshipRequestVoList(List<ImFriendshipRequestPo> imFriendshipRequestPos);
}