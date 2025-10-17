package com.xy.database.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xy.database.mapper.ImGroupMapper;
import com.xy.database.service.ImGroupService;
import com.xy.domain.po.ImGroupPo;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @author dense
 * @description 针对表【im_group】的数据库操作Service实现
 */
@Service
public class ImGroupServiceImpl extends ServiceImpl<ImGroupMapper, ImGroupPo>
        implements ImGroupService {

    @Resource
    private ImGroupMapper imGroupMapper;


    @Override
    public List<ImGroupPo> list(String userId) {
        return imGroupMapper.selectGroupsByUserId(userId);
    }

    @Override
    public List<String> selectNinePeople(String groupId) {
        return imGroupMapper.selectNinePeople(groupId);
    }
    
    @Override
    public boolean insert(ImGroupPo groupPo) {
        return this.save(groupPo);
    }
    
    @Override
    public boolean batchInsert(List<ImGroupPo> groupPoList) {
        return this.saveBatch(groupPoList);
    }
    
    @Override
    public ImGroupPo selectOne(String groupId) {
        return this.getById(groupId);
    }
    
    @Override
    public ImGroupPo selectById(String groupId) {
        return this.getById(groupId);
    }
    
    @Override
    public long count() {
        return this.count();
    }
    
    @Override
    public List<ImGroupPo> selectList() {
        return this.list();
    }
    
    @Override
    public boolean update(ImGroupPo groupPo) {
        return this.updateById(groupPo);
    }
    
    @Override
    public boolean deleteById(String groupId) {
        return this.removeById(groupId);
    }
}