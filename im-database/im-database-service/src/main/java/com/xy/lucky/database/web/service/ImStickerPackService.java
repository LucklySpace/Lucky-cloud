package com.xy.lucky.database.web.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xy.lucky.database.web.mapper.ImStickerPackMapper;
import com.xy.lucky.domain.po.ImStickerPackPo;
import com.xy.lucky.rpc.api.database.sticker.StickerPackDubboService;
import lombok.RequiredArgsConstructor;
import org.apache.dubbo.config.annotation.DubboService;

import java.util.List;

@DubboService
@RequiredArgsConstructor
public class ImStickerPackService extends ServiceImpl<ImStickerPackMapper, ImStickerPackPo>
        implements StickerPackDubboService {

    @Override
    public ImStickerPackPo queryOne(String packId) {
        return super.getById(packId);
    }

    @Override
    public ImStickerPackPo queryByCode(String code) {
        return super.getOne(new LambdaQueryWrapper<ImStickerPackPo>().eq(ImStickerPackPo::getCode, code));
    }

    @Override
    public List<ImStickerPackPo> queryList() {
        return super.list();
    }

    @Override
    public Boolean creat(ImStickerPackPo packPo) {
        return super.save(packPo);
    }

    @Override
    public Boolean modify(ImStickerPackPo packPo) {
        return super.updateById(packPo);
    }

    @Override
    public Boolean removeOne(String packId) {
        return super.removeById(packId);
    }
}
