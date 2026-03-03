package com.xy.lucky.chat.service.impl;

import com.xy.lucky.chat.domain.mapper.StickerBeanMapper;
import com.xy.lucky.chat.domain.vo.StickerRespVo;
import com.xy.lucky.chat.domain.vo.StickerVo;
import com.xy.lucky.chat.exception.StickerException;
import com.xy.lucky.chat.service.UserStickerService;
import com.xy.lucky.domain.po.ImStickerPackPo;
import com.xy.lucky.domain.po.ImStickerPo;
import com.xy.lucky.rpc.api.database.sticker.ImUserStickerPackDubboService;
import com.xy.lucky.rpc.api.database.sticker.StickerDubboService;
import com.xy.lucky.rpc.api.database.sticker.StickerPackDubboService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserStickerServiceImpl implements UserStickerService {

    private final StickerBeanMapper stickerBeanMapper;
    @DubboReference
    private ImUserStickerPackDubboService dubboService;
    @DubboReference
    private StickerDubboService stickerDubboService;
    @DubboReference
    private StickerPackDubboService stickerPackDubboService;

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

    /**
     * 查询表情包详情（按 packId）
     */
    @Override
    public StickerRespVo getPackId(String packId) {

        ImStickerPackPo imStickerPackPo = Optional.of(stickerPackDubboService.queryOne(packId))
                .orElseThrow(() -> new StickerException("表情包不存在"));

        StickerRespVo vo = stickerBeanMapper.toRespVo(imStickerPackPo);

        List<StickerVo> list = stickerDubboService.queryListByPackId(packId)
                .stream().map(stickerBeanMapper::toVo).toList();

        Optional.of(list).ifPresent(vo::setStickers);

        return vo;
    }


    /**
     * 查询表情包详情（按 stickerId）
     */
    @Override
    public StickerVo getStickerId(String stickerId) {

        ImStickerPo imStickerPo = Optional.of(stickerDubboService.queryOne(stickerId))
                .orElseThrow(() -> new StickerException("表情不存在"));

        return stickerBeanMapper.toVo(imStickerPo);
    }
}

