package com.xy.server.service;

import com.xy.domain.dto.UserDto;
import com.xy.domain.vo.UserVo;
import com.xy.general.response.domain.Result;

import java.util.List;

public interface UserService {

    /**
     * 查询用户列表
     *
     * @param userDto 用户信息
     * @return 用户列表
     */
    List<UserVo> list(UserDto userDto);

    /**
     * 根据ID获取用户信息
     *
     * @param userId 用户ID
     * @return 用户信息
     */
    UserVo one(String userId);

    /**
     * 创建用户
     *
     * @param userDto 用户信息
     * @return 创建的用户信息
     */
    UserVo create(UserDto userDto);

    /**
     * 更新用户信息
     *
     * @param userDto 用户信息
     * @return 更新结果
     */
    Result update(UserDto userDto);

    /**
     * 删除用户
     *
     * @param userId 用户ID
     * @return 删除结果
     */
    Result delete(String userId);
}