package com.xy.server.service.impl;

import com.xy.domain.dto.UserDto;
import com.xy.domain.po.ImUserDataPo;
import com.xy.domain.vo.UserVo;
import com.xy.general.response.domain.Result;
import com.xy.server.api.feign.database.user.ImUserFeign;
import com.xy.server.service.UserService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class UserServiceImpl implements UserService {

    // 分布式锁常量
    private static final String LOCK_USER_PREFIX = "lock:user:";
    private static final long LOCK_WAIT_TIME = 3L; // 锁等待时间（秒）
    private static final long LOCK_LEASE_TIME = 10L; // 锁持有时间（秒）

    @Resource
    private ImUserFeign imUserFeign;

    @Resource
    private RedissonClient redissonClient;

    @Override
    public List<UserVo> list(UserDto userDto) {
        long start = System.currentTimeMillis();
        try {
            log.debug("list用户完成 耗时:{}ms", System.currentTimeMillis() - start);
            return List.of(); // 示例返回空列表
        } catch (Exception e) {
            log.error("list用户异常", e);
            throw new RuntimeException("获取用户列表失败");
        }
    }

    @Override
    public UserVo one(String userId) {
        long start = System.currentTimeMillis();
        if (userId == null) {
            log.warn("one参数无效");
            throw new RuntimeException("参数错误");
        }

        String lockKey = LOCK_USER_PREFIX + "read:" + userId;
        RLock readLock = redissonClient.getLock(lockKey);
        try {
            if (!readLock.tryLock(LOCK_WAIT_TIME, LOCK_LEASE_TIME, TimeUnit.SECONDS)) {
                log.warn("无法获取one用户读锁 userId={}", userId);
                throw new RuntimeException("用户读取中，请稍后重试");
            }

            ImUserDataPo userDataPo = imUserFeign.getOne(userId);
            UserVo userVo = new UserVo();
            if (userDataPo != null) {
                BeanUtils.copyProperties(userDataPo, userVo);
            }

            log.debug("one用户完成 userId={} 耗时:{}ms", userId, System.currentTimeMillis() - start);
            return userVo;
        } catch (Exception e) {
            log.error("one用户异常 userId={}", userId, e);
            throw new RuntimeException("获取用户失败");
        } finally {
            if (readLock.isHeldByCurrentThread()) {
                readLock.unlock();
            }
        }
    }

    @Override
    public UserVo create(UserDto userDto) {
        long start = System.currentTimeMillis();
        // 创建用户的具体逻辑需要根据实际业务需求实现
        // 这里仅提供框架示例
        try {
            log.debug("create用户完成 耗时:{}ms", System.currentTimeMillis() - start);
            return new UserVo(); // 示例返回空对象
        } catch (Exception e) {
            log.error("create用户异常", e);
            throw new RuntimeException("创建用户失败");
        }
    }

    @Override
    public Result update(UserDto userDto) {
        long start = System.currentTimeMillis();
        if (userDto == null || userDto.getUserId() == null) {
            log.warn("update参数无效");
            return Result.failed("参数错误");
        }

        String lockKey = LOCK_USER_PREFIX + "update:" + userDto.getUserId();
        RLock lock = redissonClient.getLock(lockKey);
        try {
            if (!lock.tryLock(LOCK_WAIT_TIME, LOCK_LEASE_TIME, TimeUnit.SECONDS)) {
                log.warn("无法获取update用户锁 userId={}", userDto.getUserId());
                return Result.failed("用户更新中，请稍后重试");
            }

            ImUserDataPo userDataPo = new ImUserDataPo();
            BeanUtils.copyProperties(userDto, userDataPo);

            if(imUserFeign.update(userDataPo)){
                // 实际更新逻辑需要根据业务需求实现
                log.debug("update用户完成 userId={} 耗时:{}ms", userDto.getUserId(), System.currentTimeMillis() - start);
                return Result.success(true);
            } else{
                // 实际更新逻辑需要根据业务需求实现
                log.debug("update用户失败 userId={} 耗时:{}ms", userDto.getUserId(), System.currentTimeMillis() - start);
                return Result.failed("更新用户失败");
            }

        } catch (Exception e) {
            log.error("update用户异常 userId={}", userDto.getUserId(), e);
            throw new RuntimeException("更新用户失败");
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    @Override
    public Result delete(String userId) {
        long start = System.currentTimeMillis();
        if (userId == null) {
            log.warn("delete参数无效");
            return Result.failed("参数错误");
        }

        String lockKey = LOCK_USER_PREFIX + "delete:" + userId;
        RLock lock = redissonClient.getLock(lockKey);
        try {
            if (!lock.tryLock(LOCK_WAIT_TIME, LOCK_LEASE_TIME, TimeUnit.SECONDS)) {
                log.warn("无法获取delete用户锁 userId={}", userId);
                return Result.failed("用户删除中，请稍后重试");
            }

            // 实际删除逻辑需要根据业务需求实现
            log.debug("delete用户完成 userId={} 耗时:{}ms", userId, System.currentTimeMillis() - start);
            return Result.success();
        } catch (Exception e) {
            log.error("delete用户异常 userId={}", userId, e);
            throw new RuntimeException("删除用户失败");
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }
}