package com.xy.database.service.impl;

import cn.hutool.core.date.DateField;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xy.database.mapper.ImUserDataMapper;
import com.xy.database.mapper.ImUserMapper;
import com.xy.database.service.ImUserService;
import com.xy.domain.dto.LoginDto;
import com.xy.domain.po.ImUserDataPo;
import com.xy.domain.po.ImUserPo;
import com.xy.domain.vo.LoginVo;
import com.xy.domain.vo.UserVo;
import com.xy.core.utils.JwtUtil;
import jakarta.annotation.Resource;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

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

    @Override
    public LoginVo login(LoginDto loginDto) {

//        QueryWrapper<ImUserPo> query = new QueryWrapper<>();
//        query.eq("user_id", loginDto.getUserId());
//        ImUserPo imUserPo = imUserMapper.selectOne(query);
//
//        if (imUserPo == null || StrUtil.isEmpty(imUserPo.getUserId())) {
//            throw new BusinessException(ResultEnum.USER_EMPTY);
//        }
//
//        if (!loginDto.getPassword().equals(imUserPo.getPassword())) {
//            throw new BusinessException(ResultEnum.PASSWD_ERROR);
//        }
//
//        String token = JwtUtil.createToken(loginDto.getUserId(), 24, DateField.HOUR);
//
//        LoginVo loginVo = new LoginVo().setUserId(loginDto.getUserId()).setToken(token);
//
//        return loginVo;
        return null;
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

}




