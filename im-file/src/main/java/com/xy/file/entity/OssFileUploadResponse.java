package com.xy.file.entity;

import lombok.*;

@Data
@Builder
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class OssFileUploadResponse {

    private String minIoUrl;

    private String nginxUrl;

}
