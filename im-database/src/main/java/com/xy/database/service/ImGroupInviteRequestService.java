package com.xy.database.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.xy.domain.po.ImGroupInviteRequestPo;

/**
 * 群聊邀请请求服务类
 */
public interface ImGroupInviteRequestService extends IService<ImGroupInviteRequestPo> {

    /**
     * 保存群聊邀请请求
     */
    boolean saveOrUpdate(ImGroupInviteRequestPo imGroupInviteRequestPo);

    /**
     * 根据ID删除群聊邀请请求
     */
    boolean remove(String requestId);

    /**
     * 根据ID查询群聊邀请请求
     */
    ImGroupInviteRequestPo getOne(ImGroupInviteRequestPo imGroupInviteRequestPo);
}