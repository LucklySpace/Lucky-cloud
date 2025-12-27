package com.xy.lucky.dubbo.api.database.group;

import com.xy.lucky.domain.po.ImGroupPo;

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
    List<ImGroupPo> queryList(String userId);

    /**
     * 获取群信息
     *
     * @param groupId 群ID
     * @return 群信息
     */
    ImGroupPo queryOne(String groupId);

    /**
     * 插入群信息
     *
     * @param groupPo 群信息
     * @return 是否成功
     */
    Boolean creat(ImGroupPo groupPo);

    /**
     * 更新群信息
     *
     * @param groupPo 群信息
     * @return 是否成功
     */
    Boolean modify(ImGroupPo groupPo);


    /**
     * 批量插入群信息
     *
     * @param list 群信息列表
     * @return 是否成功
     */
    Boolean creatBatch(List<ImGroupPo> list);

    /**
     * 删除群信息
     *
     * @param groupId 群ID
     * @return 是否成功
     */
    Boolean removeOne(String groupId);
}
