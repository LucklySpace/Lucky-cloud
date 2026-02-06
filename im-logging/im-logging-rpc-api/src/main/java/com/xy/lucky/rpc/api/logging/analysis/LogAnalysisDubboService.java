package com.xy.lucky.rpc.api.logging.analysis;

import java.util.Map;

/**
 * 日志分析Dubbo服务接口
 */
public interface LogAnalysisDubboService {

    /**
     * 聚合日志统计
     * 更新Redis中的级别、模块和小时桶计数
     *
     * @param record 日志记录
     */
    void aggregate(Object record);

    /**
     * 获取日志统计概览
     *
     * @return 统计数据（包含各级别日志数量、模块统计等）
     */
    Map<String, Object> overview();

    /**
     * 获取小时级日志序列
     *
     * @param level 日志级别
     * @param hours 最近小时数
     * @return 时间序列数据（key:小时，value:数量）
     */
    Map<String, Long> hourlySeries(String level, int hours);
}
