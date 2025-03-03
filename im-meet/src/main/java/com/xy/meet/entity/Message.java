package com.xy.meet.entity;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Set;

public class Message {

    private String type;
    private String roomId;
    private String userId;

    private Set<User> users;
    private String body;

    public Message() {
    }

    public Message(String type, String roomId, String userId) {
        this.type = type;
        this.roomId = roomId;
        this.userId = userId;
    }

    public String getType() {
        return type;
    }

    @JsonProperty("type")
    public void setType(String type) {
        this.type = type;
    }

    public String getRoomId() {
        return roomId;
    }

    @JsonProperty("roomId")
    public void setRoomId(String roomId) {
        this.roomId = roomId;
    }

    public String getUserId() {
        return userId;
    }

    @JsonProperty("userId")
    public void setUserId(String userId) {
        this.userId = userId;
    }

    public Set<User> getUsers() {
        return users;
    }

    public void setUsers(Set<User> users) {
        this.users = users;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }
}
