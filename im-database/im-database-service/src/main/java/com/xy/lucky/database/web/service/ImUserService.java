package com.xy.lucky.database.web.service;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xy.lucky.database.web.mapper.ImUserMapper;
import com.xy.lucky.domain.po.ImUserPo;
import com.xy.lucky.rpc.api.database.user.ImUserDubboService;
import lombok.RequiredArgsConstructor;
import org.apache.dubbo.config.annotation.DubboService;

import java.util.List;

@DubboService
@RequiredArgsConstructor
public class ImUserService extends ServiceImpl<ImUserMapper, ImUserPo>
        implements ImUserDubboService {

    private final ImUserMapper imUserMapper;

    @Override
    public List<ImUserPo> queryList() {
        return super.list();
    }

    @Override
    public ImUserPo queryOne(String userId) {
        return super.getById(userId);
    }

    @Override
    public Boolean creat(ImUserPo userPo) {
        return super.save(userPo);
    }

    @Override
    public Boolean creatBatch(List<ImUserPo> userPoList) {
        return !imUserMapper.insert(userPoList).isEmpty();
    }

    @Override
    public Boolean modify(ImUserPo userPo) {
        return super.updateById(userPo);
    }

    @Override
    public ImUserPo queryOneByMobile(String phoneNumber) {
        Wrapper<ImUserPo> queryWrapper = Wrappers.<ImUserPo>lambdaQuery()
                .eq(ImUserPo::getMobile, phoneNumber);
        return super.getOne(queryWrapper);
    }

    @Override
    public Boolean removeOne(String userId) {
        return super.removeById(userId);
    }

    public long queryCount() {
        return super.count();
    }

}
