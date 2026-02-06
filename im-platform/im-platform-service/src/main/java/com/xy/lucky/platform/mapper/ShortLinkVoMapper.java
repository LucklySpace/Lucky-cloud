package com.xy.lucky.platform.mapper;

import com.xy.lucky.platform.domain.po.ShortLinkPo;
import com.xy.lucky.platform.domain.vo.ShortLinkVo;
import org.mapstruct.Mapper;

/**
 * 短链接VO映射器
 */
@Mapper(componentModel = "spring")
public interface ShortLinkVoMapper {

    ShortLinkVo toVo(ShortLinkPo entity);
}
