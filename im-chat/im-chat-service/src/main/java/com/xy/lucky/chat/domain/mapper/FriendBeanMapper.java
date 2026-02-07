package com.xy.lucky.chat.domain.mapper;

import com.xy.lucky.domain.po.ImFriendshipPo;
import com.xy.lucky.chat.domain.dto.FriendDto;
import com.xy.lucky.chat.domain.vo.FriendVo;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * 好友相关实体映射
 */
@Mapper
public interface FriendBeanMapper {

    FriendBeanMapper INSTANCE = Mappers.getMapper(FriendBeanMapper.class);

    /**
     * ImFriendshipPo -> FriendVo
     */
    @Mappings({
            @Mapping(source = "ownerId", target = "userId"),
            @Mapping(source = "toId", target = "friendId")
    })
    FriendVo toFriendVo(ImFriendshipPo imFriendshipPo);

    /**
     * FriendDto -> ImFriendshipPo
     */
    @Mappings({
            @Mapping(source = "fromId", target = "ownerId")
    })
    ImFriendshipPo toImFriendshipPo(FriendDto friendDto);

    /**
     * ImFriendshipPo -> FriendDto
     */
    @Mappings({
            @Mapping(source = "ownerId", target = "fromId")
    })
    FriendDto toFriendDto(ImFriendshipPo imFriendshipPo);

    /**
     * List<ImFriendshipPo> -> List<FriendVo>
     */
    List<FriendVo> toFriendVoList(List<ImFriendshipPo> imFriendshipPos);
}