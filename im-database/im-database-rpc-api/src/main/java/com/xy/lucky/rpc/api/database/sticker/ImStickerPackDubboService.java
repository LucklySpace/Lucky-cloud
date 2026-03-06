package com.xy.lucky.rpc.api.database.sticker;

import com.xy.lucky.domain.po.ImStickerPackPo;

import java.util.List;

public interface ImStickerPackDubboService {

    /**
     * 查询单个
     */
    ImStickerPackPo queryOne(String packId);

    ImStickerPackPo queryByCode(String code);

    List<ImStickerPackPo> queryList();

    Boolean creat(ImStickerPackPo packPo);

    Boolean modify(ImStickerPackPo packPo);

    Boolean removeOne(String packId);
}
