package com.xy.database.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.xy.domain.dto.LoginDto;
import com.xy.domain.po.ImUserPo;
import com.xy.domain.vo.LoginVo;
import com.xy.domain.vo.UserVo;

import java.util.List;

/**
 * @author dense
 * @description 针对表【im_user】的数据库操作Service
 * @createDate 2024-03-17 01:34:00
 */
public interface ImUserService extends IService<ImUserPo> {

    /**
     * 插入用户信息
     * @param userPo 用户信息
     * @return 是否成功
     */
    boolean insert(ImUserPo userPo);

    /**
     * 批量插入用户信息
     * @param userPoList 用户信息列表
     * @return 是否成功
     */
    boolean batchInsert(List<ImUserPo> userPoList);

    /**
     * 查询单条用户信息
     * @param userId 用户ID
     * @return 用户信息
     */
    ImUserPo selectOne(String userId);
    
    /**
     * 根据ID查询用户信息
     * @param userId 用户ID
     * @return 用户信息
     */
    ImUserPo selectById(String userId);
    
    /**
     * 统计用户数量
     * @return 用户数量
     */
    long count();
    
    /**
     * 查询用户列表
     * @return 用户列表
     */
    List<ImUserPo> selectList();
    
    /**
     * 更新用户信息
     * @param userPo 用户信息
     * @return 是否成功
     */
    boolean update(ImUserPo userPo);
    
    /**
     * 根据ID删除用户
     * @param userId 用户ID
     * @return 是否成功
     */
    boolean deleteById(String userId);

    LoginVo login(LoginDto loginDto);

    UserVo info(String userId);

    LoginVo refreshToken(String token);

}