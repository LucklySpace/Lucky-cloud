package com.xy.server.api.database.message;


import com.xy.domain.po.ImGroupMessagePo;
import com.xy.domain.po.ImGroupMessageStatusPo;
import com.xy.domain.po.ImSingleMessagePo;
import com.xy.server.api.FeignRequestInterceptor;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/**
 * 消息操作数据库远程调用接口
 */
@FeignClient(contextId = "message", value = "im-database", path = "/api/v1/database", configuration = FeignRequestInterceptor.class)
public interface ImMessageFeign {

    /**
     * 根据id查询私聊消息
     *
     * @param messageId 消息id
     * @return 私聊消息
     */
    @GetMapping("/single/message/getById")
    ImSingleMessagePo getSingleMessageById(@RequestParam("messageId") String messageId);

    /**
     * 根据id查询群聊消息
     *
     * @param messageId 群聊消息id
     * @return 群聊消息
     */
    @GetMapping("/group/message/getById")
    ImGroupMessagePo getGroupMessageById(@RequestParam("messageId") String messageId);

    /**
     * 获取用户私聊消息
     *
     * @param userId   用户id
     * @param sequence 序列号
     * @return 用户私聊消息
     */
    @GetMapping("/single/message/list")
    List<ImSingleMessagePo> getSingleMessageList(@RequestParam("userId") String userId, @RequestParam("sequence") Long sequence);


    /**
     * 获取用户群聊消息
     *
     * @param userId   用户id
     * @param sequence 序列号
     * @return 用户群聊消息
     */
    @GetMapping("/group/message/list")
    List<ImGroupMessagePo> getGroupMessageList(@RequestParam("userId") String userId, @RequestParam("sequence") Long sequence);

    /**
     * 插入或更新私聊消息
     *
     * @param messagePo 私聊消息
     */
    @PostMapping("/single/message/saveOrUpdate")
    Boolean singleMessageSaveOrUpdate(@RequestBody ImSingleMessagePo messagePo);

    /**
     * 插入或更新群聊消息
     *
     * @param update 群聊消息
     * @return 是否成功
     */
    @PostMapping("/group/message/saveOrUpdate")
    Boolean groupMessageSaveOrUpdate(@RequestBody ImGroupMessagePo update);

    /**
     * 获取最后一条消息
     *
     * @param fromId 发送者id
     * @param toId   接受者id
     * @return 私聊消息
     */
    @GetMapping("/single/message/last")
    ImSingleMessagePo plast(@RequestParam("fromId") String fromId, @RequestParam("toId") String toId);

    /**
     * 获取最后一条消息
     *
     * @param userId  用户id
     * @param groupId 群聊id
     * @return 私聊消息
     */
    @GetMapping("/group/message/last")
    ImGroupMessagePo glast(@RequestParam("userId") String userId, @RequestParam("groupId") String groupId);


    /**
     * 批量插入群聊消息状态
     *
     * @param groupReadStatusList 群聊消息群成员阅读状态集合
     */
    @PostMapping("/group/message/status/batch/insert")
    Boolean groupMessageStatusBatchInsert(@RequestBody List<ImGroupMessageStatusPo> groupReadStatusList);

    /**
     * 查询阅读状态
     *
     * @param fromId 发送者id
     * @param toId   接受者id
     * @param code   状态码
     * @return 未读条数
     */
    @GetMapping("/single/message/selectReadStatus")
    Integer pSelectReadStatus(@RequestParam("fromId") String fromId, @RequestParam("toId") String toId, @RequestParam("code") Integer code);


    /**
     * 批量查询阅读状态
     *
     * @param groupId 群聊id
     * @param toId    接受者id
     * @param code    状态码
     * @return 未读条数
     */
    @GetMapping("/group/message/selectReadStatus")
    Integer gSelectReadStatus(@RequestParam("groupId") String groupId, @RequestParam("toId") String toId, @RequestParam("code") Integer code);


}
