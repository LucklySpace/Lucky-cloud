package com.xy.lucky.rpc.api.platform.shortlink;

import com.xy.lucky.rpc.api.platform.dto.ShortLinkDto;
import com.xy.lucky.rpc.api.platform.dto.ShortLinkRedirectDto;
import com.xy.lucky.rpc.api.platform.vo.ShortLinkVo;

/**
 * 短链 Dubbo 服务接口
 *
 * @author Lucky Platform
 * @since 1.0.0
 */
public interface ShortLinkDubboService {

    /**
     * 创建短链
     *
     * @param request 短链创建请求
     * @return 短链信息
     */
    ShortLinkVo createShortLink(ShortLinkDto request);

    /**
     * 重定向短链
     *
     * @param request 短链重定向请求
     * @return 短链信息（包含原始URL）
     */
    ShortLinkVo redirect(ShortLinkRedirectDto request);

    /**
     * 禁用短链
     *
     * @param shortCode 短码
     */
    void disable(String shortCode);

    /**
     * 查询短链信息
     *
     * @param shortCode 短码
     * @return 短链信息
     */
    ShortLinkVo info(String shortCode);
}
