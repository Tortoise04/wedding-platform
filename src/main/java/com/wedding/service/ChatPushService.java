package com.wedding.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wedding.entity.ConsultMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

// 厨师:把「有新消息」这声铃,敲到该听到的人耳边。
//
// 连接池只认 userId(令牌里那个,不是会话里的 user_id / merchant_id):
// 同一个人开几个标签页就有几条连接,一条都不能落下;推的时候该用户的每条连接都发一遍,
// 发送方自己的那几条也发 —— 前端按 message.id 去重,多标签页才能跟着一起上屏。
@Service
@RequiredArgsConstructor
@Slf4j
public class ChatPushService {

    // 用 Spring Boot 自动配置的那个 ObjectMapper,与 HTTP 接口同一套序列化口径:
    // 这样 WS 推下来的 message.createdAt 和 /messages 拉上来的写法完全一致("2026-11-02T10:11:12"),
    // 前端不必为两条通道写两种日期解析。
    private final ObjectMapper objectMapper;

    // 连接池:userId → 该用户的全部连接。ConcurrentHashMap 是为了 afterCommit 那条线程
    // 与 Tomcat 的 WS 线程同时读写时不出乱子。
    private final Map<Long, Set<WebSocketSession>> sessions = new ConcurrentHashMap<>();

    // 连接建立且令牌验过之后登记
    public void register(Long userId, WebSocketSession session) {
        sessions.computeIfAbsent(userId, k -> ConcurrentHashMap.newKeySet()).add(session);
    }

    // 连接断开时销号;该用户最后一条连接走掉后把空壳也摘了,免得连接池一直涨
    public void unregister(Long userId, WebSocketSession session) {
        if (userId == null) {
            return;
        }
        Set<WebSocketSession> set = sessions.get(userId);
        if (set == null) {
            return;
        }
        set.remove(session);
        if (set.isEmpty()) {
            sessions.remove(userId, set);
        }
    }

    // 等事务提交之后再推。
    //
    // 发消息那条路在 @Transactional 里跑:此刻直接推,对方一收到就回拉消息列表,
    // 读到的还是提交前的未读数与最后一条摘要 —— 屏幕上多了一条消息,列表里却纹丝不动。
    // 所以挂到 afterCommit;不在事务里时(直接调用 / 单测)没有「提交」可言,当场发。
    public void pushMessageAfterCommit(Long conversationId, ConsultMessage message, Long... targetUserIds) {
        Runnable task = () -> pushMessage(conversationId, message, targetUserIds);
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            task.run();
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                try {
                    task.run();
                } catch (RuntimeException e) {
                    // afterCommit 里往外抛会顶到提交那一侧,让人以为「消息没发成功」;
                    // 推送只是通知,丢了顶多对方多点一次刷新,绝不能让发消息这条主路跟着失败。
                    log.warn("咨询消息推送失败: consultationId = {}, messageId = {}",
                            conversationId, message.getId(), e);
                }
            }
        });
    }

    // 业务帧只有这一种:{"type":"message","conversationId":123,"message":{...}}
    // 目标用户传进来的是「会话双方」,谁在线谁收,一个都不在线就安静地什么也不做。
    public void pushMessage(Long conversationId, ConsultMessage message, Long... targetUserIds) {
        String payload;
        try {
            Map<String, Object> frame = new LinkedHashMap<>();
            frame.put("type", "message");
            frame.put("conversationId", conversationId);
            frame.put("message", message);
            payload = objectMapper.writeValueAsString(frame);
        } catch (Exception e) {
            // 推送是「通知」,序列化失败也只是这一声没响,绝不能把发消息那条主路带塌
            log.warn("咨询消息序列化失败: consultationId = {}, messageId = {}", conversationId, message.getId(), e);
            return;
        }

        TextMessage text = new TextMessage(payload);
        // 去重:双方 userId 理论上不会撞,但同一个 userId 传两遍就会把同一条帧发两次,
        // 一句 Set 的成本换掉前端那次多余的去重。
        Set<Long> targets = new LinkedHashSet<>();
        for (Long uid : targetUserIds) {
            if (uid != null) {
                targets.add(uid);
            }
        }
        for (Long uid : targets) {
            Set<WebSocketSession> set = sessions.get(uid);
            if (set == null) {
                continue;
            }
            for (WebSocketSession session : set) {
                sendQuietly(session, text);
            }
        }
    }

    // 单条连接发送:一条发不出去不影响其余几条
    private void sendQuietly(WebSocketSession session, TextMessage text) {
        if (!session.isOpen()) {
            return;
        }
        try {
            // Tomcat 的 WS 会话不允许两个线程同时写,同一个人在多个会话里同时收到推送会撞上
            // "TEXT_FULL_WRITING";锁在会话本身上,只串行化对同一条连接的写,不同连接互不等待。
            synchronized (session) {
                session.sendMessage(text);
            }
        } catch (Exception e) {
            log.warn("咨询消息推送失败: sessionId = {}", session.getId(), e);
        }
    }
}
