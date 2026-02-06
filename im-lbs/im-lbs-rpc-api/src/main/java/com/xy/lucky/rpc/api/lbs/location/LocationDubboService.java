package com.xy.lucky.rpc.api.lbs.location;

import com.xy.lucky.rpc.api.lbs.dto.LocationUpdateDto;
import com.xy.lucky.rpc.api.lbs.dto.NearbySearchDto;
import com.xy.lucky.rpc.api.lbs.vo.LocationVo;

import java.util.List;

/**
 * 位置服务Dubbo接口
 */
public interface LocationDubboService {
    /**
     * 上报用户位置
     */
    void updateLocation(String userId, LocationUpdateDto dto);

    /**
     * 搜索附近用户
     */
    List<LocationVo> searchNearby(String userId, NearbySearchDto dto);

    /**
     * 清理不活跃用户位置缓存
     */
    void clearInactiveUsers();
}
