package com.xy.lucky.business.service;


import com.xy.lucky.business.domain.dto.FriendDto;
import com.xy.lucky.business.domain.dto.FriendRequestDto;
import com.xy.lucky.business.domain.vo.FriendVo;

import java.util.List;

public interface RelationshipService {

    List<?> contacts(String userId, Long sequence);

    List<?> groups(String userId);

    List<?> newFriends(String userId);

    FriendVo getFriendInfo(FriendDto friendDto);

    List<?> getFriendInfoList(FriendDto friendDto);

    String addFriend(FriendRequestDto friendRequestDto);

    void approveFriend(FriendRequestDto friendRequestDto);

    void delFriend(FriendDto friendDto);

    Boolean updateFriendRemark(FriendDto friendDto);
}
