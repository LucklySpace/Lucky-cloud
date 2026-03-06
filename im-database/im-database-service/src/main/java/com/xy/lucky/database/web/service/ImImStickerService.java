package com.xy.lucky.database.web.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xy.lucky.database.web.mapper.ImStickerMapper;
import com.xy.lucky.domain.po.ImStickerPo;
import com.xy.lucky.rpc.api.database.sticker.ImStickerDubboService;
import lombok.RequiredArgsConstructor;
import org.apache.dubbo.config.annotation.DubboService;

import java.util.List;

@DubboService
@RequiredArgsConstructor
public class ImImStickerService extends ServiceImpl<ImStickerMapper, ImStickerPo>
        implements ImStickerDubboService {

    @Override
    public ImStickerPo queryOne(String stickerId) {
        return super.getById(stickerId);
    }

    @Override
    public List<ImStickerPo> queryListByPackId(String packId) {
        return super.list(Wrappers.<ImStickerPo>lambdaQuery()
                .eq(ImStickerPo::getPackId, packId)
                .orderByAsc(ImStickerPo::getSort));
    }

    @Override
    public Boolean creat(ImStickerPo imStickerPo) {
        return super.save(imStickerPo);
    }

    @Override
    public Boolean modify(ImStickerPo imStickerPo) {
        return super.updateById(imStickerPo);
    }

    @Override
    public Boolean removeOne(String stickerId) {
        return super.removeById(stickerId);
    }
}
