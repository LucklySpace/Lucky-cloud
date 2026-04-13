package com.xy.lucky.auth.service.impl;

import com.xy.lucky.auth.service.TokenVersionService;
import com.xy.lucky.auth.utils.RedisCache;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class TokenVersionServiceImpl implements TokenVersionService {

    /**
     * 用户全局令牌版本号 key: auth:version:{userId}
     */
    private static final String VERSION_KEY_PREFIX = "im:auth:token:version:";

    private final RedisCache redisCache;

    @Override
    public long getCurrentVersion(String userId) {
        String key = VERSION_KEY_PREFIX + userId;
        Long version = redisCache.get(key);
        return Optional.ofNullable(version).orElse(0L);
    }

    @Override
    public long incrementVersion(String userId) {
        String key = VERSION_KEY_PREFIX + userId;
        long newVersion = redisCache.incr(key, 1);
        log.info("用户 {} 令牌版本递增至 {}", userId, newVersion);
        return newVersion;
    }

    @Override
    public boolean isVersionValid(String userId, long tokenVersion) {
        long currentVersion = getCurrentVersion(userId);
        // 令牌版本必须大于等于当前版本才有效
        // 注意：这里使用 >= 而不是 ==，允许在版本递增前签发的令牌继续使用
        // 如果需要更严格的控制，可以改为 ==
        boolean valid = tokenVersion >= currentVersion;
        if (!valid) {
            log.debug("令牌版本校验失败：userId={}, tokenVersion={}, currentVersion={}",
                    userId, tokenVersion, currentVersion);
        }
        return valid;
    }

}

