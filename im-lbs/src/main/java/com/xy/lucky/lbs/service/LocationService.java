package com.xy.lucky.lbs.service;

import com.xy.lucky.lbs.domain.dto.LocationUpdateDto;
import com.xy.lucky.lbs.domain.dto.NearbySearchDto;
import com.xy.lucky.lbs.domain.vo.LocationVo;

import java.util.List;

public interface LocationService {
    /**
     * 上报用户位置
     *
     * @param userId 用户ID
     * @param dto    位置数据
     */
    void updateLocation(String userId, LocationUpdateDto dto);

    /**
     * 搜索附近用户
     *
     * @param userId 当前用户ID
     * @param dto    搜索参数
     * @return 附近用户列表
     */
    List<LocationVo> searchNearby(String userId, NearbySearchDto dto);

    /**
     * 清理不活跃用户位置缓存
     */
    void clearInactiveUsers();
}
