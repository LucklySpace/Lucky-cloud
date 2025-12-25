package com.xy.lucky.logging.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SearchResponseVo {
    private List<LogRecordVo> hits;
    private long total;
}
