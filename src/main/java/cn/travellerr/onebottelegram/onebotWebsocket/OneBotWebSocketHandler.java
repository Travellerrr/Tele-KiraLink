package cn.travellerr.onebottelegram.onebotWebsocket;

import cn.travellerr.onebottelegram.TelegramOnebotAdapter;
import cn.travellerr.onebottelegram.onebotWebsocket.onebotSerialize.OnebotAction;
import cn.travellerr.onebottelegram.telegramApi.TelegramApi;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.StreamReadConstraints;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.JsonObject;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class OneBotWebSocketHandler extends TextWebSocketHandler {

    public static OneBotWebSocketHandler INSTANCE = new OneBotWebSocketHandler();

    private static final Logger log = LoggerFactory.getLogger(OneBotWebSocketHandler.class);
    public static final Set<WebSocketSession> sessions = ConcurrentHashMap.newKeySet();
    private final ObjectMapper objectMapper = new ObjectMapper(JsonFactory.builder().streamReadConstraints(StreamReadConstraints.builder()
            .maxStringLength(102400000).build()).build());

    @Override
    public void afterConnectionEstablished(@NotNull WebSocketSession session) {
        if (!isValidAccessToken(session)) {
            log.warn("无效的 accessToken，拒绝连接: {}", session.getId());
            try {
                session.close(CloseStatus.BAD_DATA);
            } catch (IOException e) {
                log.error("关闭连接失败", e);
            }
            return;
        }
        sessions.add(session);
        log.info("新的 OneBot 客户端连接: {}", session.getId());
        try {
            JsonObject metaMessage = new JsonObject();
            metaMessage.addProperty("time", System.currentTimeMillis() / 1000);
            metaMessage.addProperty("meta_event_type", "lifecycle");
            metaMessage.addProperty("post_type", "meta_event");
            metaMessage.addProperty("sub_type", "connect");
            metaMessage.addProperty("self_id", TelegramApi.getMeResponse.user().id());
            log.info("TKL发送meta消息: {}", metaMessage);
            session.sendMessage(new TextMessage(metaMessage.toString()));
        } catch (IOException e) {
            log.error("发送meta消息失败", e);
        }
    }

    private boolean isValidAccessToken(WebSocketSession session) {
        String query = session.getUri() != null ? session.getUri().getQuery() : null;
        String token = "";

        if (query != null && !query.isEmpty()) {
            for (String param : query.split("&")) {
                if (param.startsWith("access_token=")) {
                    token = param.substring("access_token=".length());
                    break;
                }
            }
        }

        if (token.isEmpty()) {
            String authHeader = session.getHandshakeHeaders().getFirst("Authorization");
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                token = authHeader.substring("Bearer ".length());
            }
        }
        String expectedToken = TelegramOnebotAdapter.config.getOnebot().getToken();
        if (!expectedToken.equals(token)) {
            log.warn("access_token 不匹配: {}", token);
            return false;
        }
        return true;
    }

    @Override
    protected void handleTextMessage(@NotNull WebSocketSession session, TextMessage message) {
        try {
            JsonNode payload = objectMapper.readTree(message.getPayload());
            log.info("TKL收到到消息 <-- {}", payload);

            // 处理 OneBot 协议消息（示例：处理心跳）
            if (payload.has("meta_event_type") &&
                    "heartbeat".equals(payload.get("meta_event_type").asText())) {
                handleHeartbeat(session, payload);
            } else {
                OnebotAction.handleAction(session, message.getPayload());
            }

        } catch (IOException e) {
            log.error("消息解析失败", e);
        }
    }


    private void handleHeartbeat(WebSocketSession session, JsonNode payload) {
        try {
            Map<String, Object> response = Map.of(
                    "time", payload.get("time").asLong(),
                    "interval", 5000,
                    "status", Map.of("online", true)
            );
            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(response)));
        } catch (IOException e) {
            log.error("心跳响应失败", e);
        }
    }

    @Override
    public void afterConnectionClosed(@NotNull WebSocketSession session, @NotNull CloseStatus status) {
        sessions.remove(session);
        log.info("OneBot 客户端断开: {} - {}", session.getId(), status);
    }

    // 广播消息给所有客户端
    public static void broadcast(String message) {
        sessions.forEach(session -> {
            try {
                if (session.isOpen()) {
                    session.sendMessage(new TextMessage(message));
                }
            } catch (IOException e) {
                log.error("广播消息失败", e);
            }
        });
    }



}