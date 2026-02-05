package com.xy.lucky.api.user;

import com.xy.lucky.domain.po.ImUserPo;

import java.util.List;

/**
 * 用户Dubbo服务接口
 */
public interface ImUserDubboService {

    /**
     * 获取用户列表
     */
    List<ImUserPo> queryList();

    /**
     * 获取用户信息
     */
    ImUserPo queryOne(String userId);

    /**
     * 创建用户信息
     */
    Boolean creat(ImUserPo userDataPo);

    /**
     * 批量创建用户信息
     */
    Boolean creatBatch(List<ImUserPo> userDataPoList);

    /**
     * 更新用户信息
     */
    Boolean modify(ImUserPo userDataPo);

    /**
     * 删除用户信息
     */
    Boolean removeOne(String userId);

    /**
     * 根据手机号获取用户信息
     */
    ImUserPo queryOneByMobile(String phoneNumber);
}
