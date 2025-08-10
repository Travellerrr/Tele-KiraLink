package cn.travellerr.onebottelegram.converter;

import cn.chahuyun.hibernateplus.HibernateFactory;
import cn.hutool.core.codec.Base64Encoder;
import cn.hutool.core.date.DateUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.travellerr.onebotApi.*;
import cn.travellerr.onebottelegram.TelegramOnebotAdapter;
import cn.travellerr.onebottelegram.hibernate.HibernateUtil;
import cn.travellerr.onebottelegram.hibernate.entity.Group;
import cn.travellerr.onebottelegram.hibernate.entity.Message;
import cn.travellerr.onebottelegram.model.Messages;
import cn.travellerr.onebottelegram.onebotWebsocket.OneBotWebSocketHandler;
import cn.travellerr.onebottelegram.telegramApi.TelegramApi;
import cn.travellerr.onebottelegram.webui.api.LogWebSocketHandler;
import com.google.gson.JsonArray;
import com.pengrad.telegrambot.model.Chat;
import com.pengrad.telegrambot.model.PhotoSize;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.model.request.InlineKeyboardButton;
import com.pengrad.telegrambot.model.request.InlineKeyboardMarkup;
import com.pengrad.telegrambot.model.request.ReplyParameters;
import com.pengrad.telegrambot.request.GetChatMemberCount;
import com.pengrad.telegrambot.request.GetFile;
import com.pengrad.telegrambot.request.SendMessage;
import okhttp3.Request;
import okhttp3.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class TelegramToOnebot implements ApplicationRunner {

    public static final Map<Integer, Long> messageIdToChatId = new java.util.LinkedHashMap<>(1024, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<Integer, Long> eldest) {
            return size() > 512;
        }
    };


    private static final Logger log = LoggerFactory.getLogger(TelegramToOnebot.class);

    public static void forwardToOnebot(Update update) {
        if (update.message() != null&&(update.message().text() != null || update.message().photo() != null)) {

            messageIdToChatId.put(update.message().messageId(), update.message().chat().id());

            // 截取"@"前消息
            String realMessage = Optional.ofNullable(update.message().text())
                    .orElse(Optional.ofNullable(update.message().caption()).orElse(""))
                    .replace("@" + TelegramApi.getMeResponse.user().username(), "")
                    .trim();

            long fromId = Math.abs(update.message().from().id());

            String username = update.message().from().username();

            String firstName = update.message().from().firstName();

            if (username == null) {
                username = update.message().from().firstName();
            }

            if (realMessage.toLowerCase(Locale.ROOT).equals("/toamenu")) {

                log.info("内置菜单指令，已自动处理");

                TelegramApi.bot.execute(
                        new SendMessage(update.message().chat().id(), "菜单")
                            .replyMarkup(buildMenuButtons())
                            .replyParameters(new ReplyParameters(update.message().messageId(), update.message().chat().id()))
                );
                return;
            }

            JSONObject object;
            realMessage = serializeCommand(realMessage);

            JSONArray message = new JSONArray();

            if (update.message().messageThreadId() != null) {
                Reply reply = new Reply(update.message().messageThreadId());
                message.add(reply);
            }

            if (update.message().photo() != null) {
                for (PhotoSize photo : getEveryPhoto(update.message().photo())) {
                    String filePath = TelegramApi.bot.getFullFilePath(
                            TelegramApi.bot.execute(new GetFile(photo.fileId())).file()
                    );

                    try {
                        Image image;
                        if (TelegramOnebotAdapter.config.getOnebot().isPicBase64()) {
                            try (Response response = TelegramApi.okHttpClient.newCall(new Request.Builder().url(filePath).build()).execute();
                                 InputStream in = Objects.requireNonNull(response.body()).byteStream()) {
                                image = new Image("base64://" + Base64Encoder.encode(in.readAllBytes()));
                            }
                        } else {
                            image = new Image(filePath);
                        }
                        message.add(image);
                    } catch (Exception e) {
                        log.error("Failed to retrieve image: {}", e.getMessage());
                    }
                }
            }

            message.add(new Text(realMessage));

            if (update.message().chat().type().equals(Chat.Type.group) || update.message().chat().type().equals(Chat.Type.supergroup)) {
                Group group;

                if (!(update.message().senderChat() == null)) {
                    if (TelegramOnebotAdapter.config.getOnebot().isBanGroupUser()) {
                        log.info("根据配置设置，已打断群组身份发送消息的转发");
                        if (!TelegramOnebotAdapter.config.getOnebot().getGroupUserWarning().isEmpty() && realMessage.startsWith(TelegramOnebotAdapter.config.getCommand().getPrefix())) {
                            SendMessage sendMessage = new SendMessage(update.message().chat().id(), TelegramOnebotAdapter.config.getOnebot().getGroupUserWarning());
                            sendMessage.replyParameters(new ReplyParameters(update.message().messageId(), update.message().chat().id()));
                            TelegramApi.bot.execute(sendMessage);
                        }
                        return;
                    }
                    username = update.message().senderChat().username();
                    fromId = Math.abs(update.message().senderChat().id());
                    firstName = update.message().senderChat().title();
                }

                group = HibernateFactory.selectOne(Group.class, update.message().chat().id());

                if (group == null) {
                    int memberCount = TelegramApi.bot.execute(new GetChatMemberCount(update.message().chat().id())).count();
                    group = Group.builder()
                            .groupId(update.message().chat().id())
                            .groupName(update.message().chat().title())
                            .memberCount(memberCount)
                            .build();
                }
                group.addMemberId(fromId);
                group.addMemberUsernames(username);
                HibernateFactory.merge(group);

                if (realMessage.contains("@")) {
                    List<Long> atList = new ArrayList<>();
                    List<String> messageList = new ArrayList<>();
                    Matcher matcher = Pattern.compile("@(\\S+?)(\\s|$)").matcher(realMessage);

                    final var membersIdList = group.getMembersIdList();
                    final var memberUsernameList = group.getMemberUsernamesList();

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
                        Text messageObject = new Text(messageList.get(i));
                        if (!messageObject.getData().getText().isEmpty()) {
                            message.add(messageObject);
                        }
                        if (i < atList.size()) {
                            At atObject = new At(atList.get(i));
                            message.add(atObject);
                        }
                    }
                }

                Sender groupSender = new Sender(fromId, username, firstName, "unknown", 0, "虚拟地区", "0", "member", "");
                GroupMessage groupMessage = new GroupMessage(System.currentTimeMillis(), TelegramApi.getMeResponse.user().id(), "message", "group", "normal", update.message().messageId(), -update.message().chat().id(), fromId, null, realMessage, 0, groupSender);

                object = new JSONObject(groupMessage);
            } else {
                Sender sender = new Sender(fromId, username, firstName, "unknown", 0, null, null, null, null);
                PrivateMessage privateMessage = new PrivateMessage(System.currentTimeMillis(), TelegramApi.getMeResponse.user().id(), "message", "private", "friend", update.message().messageId(), fromId, realMessage, 0, sender);
                object = new JSONObject(privateMessage);
            }

            if (!TelegramOnebotAdapter.config.getOnebot().isUseArray()) {
                object.set("message", arrayMessageToString(message));
            } else {
                object.set("message", message);
            }

            log.info("发送消息至 Onebot --> {}", object);

            HibernateFactory.merge(new Message(update.message(), object.toString()));
            OneBotWebSocketHandler.broadcast(object.toString());
            LogWebSocketHandler.broadcast(handleTextMessage(object.toString()));
        }
    }

    private static List<PhotoSize> getEveryPhoto(PhotoSize[] photo) {
        List<PhotoSize> photos = new ArrayList<>();
        for (int i = photo.length-1; i >= 0; i--) {
            if (i == photo.length-1 && photo[i] != null) {
                photos.add(photo[i]);
                continue;
            }
            if (photo[i] != null && !photo[i].fileId().equals(photo[i + 1].fileId())
                && !photo[i].fileUniqueId().startsWith(photo[i + 1].fileUniqueId().substring(0, photo[i + 1].fileUniqueId().length() - 1))) {
                photos.add(photo[i]);
            }
        }
        return photos;
    }

    public static InlineKeyboardMarkup buildMenuButtons() {
        Map<String, String> keyboardButtons = new LinkedHashMap<>(TelegramOnebotAdapter.config.getCommand().getMenu());

        List<InlineKeyboardButton> buttons = keyboardButtons.entrySet().stream()
            .map(entry -> {
                InlineKeyboardButton button = new InlineKeyboardButton(entry.getKey());
                if (entry.getValue().matches("^http(s)?://([\\w-]+\\.)+[\\w-]+(/[\\w- ./?%&=]*)?$")) {
                    button.url(entry.getValue());
                } else {
                    button.callbackData(entry.getValue());
                }
                return button;
            })
            .toList();

        InlineKeyboardButton[][] buttonArray = new InlineKeyboardButton[(keyboardButtons.size() + 2) / 3][];
        for (int i = 0; i < buttonArray.length; i++) {
            buttonArray[i] = buttons.subList(i * 3, Math.min((i + 1) * 3, keyboardButtons.size())).toArray(new InlineKeyboardButton[0]);
        }

        return new InlineKeyboardMarkup(buttonArray);
    }

    private static String serializeCommand(String realMessage) {
        String tempStr = String.copyValueOf(realMessage.toCharArray());
        Map<String, String> commandMap = TelegramOnebotAdapter.config.getCommand().getCommandMap();
        String prefix = TelegramOnebotAdapter.config.getCommand().getPrefix();
        for (Map.Entry<String, String> entry : commandMap.entrySet()) {
            String key = prefix+entry.getKey();
            String value = prefix+entry.getValue();
            tempStr = tempStr.replace(key + ' ', value + ' ');

            if (tempStr.endsWith(key)) {
                int lastIndex = tempStr.lastIndexOf(key);
                tempStr = tempStr.substring(0, lastIndex) + value + tempStr.substring(lastIndex + key.length());
            }
        }

        return tempStr;
    }

    public static String arrayMessageToString(JSONArray arrayMessage) {
        StringBuilder message = new StringBuilder();
        for (int i = 0; i < arrayMessage.size(); i++) {
            JSONObject messageObject = arrayMessage.getJSONObject(i);
            if (messageObject.getStr("type").equals("text")) {
                message.append(specialCharacterEscape(messageObject.getJSONObject("data").getStr("text")));
            } else if (messageObject.getStr("type").equals("at")) {
                message.append("[CQ:at,qq=").append(specialCharacterEscape(messageObject.getJSONObject("data").getStr("qq"))).append("]");
            } else if(messageObject.getStr("type").equals("image")) {
                message.append("[CQ:image,file=").append(specialCharacterEscape(messageObject.getJSONObject("data").getStr("file"))).append("]");
            }
        }
        return message.toString();
    }

    public static String stringMessageToArray(String message) {
        JsonArray arrayMessage = new JsonArray();
        Matcher matcher = Pattern.compile("\\[CQ:(\\S+?)(,\\S+)?]").matcher(message);
        int lastIndex = 0;
        while (matcher.find()) {
            String msg = message.substring(lastIndex, matcher.start());
            if (!msg.isEmpty()) {
                Messages.Text messageObject = new Messages.Text(specialCharacterUnescape(msg));
                arrayMessage.add(messageObject);
            }
            lastIndex = matcher.end();
            String type = matcher.group(1);
            String data = matcher.group(2);
            if (type.equals("at")) {
                Messages.At atObject = new Messages.At(Long.parseLong(data.substring(4)));
                arrayMessage.add(atObject);
            } else if (type.equals("image")) {
                Messages.Image imageObject = new Messages.Image(data.substring(5));
                arrayMessage.add(imageObject);
            } else if (type.equals("record")) {
                Messages.Record recordObject = new Messages.Record(data.substring(5));
                arrayMessage.add(recordObject);
            }
        }
        String msg = message.substring(lastIndex);
        if (!msg.isEmpty()) {
            Messages.Text messageObject = new Messages.Text(specialCharacterUnescape(msg));
            arrayMessage.add(messageObject);
        }
        return arrayMessage.toString();
    }

    private static String specialCharacterEscape(String message) {
        return message.replace("&", "&amp;")
                .replace("[", "&#91;")
                .replace("]", "&#93;")
                .replace(",", "&#44;");
    }

    private static String specialCharacterUnescape(String message) {
        return message.replace("&#44;", ",")
                .replace("&#93;", "]")
                .replace("&#91;", "[")
                .replace("&amp;", "&");
    }

    @Override
    public void run(ApplicationArguments args) {
        log.info("Telegram to Onebot converter is running...");
        HibernateUtil.init(TelegramOnebotAdapter.INSTANCE);
        TelegramApi.init();
    }


    public static String handleTextMessage(String message) {
        final JSONObject jsonObject = new JSONObject(message);
        if (jsonObject.isNull("sender")) {
            LogWebSocketHandler.broadcast(jsonObject.toString());
            return jsonObject.toString();
        }
        boolean isGroup = jsonObject.getInt("group_id") != null;

        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(DateUtil.format(new Date(jsonObject.getLong("time")), "yyyy-MM-dd HH:mm:ss")).append(" ");
        stringBuilder.append("[");
        if (isGroup) {
            stringBuilder.append("群组").append(jsonObject.getInt("group_id")).append(" ");
        } else {
            stringBuilder.append("私聊 ");
        }
        stringBuilder.append(jsonObject.getJSONObject("sender").getStr("card")).append("(")
                .append(jsonObject.getJSONObject("sender").getStr("user_id")).append(")]: ")
                .append(TelegramToOnebot.arrayMessageToString(jsonObject.getJSONArray("message")));

        return stringBuilder.toString();
    }
}
