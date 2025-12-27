package com.xy.lucky.database.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xy.lucky.database.mapper.ImGroupMapper;
import com.xy.lucky.domain.po.ImGroupPo;
import com.xy.lucky.dubbo.api.database.group.ImGroupDubboService;
import lombok.RequiredArgsConstructor;
import org.apache.dubbo.config.annotation.DubboService;

import java.util.List;

@DubboService
@RequiredArgsConstructor
public class ImGroupService extends ServiceImpl<ImGroupMapper, ImGroupPo>
        implements ImGroupDubboService {

    private final ImGroupMapper imGroupMapper;

    @Override
    public List<ImGroupPo> queryList(String userId) {
        return imGroupMapper.selectGroupsByUserId(userId);
    }

    @Override
    public ImGroupPo queryOne(String groupId) {
        return this.getById(groupId);
    }

    @Override
    public Boolean creat(ImGroupPo groupPo) {
        return this.save(groupPo);
    }

    @Override
    public Boolean creatBatch(List<ImGroupPo> list) {
        return !imGroupMapper.insert(list).isEmpty();
    }

    @Override
    public Boolean modify(ImGroupPo groupPo) {
        return this.updateById(groupPo);
    }

    @Override
    public Boolean removeOne(String groupId) {
        return this.removeById(groupId);
    }

    public long queryCount() {
        return imGroupMapper.selectCount(null);
    }

}
