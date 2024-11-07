package com.xy.server.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xy.server.domain.po.ImPrivateMessagePo;
import com.xy.server.mapper.ImPrivateMessageMapper;
import com.xy.server.service.ImPrivateMessageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * @author dense
 * @description 针对表【im_private_message】的数据库操作Service实现
 * @createDate 2024-03-28 23:00:15
 */
@Slf4j(topic = "single")
@Service
public class ImPrivateMessageServiceImpl extends ServiceImpl<ImPrivateMessageMapper, ImPrivateMessagePo>
        implements ImPrivateMessageService {

}




