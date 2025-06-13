package com.xy.auth.service.impl;


import com.xy.auth.api.database.user.ImUserFeign;
import com.xy.auth.security.RSAKeyProperties;
import com.xy.auth.security.exception.AuthenticationFailException;
import com.xy.auth.service.ImUserService;
import com.xy.auth.utils.RSAUtil;
import com.xy.auth.utils.RedisUtil;
import com.xy.domain.po.ImUserDataPo;
import com.xy.domain.po.ImUserPo;
import com.xy.domain.vo.UserVo;
import com.xy.response.domain.ResultCode;
import jakarta.annotation.Resource;
import org.springframework.beans.BeanUtils;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import static com.xy.auth.constant.QrcodeConstant.QRCODE_PREFIX;


/**
 * @author dense
 */
@Service
public class ImUserServiceImpl implements ImUserService {

    public static final String IMUSERPREFIX = "IM-USER-";

    @Resource
    private ImUserFeign imUserFeign;

    @Resource
    private RedisUtil redisUtil;

    @Resource
    private RSAKeyProperties rsaKeyProperties;

    @Override
    public UserVo info(String userId) {

        ImUserDataPo imUserDataPo = imUserFeign.getOneUserData(userId);

        UserVo userVo = new UserVo();

        if (imUserDataPo != null) {
            BeanUtils.copyProperties(imUserDataPo, userVo);
        }

        return userVo;
    }

    @Override
    public boolean isOnline(String userId) {
        LinkedHashMap str = redisUtil.get(IMUSERPREFIX + userId);
        return Objects.nonNull(str);
    }


    /**
     * 校验 用户名密码
     *
     * @param userId   用户账号
     * @param password 密码
     * @return 用户信息
     * @throws UsernameNotFoundException 用户未找到
     * @throws BadCredentialsException   密码验证失败
     */
    public ImUserPo verifyUserByUsername(String userId, String password) throws AuthenticationFailException {


        ImUserPo user = imUserFeign.getOneUser(userId);

        if (Objects.isNull(user)) {
            // 账户未找到
            throw new AuthenticationFailException(ResultCode.ACCOUNT_NOT_FOUND);
        }

        String decryptedPassword = this.decryptPassword(password);

        // 使用更轻量级的密码比对方式,BCryptPasswordEncoder的matches方法
        BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

        if (!passwordEncoder.matches(decryptedPassword, user.getPassword())) {
            // 用户名或密码错误
            throw new AuthenticationFailException(ResultCode.INVALID_CREDENTIALS);
        }

        return user;
    }

    /**
     * 校验二维码 及 临时密码
     *
     * @param qrcode
     * @param password
     * @return
     */
    public ImUserPo verifyQrPassword(String qrcode, String password) {

        String redisKey = QRCODE_PREFIX + qrcode;

        if (!redisUtil.hasKey(QRCODE_PREFIX + qrcode)) {
            throw new AuthenticationFailException(ResultCode.QRCODE_IS_INVALID);
        }

        Map<String, Object> qrCodeInfo = redisUtil.get(redisKey);

        if (!password.equals(qrCodeInfo.get("password"))) {
            throw new AuthenticationFailException(ResultCode.QRCODE_IS_INVALID);
        }

        ImUserPo user = imUserFeign.getOneUser(qrCodeInfo.get("userId").toString());

        if (Objects.isNull(user)) {
            // 账户未找到
            throw new AuthenticationFailException(ResultCode.ACCOUNT_NOT_FOUND);
        }
        // 设置二维码授权
        //redisUtil.set(QRCODE_PREFIX + qrcode, QRCODE_AUTHORIZED, 15, TimeUnit.SECONDS);
        return user;
    }

    /**
     * 手机验证码验证
     *
     * @param phoneNumber 手机号码
     * @param mobileCode  验证码
     * @return 用户信息
     * @throws UsernameNotFoundException   用户未找到
     * @throws AuthenticationFailException 验证码错误
     */
    public ImUserPo verifyMobileCode(String phoneNumber, String mobileCode) throws UsernameNotFoundException, AuthenticationFailException {

        // 获取手机验证码缓存
        String redisCacheSmsCode = redisUtil.get("sms" + phoneNumber);

        String decryptSmsCode = decryptPassword(mobileCode);

        // 如果短信验证码不一致，则抛出异常
        if (!decryptSmsCode.equals(redisCacheSmsCode)) {
            // 验证码错误
            throw new AuthenticationFailException(ResultCode.CAPTCHA_ERROR);
        } else {
            //验证完成删除手机验证码
            redisUtil.del("sms" + phoneNumber);
        }

        ImUserPo user = imUserFeign.getOneByMobile(phoneNumber);
        // 判断用户是否为空,抛异常
        if (Objects.isNull(user)) {
            // 账户未找到
            throw new AuthenticationFailException(ResultCode.ACCOUNT_NOT_FOUND);
        }

        return user;
    }


    /**
     * rsa 解密
     *
     * @param password
     * @return
     */
    public String decryptPassword(String password) {
        try {
            // base64编码时使用加号，在URL传递时加号会被当成空格让base64字符串更改，服务器端解码出错
            String str = password.replaceAll(" ", "+");

            return RSAUtil.decrypt(str, rsaKeyProperties.getPrivateKeyStr());

        } catch (Exception e) {

            throw new AuthenticationFailException(ResultCode.INVALID_CREDENTIALS);
        }
    }


}




