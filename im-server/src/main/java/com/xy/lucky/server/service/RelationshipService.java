package com.xy.lucky.server.service;


import com.xy.lucky.domain.dto.FriendDto;
import com.xy.lucky.domain.dto.FriendRequestDto;
import com.xy.lucky.domain.vo.FriendVo;
import com.xy.lucky.domain.vo.FriendshipRequestVo;
import com.xy.lucky.domain.vo.GroupVo;
import com.xy.lucky.general.response.domain.Result;

import java.util.List;

public interface RelationshipService {

    Result<List<FriendVo>> contacts(String userId, Long sequence);

    Result<List<GroupVo>> groups(String userId);

    Result<List<FriendshipRequestVo>> newFriends(String userId);

    Result<FriendVo> getFriendInfo(FriendDto friendDto);

    Result<List<FriendVo>> getFriendInfoList(FriendDto friendDto);

    Result addFriend(FriendRequestDto friendRequestDto);

    Result approveFriend(FriendRequestDto friendRequestDto);

    Result delFriend(FriendDto friendDto);

    Result updateFriendRemark(FriendDto friendDto);
}