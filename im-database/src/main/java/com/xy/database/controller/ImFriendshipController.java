package com.xy.database.controller;


import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.xy.database.service.ImFriendshipService;
import com.xy.domain.po.ImChatPo;
import com.xy.domain.po.ImFriendshipPo;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Objects;

@Slf4j
@RestController
@RequestMapping("/api/{version}/database/friend")
@Tag(name = "ImFriendship", description = "用户好友数据库接口")
@RequiredArgsConstructor
public class ImFriendshipController {

    private final ImFriendshipService imFriendshipService;


    /**
     * 查询所有好友
     *
     */
    @GetMapping("/list")
    public List<ImFriendshipPo> list(@RequestParam("ownerId") String ownerId, @RequestParam("sequence") Long sequence) {
        return imFriendshipService.list(ownerId,sequence);
    }

}
