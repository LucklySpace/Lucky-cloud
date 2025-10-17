package com.xy.database.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.xy.domain.po.ImFriendshipRequestPo;

import java.util.List;


/**
 * @author dense
 * @description 针对表【im_friendship_request】的数据库操作Service
 */
public interface ImFriendshipRequestService extends IService<ImFriendshipRequestPo> {

    List<ImFriendshipRequestPo> selectList(String userId);

    ImFriendshipRequestPo selectOne(ImFriendshipRequestPo requestPo);

    Boolean insert(ImFriendshipRequestPo requestPo);

    Boolean update(ImFriendshipRequestPo requestPo);

    Boolean batchInsert(List<ImFriendshipRequestPo> requestPoList);

    Boolean deleteById(String requestId);
}
