package com.example.java_gobang.utils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.springframework.web.socket.WebSocketSession;

import java.util.Date;

public class JwtUtil {
    private  static long expire = 604800;
    private static String secret = "RSVhcnzsasdfdxc";

    //生成token
    public static String generateToken(String username){
        Date now = new Date();
        Date expiration = new Date(now.getTime()+1000*expire);
        return Jwts.builder()
                .setHeaderParam("type","JWT")
                .setSubject(username)
                .setIssuedAt(now)
                .setExpiration(expiration)
                .signWith(SignatureAlgorithm.HS256,secret)
                .compact();
    }

    //解析token
    public static Claims getClaimsByToken(String token){
        return Jwts.parser()
                .setSigningKey(secret)
                .parseClaimsJws(token)
                .getBody();
    }
    public static String analyseToken(WebSocketSession session){
        String query = session.getUri().getQuery();
        System.out.println(query);
        // 解析query参数，提取token信息
        String token = null;
        for (String param : query.split("&")) {
            String[] pair = param.split("=");
            if (pair.length == 2 && pair[0].equals("token")) {
                token = pair[1];
                break;
            }
        }
        return token;
    }
}
