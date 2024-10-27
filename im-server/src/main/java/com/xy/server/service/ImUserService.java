package com.xy.server.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.xy.imcore.model.IMRegisterUserDto;
import com.xy.server.domain.dto.LoginDto;
import com.xy.server.domain.vo.LoginVo;
import com.xy.server.domain.vo.UserVo;
import com.xy.server.model.ImUser;

/**
 * @author dense
 * @description 针对表【im_user】的数据库操作Service
 * @createDate 2024-03-17 01:34:00
 */
public interface ImUserService extends IService<ImUser> {

    LoginVo login(LoginDto loginDto);

    UserVo info(String user_id);

    LoginVo refreshToken(String token);

    void register(IMRegisterUserDto IMRegisterUserDto);
}
