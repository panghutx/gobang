package com.example.java_gobang.game;

import com.example.java_gobang.model.User;
import com.example.java_gobang.model.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class UserManager {
    @Autowired
    private UserMapper userMapper;

    private Map<String,User> map = new ConcurrentHashMap<>();

    public User getUser(String username){
        if(map.containsKey(username)){
            return map.get(username);
        }
        User user = userMapper.selectByName(username);
        map.put(username,user);
        return user;
    }
}
