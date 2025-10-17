package com.xy.database.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xy.database.mapper.IMOutboxPoMapper;
import com.xy.database.service.IMOutboxService;
import com.xy.domain.po.IMOutboxPo;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class IMOutboxServiceImpl extends ServiceImpl<IMOutboxPoMapper, IMOutboxPo> implements IMOutboxService {
    @Override
    public Boolean updateStatus(Long id, String status, Integer attempts) {
        UpdateWrapper<IMOutboxPo> updateWrapper = new UpdateWrapper<>();
        updateWrapper.set("status", status).set("attempts", attempts).eq("id", id);
        return this.update(updateWrapper);
    }

    @Override
    public Boolean markAsFailed(Long id, String lastError, Integer attempts) {
        UpdateWrapper<IMOutboxPo> updateWrapper = new UpdateWrapper<>();
        updateWrapper.set("last_error", lastError).set("attempts", attempts).eq("id", id);
        return this.update(updateWrapper);
    }

    @Override
    public List<IMOutboxPo> listByStatus(String status, Integer limit) {
        QueryWrapper<IMOutboxPo> query = new QueryWrapper<>();
        query.eq("status", status).last("limit " + limit);
        return this.list(query);
    }
    
    @Override
    public boolean insert(IMOutboxPo outboxPo) {
        return this.save(outboxPo);
    }
    
    @Override
    public boolean batchInsert(List<IMOutboxPo> outboxPoList) {
        return this.saveBatch(outboxPoList);
    }
    
    @Override
    public IMOutboxPo selectOne(Long id) {
        return this.getById(id);
    }
    
    @Override
    public IMOutboxPo selectById(Long id) {
        return this.getById(id);
    }

    @Override
    public List<IMOutboxPo> selectList() {
        return this.list();
    }
    
    @Override
    public boolean update(IMOutboxPo outboxPo) {
        return this.updateById(outboxPo);
    }
    
    @Override
    public boolean deleteById(Long id) {
        return this.removeById(id);
    }
}