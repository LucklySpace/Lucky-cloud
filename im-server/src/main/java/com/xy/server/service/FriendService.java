package com.xy.server.service;


import com.xy.domain.dto.FriendDto;
import com.xy.domain.vo.FriendVo;

import java.util.List;

public interface FriendService {

    List<FriendVo> list(String userId, Long sequence);

    FriendVo getFriendInfo(FriendDto friendDto);
//
//    FriendVo findFriend(FriendDto friendDto);
//
//    void addFriend(FriendRequestDto friendRequestDto);
//
//    void approveFriend(FriendshipRequestDto friendshipRequestDto);
//
//    List<FriendshipRequestVo> request(String userId);
//
//    void delFriend(FriendDto friendDto);
}
