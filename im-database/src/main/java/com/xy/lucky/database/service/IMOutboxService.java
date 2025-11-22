package com.xy.lucky.database.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.service.IService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xy.lucky.database.mapper.IMOutboxPoMapper;
import com.xy.lucky.domain.po.IMOutboxPo;
import com.xy.lucky.dubbo.api.database.outbox.IMOutboxDubboService;
import jakarta.annotation.Resource;
import org.apache.dubbo.config.annotation.DubboService;

import java.util.List;


@DubboService
public class IMOutboxService extends ServiceImpl<IMOutboxPoMapper, IMOutboxPo> implements IMOutboxDubboService, IService<IMOutboxPo> {

    @Resource
    private IMOutboxPoMapper imOutboxPoMapper;

    public List<IMOutboxPo> selectList() {
        return this.list();
    }

    public IMOutboxPo selectOne(Long id) {
        return this.getById(id);
    }

    public boolean saveOrUpdate(IMOutboxPo outboxPo) {
        return imOutboxPoMapper.insertOrUpdate(outboxPo);
    }

    public Boolean insert(IMOutboxPo outboxPo) {
        return this.save(outboxPo);
    }

    public Boolean batchInsert(List<IMOutboxPo> list) {
        return !imOutboxPoMapper.insert(list).isEmpty();
    }

    public Boolean update(IMOutboxPo outboxPo) {
        return this.updateById(outboxPo);
    }

    public Boolean deleteById(Long id) {
        return this.removeById(id);
    }

    public Boolean updateStatus(Long id, String status, Integer attempts) {
        UpdateWrapper<IMOutboxPo> updateWrapper = new UpdateWrapper<>();
        updateWrapper.set("status", status).set("attempts", attempts).eq("id", id);
        return this.update(updateWrapper);
    }

    public Boolean markAsFailed(Long id, String lastError, Integer attempts) {
        UpdateWrapper<IMOutboxPo> updateWrapper = new UpdateWrapper<>();
        updateWrapper.set("last_error", lastError).set("attempts", attempts).eq("id", id);
        return this.update(updateWrapper);
    }

    public List<IMOutboxPo> listByStatus(String status, Integer limit) {
        QueryWrapper<IMOutboxPo> query = new QueryWrapper<>();
        query.eq("status", status).last("limit " + limit);
        return this.list(query);
    }

}