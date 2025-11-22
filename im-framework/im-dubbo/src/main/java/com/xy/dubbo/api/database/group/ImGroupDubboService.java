package com.xy.dubbo.api.database.group;

import com.xy.domain.po.ImGroupPo;

import java.util.List;

/**
 * 群组Dubbo服务接口
 */
public interface ImGroupDubboService {

    /**
     * 获取群列表
     *
     * @param userId 用户ID
     * @return 群列表
     */
    List<ImGroupPo> selectList(String userId);

    /**
     * 获取群信息
     *
     * @param groupId 群ID
     * @return 群信息
     */
    ImGroupPo selectOne(String groupId);

    /**
     * 插入群信息
     *
     * @param groupPo 群信息
     * @return 是否成功
     */
    Boolean insert(ImGroupPo groupPo);

    /**
     * 更新群信息
     *
     * @param groupPo 群信息
     * @return 是否成功
     */
    Boolean update(ImGroupPo groupPo);


    /**
     * 批量插入群信息
     *
     * @param list 群信息列表
     * @return 是否成功
     */
    Boolean batchInsert(List<ImGroupPo> list);

    /**
     * 删除群信息
     *
     * @param groupId 群ID
     * @return 是否成功
     */
    Boolean deleteById(String groupId);
}