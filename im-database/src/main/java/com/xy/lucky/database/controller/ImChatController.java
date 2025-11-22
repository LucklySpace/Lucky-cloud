package com.xy.lucky.database.controller;


import com.xy.lucky.database.security.SecurityInner;
import com.xy.lucky.database.service.ImChatService;
import com.xy.lucky.domain.po.ImChatPo;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/{version}/database/chat")
@Tag(name = "ImChat", description = "用户会话数据库接口")
public class ImChatController {

    @Resource
    private ImChatService imChatService;

    /**
     * 查询用户所有会话
     *
     * @param ownerId  所属用户id
     * @param sequence 时序
     */
    @GetMapping("/selectList")
    public List<ImChatPo> selectList(@RequestParam("ownerId") String ownerId, @RequestParam("sequence") Long sequence) {
        return imChatService.selectList(ownerId, sequence);
    }

    /**
     * 查询会话
     *
     * @param ownerId  所属人
     * @param toId     会话对象
     * @param chatType 会话类型
     * @return 会话信息
     */
    @GetMapping("/selectOne")
    public ImChatPo selectOne(@RequestParam("ownerId") String ownerId, @RequestParam("toId") String toId, @RequestParam(value = "chatType", required = false) Integer chatType) {
        return imChatService.selectOne(ownerId, toId, chatType);
    }

    /**
     * 插入会话信息
     *
     * @param chatPo 会话信息
     */
    @PostMapping("/insert")
    public Boolean insert(@RequestBody ImChatPo chatPo) {
        return imChatService.insert(chatPo);
    }

    /**
     * 更新会话信息
     *
     * @param chatPo 会话信息
     */
    @SecurityInner
    @PutMapping("/update")
    public Boolean update(@RequestBody ImChatPo chatPo) {
        return imChatService.update(chatPo);
    }

    /**
     * 删除会话信息
     *
     * @param id 会话id
     */
    @DeleteMapping("/deleteById")
    public Boolean deleteById(@RequestParam("id") String id) {
        return imChatService.deleteById(id);
    }

}