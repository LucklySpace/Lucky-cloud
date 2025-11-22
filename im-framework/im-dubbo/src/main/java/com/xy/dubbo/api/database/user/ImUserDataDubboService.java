package com.xy.dubbo.api.database.user;

import com.xy.domain.po.ImUserDataPo;

import java.util.List;

/**
 * 用户Dubbo服务接口
 */
public interface ImUserDataDubboService {

    /**
     * 获取用户信息
     *
     * @param userId 用户id
     * @return 用户信息
     */
    ImUserDataPo selectOne(String userId);

    /**
     * 创建用户信息
     *
     * @param userDataPo 用户信息
     * @return 是否成功
     */
    Boolean insert(ImUserDataPo userDataPo);

    /**
     * 批量创建用户信息
     *
     * @param userDataPoList 用户信息集合
     * @return 是否成功
     */
    Boolean batchInsert(List<ImUserDataPo> userDataPoList);

    /**
     * 更新用户信息
     *
     * @param userDataPo 用户信息
     * @return 是否成功
     */
    Boolean update(ImUserDataPo userDataPo);

    /**
     * 获取用户信息
     *
     * @param keyword 关键词
     * @return 用户信息集合
     */
    List<ImUserDataPo> search(String keyword);

    /**
     * 批量获取用户
     *
     * @param userIdList 用户id集合
     * @return 用户信息集合
     */
    List<ImUserDataPo> selectByIds(List<String> userIdList);

    /**
     * 删除用户信息
     *
     * @param userId 用户id
     * @return 是否成功
     */
    Boolean deleteById(String userId);
}