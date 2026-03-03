package com.xy.lucky.chat.service;

import com.xy.lucky.chat.domain.vo.StickerRespVo;

import java.util.List;

public interface UserStickerService {

    List<String> listPackIds(String userId);

    Boolean bindPack(String userId, String packId);

    Boolean unbindPack(String userId, String packId);

    StickerRespVo getPackId(String packId);
}

