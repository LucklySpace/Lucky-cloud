package com.xy.lucky.database.web.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xy.lucky.api.message.ImSingleMessageDubboService;
import com.xy.lucky.database.web.mapper.ImSingleMessageMapper;
import com.xy.lucky.domain.po.ImSingleMessagePo;
import lombok.RequiredArgsConstructor;
import org.apache.dubbo.config.annotation.DubboService;

import java.util.List;

@DubboService
@RequiredArgsConstructor
public class ImSingleMessageService extends ServiceImpl<ImSingleMessageMapper, ImSingleMessagePo>
        implements ImSingleMessageDubboService {

    private final ImSingleMessageMapper imSingleMessageMapper;


    @Override
    public List<ImSingleMessagePo> queryList(String userId, Long sequence) {
        return imSingleMessageMapper.selectSingleMessage(userId, sequence);
    }

    @Override
    public ImSingleMessagePo queryOne(String messageId) {
        return super.getById(messageId);
    }

    @Override
    public Boolean creat(ImSingleMessagePo singleMessagePo) {
        return super.save(singleMessagePo);
    }

    @Override
    public Boolean creatBatch(List<ImSingleMessagePo> singleMessagePoList) {
        return !imSingleMessageMapper.insert(singleMessagePoList).isEmpty();
    }

    @Override
    public Boolean modify(ImSingleMessagePo singleMessagePo) {
        return super.updateById(singleMessagePo);
    }

    @Override
    public Boolean removeOne(String messageId) {
        return super.removeById(messageId);
    }

    @Override
    public ImSingleMessagePo queryLast(String fromId, String toId) {
        return imSingleMessageMapper.selectLastSingleMessage(fromId, toId);
    }

    @Override
    public Integer queryReadStatus(String fromId, String toId, Integer code) {
        return imSingleMessageMapper.selectReadStatus(fromId, toId, code);
    }

}
