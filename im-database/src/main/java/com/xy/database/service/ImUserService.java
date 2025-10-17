package com.xy.database.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.xy.domain.dto.LoginDto;
import com.xy.domain.po.ImUserPo;
import com.xy.domain.vo.LoginVo;
import com.xy.domain.vo.UserVo;


/**
 * @author dense
 * @description 针对表【im_user】的数据库操作Service
 * @createDate 2024-03-17 01:34:00
 */
public interface ImUserService extends IService<ImUserPo> {

    LoginVo login(LoginDto loginDto);

    UserVo info(String userId);

    LoginVo refreshToken(String token);

}
