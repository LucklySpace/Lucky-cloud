package com.xy.database.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xy.database.mapper.ImSingleMessageMapper;
import com.xy.database.service.ImSingleMessageService;
import com.xy.domain.po.ImSingleMessagePo;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @author dense
 * @description 针对表【im_single_message】的数据库操作Service实现
 */
@Service
public class ImSingleMessageServiceImpl extends ServiceImpl<ImSingleMessageMapper, ImSingleMessagePo>
        implements ImSingleMessageService {


    @Resource
    private ImSingleMessageMapper imSingleMessageMapper;

    @Override
    public List<ImSingleMessagePo> selectList(String userId, Long sequence) {
        return imSingleMessageMapper.selectSingleMessage(userId, sequence);
    }

    @Override
    public ImSingleMessagePo selectOne(String messageId) {
        return this.getById(messageId);
    }

    @Override
    public boolean insert(ImSingleMessagePo singleMessagePo) {
        return this.save(singleMessagePo);
    }
    
    @Override
    public boolean batchInsert(List<ImSingleMessagePo> singleMessagePoList) {
        return this.saveBatch(singleMessagePoList);
    }

    @Override
    public boolean update(ImSingleMessagePo singleMessagePo) {
        return this.updateById(singleMessagePo);
    }
    
    @Override
    public boolean deleteById(String messageId) {
        return this.removeById(messageId);
    }

    @Override
    public ImSingleMessagePo last(String fromId, String toId) {
        return imSingleMessageMapper.selectLastSingleMessage(fromId, toId);
    }

    @Override
    public Integer selectReadStatus(String fromId, String toId, Integer code) {
        return imSingleMessageMapper.selectReadStatus(fromId, toId, code);
    }
}