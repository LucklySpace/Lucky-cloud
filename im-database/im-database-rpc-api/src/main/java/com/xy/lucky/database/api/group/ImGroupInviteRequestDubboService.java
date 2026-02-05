package com.xy.lucky.database.api.group;

import com.xy.lucky.domain.po.ImGroupInviteRequestPo;

import java.util.List;

public interface ImGroupInviteRequestDubboService {

    /**
     * 查询群邀请请求列表
     *
     * @param userId
     * @return
     */
    List<ImGroupInviteRequestPo> queryList(String userId);

    /**
     * 查询一个群邀请请求
     *
     * @param imGroupInviteRequestPo
     * @return
     */
    ImGroupInviteRequestPo queryOne(ImGroupInviteRequestPo imGroupInviteRequestPo);


    /**
     * 添加群邀请请求
     *
     * @param imGroupInviteRequestPo
     * @return
     */
    Boolean creat(ImGroupInviteRequestPo imGroupInviteRequestPo);

    /**
     * 修改群邀请请求
     *
     * @param imGroupInviteRequestPo
     * @return
     */
    Boolean modify(ImGroupInviteRequestPo imGroupInviteRequestPo);

    /**
     * 删除群邀请请求
     *
     * @param requestId
     * @return
     */
    Boolean removeOne(String requestId);

    Boolean creatBatch(List<ImGroupInviteRequestPo> requests);
}
