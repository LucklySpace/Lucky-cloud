package com.xy.database.service.impl;


import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xy.database.mapper.ImFriendshipRequestMapper;
import com.xy.database.service.ImFriendshipRequestService;
import com.xy.domain.po.ImFriendshipRequestPo;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @author dense
 * @description 针对表【im_friendship_request】的数据库操作Service实现
 */
@Service
public class ImFriendshipRequestServiceImpl extends ServiceImpl<ImFriendshipRequestMapper, ImFriendshipRequestPo>
        implements ImFriendshipRequestService {

    @Resource
    private ImFriendshipRequestMapper imFriendshipRequestMapper;

    public List<ImFriendshipRequestPo> list(String userId) {
        QueryWrapper<ImFriendshipRequestPo> imFriendshipRequestQuery = new QueryWrapper<>();
        imFriendshipRequestQuery.eq("to_id", userId);
        return this.list(imFriendshipRequestQuery);
    }

}




