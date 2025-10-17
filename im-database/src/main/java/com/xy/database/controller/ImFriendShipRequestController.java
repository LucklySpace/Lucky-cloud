package com.xy.database.controller;


import com.xy.database.security.SecurityInner;
import com.xy.database.service.ImFriendshipRequestService;
import com.xy.domain.po.ImFriendshipRequestPo;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

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
    @GetMapping("/selectList")
    public List<ImFriendshipRequestPo> selectList(@RequestParam("userId") String userId) {
        return imFriendshipRequestService.selectList(userId);
    }

    /**
     * 获取好友请求
     *
     * @param requestPo 请求
     * @return
     */
    @PostMapping("/selectOne")
    public ImFriendshipRequestPo selectOne(@RequestBody ImFriendshipRequestPo requestPo) {
        return imFriendshipRequestService.selectOne(requestPo);
    }
    
    /**
     * 插入好友请求
     *
     * @param requestPo 好友请求
     * @return 是否插入成功
     */
    @PostMapping("/insert")
    public Boolean insert(@RequestBody ImFriendshipRequestPo requestPo) {
        return imFriendshipRequestService.insert(requestPo);
    }
    
    /**
     * 批量插入好友请求
     *
     * @param requestPoList 好友请求列表
     * @return 是否插入成功
     */
    @PostMapping("/batchInsert")
    public Boolean batchInsert(@RequestBody List<ImFriendshipRequestPo> requestPoList) {
        return imFriendshipRequestService.batchInsert(requestPoList);
    }
    
    /**
     * 更新好友请求
     *
     * @param requestPo 好友请求
     * @return 是否更新成功
     */
    @PutMapping("/update")
    public Boolean update(@RequestBody ImFriendshipRequestPo requestPo) {
        return imFriendshipRequestService.update(requestPo);
    }
    
    /**
     * 删除好友请求
     *
     * @param requestId 请求ID
     * @return 是否删除成功
     */
    @DeleteMapping("/deleteById")
    public Boolean deleteById(@RequestParam("requestId") String requestId) {
        return imFriendshipRequestService.deleteById(requestId);
    }
}