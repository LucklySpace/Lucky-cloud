package com.xy.lucky.server.service;

import java.util.List;

public interface UserEmojiService {

    List<String> listPackIds(String userId);

    Boolean bindPack(String userId, String packId);

    Boolean unbindPack(String userId, String packId);
}

