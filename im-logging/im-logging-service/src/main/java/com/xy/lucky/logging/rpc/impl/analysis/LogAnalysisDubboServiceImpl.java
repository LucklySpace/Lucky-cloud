package com.xy.lucky.logging.rpc.impl.analysis;

import com.xy.lucky.logging.service.LogAnalysisService;
import com.xy.lucky.rpc.api.logging.analysis.LogAnalysisDubboService;
import com.xy.lucky.rpc.api.logging.vo.LogRecordVo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboService;

import java.util.Map;

/**
 * 日志分析Dubbo服务实现
 */
@Slf4j
@DubboService
@RequiredArgsConstructor
public class LogAnalysisDubboServiceImpl implements LogAnalysisDubboService {

    private final LogAnalysisService logAnalysisService;

    @Override
    public void aggregate(Object record) {
        if (record instanceof LogRecordVo) {
            logAnalysisService.aggregate((LogRecordVo) record);
        } else {
            log.warn("Unsupported record type for aggregation: {}", record != null ? record.getClass() : "null");
        }
    }

    @Override
    public Map<String, Object> overview() {
        return logAnalysisService.overview();
    }

    @Override
    public Map<String, Long> hourlySeries(String level, int hours) {
        return logAnalysisService.hourlySeries(level, hours);
    }
}
