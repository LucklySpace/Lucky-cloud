package com.xy.lucky.rpc.api.database.sticker;

import com.xy.lucky.domain.po.ImStickerPo;

import java.util.List;

public interface ImStickerDubboService {

    ImStickerPo queryOne(String stickerId);

    List<ImStickerPo> queryListByPackId(String packId);

    Boolean creat(ImStickerPo imStickerPo);

    Boolean modify(ImStickerPo imStickerPo);

    Boolean removeOne(String stickerId);
}
