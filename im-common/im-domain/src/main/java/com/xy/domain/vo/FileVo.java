package com.xy.domain.vo;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class FileVo {

    private String name;

    private String path;

}
