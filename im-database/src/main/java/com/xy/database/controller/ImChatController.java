package com.xy.database.controller;


import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.xy.database.security.SecurityInner;
import com.xy.database.service.ImChatService;
import com.xy.domain.po.ImChatPo;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Objects;

@Slf4j
@RestController
@RequestMapping("/api/{version}/database/chat")
@Tag(name = "ImChat", description = "用户会话数据库接口")
@RequiredArgsConstructor
public class ImChatController {

    private final ImChatService imChatService;


    /**
     * 查询会话
     *
     * @param ownerId  所属人
     * @param toId     会话对象
     * @param chatType 会话类型
     * @return 会话信息
     */
    @GetMapping("/getOne")
    public ImChatPo getOne(@RequestParam("ownerId") String ownerId, @RequestParam("toId") String toId, @RequestParam(value = "chatType", required = false) Integer chatType) {
        QueryWrapper<ImChatPo> query = new QueryWrapper<>();
        query.eq("owner_id", ownerId)
                .eq("to_id", toId);
        if (Objects.nonNull(chatType)) {
            query.eq("chat_type", chatType);
        }
        return imChatService.getOne(query);
    }

    /**
     * 查询用户所有会话
     *
     * @param ownerId  所属用户id
     * @param sequence 时序
     */
    @GetMapping("/list")
    public List<ImChatPo> list(@RequestParam("ownerId") String ownerId, @RequestParam("sequence") Long sequence) {
        return imChatService.list(ownerId, sequence);
    }


    /**
     * 插入会话信息
     *
     * @param chatPo 会话信息
     */
    @PostMapping("/insert")
    public Boolean insert(@RequestBody ImChatPo chatPo) {
        return imChatService.save(chatPo);
    }


    /**
     * 更新会话信息
     *
     * @param chatPo 会话信息
     */
    @SecurityInner
    @PutMapping("/updateById")
    public Boolean updateById(@RequestBody ImChatPo chatPo) {
        return imChatService.updateById(chatPo);
    }


    /**
     * 删除会话信息
     *
     * @param chatId 会话id
     */
    @DeleteMapping("/{chatId}")
    public Boolean deleteById(@PathVariable String chatId) {
        return imChatService.removeById(chatId);
    }

}
