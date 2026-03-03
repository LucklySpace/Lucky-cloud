package com.xy.lucky.rpc.api.database.sticker;

import com.xy.lucky.domain.po.ImUserStickerPackPo;

import java.util.List;

public interface ImUserStickerPackDubboService {

    List<ImUserStickerPackPo> listByUserId(String userId);

    List<String> listPackIds(String userId);

    Boolean bindPack(String userId, String packId);

    Boolean bindPacks(String userId, List<String> packIds);

    Boolean unbindPack(String userId, String packId);
}

