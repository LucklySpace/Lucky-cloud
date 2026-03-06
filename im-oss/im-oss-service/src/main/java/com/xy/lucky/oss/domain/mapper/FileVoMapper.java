package com.xy.lucky.oss.domain.mapper;


import com.xy.lucky.oss.domain.OssFileUploadProgress;
import com.xy.lucky.oss.domain.po.OssFileImagePo;
import com.xy.lucky.oss.domain.po.OssFilePo;
import com.xy.lucky.oss.domain.vo.FileUploadProgressVo;
import com.xy.lucky.oss.domain.vo.FileVo;
import com.xy.lucky.oss.domain.vo.ImageVo;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface FileVoMapper {

    @Mapping(target = "name", source = "fileName")
    @Mapping(target = "key", source = "identifier")
    @Mapping(target = "type", source = "fileType")
    @Mapping(target = "size", source = "fileSize")
    FileVo toVo(OssFilePo entity);

    @Mapping(target = "name", source = "fileName")
    @Mapping(target = "key", source = "identifier")
    @Mapping(target = "type", source = "fileType")
    @Mapping(target = "size", source = "fileSize")
    @Mapping(target = "suffix", source = "suffix")
    ImageVo toVo(OssFileImagePo entity);


    FileUploadProgressVo toVo(OssFileUploadProgress entity);
}
