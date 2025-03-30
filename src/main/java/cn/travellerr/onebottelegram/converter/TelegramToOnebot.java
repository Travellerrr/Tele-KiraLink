package cn.travellerr.onebottelegram.converter;

import cn.chahuyun.hibernateplus.HibernateFactory;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.travellerr.onebotApi.GroupMessage;
import cn.travellerr.onebotApi.PrivateMessage;
import cn.travellerr.onebotApi.Sender;
import cn.travellerr.onebottelegram.OnebotTelegramApplication;
import cn.travellerr.onebottelegram.hibernate.HibernateUtil;
import cn.travellerr.onebottelegram.hibernate.entity.Group;
import cn.travellerr.onebottelegram.onebotWebsocket.OneBotWebSocketHandler;
import cn.travellerr.onebottelegram.telegramApi.TelegramApi;
import com.pengrad.telegrambot.model.Chat;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.request.GetChatMemberCount;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.reflections.Reflections.log;

@Component
public class TelegramToOnebot implements ApplicationRunner {

    public static final Map<Integer, Long> messageIdToChatId = new java.util.LinkedHashMap<>(1024, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<Integer, Long> eldest) {
            return size() > 512;
        }
    };

    @Override
    public void run(ApplicationArguments args) {
        System.out.println("Telegram to Onebot converter is running...");
        HibernateUtil.init(OnebotTelegramApplication.INSTANCE);
        TelegramApi.init();
    }

    public static void forwardToOnebot(Update update) {
        if (update.message() != null&&update.message().text() != null) {

            messageIdToChatId.put(update.message().messageId(), Math.abs(update.message().chat().id()));

            // 截取"@"前消息
            String realMessage = update.message().text().replace("@"+TelegramApi.getMeResponse.user().username(), "").trim();

            String username = update.message().from().username();
            if (username == null) {
                username = update.message().from().firstName();
            }

            JSONObject object;


            JSONArray message = new JSONArray().set(new JSONObject().set("type", "text").set("data", new JSONObject().set("text", realMessage)));

            if (update.message().chat().type().equals(Chat.Type.group) || update.message().chat().type().equals(Chat.Type.supergroup)) {
                Group group = null;

                try {
                    group = HibernateFactory.selectOne(Group.class, update.message().chat().id());
                } catch (Exception ignored) {
                }

                if (group == null) {
                    int memberCount = TelegramApi.bot.execute(new GetChatMemberCount(update.message().chat().id())).count();
                    group = Group.builder()
                            .groupId(update.message().chat().id())
                            .groupName(update.message().chat().title())
                            .memberCount(memberCount)
                            .build();
                    HibernateFactory.merge(group);
                }

                group.addMemberId(update.message().from().id());
                group.addMemberUsernames(username);
                HibernateFactory.merge(group);

                if (realMessage.contains("@")) {
                    List<Long> atList = new ArrayList<>();
                    List<String> messageList = new ArrayList<>();
                    Matcher matcher = Pattern.compile("@(\\S+?)(\\s|$)").matcher(realMessage);

                    List<Long> membersIdList = group.getMembersIdList();
                    List<String> memberUsernameList = group.getMemberUsernamesList();


                    int lastIndex = 0;
                    while (matcher.find()) {
                        int index = memberUsernameList.indexOf(matcher.group(1));
                        if (index != -1) {
                            atList.add(membersIdList.get(index));
                            String msg = realMessage.substring(lastIndex, matcher.start()).trim();
                            if (!messageList.isEmpty()) {
                                msg = " " + msg;
                            }
                            messageList.add(msg);
                            lastIndex = matcher.end();
                        }
                        messageList.add(" "+realMessage.substring(lastIndex).trim());
                    }

                    message = new JSONArray();
                    for (int i = 0; i < messageList.size(); i++) {
                        JSONObject messageObject = new JSONObject().set("type", "text").set("data", new JSONObject().set("text", messageList.get(i)));

                        message.add(messageObject);
                        if (i < atList.size()) {
                            JSONObject atObject = new JSONObject().set("type", "at").set("data", new JSONObject().set("qq", atList.get(i)));
                            message.add(atObject);
                        }


                    }

                    System.out.println("message: " + message);

                }

                Sender groupSender = new Sender(update.message().from().id(), username, update.message().from().firstName(), "unknown", 0, "虚拟地区", "0", "member", "");
                GroupMessage groupMessage = new GroupMessage(System.currentTimeMillis(), TelegramApi.getMeResponse.user().id(), "message", "group", "normal", update.message().messageId(), -update.message().chat().id(), update.message().from().id(), null, realMessage, 0, groupSender);

                object = new JSONObject(groupMessage);
            } else {
                Sender sender = new Sender(update.message().from().id(), username, update.message().from().firstName(), "unknown", 0, null, null, null, null);
                log.error("sender: " + sender);
                PrivateMessage privateMessage = new PrivateMessage(System.currentTimeMillis(), TelegramApi.getMeResponse.user().id(), "message", "private", "friend", update.message().messageId(), update.message().from().id(), realMessage, 0, sender);
                object = new JSONObject(privateMessage);
            }


            object.set("message", message);

            log.info("转发消息到 OneBot: {}", object);

            OneBotWebSocketHandler.broadcast(object.toString());


        }
    }
}
