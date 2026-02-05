package com.xy.lucky.server.service.impl;

import com.xy.lucky.database.api.user.ImUserDataDubboService;
import com.xy.lucky.domain.po.ImUserDataPo;
import com.xy.lucky.server.common.LockExecutor;
import com.xy.lucky.server.domain.dto.UserDto;
import com.xy.lucky.server.domain.mapper.UserDataBeanMapper;
import com.xy.lucky.server.domain.vo.UserVo;
import com.xy.lucky.server.exception.MessageException;
import com.xy.lucky.server.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * 用户服务实现
 *
 * @author xy
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    /**
     * 分布式锁前缀
     */
    private static final String LOCK_PREFIX = "lock:user:";
    private final LockExecutor lockExecutor;
    @DubboReference
    private ImUserDataDubboService userDataDubboService;

    /**
     * 查询用户列表
     *
     * @param dto 查询条件（包含用户ID）
     * @return 用户列表
     */
    @Override
    public List<UserVo> list(UserDto dto) {
        if (dto == null || !StringUtils.hasText(dto.getUserId())) {
            return Collections.emptyList();
        }

        return Optional.ofNullable(userDataDubboService.queryOne(dto.getUserId()))
                .map(po -> List.of(UserDataBeanMapper.INSTANCE.toUserVo(po)))
                .orElse(Collections.emptyList());
    }

    /**
     * 查询单个用户
     *
     * @param userId 用户ID（已在 Controller 层校验）
     * @return 用户信息
     */
    @Override
    public UserVo one(String userId) {
        return lockExecutor.execute(LOCK_PREFIX + "one:" + userId, () -> {
            ImUserDataPo po = userDataDubboService.queryOne(userId);
            return po != null ? UserDataBeanMapper.INSTANCE.toUserVo(po) : new UserVo();
        });
    }

    /**
     * 创建用户
     *
     * @param dto 用户信息（已在 Controller 层校验）
     * @return 创建后的用户信息
     */
    @Override
    public UserVo create(UserDto dto) {
        ImUserDataPo po = UserDataBeanMapper.INSTANCE.toImUserDataPo(dto);

        if (!userDataDubboService.creat(po)) {
            throw new MessageException("创建用户失败");
        }

        log.info("创建用户成功: userId={}", dto.getUserId());
                return UserDataBeanMapper.INSTANCE.toUserVo(po);
    }

    /**
     * 更新用户信息
     *
     * @param dto 用户信息（已在 Controller 层校验）
     * @return 是否成功
     */
    @Override
    public Boolean update(UserDto dto) {
        return lockExecutor.execute(LOCK_PREFIX + "update:" + dto.getUserId(), () -> {
            ImUserDataPo po = UserDataBeanMapper.INSTANCE.toImUserDataPo(dto);

            if (!userDataDubboService.modify(po)) {
                throw new MessageException("更新用户失败");
            }

            log.info("更新用户成功: userId={}", dto.getUserId());
                return true;
        });
    }

    /**
     * 删除用户
     *
     * @param userId 用户ID（已在 Controller 层校验）
     * @return 是否成功
     */
    @Override
    public Boolean delete(String userId) {
        return lockExecutor.execute(LOCK_PREFIX + "delete:" + userId, () -> {
            if (!Boolean.TRUE.equals(userDataDubboService.removeOne(userId))) {
                throw new MessageException("删除用户失败");
            }

            log.info("删除用户成功: userId={}", userId);
            return true;
        });
    }
}
