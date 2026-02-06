package com.xy.lucky.database.web.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xy.lucky.database.web.mapper.IMOutboxPoMapper;
import com.xy.lucky.domain.po.IMOutboxPo;
import com.xy.lucky.rpc.api.database.outbox.IMOutboxDubboService;
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
        String s = status == null ? null : status.trim().toUpperCase();
        int a = attempts == null ? 0 : Math.max(0, attempts);
        updateWrapper.set("status", s).set("attempts", a).eq("id", id);
        return super.update(updateWrapper);
    }

    @Override
    public Boolean modifyToFailed(Long id, String lastError, Integer attempts) {
        UpdateWrapper<IMOutboxPo> updateWrapper = new UpdateWrapper<>();
        String err = lastError == null ? null : (lastError.length() > 1024 ? lastError.substring(0, 1024) : lastError);
        int a = attempts == null ? 0 : Math.max(0, attempts);
        updateWrapper.set("last_error", err).set("attempts", a).eq("id", id);
        return super.update(updateWrapper);
    }

    @Override
    public List<IMOutboxPo> queryByStatus(String status, Integer limit) {
        String s = status == null ? null : status.trim().toUpperCase();
        int lim = limit == null ? 100 : limit;
        lim = Math.max(1, Math.min(lim, 1000));
        QueryWrapper<IMOutboxPo> query = new QueryWrapper<>();
        query.eq("status", s).last("limit " + lim);
        return super.list(query);
    }

}
