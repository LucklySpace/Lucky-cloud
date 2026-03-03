package com.xy.lucky.chat.domain.mapper;


import com.xy.lucky.chat.domain.vo.StickerRespVo;
import com.xy.lucky.chat.domain.vo.StickerVo;
import com.xy.lucky.domain.po.ImStickerPackPo;
import com.xy.lucky.domain.po.ImStickerPo;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Sticker 映射器
 */
@Mapper(componentModel = "spring")
public interface StickerBeanMapper {

    @Mapping(target = "packId", source = "id")
    StickerRespVo toRespVo(ImStickerPackPo entity);

    @Mapping(target = "stickerId", source = "id")
    StickerVo toVo(ImStickerPo entity);
}

