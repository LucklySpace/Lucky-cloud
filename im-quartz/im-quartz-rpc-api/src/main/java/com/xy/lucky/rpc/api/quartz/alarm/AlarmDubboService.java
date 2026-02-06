package com.xy.lucky.rpc.api.quartz.alarm;

import com.xy.lucky.rpc.api.quartz.dto.AlarmSendDto;
import org.apache.dubbo.common.constants.LoadbalanceRules;
import org.apache.dubbo.config.annotation.DubboService;

import java.util.List;

/**
 * 告警服务 Dubbo 服务接口
 *
 * @author Lucky
 */
@DubboService(
        interfaceClass = AlarmDubboService.class,
        loadbalance = LoadbalanceRules.ROUND_ROBIN
)
public interface AlarmDubboService {

    /**
     * 发送单个告警
     *
     * @param alarmSendDto 告警信息
     * @return 是否发送成功
     */
    Boolean sendAlarm(AlarmSendDto alarmSendDto);

    /**
     * 批量发送告警
     *
     * @param emails  接收人邮箱列表
     * @param subject 主题
     * @param content 内容
     * @return 成功发送的数量
     */
    Integer batchSendAlarm(List<String> emails, String subject, String content);

    /**
     * 发送任务失败告警
     *
     * @param taskName 任务名称
     * @param errorMsg 错误信息
     * @param emails   接收人邮箱列表
     * @return 是否发送成功
     */
    Boolean sendTaskFailureAlarm(String taskName, String errorMsg, List<String> emails);

    /**
     * 发送任务超时告警
     *
     * @param taskName       任务名称
     * @param timeoutSeconds 超时时间(秒)
     * @param emails         接收人邮箱列表
     * @return 是否发送成功
     */
    Boolean sendTaskTimeoutAlarm(String taskName, Long timeoutSeconds, List<String> emails);
}
