package com.xy.database.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.IService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xy.database.mapper.ImUserMapper;
import com.xy.domain.po.ImUserPo;
import com.xy.dubbo.api.database.user.ImUserDubboService;
import jakarta.annotation.Resource;
import org.apache.dubbo.config.annotation.DubboService;

import java.util.List;

@DubboService
public class ImUserService extends ServiceImpl<ImUserMapper, ImUserPo>
        implements ImUserDubboService, IService<ImUserPo> {

    @Resource
    private ImUserMapper imUserMapper;

    public List<ImUserPo> selectList() {
        return this.list();
    }

    public ImUserPo selectOne(String userId) {
        return this.getById(userId);
    }

    public Boolean insert(ImUserPo userPo) {
        return this.save(userPo);
    }

    public Boolean batchInsert(List<ImUserPo> userPoList) {
        return !imUserMapper.insert(userPoList).isEmpty();
    }

    public Boolean update(ImUserPo userPo) {
        return this.updateById(userPo);
    }

    @Override
    public ImUserPo selectOneByMobile(String phoneNumber) {
        QueryWrapper<ImUserPo> userPoQueryWrapper = new QueryWrapper<>();
        userPoQueryWrapper.eq("mobile", phoneNumber);
        return this.getOne(userPoQueryWrapper);
    }

    public Boolean deleteById(String userId) {
        return this.removeById(userId);
    }

    public long count() {
        return this.count();
    }

}