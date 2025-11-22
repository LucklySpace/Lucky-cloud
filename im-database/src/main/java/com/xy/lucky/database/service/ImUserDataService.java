package com.xy.lucky.database.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.IService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xy.lucky.database.mapper.ImUserDataMapper;
import com.xy.lucky.domain.po.ImUserDataPo;
import com.xy.lucky.dubbo.api.database.user.ImUserDataDubboService;
import jakarta.annotation.Resource;
import org.apache.dubbo.config.annotation.DubboService;

import java.util.List;

@DubboService
public class ImUserDataService extends ServiceImpl<ImUserDataMapper, ImUserDataPo>
        implements ImUserDataDubboService, IService<ImUserDataPo> {

    @Resource
    private ImUserDataMapper imUserDataMapper;

    public List<ImUserDataPo> selectList() {
        return this.list();
    }

    public ImUserDataPo selectOne(String id) {
        return this.getById(id);
    }

    public List<ImUserDataPo> search(String keyword) {
        QueryWrapper<ImUserDataPo> wrapper = new QueryWrapper<>();
        wrapper.select("user_id", "name", "avatar", "gender", "birthday", "location", "extra");
        wrapper.eq("user_id", keyword);
        return this.list(wrapper);
    }

    public List<ImUserDataPo> selectByIds(List<String> userIdList) {
        return this.listByIds(userIdList);
    }


    public Boolean insert(ImUserDataPo userDataPo) {
        return this.save(userDataPo);
    }


    public Boolean batchInsert(List<ImUserDataPo> userDataPoList) {
        return !imUserDataMapper.insert(userDataPoList).isEmpty();
    }

    public Boolean update(ImUserDataPo userDataPo) {
        return this.updateById(userDataPo);
    }

    public Boolean deleteById(String id) {
        return this.removeById(id);
    }
}