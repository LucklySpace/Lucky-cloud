package com.xy.lucky.database.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xy.lucky.database.mapper.IMOutboxPoMapper;
import com.xy.lucky.domain.po.IMOutboxPo;
import com.xy.lucky.dubbo.api.database.outbox.IMOutboxDubboService;
import lombok.RequiredArgsConstructor;
import org.apache.dubbo.config.annotation.DubboService;

import java.util.List;


@DubboService
@RequiredArgsConstructor
public class IMOutboxService extends ServiceImpl<IMOutboxPoMapper, IMOutboxPo> implements IMOutboxDubboService {

    private final IMOutboxPoMapper imOutboxPoMapper;

    @Override
    public List<IMOutboxPo> queryList() {
        return super.list();
    }

    @Override
    public IMOutboxPo queryOne(Long id) {
        return super.getById(id);
    }

    @Override
    public boolean creatOrModify(IMOutboxPo outboxPo) {
        return imOutboxPoMapper.insertOrUpdate(outboxPo);
    }

    @Override
    public Boolean creat(IMOutboxPo outboxPo) {
        return super.save(outboxPo);
    }

    @Override
    public Boolean creatBatch(List<IMOutboxPo> list) {
        return !imOutboxPoMapper.insert(list).isEmpty();
    }

    @Override
    public Boolean modify(IMOutboxPo outboxPo) {
        return super.updateById(outboxPo);
    }

    @Override
    public Boolean removeOne(Long id) {
        return super.removeById(id);
    }

    @Override
    public Boolean modifyStatus(Long id, String status, Integer attempts) {
        UpdateWrapper<IMOutboxPo> updateWrapper = new UpdateWrapper<>();
        updateWrapper.set("status", status).set("attempts", attempts).eq("id", id);
        return super.update(updateWrapper);
    }

    @Override
    public Boolean modifyToFailed(Long id, String lastError, Integer attempts) {
        UpdateWrapper<IMOutboxPo> updateWrapper = new UpdateWrapper<>();
        updateWrapper.set("last_error", lastError).set("attempts", attempts).eq("id", id);
        return super.update(updateWrapper);
    }

    @Override
    public List<IMOutboxPo> queryByStatus(String status, Integer limit) {
        QueryWrapper<IMOutboxPo> query = new QueryWrapper<>();
        query.eq("status", status).last("limit " + limit);
        return super.list(query);
    }

}
