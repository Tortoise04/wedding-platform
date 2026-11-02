package com.wedding.config;

import com.wedding.ws.ChatWebSocketHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

// Web 配置:给原生 WebSocket 挂一块门牌(路径 /ws/chat)。
//
// 不上 STOMP / SockJS:前后端只需要一条文本通道传「有你的新消息」这一句话,
// 引消息代理等于为了喊一嗓子配一整套邮局 —— 多一层协议、多一份配置、多一处会坏的地方。
@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketConfigurer {

    // 握手后接手这条连接的处理器(验令牌 / 心跳 / 断线销号)
    private final ChatWebSocketHandler chatWebSocketHandler;

    // 只放行本地前端的两个来源:浏览器眼里 localhost 与 127.0.0.1 是两个不同的 origin,
    // 漏掉哪一个,用另一种方式打开前端的人握手就会被 403 挡在门外。
    @Override
    public void registerWebSocketHandlers(@NonNull WebSocketHandlerRegistry registry) {
        registry.addHandler(chatWebSocketHandler, "/ws/chat")
                .setAllowedOriginPatterns("http://localhost:5173", "http://127.0.0.1:5173");
    }
}
