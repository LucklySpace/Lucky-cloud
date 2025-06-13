package com.xy.server.api.database.chat;

import com.xy.domain.po.ImChatPo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 调用消息库
 */
@FeignClient(contextId = "chat", value = "im-database", path = "/api/v1/database/chat")
public interface ImChatFeign {


    /**
     * 查询某个会话
     *
     * @param ownerId  所属人
     * @param toId     会话对象
     * @param chatType 会话类型
     * @return 会话信息
     */
    @GetMapping("/getOne")
    ImChatPo getOne(@RequestParam("ownerId") String ownerId, @RequestParam("toId") String toId, @RequestParam(value = "chatType", required = false) Integer chatType);


    /**
     * 查询用户所有会话
     *
     * @param ownerId  所属用户id
     * @param sequence 时序
     */
    @GetMapping("/list")
    List<ImChatPo> getChatList(@RequestParam("ownerId") String ownerId, @RequestParam("sequence") Long sequence);


    /**
     * 插入会话信息
     *
     * @param chatPo 会话信息
     */
    @PostMapping("/insert")
    Boolean insert(@RequestBody ImChatPo chatPo);


    /**
     * 更新会话信息
     *
     * @param chatPo 会话信息
     */
    @PutMapping("/updateById")
    Boolean updateById(@RequestBody ImChatPo chatPo);

//    /**
//     * 删除会话信息
//     *
//     * @param chatId 会话id
//     */
//    @DeleteMapping("/{chatId}")
//    Boolean deleteById(@PathVariable String chatId);

}
