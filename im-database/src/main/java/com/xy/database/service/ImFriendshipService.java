package com.xy.database.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.xy.domain.po.ImFriendshipPo;

import java.util.List;

/**
 * @author dense
 * @description 针对表【im_friendship】的数据库操作Service
 */
public interface ImFriendshipService extends IService<ImFriendshipPo> {

    List<ImFriendshipPo> list(String ownerId, Long sequence);

    ImFriendshipPo getOne(String ownerId, String friendId);

}
