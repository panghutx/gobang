package com.example.java_gobang.api;

import com.example.java_gobang.game.*;
import com.example.java_gobang.model.User;
import com.example.java_gobang.model.UserMapper;
import com.example.java_gobang.utils.JwtUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class PVEAPI extends TextWebSocketHandler {
    private ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private OnlineUserManager onlineUserManager;

    @Autowired
    private RoomManager roomManager;
    @Resource
    private UserMapper userMapper;
    @Autowired
    private UserManager userManager;
    private Map<Integer,PVERoom> rooms = new HashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        System.out.println("建立连接");
        GameReadyResponse readyResponse = new GameReadyResponse();
        // 1. 先获取到用户的身份信息. (从 HttpSession 里拿到当前用户的对象)
        String token = JwtUtil.analyseToken(session);
        // 现在您可以使用token进行身份验证或其他操作

        String username = JwtUtil.getClaimsByToken(token).getSubject();
        User user = userManager.getUser(username);
        if (user == null) {
            readyResponse.setOk(false);
            readyResponse.setReason("用户尚未登录!");
            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(readyResponse)));
            return;
        }
        PVERoom room = new PVERoom();
        rooms.put(user.getUserId(),room);

        // 3. 判定当前是不是多开 (该用户是不是已经在其他地方进入游戏了)
        //    前面准备了一个 OnlineUserManager
        if (onlineUserManager.getFromGameHall(user.getUserId()) != null
                || onlineUserManager.getFromGameRoom(user.getUserId()) != null) {
            // 如果一个账号, 一边是在游戏大厅, 一边是在游戏房间, 也视为多开~~
            readyResponse.setOk(true);
            readyResponse.setReason("禁止多开游戏页面");
            readyResponse.setMessage("repeatConnection");
            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(readyResponse)));
            return;
        }

        // 4. 设置当前玩家上线!
//        onlineUserManager.enterGameRoom(user.getUserId(), session);

        readyResponse.setOk(true);
        readyResponse.setMessage("已连接");
        readyResponse.setWhiteUser(1);
        session.sendMessage(new TextMessage(objectMapper.writeValueAsString(readyResponse)));
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String token = JwtUtil.analyseToken(session);

        String username = JwtUtil.getClaimsByToken(token).getSubject();
        User user = userManager.getUser(username);
        //客户端传来棋盘信息
        String payload = message.getPayload();
        System.out.println(payload);
//        PVERoom room = new PVERoom();
        PVERoom room = rooms.get(user.getUserId());
        // 3. 通过 room 对象来处理这次具体的请求
        room.putChess(message.getPayload(),session);
    }


    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        String token = JwtUtil.analyseToken(session);

        String username = JwtUtil.getClaimsByToken(token).getSubject();
        User user = userManager.getUser(username);
        if(user==null){
            return;
        }
        rooms.remove(user.getUserId());
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        String token = JwtUtil.analyseToken(session);

        String username = JwtUtil.getClaimsByToken(token).getSubject();
        User user = userManager.getUser(username);
        if(user==null){
            return;
        }
        rooms.remove(user.getUserId());
    }

}
