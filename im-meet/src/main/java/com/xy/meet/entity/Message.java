package com.xy.meet.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

import lombok.*;

import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Message {

    private String type;
    private String roomId;
    private String userId;
    private Set<User> users;
    private String body;
    private String stream; // 可选扩展字段
    private User user;

    public Message(String type, String roomId) {
        this.type = type;
        this.roomId = roomId;
    }
}