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
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

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
    public List<UserVo> list(UserDto userDto) {
        long start = System.currentTimeMillis();
        try {
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
    }

    @Override
    public UserVo one(String userId) {
        if (userId == null) {
            log.warn("one参数无效");
            throw new RuntimeException("参数错误");
        }

        String lockKey = LOCK_USER_PREFIX + "read:" + userId;
        return withLockSync(lockKey, "读取用户 " + userId, () -> {
            long start = System.currentTimeMillis();
            ImUserDataPo userDataPo = imUserDataDubboService.queryOne(userId);
            UserVo userVo = UserDataBeanMapper.INSTANCE.toUserVo(userDataPo);
            if (userVo == null) {
                userVo = new UserVo();
            }
            log.debug("one用户完成 userId={} 耗时:{}ms", userId, System.currentTimeMillis() - start);
            return userVo;
        });
    }

    @Override
    public UserVo create(UserDto userDto) {
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
    }

    @Override
    public Boolean update(UserDto userDto) {
        if (userDto == null || userDto.getUserId() == null) {
            log.warn("update参数无效");
            throw new RuntimeException("参数错误");
        }

        String lockKey = LOCK_USER_PREFIX + "update:" + userDto.getUserId();
        return withLockSync(lockKey, "更新用户 " + userDto.getUserId(), () -> {
            long start = System.currentTimeMillis();
            ImUserDataPo userDataPo = UserDataBeanMapper.INSTANCE.toImUserDataPo(userDto);
            boolean success = imUserDataDubboService.modify(userDataPo);
            if (success) {
                log.debug("update用户完成 userId={} 耗时:{}ms", userDto.getUserId(), System.currentTimeMillis() - start);
                return true;
            }
            log.debug("update用户失败 userId={} 耗时:{}ms", userDto.getUserId(), System.currentTimeMillis() - start);
            throw new RuntimeException("更新用户失败");
        });
    }

    @Override
    public Boolean delete(String userId) {
        if (userId == null) {
            log.warn("delete参数无效");
            throw new RuntimeException("参数错误");
        }

        String lockKey = LOCK_USER_PREFIX + "delete:" + userId;
        return withLockSync(lockKey, "删除用户 " + userId, () -> {
            long start = System.currentTimeMillis();
            Boolean success = imUserDataDubboService.removeOne(userId);
            log.debug("delete用户完成 userId={} 耗时:{}ms", userId, System.currentTimeMillis() - start);
            if (Boolean.TRUE.equals(success)) {
                return true;
            }
            throw new RuntimeException("删除失败");
        });
    }

    private <T> T withLockSync(String key, String logDesc, ThrowingSupplier<T> action) {
        RLock lock = redissonClient.getLock(key);
        boolean acquired = false;
        try {
            acquired = lock.tryLock(LOCK_WAIT_TIME, LOCK_LEASE_TIME, TimeUnit.SECONDS);
            if (!acquired) {
                throw new MessageException("无法获取锁: " + logDesc);
            }
            return action.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new MessageException("无法获取锁: " + logDesc);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            try {
                if (acquired && lock.isHeldByCurrentThread()) {
                    lock.unlock();
                }
            } catch (Exception ignored) {
            }
        }
    }

    @FunctionalInterface
    private interface ThrowingSupplier<T> {
        T get() throws Exception;
    }
}
