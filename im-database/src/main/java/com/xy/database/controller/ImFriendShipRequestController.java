package com.xy.database.controller;


import com.xy.database.security.SecurityInner;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@SecurityInner
@RestController
@RequestMapping("/api/{version}/database/friend/ship")
@Tag(name = "ImFriendShipRequest", description = "好友关系数据库接口")
@RequiredArgsConstructor
public class ImFriendShipRequestController {
}
