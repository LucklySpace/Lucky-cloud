package com.xy.lucky.business.service.impl;

import com.xy.lucky.business.domain.mapper.StickerBeanMapper;
import com.xy.lucky.business.domain.vo.StickerRespVo;
import com.xy.lucky.business.domain.vo.StickerVo;
import com.xy.lucky.business.exception.StickerException;
import com.xy.lucky.business.service.UserStickerService;
import com.xy.lucky.domain.po.ImStickerPackPo;
import com.xy.lucky.domain.po.ImStickerPo;
import com.xy.lucky.rpc.api.database.sticker.ImStickerDubboService;
import com.xy.lucky.rpc.api.database.sticker.ImStickerPackDubboService;
import com.xy.lucky.rpc.api.database.sticker.ImUserStickerPackDubboService;
import com.xy.lucky.rpc.api.oss.file.FileDubboService;
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
    private ImStickerDubboService imStickerDubboService;
    @DubboReference
    private ImStickerPackDubboService imStickerPackDubboService;
    @DubboReference
    private FileDubboService fileDubboService;

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

        ImStickerPackPo imStickerPackPo = Optional.of(imStickerPackDubboService.queryOne(packId))
                .orElseThrow(() -> new StickerException("表情包不存在"));

        StickerRespVo vo = stickerBeanMapper.toRespVo(imStickerPackPo);

        List<ImStickerPo> imStickerPos = imStickerDubboService.queryListByPackId(packId);

        // 获取表情包图片的预签名URL
        imStickerPos.forEach(imStickerPo -> imStickerPo.setUrl(fileDubboService.getPresignedPutUrl(imStickerPo.getBucket(), imStickerPo.getObjectKey(), 60 * 10 * 1000)));

        vo.setStickers(imStickerPos.stream().map(stickerBeanMapper::toVo).toList());

        return vo;
    }

    /**
     * 查询表情包详情（按 stickerId）
     */
    @Override
    public StickerVo getStickerId(String stickerId) {

        ImStickerPo imStickerPo = Optional.of(imStickerDubboService.queryOne(stickerId))
                .orElseThrow(() -> new StickerException("表情不存在"));

        return stickerBeanMapper.toVo(imStickerPo);
    }
}

