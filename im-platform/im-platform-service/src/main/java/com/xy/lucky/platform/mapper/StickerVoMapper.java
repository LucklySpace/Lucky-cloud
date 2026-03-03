package com.xy.lucky.platform.mapper;

import com.xy.lucky.platform.domain.po.StickerPackPo;
import com.xy.lucky.platform.domain.po.StickerPo;
import com.xy.lucky.platform.domain.vo.StickerPackVo;
import com.xy.lucky.platform.domain.vo.StickerRespVo;
import com.xy.lucky.platform.domain.vo.StickerVo;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Sticker 映射器
 */
@Mapper(componentModel = "spring")
public interface StickerVoMapper {

    @Mapping(target = "packId", source = "id")
    StickerPackVo toVo(StickerPackPo entity);

    @Mapping(target = "packId", source = "id")
    StickerRespVo toRespVo(StickerPackPo entity);

    @Mapping(target = "stickerId", source = "id")
    StickerVo toVo(StickerPo entity);
}

