package com.xy.lucky.server.service.impl;

import com.xy.lucky.domain.dto.UserDto;
import com.xy.lucky.domain.mapper.UserDataBeanMapper;
import com.xy.lucky.domain.po.ImUserDataPo;
import com.xy.lucky.domain.vo.UserVo;
import com.xy.lucky.dubbo.web.api.database.user.ImUserDataDubboService;
import com.xy.lucky.server.exception.MessageException;
import com.xy.lucky.server.service.UserService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.dubbo.config.annotation.DubboReference;
import org.redisson.api.RLockReactive;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class UserServiceImpl implements UserService {

    // 分布式锁常量
    private static final String LOCK_USER_PREFIX = "lock:user:";
    private static final long LOCK_WAIT_TIME = 3L; // 锁等待时间（秒）
    private static final long LOCK_LEASE_TIME = 10L; // 锁持有时间（秒）

    @DubboReference
    private ImUserDataDubboService imUserDataDubboService;

    @Resource
    private RedissonClient redissonClient;

    @Override
    public Mono<List<UserVo>> list(UserDto userDto) {
        return Mono.fromCallable(() -> {
            long start = System.currentTimeMillis();
            try {
                // Currently only support search by userId as keyword if provided
                // Or if we had a proper list method in Dubbo service.
                // Assuming basic implementation based on available Dubbo methods.
                if (userDto != null && StringUtils.isNotBlank(userDto.getUserId())) {
                    ImUserDataPo po = imUserDataDubboService.queryOne(userDto.getUserId());
                    if (po != null) {
                        log.debug("list用户完成 耗时:{}ms", System.currentTimeMillis() - start);
                        return List.of(UserDataBeanMapper.INSTANCE.toUserVo(po));
                    }
                }
                log.debug("list用户完成 (empty) 耗时:{}ms", System.currentTimeMillis() - start);
                return Collections.<UserVo>emptyList();
            } catch (Exception e) {
                log.error("list用户异常", e);
                throw new RuntimeException("获取用户列表失败");
            }
        }).subscribeOn(Schedulers.boundedElastic());
    }

    @Override
    public Mono<UserVo> one(String userId) {
        if (userId == null) {
            log.warn("one参数无效");
            return Mono.error(new RuntimeException("参数错误"));
        }

        String lockKey = LOCK_USER_PREFIX + "read:" + userId;
        return withLock(lockKey, Mono.defer(() -> {
            long start = System.currentTimeMillis();
            return Mono.fromCallable(() -> imUserDataDubboService.queryOne(userId))
                    .subscribeOn(Schedulers.boundedElastic())
                    .map(userDataPo -> {
                        UserVo userVo = UserDataBeanMapper.INSTANCE.toUserVo(userDataPo);
                        // Handle null case if selectOne returns null
                        if (userVo == null) {
                            userVo = new UserVo();
                        }
                        log.debug("one用户完成 userId={} 耗时:{}ms", userId, System.currentTimeMillis() - start);
                        return userVo;
                    });
        }), "读取用户 " + userId);
    }

    @Override
    public Mono<UserVo> create(UserDto userDto) {
        return Mono.fromCallable(() -> {
            long start = System.currentTimeMillis();
            try {
                ImUserDataPo po = UserDataBeanMapper.INSTANCE.toImUserDataPo(userDto);
                if (StringUtils.isBlank(po.getUserId())) {
                    throw new RuntimeException("UserId不能为空");
                }
                boolean success = imUserDataDubboService.creat(po);
                if (success) {
                    log.debug("create用户完成 耗时:{}ms", System.currentTimeMillis() - start);
                    return UserDataBeanMapper.INSTANCE.toUserVo(po);
                } else {
                    throw new RuntimeException("创建用户失败");
                }
            } catch (Exception e) {
                log.error("create用户异常", e);
                throw new RuntimeException("创建用户失败");
            }
        }).subscribeOn(Schedulers.boundedElastic());
    }

    @Override
    public Mono<Boolean> update(UserDto userDto) {
        if (userDto == null || userDto.getUserId() == null) {
            log.warn("update参数无效");
            return Mono.error(new RuntimeException("参数错误"));
        }

        String lockKey = LOCK_USER_PREFIX + "update:" + userDto.getUserId();
        return withLock(lockKey, Mono.defer(() -> {
            long start = System.currentTimeMillis();
            ImUserDataPo userDataPo = UserDataBeanMapper.INSTANCE.toImUserDataPo(userDto);

            return Mono.fromCallable(() -> imUserDataDubboService.modify(userDataPo))
                    .subscribeOn(Schedulers.boundedElastic())
                    .map(success -> {
                        if (success) {
                            log.debug("update用户完成 userId={} 耗时:{}ms", userDto.getUserId(), System.currentTimeMillis() - start);
                            return true;
                        } else {
                            log.debug("update用户失败 userId={} 耗时:{}ms", userDto.getUserId(), System.currentTimeMillis() - start);
                            throw new RuntimeException("更新用户失败");
                        }
                    });
        }), "更新用户 " + userDto.getUserId());
    }

    @Override
    public Mono<Boolean> delete(String userId) {
        if (userId == null) {
            log.warn("delete参数无效");
            return Mono.error(new RuntimeException("参数错误"));
        }

        String lockKey = LOCK_USER_PREFIX + "delete:" + userId;
        return withLock(lockKey, Mono.defer(() -> {
            long start = System.currentTimeMillis();
            return Mono.fromCallable(() -> imUserDataDubboService.removeOne(userId))
                    .subscribeOn(Schedulers.boundedElastic())
                    .map(success -> {
                        log.debug("delete用户完成 userId={} 耗时:{}ms", userId, System.currentTimeMillis() - start);
                        if (Boolean.TRUE.equals(success)) {
                            return true;
                        }
                        throw new RuntimeException("删除失败");
                    });
        }), "删除用户 " + userId);
    }

    private <T> Mono<T> withLock(String key, Mono<T> action, String logDesc) {
        RLockReactive lock = redissonClient.reactive().getLock(key);
        return lock.tryLock(LOCK_WAIT_TIME, LOCK_LEASE_TIME, TimeUnit.SECONDS)
                .flatMap(acquired -> {
                    if (!acquired) {
                        return Mono.error(new MessageException("无法获取锁: " + logDesc));
                    }
                    return action
                            .flatMap(res ->
                                    lock.isHeldByThread(Thread.currentThread().threadId())
                                            .flatMap(held -> held ? lock.unlock() : Mono.empty())
                                            .onErrorResume(e -> Mono.empty())
                                            .thenReturn(res)
                            )
                            .onErrorResume(e ->
                                    lock.isHeldByThread(Thread.currentThread().threadId())
                                            .flatMap(held -> held ? lock.unlock() : Mono.empty())
                                            .onErrorResume(unlockErr -> Mono.empty())
                                            .then(Mono.error(e))
                            );
                });
    }
}
