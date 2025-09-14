package com.xy.meet.entity;

import lombok.*;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;


/**
 * 房间状态
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Room {

    /** 房间 ID，若未加入则为 null */
    private String roomId;

    /** 当前房间使用的布局模式 */
    private LayoutMode layoutMode;

    /** 房间内所有参与者列表 — 使用并发安全的 Set */
    @Builder.Default
    private Set<User> users = ConcurrentHashMap.newKeySet();

    /** 房间是否存在屏幕共享（true 表示有人在共享） */
    private boolean isScreenShared;

    /** 房主（主持人）的 ID，可选 */
    private String hostId;

    // 枚举定义
    public enum LayoutMode {
        GRID, SPEAKER, RIGHT_LIST
    }

    public Room(String roomId) {
        this.roomId = roomId;
        this.users = ConcurrentHashMap.newKeySet();
    }

    public User getUserById(String userId) {
        if (userId == null) return null;
        return users.stream().filter(u -> userId.equals(u.getUserId())).findFirst().orElse(null);
    }

    public boolean addUser(User user) {
        if (user == null || user.getUserId() == null) return false;
        return users.add(user);
    }

    public boolean removeUserById(String userId) {
        User user = getUserById(userId);
        if (user != null) {
            return users.remove(user);
        }
        return false;
    }

    public boolean isEmpty() {
        return users.isEmpty();
    }

    /** 获取用户集合的安全快照（不可变） */
    public Set<User> snapshotUsers() {
        return Collections.unmodifiableSet(new HashSet<>(users));
    }
}