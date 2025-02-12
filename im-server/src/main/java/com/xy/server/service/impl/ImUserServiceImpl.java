package com.xy.server.service.impl;

import cn.hutool.core.date.DateField;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xy.imcore.model.IMRegisterUserDto;
import com.xy.imcore.utils.JwtUtil;
import com.xy.server.domain.dto.LoginDto;
import com.xy.server.domain.po.ImUserDataPo;
import com.xy.server.domain.po.ImUserPo;
import com.xy.server.domain.vo.LoginVo;
import com.xy.server.domain.vo.UserVo;
import com.xy.server.exception.BusinessException;
import com.xy.server.mapper.ImUserDataMapper;
import com.xy.server.mapper.ImUserMapper;
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
public class ImUserServiceImpl extends ServiceImpl<ImUserMapper, ImUserPo>
        implements ImUserService {

    @Resource
    private ImUserMapper imUserMapper;

    @Resource
    private ImUserDataMapper imUserDataMapper;

    @Resource
    private RedisUtil redisUtil;

    @Override
    public LoginVo login(LoginDto loginDto) {

        QueryWrapper<ImUserPo> query = new QueryWrapper<>();
        query.eq("user_id", loginDto.getUserId());
        ImUserPo imUserPo = imUserMapper.selectOne(query);

        if (imUserPo == null || StrUtil.isEmpty(imUserPo.getUserId())) {
            throw new BusinessException(ResultEnum.USER_EMPTY);
        }

        if (!loginDto.getPassword().equals(imUserPo.getPassword())) {
            throw new BusinessException(ResultEnum.PASSWD_ERROR);
        }

        String token = JwtUtil.createToken(loginDto.getUserId(), 24, DateField.HOUR);

        LoginVo loginVo = new LoginVo().setUserId(loginDto.getUserId()).setToken(token);

        return loginVo;
    }

    @Override
    public UserVo info(String userId) {

        QueryWrapper<ImUserDataPo> query = new QueryWrapper<>();

        query.eq("user_id", userId);

        ImUserDataPo imUserDataPo = imUserDataMapper.selectOne(query);

        UserVo userVo = new UserVo();

        if (imUserDataPo != null) {
            BeanUtils.copyProperties(imUserDataPo, userVo);
        }

        return userVo;
    }

    @Override
    public LoginVo refreshToken(String token) {
        String username = JwtUtil.getUsername(token);
        String refreshToken = JwtUtil.createToken(username, 24, DateField.HOUR);
        LoginVo loginVo = new LoginVo().setUserId(username).setToken(refreshToken);
        return loginVo;
    }

    @Override
    public void register(IMRegisterUserDto IMRegisterUserDto) {
        redisUtil.set(IMUSERPREFIX + IMRegisterUserDto.getUserId(), IMRegisterUserDto);
    }
}




