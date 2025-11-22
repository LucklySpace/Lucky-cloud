package com.xy.dubbo.api.database.group;

import com.xy.domain.po.ImGroupInviteRequestPo;

import java.util.List;

public interface ImGroupInviteRequestDubboService {

    /**
     * 查询群邀请请求列表
     *
     * @param userId
     * @return
     */
    List<ImGroupInviteRequestPo> selectList(String userId);

    /**
     * 查询一个群邀请请求
     *
     * @param imGroupInviteRequestPo
     * @return
     */
    ImGroupInviteRequestPo selectOne(ImGroupInviteRequestPo imGroupInviteRequestPo);


    /**
     * 添加群邀请请求
     *
     * @param imGroupInviteRequestPo
     * @return
     */
    Boolean insert(ImGroupInviteRequestPo imGroupInviteRequestPo);

    /**
     * 修改群邀请请求
     *
     * @param imGroupInviteRequestPo
     * @return
     */
    Boolean update(ImGroupInviteRequestPo imGroupInviteRequestPo);

    /**
     * 删除群邀请请求
     *
     * @param requestId
     * @return
     */
    Boolean deleteById(String requestId);

    Boolean batchInsert(List<ImGroupInviteRequestPo> requests);
}
