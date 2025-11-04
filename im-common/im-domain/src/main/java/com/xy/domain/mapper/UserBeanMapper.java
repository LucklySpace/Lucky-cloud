package com.xy.domain.mapper;

import com.xy.domain.dto.UserDto;
import com.xy.domain.po.ImUserPo;
import com.xy.domain.vo.UserVo;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * 用户相关实体映射
 */
@Mapper
public interface UserBeanMapper {

    UserBeanMapper INSTANCE = Mappers.getMapper(UserBeanMapper.class);

    /**
     * ImUserPo -> UserVo
     */
    @Mappings({
            @Mapping(source = "userName", target = "name")
    })
    UserVo toUserVo(ImUserPo imUserPo);

    /**
     * UserVo -> ImUserPo
     */
    @Mappings({
            @Mapping(source = "name", target = "userName")
    })
    ImUserPo toImUserPo(UserVo userVo);

    /**
     * UserDto -> ImUserPo
     */
    @Mappings({
            @Mapping(source = "name", target = "userName")
    })
    ImUserPo toImUserPo(UserDto userDto);

    /**
     * ImUserPo -> UserDto
     */
    @Mappings({
            @Mapping(source = "userName", target = "name")
    })
    UserDto toUserDto(ImUserPo imUserPo);

    /**
     * UserDto -> UserVo
     */
    UserVo toUserVo(UserDto userDto);

    /**
     * UserVo -> UserDto
     */
    UserDto toUserDto(UserVo userVo);

    /**
     * List<ImUserPo> -> List<UserVo>
     */
    List<UserVo> toUserVoList(List<ImUserPo> imUserPos);
}