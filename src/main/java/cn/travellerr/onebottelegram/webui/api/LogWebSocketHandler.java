package cn.travellerr.onebottelegram.webui.api;


import cn.travellerr.onebottelegram.onebotWebsocket.OneBotWebSocketHandler;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class LogWebSocketHandler extends TextWebSocketHandler {

    public static final Set<WebSocketSession> sessions = ConcurrentHashMap.newKeySet();
    private static final Logger log = LoggerFactory.getLogger(OneBotWebSocketHandler.class);

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

    @Override
    public void afterConnectionEstablished(@NotNull org.springframework.web.socket.WebSocketSession session) {
        sessions.add(session);
    }

    @Override
    public void afterConnectionClosed(@NotNull org.springframework.web.socket.WebSocketSession session, @NotNull org.springframework.web.socket.CloseStatus status) {
        sessions.remove(session);
    }
}
