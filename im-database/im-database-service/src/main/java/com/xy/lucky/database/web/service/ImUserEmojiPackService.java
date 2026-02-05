package com.xy.lucky.database.web.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xy.lucky.api.emoji.ImUserEmojiPackDubboService;
import com.xy.lucky.database.web.mapper.ImUserEmojiPackMapper;
import com.xy.lucky.database.web.utils.MybatisBatchExecutor;
import com.xy.lucky.domain.po.ImUserEmojiPackPo;
import lombok.RequiredArgsConstructor;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;

@DubboService
@RequiredArgsConstructor
public class ImUserEmojiPackService extends ServiceImpl<ImUserEmojiPackMapper, ImUserEmojiPackPo>
        implements ImUserEmojiPackDubboService {

    private final ImUserEmojiPackMapper mapper;
    private final MybatisBatchExecutor batchExecutor;

    @Override
    public List<ImUserEmojiPackPo> listByUserId(String userId) {
        return mapper.selectByUserId(userId);
    }

    @Override
    public List<String> listPackIds(String userId) {
        return mapper.selectPackIdsByUserId(userId);
    }

    @Override
    public Boolean bindPack(String userId, String packCode) {
        if (userId == null || packCode == null) {
            return false;
        }
        QueryWrapper<ImUserEmojiPackPo> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId).eq("pack_id", packCode);
        ImUserEmojiPackPo exist = super.getOne(wrapper);
        if (exist != null) {
            UpdateWrapper<ImUserEmojiPackPo> uw = new UpdateWrapper<>();
            uw.eq("user_id", userId).eq("pack_id", packCode).set("del_flag", 1);
            return super.update(uw);
        }
        ImUserEmojiPackPo po = new ImUserEmojiPackPo();
        po.setId(userId + ":" + packCode);
        po.setUserId(userId);
        po.setPackId(packCode);
        po.setDelFlag(1);
        return super.save(po);
    }

    @Override
    public Boolean bindPacks(String userId, List<String> packCodes) {
        if (userId == null || CollectionUtils.isEmpty(packCodes)) {
            return false;
        }
        List<ImUserEmojiPackPo> list = new ArrayList<>();
        for (String code : packCodes) {
            if (code == null) continue;
            ImUserEmojiPackPo po = new ImUserEmojiPackPo();
            po.setId(userId + ":" + code);
            po.setUserId(userId);
            po.setPackId(code);
            po.setDelFlag(1);
            list.add(po);
        }
        batchExecutor.batchSave(list, ImUserEmojiPackMapper.class, ImUserEmojiPackMapper::insert);
        return true;
    }

    @Override
    public Boolean unbindPack(String userId, String packCode) {
        if (userId == null || packCode == null) {
            return false;
        }
        UpdateWrapper<ImUserEmojiPackPo> uw = new UpdateWrapper<>();
        uw.eq("user_id", userId).eq("pack_id", packCode).set("del_flag", 0);
        return super.update(uw);
    }
}

