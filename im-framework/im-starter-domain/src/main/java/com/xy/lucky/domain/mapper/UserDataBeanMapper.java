package com.xy.lucky.domain.mapper;

import com.xy.lucky.domain.dto.UserDto;
import com.xy.lucky.domain.po.ImUserDataPo;
import com.xy.lucky.domain.vo.FriendVo;
import com.xy.lucky.domain.vo.UserVo;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * 用户数据相关实体映射
 */
@Mapper
public interface UserDataBeanMapper {

    UserDataBeanMapper INSTANCE = Mappers.getMapper(UserDataBeanMapper.class);

    /**
     * ImUserDataPo -> UserVo
     */
    UserVo toUserVo(ImUserDataPo imUserDataPo);

    /**
     * UserVo -> ImUserDataPo
     */
    ImUserDataPo toImUserDataPo(UserVo userVo);

    /**
     * UserDto -> ImUserDataPo
     */
    ImUserDataPo toImUserDataPo(UserDto userDto);

    /**
     * ImUserDataPo -> UserDto
     */
    UserDto toUserDto(ImUserDataPo imUserDataPo);

    /**
     * UserDto -> UserVo
     */
    UserVo toUserVo(UserDto userDto);

    /**
     * UserVo -> UserDto
     */
    UserDto toUserDto(UserVo userVo);

    /**
     * ImUserDataPo -> FriendVo
     */
    FriendVo toFriendVo(ImUserDataPo imUserDataPo);

    /**
     * List<ImUserDataPo> -> List<UserVo>
     */
    List<UserVo> toUserVoList(List<ImUserDataPo> imUserDataPos);


}