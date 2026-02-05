package com.xy.lucky.database.api.emoji;

import com.xy.lucky.domain.po.ImUserEmojiPackPo;

import java.util.List;

public interface ImUserEmojiPackDubboService {

    List<ImUserEmojiPackPo> listByUserId(String userId);

    List<String> listPackIds(String userId);

    Boolean bindPack(String userId, String packId);

    Boolean bindPacks(String userId, List<String> packIds);

    Boolean unbindPack(String userId, String packId);
}

