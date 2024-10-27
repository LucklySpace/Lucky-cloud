package com.xy.server.service.impl;

import cn.hutool.core.date.DateField;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xy.imcore.model.IMRegisterUserDto;
import com.xy.imcore.utils.JwtUtil;
import com.xy.server.domain.dto.LoginDto;
import com.xy.server.domain.vo.LoginVo;
import com.xy.server.domain.vo.UserVo;
import com.xy.server.exception.BusinessException;
import com.xy.server.mapper.ImUserDataMapper;
import com.xy.server.mapper.ImUserMapper;
import com.xy.server.model.ImUser;
import com.xy.server.model.ImUserData;
import com.xy.server.response.ResultEnum;
import com.xy.server.service.ImUserService;
import com.xy.server.utils.RedisUtil;
import jakarta.annotation.Resource;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import static com.xy.imcore.constants.Constant.IMUSERPREFIX;

/**
 * @author dense
 * @description 针对表【im_user】的数据库操作Service实现
 * @createDate 2024-03-17 01:34:00
 */
@Service
public class ImUserServiceImpl extends ServiceImpl<ImUserMapper, ImUser>
        implements ImUserService {

    @Resource
    private ImUserMapper imUserMapper;

    @Resource
    private ImUserDataMapper imUserDataMapper;

    @Resource
    private RedisUtil redisUtil;

    @Override
    public LoginVo login(LoginDto loginDto) {

        QueryWrapper<ImUser> query = new QueryWrapper<>();
        query.eq("user_id", loginDto.getUser_id());
        ImUser imUser = imUserMapper.selectOne(query);

        if (imUser == null || StrUtil.isEmpty(imUser.getUser_id())) {
            throw new BusinessException(ResultEnum.USER_EMPTY);
        }

        if (!loginDto.getPassword().equals(imUser.getPassword())) {
            throw new BusinessException(ResultEnum.PASSWD_ERROR);
        }

        String token = JwtUtil.createToken(loginDto.getUser_id(), 24, DateField.HOUR);

        LoginVo loginVo = new LoginVo().setUser_id(loginDto.getUser_id()).setToken(token);

        return loginVo;
    }

    @Override
    public UserVo info(String user_id) {

        QueryWrapper<ImUserData> query = new QueryWrapper<>();

        query.eq("user_id", user_id);

        ImUserData imUserData = imUserDataMapper.selectOne(query);

        UserVo userVo = new UserVo();

        if (imUserData != null) {
            BeanUtils.copyProperties(imUserData, userVo);
        }

        return userVo;
    }

    @Override
    public LoginVo refreshToken(String token) {
        String username = JwtUtil.getUsername(token);
        String refreshToken = JwtUtil.createToken(username, 24, DateField.HOUR);
        LoginVo loginVo = new LoginVo().setUser_id(username).setToken(refreshToken);
        return loginVo;
    }

    @Override
    public void register(IMRegisterUserDto IMRegisterUserDto) {
        redisUtil.set(IMUSERPREFIX + IMRegisterUserDto.getUser_id(), IMRegisterUserDto);
    }
}




