package com.xy.lucky.platform.mapper;

import com.xy.lucky.platform.domain.po.EmojiPackPo;
import com.xy.lucky.platform.domain.po.EmojiPo;
import com.xy.lucky.platform.domain.vo.EmojiPackVo;
import com.xy.lucky.platform.domain.vo.EmojiVo;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Emoji 映射器
 */
@Mapper(componentModel = "spring")
public interface EmojiVoMapper {

    @Mapping(target = "packId", source = "id")
    EmojiPackVo toVo(EmojiPackPo entity);

    @Mapping(target = "emojiId", source = "id")
    EmojiVo toVo(EmojiPo entity);
}

