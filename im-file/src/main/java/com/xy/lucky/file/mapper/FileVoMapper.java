package com.xy.lucky.file.mapper;


import com.xy.lucky.file.domain.OssFileUploadProgress;
import com.xy.lucky.file.domain.po.OssFileImagePo;
import com.xy.lucky.file.domain.po.OssFilePo;
import com.xy.lucky.file.domain.vo.FileUploadProgressVo;
import com.xy.lucky.file.domain.vo.FileVo;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface FileVoMapper {

    @Mapping(target = "name", source = "fileName")
    @Mapping(target = "path", source = "path")
    FileVo toVo(OssFilePo entity);

    @Mapping(target = "name", source = "fileName")
    @Mapping(target = "path", source = "path")
    @Mapping(target = "thumbnailPath", source = "thumbnailPath")
    FileVo toVo(OssFileImagePo entity);


    FileUploadProgressVo toVo(OssFileUploadProgress entity);
}
