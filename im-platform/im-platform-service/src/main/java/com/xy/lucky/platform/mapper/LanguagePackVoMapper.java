package com.xy.lucky.platform.mapper;

import com.xy.lucky.platform.domain.po.LanguagePackPo;
import com.xy.lucky.platform.domain.vo.LanguagePackVo;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * 语言包映射器
 */
@Mapper(componentModel = "spring")
public interface LanguagePackVoMapper {

    @Mapping(target = "createTime", expression = "java(entity.getCreateTime() == null ? null : entity.getCreateTime().toString())")
    LanguagePackVo toVo(LanguagePackPo entity);
}

