package com.xy.lucky.server.service;

import com.xy.lucky.domain.dto.UserDto;
import com.xy.lucky.domain.vo.UserVo;
import com.xy.lucky.general.response.domain.Result;

import java.util.List;

public interface UserService {


    List<UserVo> list(UserDto userDto);

    UserVo one(String userId);

    UserVo create(UserDto userDto);

    Result update(UserDto userDto);

    Result delete(String userId);
}