package com.xy.lucky.database.controller;

import com.xy.lucky.database.security.SecurityInner;
import com.xy.lucky.database.service.ImGroupInviteRequestService;
import com.xy.lucky.domain.po.ImGroupInviteRequestPo;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@SecurityInner
@RestController
@RequestMapping("/api/{version}/database/group/invite")
@Tag(name = "ImFriendShip", description = "群邀请请求接口")
public class ImGroupInviteRequestController {

    @Resource
    private ImGroupInviteRequestService imGroupInviteRequestService;

    /**
     * 获取群邀请请求
     *
     * @param requestId
     * @return
     */
    @GetMapping("/selectOne")
    public ImGroupInviteRequestPo getOne(@RequestParam("requestId") String requestId) {
        return imGroupInviteRequestService.getById(requestId);
    }

    /**
     * 保存或更新群邀请请求
     *
     * @param requestPo
     * @return
     */
    @PostMapping("/saveOrUpdate")
    public Boolean saveOrUpdate(@RequestBody ImGroupInviteRequestPo requestPo) {
        return imGroupInviteRequestService.saveOrUpdate(requestPo);
    }

    @PostMapping("/saveOrUpdate/batch")
    public Boolean saveOrUpdateBatch(@RequestBody List<ImGroupInviteRequestPo> requestPoList) {
        return imGroupInviteRequestService.saveOrUpdateBatch(requestPoList);
    }

    /**
     * 删除群邀请请求
     *
     * @param requestId
     * @return
     */
    @GetMapping("/deleteById")
    public Boolean deleteById(@RequestParam("requestId") String requestId) {
        return imGroupInviteRequestService.deleteById(requestId);
    }

}
