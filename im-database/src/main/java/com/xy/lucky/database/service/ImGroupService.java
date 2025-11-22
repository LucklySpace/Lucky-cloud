package com.xy.lucky.database.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xy.lucky.database.mapper.ImGroupMapper;
import com.xy.lucky.domain.po.ImGroupPo;
import com.xy.lucky.dubbo.api.database.group.ImGroupDubboService;
import jakarta.annotation.Resource;
import org.apache.dubbo.config.annotation.DubboService;

import java.util.List;

@DubboService
public class ImGroupService extends ServiceImpl<ImGroupMapper, ImGroupPo>
        implements ImGroupDubboService, IService<ImGroupPo> {

    @Resource
    private ImGroupMapper imGroupMapper;

    public List<ImGroupPo> selectList(String userId) {
        return imGroupMapper.selectGroupsByUserId(userId);
    }


    public ImGroupPo selectOne(String groupId) {
        return this.getById(groupId);
    }


    public Boolean insert(ImGroupPo groupPo) {
        return this.save(groupPo);
    }


    public Boolean batchInsert(List<ImGroupPo> list) {
        return !imGroupMapper.insert(list).isEmpty();
    }


    public Boolean update(ImGroupPo groupPo) {
        return this.updateById(groupPo);
    }


    public Boolean deleteById(String groupId) {
        return this.removeById(groupId);
    }


    public long count() {
        return imGroupMapper.selectCount(null);
    }

}