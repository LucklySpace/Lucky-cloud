package com.xy.server.service;

import com.xy.domain.dto.UserDto;
import com.xy.domain.vo.UserVo;
import com.xy.general.response.domain.Result;

import java.util.List;

public interface UserService {


    List<UserVo> list(UserDto userDto);

    UserVo one(String userId);

    UserVo create(UserDto userDto);

    Result update(UserDto userDto);

    Result delete(String userId);
}