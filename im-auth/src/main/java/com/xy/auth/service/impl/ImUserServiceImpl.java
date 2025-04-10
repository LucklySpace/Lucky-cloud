package com.xy.auth.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xy.auth.domain.dto.ImUserDataDto;
import com.xy.auth.domain.dto.ImUserDto;
import com.xy.auth.domain.vo.UserVo;
import com.xy.auth.mapper.ImUserDataMapper;
import com.xy.auth.mapper.ImUserMapper;
import com.xy.auth.security.RSAKeyProperties;
import com.xy.auth.security.exception.AuthenticationFailException;
import com.xy.auth.service.ImUserService;
import com.xy.auth.utils.RSAUtil;
import com.xy.auth.utils.RedisUtil;
import jakarta.annotation.Resource;
import org.springframework.beans.BeanUtils;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import static com.xy.auth.constant.QrcodeConstant.QRCODE_PREFIX;
import static com.xy.auth.response.ResultCode.*;


/**
 * @author dense
 */
@Service
public class ImUserServiceImpl extends ServiceImpl<ImUserMapper, ImUserDto>
        implements ImUserService, UserDetailsService {

    public static final String IMUSERPREFIX = "IM-USER-";

    @Resource
    private ImUserDataMapper imUserDataMapper;

    @Resource
    private RedisUtil redisUtil;

    @Resource
    private RSAKeyProperties rsaKeyProperties;

    @Override
    public UserVo info(String userId) {

        QueryWrapper<ImUserDataDto> query = new QueryWrapper<>();

        query.eq("user_id", userId);

        ImUserDataDto imUserDataDto = imUserDataMapper.selectOne(query);

        UserVo userVo = new UserVo();

        if (imUserDataDto != null) {
            BeanUtils.copyProperties(imUserDataDto, userVo);
        }

        return userVo;
    }

    @Override
    public boolean isOnline(String userId) {
        LinkedHashMap str = redisUtil.get(IMUSERPREFIX + userId);
        return Objects.nonNull(str);
    }


    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return null;
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
    public ImUserDto verifyUserByUsername(String userId, String password) throws AuthenticationFailException {

        // 使用select方法只查询需要的字段，避免加载整个实体对象
        QueryWrapper<ImUserDto> wrapper = new QueryWrapper<>();
        // 填充用户名
        wrapper.select().eq("user_id", userId);

        ImUserDto user = this.getOne(wrapper);

        if (Objects.isNull(user)) {
            // 账户未找到
            throw new AuthenticationFailException(ACCOUNT_NOT_FOUND);
        }

        String decryptedPassword = this.decryptPassword(password);

        // 使用更轻量级的密码比对方式,BCryptPasswordEncoder的matches方法
        BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

        if (!passwordEncoder.matches(decryptedPassword, user.getPassword())) {
            // 用户名或密码错误
            throw new AuthenticationFailException(INVALID_CREDENTIALS);
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
    public ImUserDto verifyQrPassword(String qrcode, String password) {

        String redisKey = QRCODE_PREFIX + qrcode;

        if (!redisUtil.hasKey(QRCODE_PREFIX + qrcode)) {
            throw new AuthenticationFailException(QRCODE_IS_INVALID);
        }

        Map<String, Object> qrCodeInfo = redisUtil.get(redisKey);

        if (!password.equals(qrCodeInfo.get("password"))) {
            throw new AuthenticationFailException(QRCODE_IS_INVALID);
        }

        // 使用select方法只查询需要的字段，避免加载整个实体对象
        QueryWrapper<ImUserDto> wrapper = new QueryWrapper<>();
        // 填充用户名
        wrapper.select().eq("user_id", qrCodeInfo.get("userId"));
        ImUserDto user = this.getOne(wrapper);

        if (Objects.isNull(user)) {
            // 账户未找到
            throw new AuthenticationFailException(ACCOUNT_NOT_FOUND);
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
    public ImUserDto verifyMobileCode(String phoneNumber, String mobileCode) throws UsernameNotFoundException, AuthenticationFailException {

        // 获取手机验证码缓存
        String redisCacheSmsCode = redisUtil.get("sms" + phoneNumber);

        String decryptSmsCode = decryptPassword(mobileCode);

        // 如果短信验证码不一致，则抛出异常
        if (!decryptSmsCode.equals(redisCacheSmsCode)) {
            // 验证码错误
            throw new AuthenticationFailException(CAPTCHA_ERROR);
        } else {
            //验证完成删除手机验证码
            redisUtil.del("sms" + phoneNumber);
        }

        // 使用select方法只查询需要的字段，避免加载整个实体对象
        QueryWrapper<ImUserDto> wrapper = new QueryWrapper<>();
        wrapper.select().like("mobile", phoneNumber);
        ImUserDto user = this.getOne(wrapper);

        // 判断用户是否为空,抛异常
        if (Objects.isNull(user)) {
            // 账户未找到
            throw new AuthenticationFailException(ACCOUNT_NOT_FOUND);
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

            throw new AuthenticationFailException(AUTHENTICATION_FAILED);
        }
    }


}




