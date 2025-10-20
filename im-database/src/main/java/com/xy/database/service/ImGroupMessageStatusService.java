package com.xy.database.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xy.database.mapper.ImGroupMessageStatusMapper;
import com.xy.domain.po.ImGroupMessageStatusPo;
import org.springframework.stereotype.Service;

@Service
public class ImGroupMessageStatusService extends ServiceImpl<ImGroupMessageStatusMapper, ImGroupMessageStatusPo>
        implements IService<ImGroupMessageStatusPo> {

}




