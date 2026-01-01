package com.xy.lucky.database.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xy.lucky.database.mapper.ImUserDataMapper;
import com.xy.lucky.domain.po.ImUserDataPo;
import com.xy.lucky.dubbo.web.api.database.user.ImUserDataDubboService;
import lombok.RequiredArgsConstructor;
import org.apache.dubbo.config.annotation.DubboService;

import java.util.List;

@DubboService
@RequiredArgsConstructor
public class ImUserDataService extends ServiceImpl<ImUserDataMapper, ImUserDataPo>
        implements ImUserDataDubboService {

    private final ImUserDataMapper imUserDataMapper;

    public List<ImUserDataPo> queryList() {
        return super.list();
    }

    @Override
    public ImUserDataPo queryOne(String id) {
        return super.getById(id);
    }

    @Override
    public List<ImUserDataPo> queryByKeyword(String keyword) {
        QueryWrapper<ImUserDataPo> wrapper = new QueryWrapper<>();
        wrapper.select("user_id", "name", "avatar", "gender", "birthday", "location", "extra");
        wrapper.eq("user_id", keyword);
        return super.list(wrapper);
    }

    @Override
    public List<ImUserDataPo> queryListByIds(List<String> userIdList) {
        return super.listByIds(userIdList);
    }


    @Override
    public Boolean creat(ImUserDataPo userDataPo) {
        return super.save(userDataPo);
    }


    @Override
    public Boolean creatBatch(List<ImUserDataPo> userDataPoList) {
        return !imUserDataMapper.insert(userDataPoList).isEmpty();
    }

    @Override
    public Boolean modify(ImUserDataPo userDataPo) {
        return super.updateById(userDataPo);
    }

    @Override
    public Boolean removeOne(String id) {
        return super.removeById(id);
    }
}
