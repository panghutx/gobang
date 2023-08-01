package com.example.java_gobang.game;

import com.example.java_gobang.model.User;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.LinkedList;
import java.util.Queue;

// 这个类表示 "匹配器", 通过这个类负责完成整个匹配功能
@Component
public class Matcher {
    // 创建三个匹配队列
    private Queue<User> normalQueue = new LinkedList<>();
    private Queue<User> highQueue = new LinkedList<>();
    private Queue<User> veryHighQueue = new LinkedList<>();

    @Autowired
    private OnlineUserManager onlineUserManager;

    @Autowired
    private RoomManager roomManager;

    private ObjectMapper objectMapper = new ObjectMapper();

    // 操作匹配队列的方法.
    // 把玩家放到匹配队列中
    public void add(User user) {
        if (user.getScore() < 2000) {
            synchronized (normalQueue) {
                System.out.println("加入前"+normalQueue.size());
                normalQueue.offer(user);
                normalQueue.notify();
                System.out.println("把玩家 " + user.getUsername() + " 加入到了 normalQueue 中!");
                System.out.println("加入后"+normalQueue.size());
            }

        } else if (user.getScore() >= 2000 && user.getScore() < 3000) {
            synchronized (highQueue) {
                highQueue.offer(user);
                highQueue.notify();
            }
            System.out.println("把玩家 " + user.getUsername() + " 加入到了 highQueue 中!");
        } else {
            synchronized (veryHighQueue) {
                veryHighQueue.offer(user);
                veryHighQueue.notify();
            }
            System.out.println("把玩家 " + user.getUsername() + " 加入到了 veryHighQueue 中!");
        }
    }

    // 当玩家点击停止匹配的时候, 就需要把玩家从匹配队列中删除
    public void remove(User user) {
        if (user.getScore() < 2000) {
            synchronized (normalQueue) {
                System.out.println("移除前"+normalQueue.size());
                normalQueue.remove(user);
                System.out.println("把玩家 " + user.getUsername() + " 移除了 normalQueue!");

            }

        } else if (user.getScore() >= 2000 && user.getScore() < 3000) {
            synchronized (highQueue) {
                highQueue.remove(user);
            }
            System.out.println("把玩家 " + user.getUsername() + " 移除了 highQueue!");
        } else {
            synchronized (veryHighQueue) {
                veryHighQueue.remove(user);
            }
            System.out.println("把玩家 " + user.getUsername() + " 移除了 veryHighQueue!");
        }
    }

    public Matcher() {
        // 创建三个线程, 分别针对这三个匹配队列, 进行操作.
        Thread t1 = new Thread() {
            @Override
            public void run() {
                // 扫描 normalQueue
                while (true) {
                    handlerMatch(normalQueue);
                }
            }
        };
        t1.start();

        Thread t2 = new Thread(){
            @Override
            public void run() {
                while (true) {
                    handlerMatch(highQueue);
                }
            }
        };
        t2.start();

        Thread t3 = new Thread() {
            @Override
            public void run() {
                while (true) {
                    handlerMatch(veryHighQueue);
                }
            }
        };
        t3.start();
    }

    private void handlerMatch(Queue<User> matchQueue) {
        synchronized (matchQueue) {
            try {
                // 1. 检测队列中元素个数是否达到 2
                //    队列的初始情况可能是 空
                //    如果往队列中添加一个元素, 这个时候, 仍然是不能进行后续匹配操作的.
                //    因此在这里使用 while 循环检查是更合理的~~
                while (matchQueue.size() < 2) {
                    matchQueue.wait();
                }
//                System.out.println(matchQueue.size());
                // 2. 尝试从队列中取出两个玩家
                User player1 = matchQueue.poll();
                User player2 = matchQueue.poll();
                System.out.println("匹配出两个玩家: " + player1.getUsername() + ", " + player2.getUsername());
                // 3. 获取到玩家的 websocket 的会话
                //    获取到会话的目的是为了告诉玩家, 你排到了~~
                WebSocketSession session1 = onlineUserManager.getFromGameHall(player1.getUserId());
                System.out.println("session1"+session1);
                WebSocketSession session2 = onlineUserManager.getFromGameHall(player2.getUserId());
                System.out.println("session2"+session2);
                // 理论上来说, 匹配队列中的玩家一定是在线的状态.
                // 因为前面的逻辑里进行了处理, 当玩家断开连接的时候就把玩家从匹配队列中移除了.
                // 但是此处仍然进行一次判定~~
                if (session1 == null) {
                    System.out.println("玩家1不在线了");
                    // 如果玩家1 现在不在线了, 就把玩家2 重新放回到匹配队列中
                    matchQueue.offer(player2);
                    return;
                }
                if (session2 == null) {
                    System.out.println("玩家2不在线了");
                    // 如果玩家2 现在下线了, 就把玩家1 重新放回匹配队列中
                    matchQueue.offer(player1);
                    return;
                }
                // 当前能否排到两个玩家是同一个用户的情况嘛? 一个玩家入队列了两次??
                // 理论上也不会存在~~
                // 1) 如果玩家下线, 就会对玩家移出匹配队列
                // 2) 又禁止了玩家多开.
                // 但是仍然在这里多进行一次判定, 以免前面的逻辑出现 bug 时带来严重的后果.
                if (session1 == session2) {
                    // 把其中的一个玩家放回匹配队列.
                    System.out.println("两个人是同一个？");
                    matchQueue.offer(player1);
                    return;
                }

                // 4. 把这两个玩家放到一个游戏房间中.
                // 一会再实现这里
                Room room = new Room();
                roomManager.add(room, player1.getUserId(), player2.getUserId());

                // 5. 给玩家反馈信息: 你匹配到对手了~
                //    通过 websocket 返回一个 message 为 'matchSuccess' 这样的响应
                //    此处是要给两个玩家都返回 "匹配成功" 这样的信息.
                //    因此就需要返回两次
                MatchResponse response1 = new MatchResponse();
                response1.setOk(true);
                response1.setMessage("matchSuccess");
                String json1 = objectMapper.writeValueAsString(response1);
                session1.sendMessage(new TextMessage(json1));

                MatchResponse response2 = new MatchResponse();
                response2.setOk(true);
                response2.setMessage("matchSuccess");
                String json2 = objectMapper.writeValueAsString(response2);
                session2.sendMessage(new TextMessage(json2));
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
