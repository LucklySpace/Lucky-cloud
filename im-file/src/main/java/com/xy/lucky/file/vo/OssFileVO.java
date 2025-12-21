package com.xy.lucky.file.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OssFileVO {
    private Long id;
    private String bucketName;
    private String identifier;
    private String fileName;
    private String fileType;
    private String objectKey;
    private String contentType;
    private Long fileSize;
    private Integer isFinish;
    private String path;
}
