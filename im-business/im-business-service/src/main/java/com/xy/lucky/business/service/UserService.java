package com.xy.lucky.business.service;

import com.xy.lucky.business.domain.dto.UserDto;
import com.xy.lucky.business.domain.vo.UserVo;

import java.util.List;

public interface UserService {


    List<UserVo> list(UserDto userDto);

    UserVo one(String userId);

    UserVo create(UserDto userDto);

    Boolean update(UserDto userDto);

    Boolean delete(String userId);
}
