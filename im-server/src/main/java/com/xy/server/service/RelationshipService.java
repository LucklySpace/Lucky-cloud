package com.xy.server.service;


import com.xy.domain.dto.FriendDto;
import com.xy.domain.vo.FriendVo;
import com.xy.domain.vo.FriendshipRequestVo;
import com.xy.domain.vo.GroupVo;

import java.util.List;

public interface RelationshipService {

    List<FriendVo> contacts(String userId);

    List<GroupVo> groups(String userId);

    List<FriendshipRequestVo> newFriends(String userId);

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
