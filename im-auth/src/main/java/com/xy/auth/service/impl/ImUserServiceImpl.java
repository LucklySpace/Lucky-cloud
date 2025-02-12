package com.xy.auth.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xy.auth.domain.vo.UserVo;
import com.xy.auth.entity.ImUser;
import com.xy.auth.entity.ImUserData;
import com.xy.auth.mapper.ImUserDataMapper;
import com.xy.auth.mapper.ImUserMapper;
import com.xy.auth.service.ImUserService;
import com.xy.auth.utils.RedisUtil;
import jakarta.annotation.Resource;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Objects;
import java.util.Optional;


/**
 * @author dense
 * @description 针对表【im_user】的数据库操作Service实现
 * @createDate 2024-03-17 01:34:00
 */
@Service
public class ImUserServiceImpl extends ServiceImpl<ImUserMapper, ImUser>
        implements ImUserService {

    public static final String IMUSERPREFIX = "IM-USER-";

    @Resource
    private ImUserDataMapper imUserDataMapper;

    @Resource
    private RedisUtil redisUtil;

    @Override
    public UserVo info(String userId) {

        QueryWrapper<ImUserData> query = new QueryWrapper<>();

        query.eq("user_id", userId);

        ImUserData imUserData = imUserDataMapper.selectOne(query);

        UserVo userVo = new UserVo();

        if (imUserData != null) {
            BeanUtils.copyProperties(imUserData, userVo);
        }

        return userVo;
    }

    @Override
    public boolean isOnline(String userId) {
        LinkedHashMap str = redisUtil.get(IMUSERPREFIX + userId);
        return Objects.nonNull(str) ;
    }


}




