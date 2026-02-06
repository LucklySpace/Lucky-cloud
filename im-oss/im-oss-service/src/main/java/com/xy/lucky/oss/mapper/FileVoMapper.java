package com.xy.lucky.oss.mapper;


import com.xy.lucky.oss.domain.OssFileUploadProgress;
import com.xy.lucky.oss.domain.po.OssFileImagePo;
import com.xy.lucky.oss.domain.po.OssFilePo;
import com.xy.lucky.oss.domain.vo.FileUploadProgressVo;
import com.xy.lucky.oss.domain.vo.FileVo;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface FileVoMapper {

    @Mapping(target = "name", source = "fileName")
    @Mapping(target = "path", source = "path")
    @Mapping(target = "type", source = "fileType")
    @Mapping(target = "size", source = "fileSize")
    FileVo toVo(OssFilePo entity);

    @Mapping(target = "name", source = "fileName")
    @Mapping(target = "path", source = "path")
    @Mapping(target = "type", source = "fileType")
    @Mapping(target = "size", source = "fileSize")
    @Mapping(target = "thumbnailPath", source = "thumbnailPath")
    FileVo toVo(OssFileImagePo entity);


    FileUploadProgressVo toVo(OssFileUploadProgress entity);
}
