package com.xy.database.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xy.database.mapper.ImUserDataMapper;
import com.xy.database.service.ImUserDataService;
import com.xy.domain.po.ImUserDataPo;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ImUserDataServiceImpl extends ServiceImpl<ImUserDataMapper, ImUserDataPo>
        implements ImUserDataService {
        
    @Override
    public boolean insert(ImUserDataPo userDataPo) {
        return this.save(userDataPo);
    }
    
    @Override
    public boolean batchInsert(List<ImUserDataPo> userDataPoList) {
        return this.saveBatch(userDataPoList);
    }
    
    @Override
    public ImUserDataPo selectOne(String id) {
        return this.getById(id);
    }
    
    @Override
    public ImUserDataPo selectById(String id) {
        return this.getById(id);
    }
    
    @Override
    public List<ImUserDataPo> selectList() {
        return this.list();
    }
    
    @Override
    public boolean update(ImUserDataPo userDataPo) {
        return this.updateById(userDataPo);
    }
    
    @Override
    public boolean deleteById(String id) {
        return this.removeById(id);
    }
}