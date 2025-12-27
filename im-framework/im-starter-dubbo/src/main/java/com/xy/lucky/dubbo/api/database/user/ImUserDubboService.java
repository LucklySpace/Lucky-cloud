package com.xy.lucky.dubbo.api.database.user;

import com.xy.lucky.domain.po.ImUserPo;

import java.util.List;

/**
 * 用户Dubbo服务接口
 */
public interface ImUserDubboService {

    /**
     * 获取用户列表
     *
     * @return 用户列表
     */
    List<ImUserPo> selectList();

    /**
     * 获取用户信息
     *
     * @param userId 用户id
     * @return 用户信息
     */
    ImUserPo selectOne(String userId);

    /**
     * 创建用户信息
     *
     * @param userDataPo 用户信息
     * @return 是否成功
     */
    Boolean insert(ImUserPo userDataPo);

    /**
     * 批量创建用户信息
     *
     * @param userDataPoList 用户信息列表
     * @return 是否成功
     */
    Boolean batchInsert(List<ImUserPo> userDataPoList);

    /**
     * 更新用户信息
     *
     * @param userDataPo 用户信息
     * @return 是否成功
     */
    Boolean update(ImUserPo userDataPo);

    /**
     * 删除用户信息
     *
     * @param userId 用户id
     * @return 是否成功
     */
    Boolean deleteById(String userId);

    /**
     * 根据手机号获取用户信息
     *
     * @param phoneNumber 手机号
     * @return 用户信息
     */
    ImUserPo selectOneByMobile(String phoneNumber);
}