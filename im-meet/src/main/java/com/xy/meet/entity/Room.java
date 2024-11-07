package com.xy.meet.entity;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class Room {

    // 房间ID
    private String roomId;
    // 房间用户
    private Set<User> users;

    public Room(String roomId) {
        this.roomId = roomId;
        this.users = ConcurrentHashMap.newKeySet();
    }

    public String getRoomId() {
        return roomId;
    }

    public Set<User> getUsers() {
        return users;
    }

    public void addUser(User user) {
        users.add(user);
    }

    public User getUser(String userId) {
        return users.stream().filter(user -> user.getUserId().equals(userId)).findFirst().orElse(null);
    }

    public void removeUser(String userId) {
        User user = getUser(userId);
        if (user != null) {
            users.remove(user);
        }
    }

    public boolean isEmpty() {
        return users.isEmpty();
    }
}