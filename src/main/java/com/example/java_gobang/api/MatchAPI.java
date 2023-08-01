package com.example.java_gobang.api;

import com.example.java_gobang.game.*;
import com.example.java_gobang.model.User;
import com.example.java_gobang.model.UserMapper;
import com.example.java_gobang.utils.JwtUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.ibatis.jdbc.Null;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.handler.TextWebSocketHandler;

@EnableWebSocket
// 通过这个类来处理匹配功能中的 websocket 请求
@Component
public class MatchAPI extends TextWebSocketHandler {
    private ObjectMapper objectMapper = new ObjectMapper();
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private OnlineUserManager onlineUserManager;
    @Autowired
    private UserManager userManager;
    @Autowired
    private Matcher matcher;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        // 玩家上线, 加入到 OnlineUserManager 中

        MatchResponse response = new MatchResponse();
        String token = JwtUtil.analyseToken(session);

        // 现在您可以使用token进行身份验证或其他操作
        System.out.println("Received token: " + token);
        if(token==null){
            response.setOk(false);
            response.setMessage("请登录后重试");
            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(response)));
        }
        String username = JwtUtil.getClaimsByToken(token).getSubject();
        User user = userManager.getUser(username);
        try {
//            // 2. 先判定当前用户是否已经登录过(已经是在线状态), 如果是已经在线, 就不该继续进行后续逻辑.
//            if (onlineUserManager.getFromGameHall(user.getUserId()) != null
//                    || onlineUserManager.getFromGameRoom(user.getUserId()) != null) {
//                // 当前用户已经登录了!!
//                // 针对这个情况要告知客户端, 你这里重复登录了.
//                response.setOk(false);
//                response.setReason("检测到多开行为，请勿多开！");
//                response.setMessage("repeatConnection");
//                session.sendMessage(new TextMessage(objectMapper.writeValueAsString(response)));
//                // 此处直接关闭有些太激进了, 还是返回一个特殊的 message , 供客户端来进行判定, 由客户端负责进行处理
//                // session.close();
//                return;
//            }

            // 3. 拿到了身份信息之后, 就可以把玩家设置成在线状态了
            onlineUserManager.enterGameHall(user.getUserId(), session);
            System.out.println("玩家 " + user.getUsername() + " 进入游戏大厅!");
        } catch (NullPointerException e) {
            System.out.println("[MatchAPI.afterConnectionEstablished] 当前用户未登录!");
            // e.printStackTrace();
            // 出现空指针异常, 说明当前用户的身份信息是空, 用户未登录呢.
            // 把当前用户尚未登录这个信息给返回回去~~
            response = new MatchResponse();
            response.setOk(false);
            response.setReason("您尚未登录，请登录后重试");
            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(response)));
        }
    }



    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        // 实现处理开始匹配请求和处理停止匹配请求.
        // 获取到客户端给服务器发送的数据
        MatchResponse response = new MatchResponse();
        String token = JwtUtil.analyseToken(session);
        String username = JwtUtil.getClaimsByToken(token).getSubject();
        User user = userManager.getUser(username);
        String payload = message.getPayload();
//         当前这个数据载荷是一个 JSON 格式的字符串, 就需要把它转成 Java 对象. MatchRequest
        MatchRequest request = objectMapper.readValue(payload, MatchRequest.class);
        response = new MatchResponse();
        if (request.getMessage().equals("startMatch")) {
            // 进入匹配队列
            matcher.add(user);
            // 把玩家信息放入匹配队列之后, 就可以返回一个响应给客户端了.
            response.setOk(true);
            response.setMessage("startMatch");
        } else if (request.getMessage().equals("stopMatch")) {
            // 退出匹配队列
            matcher.remove(user);
            // 移除之后, 就可以返回一个响应给客户端了.
            response.setOk(true);
            response.setMessage("stopMatch");
        } else {
            response.setOk(false);
            response.setReason("非法的匹配请求");
        }
        String jsonString = objectMapper.writeValueAsString(response);
        session.sendMessage(new TextMessage(jsonString));
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        try {
            // 玩家下线, 从 OnlineUserManager 中删除
            String token = JwtUtil.analyseToken(session);
            String username = JwtUtil.getClaimsByToken(token).getSubject();
            User user = userManager.getUser(username);
            WebSocketSession tmpSession = onlineUserManager.getFromGameHall(user.getUserId());
            if (tmpSession == session) {
                onlineUserManager.exitGameHall(user.getUserId());
                System.out.println("删除成功");
            }
            // 如果玩家正在匹配中, 而 websocket 连接断开了, 就应该移除匹配队列
            matcher.remove(user);
        } catch (NullPointerException e) {
            System.out.println("[MatchAPI.handleTransportError] 当前用户未登录!");

        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        try {
            // 玩家下线, 从 OnlineUserManager 中删除
            String token = JwtUtil.analyseToken(session);
            String username = JwtUtil.getClaimsByToken(token).getSubject();
            User user = userManager.getUser(username);
            WebSocketSession tmpSession = onlineUserManager.getFromGameHall(user.getUserId());
            if (tmpSession == session) {
                onlineUserManager.exitGameHall(user.getUserId());
            }
            // 如果玩家正在匹配中, 而 websocket 连接断开了, 就应该移除匹配队列
            matcher.remove(user);
        } catch (NullPointerException e) {
            System.out.println("[MatchAPI.afterConnectionClosed] 当前用户未登录!");

        }
    }
}
