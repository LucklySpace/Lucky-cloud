package com.xy.lucky.server.service;


import com.xy.lucky.domain.dto.FriendDto;
import com.xy.lucky.domain.dto.FriendRequestDto;
import com.xy.lucky.domain.vo.FriendVo;
import reactor.core.publisher.Mono;

import java.util.List;

public interface RelationshipService {

    Mono<List<?>> contacts(String userId, Long sequence);

    Mono<List<?>> groups(String userId);

    Mono<List<?>> newFriends(String userId);

    Mono<FriendVo> getFriendInfo(FriendDto friendDto);

    Mono<List<?>> getFriendInfoList(FriendDto friendDto);

    Mono<String> addFriend(FriendRequestDto friendRequestDto);

    Mono<Void> approveFriend(FriendRequestDto friendRequestDto);

    Mono<Void> delFriend(FriendDto friendDto);

    Mono<Boolean> updateFriendRemark(FriendDto friendDto);
}
