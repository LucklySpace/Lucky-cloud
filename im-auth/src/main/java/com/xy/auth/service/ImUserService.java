package com.xy.auth.service;


import com.xy.domain.po.ImUserPo;
import com.xy.domain.vo.UserVo;

/**
 * @author dense
 * @description 针对表【im_user】的数据库操作Service
 * @createDate 2024-03-17 01:34:00
 */
public interface ImUserService {

    UserVo info(String userId);

    boolean isOnline(String userId);

    ImUserPo verifyMobileCode(String phoneNumber, String smsCode);

    ImUserPo verifyUserByUsername(String userId, String password);

    ImUserPo verifyQrPassword(String qrcode, String password);
}
