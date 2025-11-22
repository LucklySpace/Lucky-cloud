package com.xy.lucky.database.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xy.lucky.database.mapper.ImSingleMessageMapper;
import com.xy.lucky.domain.po.ImSingleMessagePo;
import com.xy.lucky.dubbo.api.database.message.ImSingleMessageDubboService;
import jakarta.annotation.Resource;
import org.apache.dubbo.config.annotation.DubboService;

import java.util.List;

@DubboService
public class ImSingleMessageService extends ServiceImpl<ImSingleMessageMapper, ImSingleMessagePo>
        implements ImSingleMessageDubboService, IService<ImSingleMessagePo> {

    @Resource
    private ImSingleMessageMapper imSingleMessageMapper;


    public List<ImSingleMessagePo> selectList(String userId, Long sequence) {
        return imSingleMessageMapper.selectSingleMessage(userId, sequence);
    }

    public ImSingleMessagePo selectOne(String messageId) {
        return this.getById(messageId);
    }

    public Boolean insert(ImSingleMessagePo singleMessagePo) {
        return this.save(singleMessagePo);
    }

    public Boolean batchInsert(List<ImSingleMessagePo> singleMessagePoList) {
        return !imSingleMessageMapper.insert(singleMessagePoList).isEmpty();
    }

    public Boolean update(ImSingleMessagePo singleMessagePo) {
        return this.updateById(singleMessagePo);
    }

    public Boolean deleteById(String messageId) {
        return this.removeById(messageId);
    }

    public ImSingleMessagePo last(String fromId, String toId) {
        return imSingleMessageMapper.selectLastSingleMessage(fromId, toId);
    }

    public Integer selectReadStatus(String fromId, String toId, Integer code) {
        return imSingleMessageMapper.selectReadStatus(fromId, toId, code);
    }

}