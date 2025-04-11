package cn.travellerr.onebottelegram.webui.api;

import cn.hutool.json.JSONObject;
import cn.travellerr.onebottelegram.TelegramOnebotAdapter;
import cn.travellerr.onebottelegram.converter.TelegramToOnebot;
import org.jetbrains.annotations.NotNull;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.handler.TextWebSocketHandler;


public class WebSocketMessage {

    public static final StandardWebSocketClient client = new StandardWebSocketClient();

    private static final WebSocketHandler handler = new TextWebSocketHandler() {
        @Override
        public void afterConnectionEstablished(@NotNull WebSocketSession session) throws Exception {
            session.sendMessage(new TextMessage(new JSONObject().set("action", "get_version_info").set("echo", 0).toString()));
        }

        @Override
        protected void handleTextMessage(@NotNull WebSocketSession session, TextMessage message) {
            final JSONObject jsonObject = new JSONObject(message.getPayload());
            if (jsonObject.isNull("sender")) {
                LogWebSocketHandler.broadcast(jsonObject.toString());
                return;
            }
            boolean isGroup = jsonObject.getInt("group_id") != null;

            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("[");
            if (isGroup) {
                stringBuilder.append("群组").append(jsonObject.getInt("group_id")).append(" ");
            } else {
                stringBuilder.append("私聊 ");
            }
            stringBuilder.append(jsonObject.getJSONObject("sender").getStr("card")).append("(")
                    .append(jsonObject.getJSONObject("sender").getStr("user_id")).append(")]: ")
                    .append(TelegramToOnebot.arrayMessageToString(jsonObject.getJSONArray("message")));

            LogWebSocketHandler.broadcast(stringBuilder.toString());
        }
    };

    public static void init() {
        client.execute(handler, "ws://127.0.0.1:"+ TelegramOnebotAdapter.config.getOnebot().getPort()
        + "/" + TelegramOnebotAdapter.config.getOnebot().getPath());
    }
}
