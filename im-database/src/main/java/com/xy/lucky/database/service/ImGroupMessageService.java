package com.xy.lucky.database.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xy.lucky.database.mapper.ImGroupMessageMapper;
import com.xy.lucky.database.mapper.ImGroupMessageStatusMapper;
import com.xy.lucky.domain.po.ImGroupMessagePo;
import com.xy.lucky.domain.po.ImGroupMessageStatusPo;
import com.xy.lucky.dubbo.api.database.message.ImGroupMessageDubboService;
import jakarta.annotation.Resource;
import org.apache.dubbo.config.annotation.DubboService;

import java.util.List;

@DubboService
public class ImGroupMessageService extends ServiceImpl<ImGroupMessageMapper, ImGroupMessagePo>
        implements ImGroupMessageDubboService, IService<ImGroupMessagePo> {

    @Resource
    private ImGroupMessageMapper imGroupMessageMapper;

    @Resource
    private ImGroupMessageStatusMapper imGroupMessageStatusMapper;


    public List<ImGroupMessagePo> selectList(String userId, Long sequence) {
        return imGroupMessageMapper.selectGroupMessage(userId, sequence);
    }

    public ImGroupMessagePo selectOne(String messageId) {
        return this.getById(messageId);
    }

    public boolean insert(ImGroupMessagePo groupMessagePo) {
        return this.save(groupMessagePo);
    }

    public boolean batchInsert(List<ImGroupMessageStatusPo> groupMessagePoList) {
        return !imGroupMessageStatusMapper.insert(groupMessagePoList).isEmpty();
    }

    public boolean update(ImGroupMessagePo groupMessagePo) {
        return this.updateById(groupMessagePo);
    }

    public boolean deleteById(String messageId) {
        return this.removeById(messageId);
    }

    public ImGroupMessagePo last(String userId, String groupId) {
        return imGroupMessageMapper.selectLastGroupMessage(userId, groupId);
    }


    public Integer selectReadStatus(String groupId, String toId, Integer code) {
        return imGroupMessageMapper.selectReadStatus(groupId, toId, code);
    }

}