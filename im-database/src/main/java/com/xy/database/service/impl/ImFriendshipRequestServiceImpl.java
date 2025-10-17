package com.xy.database.service.impl;


import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xy.core.utils.StringUtils;
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

    @Override
    public List<ImFriendshipRequestPo> selectList(String userId) {
        QueryWrapper<ImFriendshipRequestPo> imFriendshipRequestQuery = new QueryWrapper<>();
        imFriendshipRequestQuery.eq("to_id", userId);
        return this.list(imFriendshipRequestQuery);
    }

    @Override
    public ImFriendshipRequestPo selectOne(ImFriendshipRequestPo requestPo) {

        QueryWrapper<ImFriendshipRequestPo> imFriendshipRequestQuery = new QueryWrapper<>();

        if (StringUtils.hasText(requestPo.getId())) {
            imFriendshipRequestQuery.eq("id", requestPo.getId());
        }
        if (StringUtils.hasText(requestPo.getFromId()) && StringUtils.hasText(requestPo.getToId())) {
            imFriendshipRequestQuery.eq("from_id", requestPo.getFromId()).and(wrapper -> wrapper.eq("to_id", requestPo.getToId()));
        }

        return this.getOne(imFriendshipRequestQuery);
    }

    @Override
    public Boolean insert(ImFriendshipRequestPo requestPo) {
        return this.insert(requestPo);
    }

    @Override
    public Boolean update(ImFriendshipRequestPo requestPo) {
        return this.updateById(requestPo);
    }

    @Override
    public Boolean batchInsert(List<ImFriendshipRequestPo> requestPoList) {
        return this.batchInsert(requestPoList);
    }

    @Override
    public Boolean deleteById(String requestId) {
        return this.deleteById(requestId);
    }


}




