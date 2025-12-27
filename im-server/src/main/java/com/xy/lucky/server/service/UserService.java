package com.xy.lucky.server.service;

import com.xy.lucky.domain.dto.UserDto;
import com.xy.lucky.domain.vo.UserVo;
import reactor.core.publisher.Mono;

import java.util.List;

public interface UserService {


    Mono<List<UserVo>> list(UserDto userDto);

    Mono<UserVo> one(String userId);

    Mono<UserVo> create(UserDto userDto);

    Mono<Boolean> update(UserDto userDto);

    Mono<Boolean> delete(String userId);
}
