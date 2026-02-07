package com.xy.lucky.chat.service;

import com.xy.lucky.chat.domain.dto.UserDto;
import com.xy.lucky.chat.domain.vo.UserVo;

import java.util.List;

public interface UserService {


    List<UserVo> list(UserDto userDto);

    UserVo one(String userId);

    UserVo create(UserDto userDto);

    Boolean update(UserDto userDto);

    Boolean delete(String userId);
}
