package com.xy.lucky.database.web.service;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xy.lucky.database.web.mapper.ImUserDataMapper;
import com.xy.lucky.domain.po.ImUserDataPo;
import com.xy.lucky.rpc.api.database.user.ImUserDataDubboService;
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
        Wrapper<ImUserDataPo> wrapper = Wrappers.<ImUserDataPo>lambdaQuery()
                .select(ImUserDataPo::getUserId, ImUserDataPo::getName, ImUserDataPo::getAvatar, ImUserDataPo::getGender,
                        ImUserDataPo::getBirthday, ImUserDataPo::getLocation, ImUserDataPo::getExtra)
                .eq(ImUserDataPo::getUserId, keyword);
        return super.list(wrapper);
    }

    @Override
    public List<ImUserDataPo> queryListByIds(List<String> userIdList) {
        return imUserDataMapper.selectByIds(userIdList);
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
