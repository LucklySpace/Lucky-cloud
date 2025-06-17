package com.xy.database.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xy.database.mapper.ImPrivateMessageMapper;
import com.xy.database.service.ImPrivateMessageService;
import com.xy.domain.po.ImPrivateMessagePo;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @author dense
 * @description 针对表【im_private_message】的数据库操作Service实现
 */
@Service
public class ImPrivateMessageServiceImpl extends ServiceImpl<ImPrivateMessageMapper, ImPrivateMessagePo>
        implements ImPrivateMessageService {


    @Resource
    private ImPrivateMessageMapper imPrivateMessageMapper;

    @Override
    public List<ImPrivateMessagePo> list(String userId, Long sequence) {
        return imPrivateMessageMapper.selectSingleMessage(userId, sequence);
    }
}




