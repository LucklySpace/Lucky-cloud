package com.xy.database.controller;


import com.xy.database.service.ImFriendshipService;
import com.xy.domain.po.ImFriendshipPo;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/{version}/database/friend")
@Tag(name = "ImFriendship", description = "用户好友数据库接口")
@RequiredArgsConstructor
public class ImFriendshipController {

    private final ImFriendshipService imFriendshipService;

    /**
     * 查询所有好友
     */
    @GetMapping("/list")
    public List<ImFriendshipPo> list(@RequestParam("ownerId") String ownerId) {
        return imFriendshipService.list(ownerId);
    }

    @GetMapping("getOne")
    public ImFriendshipPo getOne(@RequestParam("ownerId") String ownerId, @RequestParam("friendId") String friendId) {
        return imFriendshipService.getOne(ownerId, friendId);
    }

}
