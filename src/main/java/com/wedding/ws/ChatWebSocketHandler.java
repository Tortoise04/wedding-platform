package com.wedding.ws;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wedding.service.ChatPushService;
import com.wedding.util.JwtUtil;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import org.springframework.web.util.UriComponentsBuilder;

// 服务员:守在 /ws/chat 门口的那位 —— 进门先验令牌,坐下之后只认「ping」这一句话。
//
// 路径故意留在 /api 之外:JwtInterceptor 只登记了 /api/**(见 WebMvcConfig),
// 握手根本走不到那道岗,所以令牌在这里自己验;验不过就把门关上,而不是放进来再晾着。
@Component
@RequiredArgsConstructor
@Slf4j
public class ChatWebSocketHandler extends TextWebSocketHandler {

    // 令牌缺失 / 无效 / 过期的统一关门码:4000-4999 是应用自定义段,
    // 前端看到 4001 就知道「该重新登录了」,不会傻乎乎地原地重连。
    static final CloseStatus UNAUTHORIZED = new CloseStatus(4001, "unauthorized");

    // 会话属性里的键:握过手之后靠它找回「这条连接是谁的」,断线销号也要用
    private static final String ATTR_USER_ID = "userId";

    private final JwtUtil jwtUtil;
    private final ChatPushService chatPushService;
    private final ObjectMapper objectMapper;

    // 握手成功:验令牌 → 登记进连接池。令牌不对直接 4001 关门,不登记也就没有推送目标。
    @Override
    public void afterConnectionEstablished(@NonNull WebSocketSession session) throws Exception {
        Long userId = resolveUserId(session);
        if (userId == null) {
            session.close(UNAUTHORIZED);
            return;
        }
        session.getAttributes().put(ATTR_USER_ID, userId);
        chatPushService.register(userId, session);
        log.info("咨询推送已连接: userId = {}, sessionId = {}", userId, session.getId());
    }

    // 客户端 → 服务端只有一种帧:{"type":"ping"},回一句 pong;
    // 其它帧一律当没听见 —— 一条脏数据不该把一条好端端的连接打断。
    @Override
    protected void handleTextMessage(@NonNull WebSocketSession session, @NonNull TextMessage message) {
        JsonNode node;
        try {
            node = objectMapper.readTree(message.getPayload());
        } catch (Exception e) {
            return;
        }
        if (node != null && "ping".equals(node.path("type").asText())) {
            sendPong(session);
        }
    }

    // 断线销号:连接池里留着死连接,推送就会一直往一个关了的门里送
    @Override
    public void afterConnectionClosed(@NonNull WebSocketSession session, @NonNull CloseStatus status) {
        Object userId = session.getAttributes().get(ATTR_USER_ID);
        if (userId instanceof Long id) {
            chatPushService.unregister(id, session);
            log.info("咨询推送已断开: userId = {}, sessionId = {}, status = {}", id, session.getId(), status);
        }
    }

    @Override
    public void handleTransportError(@NonNull WebSocketSession session, @NonNull Throwable exception) {
        log.warn("咨询推送连接异常: sessionId = {}", session.getId(), exception);
    }

    // 从 ?token= 里取令牌并验签,取出 userId(Long) 与 role(String)。
    // 只认解析成功且带 userId 的令牌:缺一个都算没证,交给 afterConnectionEstablished 关门。
    private Long resolveUserId(WebSocketSession session) {
        try {
            String token = UriComponentsBuilder.fromUri(session.getUri()).build()
                    .getQueryParams().getFirst("token");
            if (token == null || token.isBlank()) {
                return null;
            }
            Claims claims = jwtUtil.parseToken(token);
            Object raw = claims.get("userId");
            return raw instanceof Number number ? number.longValue() : null;
        } catch (Exception e) {
            // 被篡改 / 过期 / 格式不对全落到这里,在 WS 里抛异常只会得到一个莫名其妙的 1006,
            // 不如统一按「没证」处理,让前端拿到可读的 4001。
            return null;
        }
    }

    private void sendPong(WebSocketSession session) {
        try {
            synchronized (session) {
                session.sendMessage(new TextMessage("{\"type\":\"pong\"}"));
            }
        } catch (Exception e) {
            log.warn("心跳回包失败: sessionId = {}", session.getId(), e);
        }
    }
}
