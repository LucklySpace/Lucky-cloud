package com.xy.server.service;


import com.xy.domain.dto.FriendDto;
import com.xy.domain.dto.FriendRequestDto;
import com.xy.domain.vo.FriendVo;
import com.xy.domain.vo.FriendshipRequestVo;
import com.xy.domain.vo.GroupVo;
import com.xy.general.response.domain.Result;

import java.util.List;

public interface RelationshipService {

    Result<List<FriendVo>> contacts(String userId);

    Result<List<GroupVo>> groups(String userId);

    Result<List<FriendshipRequestVo>> newFriends(String userId);

    Result<FriendVo> getFriendInfo(FriendDto friendDto);

    Result<List<FriendVo>> getFriendInfoList(FriendDto friendDto);

    Result addFriend(FriendRequestDto friendRequestDto);

    Result approveFriend(FriendRequestDto friendRequestDto);

    Result delFriend(FriendDto friendDto);
}