package com.xy.lucky.rpc.api.quartz.task;

import com.xy.lucky.rpc.api.quartz.dto.TaskInfoDto;
import com.xy.lucky.rpc.api.quartz.dto.TaskQueryDto;
import com.xy.lucky.rpc.api.quartz.vo.TaskInfoVo;
import org.apache.dubbo.common.constants.LoadbalanceRules;
import org.apache.dubbo.config.annotation.DubboService;

import java.util.List;

/**
 * 任务管理 Dubbo 服务接口
 *
 * @author Lucky
 */
@DubboService(
        interfaceClass = TaskDubboService.class,
        loadbalance = LoadbalanceRules.ROUND_ROBIN
)
public interface TaskDubboService {

    /**
     * 添加任务
     *
     * @param taskInfoDto 任务信息
     * @return 任务ID
     */
    Long addTask(TaskInfoDto taskInfoDto);

    /**
     * 更新任务
     *
     * @param taskInfoDto 任务信息
     * @return 是否成功
     */
    Boolean updateTask(TaskInfoDto taskInfoDto);

    /**
     * 启动任务
     *
     * @param id 任务ID
     * @return 是否成功
     */
    Boolean startTask(Long id);

    /**
     * 停止任务
     *
     * @param id 任务ID
     * @return 是否成功
     */
    Boolean stopTask(Long id);

    /**
     * 删除任务
     *
     * @param id 任务ID
     * @return 是否成功
     */
    Boolean deleteTask(Long id);

    /**
     * 立即触发一次任务
     *
     * @param id 任务ID
     * @return 是否成功
     */
    Boolean triggerTask(Long id);

    /**
     * 查询所有任务
     *
     * @return 任务列表
     */
    List<TaskInfoVo> findAll();

    /**
     * 根据ID查询任务
     *
     * @param id 任务ID
     * @return 任务信息
     */
    TaskInfoVo findById(Long id);

    /**
     * 分页查询任务
     *
     * @param queryDto 查询条件
     * @return 任务列表
     */
    List<TaskInfoVo> findByPage(TaskQueryDto queryDto);

    /**
     * 根据任务名称查询
     *
     * @param jobName 任务名称
     * @return 任务信息
     */
    TaskInfoVo findByJobName(String jobName);

    /**
     * 批量启动任务
     *
     * @param ids 任务ID列表
     * @return 成功数量
     */
    Integer batchStart(List<Long> ids);

    /**
     * 批量停止任务
     *
     * @param ids 任务ID列表
     * @return 成功数量
     */
    Integer batchStop(List<Long> ids);

    /**
     * 暂停任务
     *
     * @param id 任务ID
     * @return 是否成功
     */
    Boolean pauseTask(Long id);

    /**
     * 恢复任务
     *
     * @param id 任务ID
     * @return 是否成功
     */
    Boolean resumeTask(Long id);
}
