package com.xy.lucky.business.service;

import com.xy.lucky.business.domain.vo.StickerRespVo;
import com.xy.lucky.business.domain.vo.StickerVo;

import java.util.List;

public interface UserStickerService {

    List<String> listPackIds(String userId);

    Boolean bindPack(String userId, String packId);

    Boolean unbindPack(String userId, String packId);

    StickerVo getStickerId(String stickerId);

    StickerRespVo getPackId(String packId);
}

