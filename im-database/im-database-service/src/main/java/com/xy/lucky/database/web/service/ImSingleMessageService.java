package com.xy.lucky.database.web.service;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xy.lucky.database.web.mapper.ImSingleMessageMapper;
import com.xy.lucky.domain.po.ImSingleMessagePo;
import com.xy.lucky.rpc.api.database.message.ImSingleMessageDubboService;
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
    public Boolean modifyReadStatus(ImSingleMessagePo singleMessagePo) {
        LambdaUpdateWrapper<ImSingleMessagePo> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(ImSingleMessagePo::getFromId, singleMessagePo.getFromId())
                .eq(ImSingleMessagePo::getToId, singleMessagePo.getToId()).or(
                        wrapper -> wrapper.eq(ImSingleMessagePo::getFromId, singleMessagePo.getToId())
                                .eq(ImSingleMessagePo::getToId, singleMessagePo.getFromId())
                )
                .set(ImSingleMessagePo::getReadStatus, singleMessagePo.getReadStatus());
        return super.update(updateWrapper);
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
