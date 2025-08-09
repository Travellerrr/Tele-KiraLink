package cn.travellerr.onebottelegram.webui.api;

import cn.chahuyun.hibernateplus.HibernateFactory;
import cn.hutool.json.JSONArray;
import cn.travellerr.onebotApi.Text;
import cn.travellerr.onebottelegram.command.CommandHandler;
import cn.travellerr.onebottelegram.converter.TelegramToOnebot;
import cn.travellerr.onebottelegram.hibernate.entity.Group;
import cn.travellerr.onebottelegram.hibernate.entity.Message;
import cn.travellerr.onebottelegram.onebotWebsocket.onebotSerialize.OnebotAction;
import cn.travellerr.onebottelegram.telegramApi.TelegramApi;
import cn.travellerr.onebottelegram.webui.entity.BotInfo;
import com.google.gson.JsonArray;
import org.springframework.web.bind.annotation.*;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class ApiController {
    @GetMapping("/bot-info")
    public BotInfo getBotInfo(@RequestParam(value = "withAvatar", defaultValue = "false") boolean withAvatar) {
        return new BotInfo(TelegramApi.getMeResponse, TelegramApi.botAvatar, withAvatar);
    }

    @GetMapping("/bot-contacts")
    public List<Group> getBotContacts() {
        return HibernateFactory.selectList(Group.class);
    }

    @PostMapping("/bot-contacts/send-msg")
    public boolean sendMsgToContact(@RequestBody Map<String, Object> request) {
        try {
            String msg = (String) request.get("msg");
            long contactId = ((Number) request.get("contactId")).longValue();
            Boolean isGroup = (Boolean) request.get("isGroup");
            Text text = new Text(msg);
            JSONArray array = new JSONArray();
            array.add(text);
            OnebotAction.sendMessage("0", contactId, array.toString(), isGroup);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    @PostMapping("/toa/command")
    public String useCommand(@RequestBody Map<String, Object> request) {
        String command = (String) request.get("command");

        return CommandHandler.INSTANCE.handleCommand(command);
    }

    @PostMapping("/toa/chat-history")
    public String getChatHistory(@RequestBody Map<String, Object> request) {
        int limit = ((Number) request.get("limit")).intValue();
        List<Message> messages = HibernateFactory.selectList(Message.class);
        int toIndex = Math.min(messages.size(), limit);
        messages = messages.subList(messages.size() - toIndex, messages.size());
        messages.sort(Comparator.comparing(Message::getCreateTime));
        if (!messages.isEmpty()) {
            JsonArray realMessages = new JsonArray();
            messages.forEach(message -> realMessages.add(TelegramToOnebot.handleTextMessage(message.getMessageString())));
            return realMessages.toString();
        }
        return new JsonArray().toString();
    }
}
