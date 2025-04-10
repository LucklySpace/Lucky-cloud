package com.xy.auth.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.xy.auth.domain.dto.ImUserDto;
import com.xy.auth.domain.vo.UserVo;


/**
 * @author dense
 * @description 针对表【im_user】的数据库操作Service
 * @createDate 2024-03-17 01:34:00
 */
public interface ImUserService extends IService<ImUserDto> {

    UserVo info(String userId);

    boolean isOnline(String userId);
}
