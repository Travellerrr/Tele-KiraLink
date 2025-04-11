package cn.travellerr.onebottelegram.webui.api;

import cn.chahuyun.hibernateplus.HibernateFactory;
import cn.hutool.json.JSONArray;
import cn.travellerr.onebotApi.Text;
import cn.travellerr.onebottelegram.command.CommandHandler;
import cn.travellerr.onebottelegram.hibernate.entity.Group;
import cn.travellerr.onebottelegram.onebotWebsocket.onebotSerialize.OnebotAction;
import cn.travellerr.onebottelegram.telegramApi.TelegramApi;
import cn.travellerr.onebottelegram.webui.entity.BotInfo;
import org.springframework.web.bind.annotation.*;

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
            OnebotAction.sendMessage(0,contactId, array.toString(), isGroup);
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
}
