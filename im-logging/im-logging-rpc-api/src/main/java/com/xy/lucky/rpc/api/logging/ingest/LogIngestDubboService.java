package com.xy.lucky.rpc.api.logging.ingest;

import com.xy.lucky.rpc.api.logging.vo.LogRecordVo;

import java.util.List;

/**
 * 日志采集Dubbo服务接口
 */
public interface LogIngestDubboService {

    /**
     * 单条日志入库
     *
     * @param record 日志记录
     */
    void ingest(LogRecordVo record);

    /**
     * 批量日志入库
     *
     * @param records 日志记录列表
     */
    void ingestBatch(List<LogRecordVo> records);
}
