package com.xy.lucky.dubbo.web.api.database.user;

import com.xy.lucky.domain.po.ImUserDataPo;

import java.util.List;

/**
 * 用户Dubbo服务接口
 */
public interface ImUserDataDubboService {

    /**
     * 获取用户信息
     */
    ImUserDataPo queryOne(String userId);

    /**
     * 创建用户信息
     */
    Boolean creat(ImUserDataPo userDataPo);

    /**
     * 批量创建用户信息
     */
    Boolean creatBatch(List<ImUserDataPo> userDataPoList);

    /**
     * 更新用户信息
     */
    Boolean modify(ImUserDataPo userDataPo);

    /**
     * 获取用户信息
     */
    List<ImUserDataPo> queryByKeyword(String keyword);

    /**
     * 批量获取用户
     */
    List<ImUserDataPo> queryListByIds(List<String> userIdList);

    /**
     * 删除用户信息
     */
    Boolean removeOne(String userId);
}
