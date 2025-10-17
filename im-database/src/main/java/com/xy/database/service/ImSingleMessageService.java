package com.xy.database.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.xy.domain.po.ImSingleMessagePo;

import java.util.List;


/**
 * @author dense
 * @description 针对表【im_single_message】的数据库操作Service
 * @createDate 2024-03-28 23:00:15
 */
public interface ImSingleMessageService extends IService<ImSingleMessagePo> {

    List<ImSingleMessagePo> list(String userId, Long sequence);

    ImSingleMessagePo last(String fromId, String toId);

    Integer selectReadStatus(String fromId, String toId, Integer code);
}
