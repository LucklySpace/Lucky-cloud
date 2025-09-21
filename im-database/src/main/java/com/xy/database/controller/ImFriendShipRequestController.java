package com.xy.database.controller;


import com.xy.database.security.SecurityInner;
import com.xy.database.service.ImFriendshipRequestService;
import com.xy.domain.po.ImFriendshipRequestPo;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@SecurityInner
@RestController
@RequestMapping("/api/{version}/database/friend/request")
@Tag(name = "ImFriendShipRequest", description = "好友关系数据库接口")
@RequiredArgsConstructor
public class ImFriendShipRequestController {

    private final ImFriendshipRequestService imFriendshipRequestService;

    /**
     * 查询添加好友请求
     *
     * @param userId
     * @return
     */
    @GetMapping("/list")
    public List<ImFriendshipRequestPo> list(@RequestParam("userId") String userId) {
        return imFriendshipRequestService.list(userId);
    }
}
