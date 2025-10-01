package com.xy.file.domain;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

@Data
@Builder
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OssFileUploadResponse {

    private String minIoUrl;

    private String nginxUrl;

}
