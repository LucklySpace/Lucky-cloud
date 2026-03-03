package com.xy.lucky.database.web.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xy.lucky.domain.po.ImUserStickerPackPo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ImUserStickerPackMapper extends BaseMapper<ImUserStickerPackPo> {

    List<ImUserStickerPackPo> selectByUserId(@Param("userId") String userId);

    List<String> selectPackIdsByUserId(@Param("userId") String userId);
}

