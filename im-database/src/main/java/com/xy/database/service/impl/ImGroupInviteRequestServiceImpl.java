package com.xy.database.service.impl;


import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xy.database.mapper.ImGroupInviteRequestMapper;
import com.xy.database.service.ImGroupInviteRequestService;
import com.xy.domain.po.ImGroupInviteRequestPo;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ImGroupInviteRequestServiceImpl extends ServiceImpl<ImGroupInviteRequestMapper, ImGroupInviteRequestPo> implements ImGroupInviteRequestService {


    @Override
    public boolean saveOrUpdate(ImGroupInviteRequestPo imGroupInviteRequestPo) {
        return this.saveOrUpdate(imGroupInviteRequestPo);
    }

    @Override
    public boolean remove(String requestId) {
        return this.removeById(requestId);
    }

    @Override
    public ImGroupInviteRequestPo getOne(ImGroupInviteRequestPo imGroupInviteRequestPo) {
        return this.getById(imGroupInviteRequestPo);
    }

}
