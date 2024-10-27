package com.xy.server.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xy.server.mapper.ImGroupMapper;
import com.xy.server.model.ImGroup;
import com.xy.server.service.ImGroupService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @author dense
 * @description 针对表【im_group】的数据库操作Service实现
 * @createDate 2024-03-17 01:34:00
 */
@Service
public class ImGroupServiceImpl extends ServiceImpl<ImGroupMapper, ImGroup>
        implements ImGroupService {

    @Resource
    private ImGroupMapper imGroupMapper;


    @Override
    public List<String> selectNinePeople(String groupId) {
        return imGroupMapper.selectNinePeople(groupId);
    }
}




