package com.example.java_gobang.game;

// 这是表示一个 websocket 的匹配请求
public class MatchRequest {
    private String message = "";

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
