package com.example.java_gobang.api;

import com.example.java_gobang.game.UserResponse;
import com.example.java_gobang.model.User;
import com.example.java_gobang.model.UserMapper;
import com.example.java_gobang.utils.JwtUtil;
import com.example.java_gobang.utils.PwdUtil;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
public class UserAPI {

    @Resource
    private UserMapper userMapper;

    @PostMapping("/login")
    @ResponseBody
    public UserResponse login(@RequestBody Map<String,String> userReq, HttpServletRequest req) {
        // 根据 username 去数据库中进行查询.
        // 如果能找到匹配的用户, 并且密码也一致, 就认为登录成功
        UserResponse response = new UserResponse();
        String username = userReq.get("username");
        String password = userReq.get("password");


        User user = userMapper.selectByName(username);
        boolean decrypt = PwdUtil.decrypt(password, user.getPassword());
        System.out.println("[login] username=" + username);
        if (!decrypt) {
            // 登录失败
            System.out.println("登录失败!");
            response.setCode(5000);
            response.setMsg("登录失败,请重试");
            return response;
        }
        //登陆成功
//        HttpSession httpSession = req.getSession(true);
//        httpSession.setAttribute("user", user);
        response.setCode(3000);
        response.setMsg("登陆成功");
        response.setData(JwtUtil.generateToken(username));
        return response;
    }

    @PostMapping("/register")
    @ResponseBody
    public UserResponse register(@RequestBody Map<String,String> userReq) {
        UserResponse response = new UserResponse();
        try {
            User user = new User();
            String username=userReq.get("username");
            user.setUsername(username);
            String password = PwdUtil.encrypt(userReq.get("password"));
            user.setPassword(password);
            userMapper.insert(user);
            response.setCode(3000);
            response.setMsg("注册成功");
            response.setData(JwtUtil.generateToken(username));
            return response;
        } catch (org.springframework.dao.DuplicateKeyException e) {
            response.setCode(5000);
            response.setMsg("注册失败，请重试");
            return response;
        }
    }

    @GetMapping("/userInfo")
    @ResponseBody
    public Object getUserInfo(HttpServletRequest req) {
        try {
            HttpSession httpSession = req.getSession(false);
            User user = (User) httpSession.getAttribute("user");
            // 拿着这个 user 对象, 去数据库中找, 找到最新的数据
            User newUser = userMapper.selectByName(user.getUsername());
            return newUser;
        } catch (NullPointerException e) {
            return new User();
        }
    }

    @GetMapping("/ranked")
    @ResponseBody
    public UserResponse getRanked(){
        UserResponse response = new UserResponse();
        List<Map<String,Object>> hashMapList  = new ArrayList<>();
        List<User> userList = userMapper.selectAll();
        for (User user:userList) {
            Map<String,Object> hash = new HashMap<>();
            hash.put("username",user.getUsername());
            hash.put("score",user.getScore());
            int win = user.getWinCount();
            int total = user.getTotalCount();
            int lose = total-win;
            hash.put("count",win+"/"+lose);
            if(total!=0){
                hashMapList.add(hash);
            }
        }
        response.setCode(3000);
        response.setMsg("查询成功");
        response.setData(hashMapList);
        return response;
    }

    @GetMapping("/userinfo")
    public UserResponse ifLogin(String token){
        UserResponse response = new UserResponse();
        String username = JwtUtil.getClaimsByToken(token).getSubject();
        User user = userMapper.selectByName(username);
        if(user!=null){
            user.setPassword("还想看我密码?");
            response.setCode(3000);
            response.setData(user);
        }else {
            response.setCode(5000);
        }
        return response;
    }
}
