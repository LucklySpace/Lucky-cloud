package com.xy.lucky.database.webflux.service;

import com.xy.lucky.database.webflux.entity.ImUserDataEntity;
import com.xy.lucky.database.webflux.repository.ImUserDataRepository;
import com.xy.lucky.domain.po.ImUserDataPo;
import com.xy.lucky.database.rpc.api.database.user.ImUserDataDubboService;
import lombok.RequiredArgsConstructor;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

@Service
@DubboService
@RequiredArgsConstructor
public class ImUserDataReactiveService implements ImUserDataDubboService {

    private final ImUserDataRepository repository;

    @Override
    public Flux<ImUserDataPo> queryByKeyword(String keyword) {
        return repository.searchByKeyword(keyword).map(this::toPo);
    }

    @Override
    public Mono<ImUserDataPo> queryOne(String userId) {
        return repository.findById(userId).map(this::toPo);
    }

    @Override
    public Mono<Boolean> modify(ImUserDataPo po) {
        return repository.save(fromPo(po)).map(e -> true);
    }

    @Override
    public Flux<ImUserDataPo> queryListByIds(List<String> userIdList) {
        return repository.findByUserIdIn(userIdList).map(this::toPo);
    }

    private ImUserDataPo toPo(ImUserDataEntity e) {
        ImUserDataPo p = new ImUserDataPo();
        p.setUserId(e.getUserId());
        p.setName(e.getName());
        p.setAvatar(e.getAvatar());
        p.setGender(e.getGender());
        p.setBirthday(e.getBirthday());
        p.setLocation(e.getLocation());
        p.setSelfSignature(e.getSelfSignature());
        p.setFriendAllowType(e.getFriendAllowType());
        p.setForbiddenFlag(e.getForbiddenFlag());
        p.setDisableAddFriend(e.getDisableAddFriend());
        p.setSilentFlag(e.getSilentFlag());
        p.setUserType(e.getUserType());
        p.setExtra(e.getExtra());
        p.setCreateTime(e.getCreateTime());
        p.setUpdateTime(e.getUpdateTime());
        p.setDelFlag(e.getDelFlag());
        p.setVersion(e.getVersion());
        return p;
    }

    private ImUserDataEntity fromPo(ImUserDataPo p) {
        ImUserDataEntity e = new ImUserDataEntity();
        e.setUserId(p.getUserId());
        e.setName(p.getName());
        e.setAvatar(p.getAvatar());
        e.setGender(p.getGender());
        e.setBirthday(p.getBirthday());
        e.setLocation(p.getLocation());
        e.setSelfSignature(p.getSelfSignature());
        e.setFriendAllowType(p.getFriendAllowType());
        e.setForbiddenFlag(p.getForbiddenFlag());
        e.setDisableAddFriend(p.getDisableAddFriend());
        e.setSilentFlag(p.getSilentFlag());
        e.setUserType(p.getUserType());
        e.setExtra(p.getExtra());
        e.setCreateTime(p.getCreateTime());
        e.setUpdateTime(p.getUpdateTime());
        e.setDelFlag(p.getDelFlag());
        e.setVersion(p.getVersion());
        return e;
    }
}
