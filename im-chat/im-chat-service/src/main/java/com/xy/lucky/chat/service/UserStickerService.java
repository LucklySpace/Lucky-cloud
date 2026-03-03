package com.xy.lucky.chat.service;

import com.xy.lucky.chat.domain.vo.StickerRespVo;
import com.xy.lucky.chat.domain.vo.StickerVo;

import java.util.List;

public interface UserStickerService {

    List<String> listPackIds(String userId);

    Boolean bindPack(String userId, String packId);

    Boolean unbindPack(String userId, String packId);

    StickerVo getStickerId(String stickerId);

    StickerRespVo getPackId(String packId);
}

