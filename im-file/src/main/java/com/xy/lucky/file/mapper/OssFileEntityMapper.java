package com.xy.lucky.file.mapper;

import com.xy.lucky.file.domain.OssFile;
import com.xy.lucky.file.domain.po.OssFilePo;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface OssFileEntityMapper {

    @Mapping(target = "id", ignore = true)
    OssFilePo toEntity(OssFile source);

    OssFile toDomain(OssFilePo entity);
}
