package com.xy.lucky.rpc.api.logging.query;

import com.xy.lucky.rpc.api.logging.dto.LogQueryDto;
import com.xy.lucky.rpc.api.logging.dto.PageResult;
import com.xy.lucky.rpc.api.logging.enums.LogLevel;
import com.xy.lucky.rpc.api.logging.vo.LogRecordVo;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 日志查询Dubbo服务接口
 */
public interface LogQueryDubboService {

    /**
     * 查询日志（分页）
     *
     * @param module  模块名
     * @param start   开始时间
     * @param end     结束时间
     * @param level   日志级别
     * @param service 服务名
     * @param env     环境
     * @param page    页码（从1开始）
     * @param size    每页大小
     * @param keyword 关键字
     * @return 分页日志结果
     */
    PageResult<LogRecordVo> query(String module, LocalDateTime start, LocalDateTime end,
                                  LogLevel level, String service, String env,
                                  int page, int size, String keyword);

    /**
     * 根据查询条件查询日志（分页）
     *
     * @param queryDto 查询条件
     * @return 分页日志结果
     */
    PageResult<LogRecordVo> query(LogQueryDto queryDto);

    /**
     * 查询时间间隔内日志数量（直方图统计）
     *
     * @param module   模块名
     * @param start    开始时间
     * @param end      结束时间
     * @param level    日志级别
     * @param service  服务名
     * @param env      环境
     * @param keyword  关键字
     * @param interval 时间间隔（hour/minute）
     * @return 时间间隔内日志数量
     */
    Map<String, Long> histogram(String module, LocalDateTime start, LocalDateTime end,
                                LogLevel level, String service, String env,
                                String keyword, String interval);

    /**
     * 获取服务列表
     *
     * @param env 环境
     * @return 服务列表
     */
    List<String> listServices(String env);

    /**
     * 获取热门服务列表（按日志数量排序）
     *
     * @param start 开始时间
     * @param end   结束时间
     * @param limit 限制数量
     * @return 服务列表（包含服务名和日志数量）
     */
    List<Map<String, Object>> topServices(LocalDateTime start, LocalDateTime end, int limit);

    /**
     * 获取热门地址列表（按日志数量排序）
     *
     * @param start 开始时间
     * @param end   结束时间
     * @param limit 限制数量
     * @return 地址列表（包含地址和日志数量）
     */
    List<Map<String, Object>> topAddresses(LocalDateTime start, LocalDateTime end, int limit);

    /**
     * 获取热门错误类型列表（按数量排序）
     *
     * @param start 开始时间
     * @param end   结束时间
     * @param limit 限制数量
     * @return 错误类型列表（包含错误类型和数量）
     */
    List<Map<String, Object>> topErrorTypes(LocalDateTime start, LocalDateTime end, int limit);

    /**
     * 删除指定时间之前的日志
     *
     * @param cutoff 截止时间
     */
    void deleteBefore(LocalDateTime cutoff);

    /**
     * 删除指定模块在指定时间之前的日志
     *
     * @param module 模块名
     * @param cutoff 截止时间
     */
    void deleteModuleBefore(String module, LocalDateTime cutoff);
}
