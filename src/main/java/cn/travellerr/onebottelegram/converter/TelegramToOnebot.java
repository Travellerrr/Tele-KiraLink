package cn.travellerr.onebottelegram.converter;

import cn.chahuyun.hibernateplus.HibernateFactory;
import cn.hutool.json.JSONObject;
import cn.travellerr.onebotApi.GroupMessage;
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

@Component
public class TelegramToOnebot implements ApplicationRunner {
    @Override
    public void run(ApplicationArguments args) throws Exception {
        System.out.println("Telegram to Onebot converter is running...");
        HibernateUtil.init(OnebotTelegramApplication.INSTANCE);
        TelegramApi.init();
    }

    public static void forwardToOnebot(Update update) {
        if (update.message() != null&&update.message().text() != null) {

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

                group.addMember(update.message().from().id());
                HibernateFactory.merge(group);
            }

            // 截取"@"前消息
            String realMessage = update.message().text().replace("@"+TelegramApi.getMeResponse.user().username(), "").trim();


//            Message message = new Message(update.message().from().id(), update.message().text());
//            System.out.println("Forwarding message to Onebot: " + message);
            Sender sender = new Sender(update.message().from().id(), update.message().from().username(), update.message().from().firstName(), "unknown", 0, "虚拟地区", "0", "member", "");
            GroupMessage groupMessage = new GroupMessage(System.currentTimeMillis(), TelegramApi.getMeResponse.user().id(), "message", "group", "normal", update.message().messageId(), update.message().chat().id(), update.message().from().id(), null, realMessage, 0, sender, "array");

            JSONObject object = new JSONObject(groupMessage);
            object.set("message", "[{\"type\":\"text\",\"data\":{\"text\":\"" + realMessage + "\"}}]");

            System.out.println("Forwarding message to Onebot: " + object);

            OneBotWebSocketHandler.broadcast(object.toString());


        }
    }
}
