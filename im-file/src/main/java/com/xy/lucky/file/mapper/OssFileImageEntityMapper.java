package com.xy.lucky.file.mapper;

import com.xy.lucky.file.domain.OssFileImage;
import com.xy.lucky.file.domain.po.OssFileImagePo;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface OssFileImageEntityMapper {
    @Mapping(target = "id", ignore = true)
    OssFileImagePo toEntity(OssFileImage source);

    OssFileImage toDomain(OssFileImagePo entity);
}
