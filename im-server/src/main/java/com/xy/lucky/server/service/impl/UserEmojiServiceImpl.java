package com.xy.lucky.server.service.impl;

import com.xy.lucky.database.api.emoji.ImUserEmojiPackDubboService;
import com.xy.lucky.server.service.UserEmojiService;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
public class UserEmojiServiceImpl implements UserEmojiService {

    @DubboReference
    private ImUserEmojiPackDubboService dubboService;

    @Override
    public List<String> listPackIds(String userId) {
        return dubboService.listPackIds(userId);
    }

    @Override
    public Boolean bindPack(String userId, String packId) {
        return dubboService.bindPack(userId, packId);
    }

    @Override
    public Boolean unbindPack(String userId, String packId) {
        return dubboService.unbindPack(userId, packId);
    }
}

