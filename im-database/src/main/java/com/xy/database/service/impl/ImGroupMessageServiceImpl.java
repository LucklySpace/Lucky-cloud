package com.xy.database.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xy.database.mapper.ImGroupMessageMapper;
import com.xy.database.service.ImGroupMessageService;
import com.xy.domain.po.ImGroupMessagePo;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @author dense
 * @description 针对表【im_group_message】的数据库操作Service实现
 */
@Service
public class ImGroupMessageServiceImpl extends ServiceImpl<ImGroupMessageMapper, ImGroupMessagePo>
        implements ImGroupMessageService {

    @Resource
    private ImGroupMessageMapper imGroupMessageMapper;

    @Override
    public List<ImGroupMessagePo> selectList(String userId, Long sequence) {
        return imGroupMessageMapper.selectGroupMessage(userId, sequence);
    }

    @Override
    public ImGroupMessagePo selectOne(String messageId) {
        return this.getById(messageId);
    }

    @Override
    public boolean insert(ImGroupMessagePo groupMessagePo) {
        return this.save(groupMessagePo);
    }

    @Override
    public boolean batchInsert(List<ImGroupMessagePo> groupMessagePoList) {
        return this.saveBatch(groupMessagePoList);
    }

    @Override
    public boolean update(ImGroupMessagePo groupMessagePo) {
        return this.updateById(groupMessagePo);
    }

    @Override
    public boolean deleteById(String messageId) {
        return this.removeById(messageId);
    }

    @Override
    public ImGroupMessagePo last(String userId, String groupId) {
        return imGroupMessageMapper.selectLastGroupMessage(userId, groupId);
    }

    @Override
    public Integer selectReadStatus(String groupId, String toId, Integer code) {
        return imGroupMessageMapper.selectReadStatus(groupId, toId, code);
    }

}