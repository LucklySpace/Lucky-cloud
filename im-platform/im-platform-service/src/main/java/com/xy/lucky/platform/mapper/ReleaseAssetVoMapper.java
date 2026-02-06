package com.xy.lucky.platform.mapper;


import com.xy.lucky.platform.domain.po.AssetPo;
import com.xy.lucky.platform.domain.po.ReleasePo;
import com.xy.lucky.platform.domain.vo.AssetVo;
import com.xy.lucky.platform.domain.vo.ReleaseVo;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * 映射发布信息
 */
@Mapper(componentModel = "spring")
public interface ReleaseAssetVoMapper {

    /**
     * 转换为VO
     *
     * @param entity 实体对象
     * @return VO对象
     */
    @Mapping(target = "releaseId", source = "id")
    ReleaseVo toVo(ReleasePo entity);

    /**
     * 映射资产信息
     *
     * @param entity 资产实体对象
     * @return 资产VO对象
     */
    AssetVo toVo(AssetPo entity);
}
