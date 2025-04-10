package com.xy.auth.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xy.auth.domain.dto.ImUserDto;
import org.apache.ibatis.annotations.Mapper;

/**
 * @author dense
 * @description 针对表【im_user】的数据库操作Mapper
 * @createDate 2024-03-17 01:34:00
 * @Entity generator.domain.ImUser
 */
@Mapper
public interface ImUserMapper extends BaseMapper<ImUserDto> {

}




