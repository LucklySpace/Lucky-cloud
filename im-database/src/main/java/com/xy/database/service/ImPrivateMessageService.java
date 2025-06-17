package com.xy.database.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.xy.domain.po.ImPrivateMessagePo;

import java.util.List;


/**
 * @author dense
 * @description 针对表【im_private_message】的数据库操作Service
 * @createDate 2024-03-28 23:00:15
 */
public interface ImPrivateMessageService extends IService<ImPrivateMessagePo> {

    List<ImPrivateMessagePo> list(String userId, Long sequence);
}
