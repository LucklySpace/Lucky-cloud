package com.xy.lucky.database.web.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xy.lucky.api.message.ImGroupMessageDubboService;
import com.xy.lucky.database.web.mapper.ImGroupMessageMapper;
import com.xy.lucky.database.web.mapper.ImGroupMessageStatusMapper;
import com.xy.lucky.domain.po.ImGroupMessagePo;
import com.xy.lucky.domain.po.ImGroupMessageStatusPo;
import jakarta.annotation.Resource;
import lombok.RequiredArgsConstructor;
import org.apache.dubbo.config.annotation.DubboService;

import java.util.List;

@DubboService
@RequiredArgsConstructor
public class ImGroupMessageService extends ServiceImpl<ImGroupMessageMapper, ImGroupMessagePo>
        implements ImGroupMessageDubboService {

    private final ImGroupMessageMapper imGroupMessageMapper;

    @Resource
    private ImGroupMessageStatusMapper imGroupMessageStatusMapper;


    @Override
    public List<ImGroupMessagePo> queryList(String userId, Long sequence) {
        return imGroupMessageMapper.selectGroupMessage(userId, sequence);
    }

    @Override
    public ImGroupMessagePo queryOne(String messageId) {
        return super.getById(messageId);
    }

    @Override
    public boolean creat(ImGroupMessagePo groupMessagePo) {
        return super.save(groupMessagePo);
    }

    @Override
    public boolean creatBatch(List<ImGroupMessageStatusPo> groupMessagePoList) {
        return !imGroupMessageStatusMapper.insert(groupMessagePoList).isEmpty();
    }

    @Override
    public boolean modify(ImGroupMessagePo groupMessagePo) {
        return super.updateById(groupMessagePo);
    }

    @Override
    public boolean removeOne(String messageId) {
        return super.removeById(messageId);
    }

    @Override
    public ImGroupMessagePo queryLast(String userId, String groupId) {
        return imGroupMessageMapper.selectLastGroupMessage(userId, groupId);
    }


    @Override
    public Integer queryReadStatus(String groupId, String toId, Integer code) {
        return imGroupMessageMapper.selectReadStatus(groupId, toId, code);
    }

}
