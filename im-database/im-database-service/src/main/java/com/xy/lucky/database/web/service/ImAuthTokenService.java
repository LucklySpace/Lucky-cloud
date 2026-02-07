package com.xy.lucky.database.web.service;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xy.lucky.database.web.mapper.ImAuthTokenMapper;
import com.xy.lucky.domain.BasePo;
import com.xy.lucky.domain.po.ImAuthTokenPo;
import com.xy.lucky.rpc.api.database.auth.ImAuthTokenDubboService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.util.StringUtils;

@Slf4j
@DubboService
@RequiredArgsConstructor
public class ImAuthTokenService extends ServiceImpl<ImAuthTokenMapper, ImAuthTokenPo>
        implements ImAuthTokenDubboService {

    @Override
    public Boolean create(ImAuthTokenPo token) {
        return super.save(token);
    }

    @Override
    public Boolean markUsedByRefreshHash(String refreshTokenHash) {
        if (!StringUtils.hasText(refreshTokenHash)) {
            return Boolean.FALSE;
        }

        Wrapper<ImAuthTokenPo> updateWrapper = Wrappers.<ImAuthTokenPo>lambdaUpdate()
                .eq(ImAuthTokenPo::getRefreshTokenHash, refreshTokenHash)
                .set(ImAuthTokenPo::getUsed, 1)
                .set(BasePo::getUpdateTime, System.currentTimeMillis());
        return super.update(updateWrapper);
    }

    @Override
    public Boolean revokeByAccessHash(String accessTokenHash, String reason) {
        if (!StringUtils.hasText(accessTokenHash)) {
            return Boolean.FALSE;
        }
        UpdateWrapper<ImAuthTokenPo> uw = new UpdateWrapper<>();
        uw.eq("access_token_hash", accessTokenHash)
                .set("revoked_at", System.currentTimeMillis())
                .set("revoke_reason", reason)
                .set("update_time", System.currentTimeMillis());
        return super.update(uw);
    }

    @Override
    public Boolean revokeByRefreshHash(String refreshTokenHash, String reason) {
        if (!StringUtils.hasText(refreshTokenHash)) {
            return Boolean.FALSE;
        }
        UpdateWrapper<ImAuthTokenPo> uw = new UpdateWrapper<>();
        uw.eq("refresh_token_hash", refreshTokenHash)
                .set("revoked_at", System.currentTimeMillis())
                .set("revoke_reason", reason)
                .set("update_time", System.currentTimeMillis());
        return super.update(uw);
    }
}
