package cn.travellerr.onebottelegram.onebotWebsocket.onebotSerialize;

import cn.chahuyun.hibernateplus.HibernateFactory;
import cn.hutool.core.date.DateUtil;
import cn.travellerr.onebotApi.*;
import cn.travellerr.onebottelegram.TelegramOnebotAdapter;
import cn.travellerr.onebottelegram.converter.AudioConverter;
import cn.travellerr.onebottelegram.converter.LanguageCode;
import cn.travellerr.onebottelegram.converter.TelegramToOnebot;
import cn.travellerr.onebottelegram.converter.Translator;
import cn.travellerr.onebottelegram.hibernate.entity.Group;
import cn.travellerr.onebottelegram.model.ApiRequest;
import cn.travellerr.onebottelegram.model.Messages;
import cn.travellerr.onebottelegram.telegramApi.TelegramApi;
import com.google.gson.*;
import com.pengrad.telegrambot.model.*;
import com.pengrad.telegrambot.model.request.ChatAction;
import com.pengrad.telegrambot.model.request.ParseMode;
import com.pengrad.telegrambot.model.request.ReplyParameters;
import com.pengrad.telegrambot.request.SendMessage;
import com.pengrad.telegrambot.request.*;
import com.pengrad.telegrambot.response.BaseResponse;
import com.pengrad.telegrambot.response.GetChatResponse;
import com.pengrad.telegrambot.response.SendResponse;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.List;

import static org.reflections.Reflections.log;

public class OnebotAction {

    public static final Gson GSON = new Gson();

    public static void handleAction(WebSocketSession session, String payload) {
        ApiRequest.BaseApiRequest jsonObject = GSON.fromJson(payload, ApiRequest.BaseApiRequest.class);
        String action = jsonObject.getAction();
        String echo = jsonObject.getEcho();
        try {
        long groupId, userId;
        int msgId;
        String message, title, groupName;
        ApiRequest.Params params = jsonObject.getParams();

        switch (action) {
            case "get_version_info":
                session.sendMessage(message(echo, new GetVersionInfo("Tele-KiraLink", TelegramOnebotAdapter.VERSION, "v11")));
                log.info("发送消息至 Onebot --> {}", new GetVersionInfo("Tele-KiraLink", TelegramOnebotAdapter.VERSION, "v11"));
                break;
            case "get_login_info":
                session.sendMessage(message(echo, new GetLoginInfo(TelegramApi.getMeResponse.user().id(), TelegramApi.getMeResponse.user().username())));
                break;
            case "get_friend_list":
                session.sendMessage(getFriendList(echo));
                break;
            case "get_group_list":
                session.sendMessage(getGroupList(echo));
                break;
            case "get_group_member_list":
                groupId = -Math.abs(params.getGroupId());
                session.sendMessage(getGroupMemberList(echo, groupId));
                break;
            case "get_group_info":
                groupId = -Math.abs(params.getGroupId());
                session.sendMessage(getGroupInfo(echo, groupId));
                break;
            case "get_group_member_info":
                groupId = -params.getGroupId();
                userId = params.getUserId();
                session.sendMessage(getGroupMemberInfo(echo, groupId, userId));
                break;
            case "get_msg":
                msgId = params.getMessageId();
                session.sendMessage(getMsg(echo, msgId));
                break;
            case "set_group_ban":
                groupId = -params.getGroupId();
                userId = params.getUserId();
                int duration = params.getDuration() == 0 ? 0 : Math.max(params.getDuration(), 30);
                session.sendMessage(setGroupBan(groupId, userId, duration));
                break;
            case "delete_msg":
                msgId = params.getMessageId();
                session.sendMessage(deleteMessage(echo, msgId));
                break;
            case "send_group_msg":
                groupId = -params.getGroupId();
                message = params.getMessage().toString();
                session.sendMessage(sendMessage(echo, groupId, message, true));
                break;
            case "send_private_msg":
                userId = params.getUserId();
                message = params.getMessage().toString();
                session.sendMessage(sendMessage(echo, userId, message, false));
                break;
            case "send_msg":
                userId = params.getUserId();
                groupId = -params.getGroupId();
                message = params.getMessage().toString();
                boolean isPrivate = userId != -1L;
                long targetId = isPrivate ? userId : groupId;
                session.sendMessage(sendMessage(echo, targetId, message, !isPrivate));
                break;
            case "set_group_special_title":
                groupId = -params.getGroupId();
                userId = params.getUserId();
                title = params.getSpecialTitle();
                TelegramApi.bot.execute(new SendChatAction(groupId, ChatAction.typing));
                TelegramApi.bot.execute(new PromoteChatMember(groupId, userId).canManageChat(true));
                TelegramApi.bot.execute(new SetChatAdministratorCustomTitle(groupId, userId, title));
                break;
            case "set_group_name":
                groupId = -params.getGroupId();
                groupName = params.getGroupName();
                TelegramApi.bot.execute(new SetChatTitle(groupId, groupName));
                break;
            case "set_group_kick":
                groupId = -params.getGroupId();
                userId = params.getUserId();
                BaseResponse response = TelegramApi.bot.execute(new BanChatMember(groupId, userId).untilDate(30));
                System.out.println(response.description());
                break;
            case "set_group_admin":
                groupId = -params.getGroupId();
                userId = params.getUserId();
                TelegramApi.bot.execute(new PromoteChatMember(groupId, userId).canManageChat(true));
                break;
            case "get_avatar":
                userId = params.getUserId();
                UserProfilePhotos userProfilePhotos = TelegramApi.bot.execute(new GetUserProfilePhotos(userId)).photos();
                String avatarId = "";

                if (userProfilePhotos != null && userProfilePhotos.photos().length > 0) {
                    com.pengrad.telegrambot.model.PhotoSize[] photoSizes = userProfilePhotos.photos()[0];
                    if (photoSizes.length > 2) {
                        avatarId = TelegramApi.bot.getFullFilePath(
                            TelegramApi.bot.execute(new GetFile(photoSizes[2].fileId())).file()
                        );
                    }
                }

                JsonObject obj = data(echo, true);
                obj.addProperty("message", avatarId);
                session.sendMessage(new TextMessage(obj.toString()));
                break;
            case "get_status":
                JsonObject statusObject = data(echo, true);
                statusObject.addProperty("online", true);
                statusObject.addProperty("good", true);
                session.sendMessage(new TextMessage(statusObject.toString()));
                break;
            default:
                log.error("未知的 OneBot 消息: {}", action);
                JsonObject object = GSON.fromJson(new Data(echo, "", 1404, "failed", "").toString(), JsonObject.class);
                object.add("data", JsonNull.INSTANCE);
                session.sendMessage(new TextMessage(object.toString()));
                break;
        }
        } catch (Exception e) {
            log.error("处理 OneBot 消息失败", e);
        }
    }

    private static WebSocketMessage<?> getMsg(String echo, int msgId) {
        JsonObject msg;
        try {
            msg = HibernateFactory.selectOne(cn.travellerr.onebottelegram.hibernate.entity.Message.class, msgId)
                    .getMessage();
        } catch (NullPointerException e) {
            log.error("获取消息失败: {}", e.getMessage());
            return new TextMessage(data(echo, "", 1404, "failed", "").toString());
        }
        msg.remove("self_id");
        msg.remove("post_type");
        msg.remove("sub_type");
        msg.remove("font");
        msg.remove("raw_message");
        msg.remove("user_id");
        try {
           msg.remove("anonymous");
           msg.remove("group_id");
        } catch (Exception ignored) {
        }
        msg.add("real_id", msg.get("message_id"));
        return new TextMessage(
                data(echo, "", 0, "ok", "", msg).toString()
        );

    }

    private static WebSocketMessage<?> setGroupBan(long groupId, long userId, int duration) {
        BaseResponse response;
        if (duration != 0) {
            int offset = (int) (DateUtil.offsetSecond(new Date(), duration).getTime() / 1000);
            response = TelegramApi.bot.execute(new RestrictChatMember(groupId, userId, new ChatPermissions().canSendMessages(false).canPinMessages(false).canSendPhotos(false).canSendVideos(false)).untilDate(offset));
        } else {
            response = TelegramApi.bot.execute(new RestrictChatMember(groupId, userId, new ChatPermissions()
                    .canSendMessages(true)
                    .canSendAudios(true)
                    .canSendDocuments(true)
                    .canSendPhotos(true)
                    .canSendVideos(true)
                    .canSendVideoNotes(true)
                    .canSendVoiceNotes(true)
                    .canSendPolls(true)
                    .canSendOtherMessages(true)
                    .canAddWebPagePreviews(true)
                    .canChangeInfo(true)
                    .canInviteUsers(true)
                    .canPinMessages(true)
                    .canManageTopics(true)
                    .canPostStories(true)
                    .canEditStories(true)
                    .canDeleteStories(true)));
        }
        boolean status = response.isOk();

        return new TextMessage(data("0", status).toString());
    }


    private static WebSocketMessage<?> getGroupInfo(String echo, long groupId) {
        ChatFullInfo info = TelegramApi.bot.execute(new GetChat(groupId)).chat();
        int count = TelegramApi.bot.execute(new GetChatMemberCount(groupId)).count();
        JsonObject object = data(echo);
        object.add("data", GSON.toJsonTree(new GroupInfo(Math.abs(groupId), info.title(), count, 2000)));

        return new TextMessage(object.toString());
    }


    private static TextMessage message(String echo, Object message) {
        JsonObject object = data(echo);
        JsonElement messages = GSON.toJsonTree(message);
        object.add("data", messages);
        log.info("发送消息至 Onebot --> {}", object);
        return new TextMessage(object.toString());
    }

    private static TextMessage deleteMessage(String echo, int msgId) {
        long chatId = TelegramToOnebot.messageIdToChatId.get(msgId);
        System.out.println("删除消息: " + chatId + " " + msgId);
        TelegramApi.bot.execute(new DeleteMessage(chatId, Math.toIntExact(msgId)));
        JsonObject object = data(echo);
        object.add("data", new JsonArray());
        log.info("发送消息至 Onebot --> {}", object);
        return new TextMessage(object.toString());
    }

    private static TextMessage getFriendList(String echo) {
        JsonObject object = data(echo);
        Friend friend = new Friend(0, "Tele-KiraLink", "Tele-KiraLink");
        List<Friend> friends = List.of(friend);
        JsonArray messages = GSON.toJsonTree(friends).getAsJsonArray();
        object.add("data", messages);

        log.info("发送消息至 Onebot --> {}", object);
        return new TextMessage(object.toString());
    }

    public static TextMessage getGroupList(String echo) {
        JsonObject object = data(echo);

        List<Group> groupList = HibernateFactory.selectList(Group.class);

        if (groupList == null) {
            object.add("data", GSON.toJsonTree(new GetGroupList(List.of())));
            return new TextMessage(object.toString());
        }

        List<GroupInfo> groupInfoList = new ArrayList<>();

        for (Group group : groupList) {
            groupInfoList.add(new GroupInfo(-group.getGroupId(), group.getGroupName(), group.getMemberCount(), group.getMaxMemberCount()));
        }

        JsonArray messages = GSON.toJsonTree(groupInfoList).getAsJsonArray();

        object.add("data", messages);
        log.info("发送消息至 Onebot --> {}", object);
        return new TextMessage(object.toString());

    }

    private static TextMessage getGroupMemberList(String echo, long groupId) {
        JsonObject object = data(echo);
        Group group = HibernateFactory.selectOne(Group.class, groupId);
        if (group == null) {
            object.add("data", GSON.toJsonTree(new GetGroupMemberListResponse(List.of())));
            return new TextMessage(object.toString());
        }

        List<Long> membersIdList = group.getMembersIdList();
        List<MemberInfo> memberInfoList = new ArrayList<>();
        for (Long memberId : membersIdList) {
            memberInfoList.add(getChatMember(groupId, memberId));
        }

        JsonArray messages = GSON.toJsonTree(memberInfoList).getAsJsonArray();
        object.add("data", messages);
        log.info("发送消息至 Onebot --> {}", object);
        return new TextMessage(object.toString());
    }

    private static TextMessage getGroupMemberInfo(String echo, long groupId, long memberId) {
        JsonObject object = data(echo);

        getChatMember(groupId, memberId);
        MemberInfo memberInfo = getChatMember(groupId, memberId);


        object.add("data", GSON.toJsonTree(memberInfo));
        log.info("发送消息至 Onebot --> {}", object);
        return new TextMessage(object.toString());
    }

    public static TextMessage sendMessage(String echo, long chatId, String messageStr, boolean isGroup) {
        String realMessage = messageStr;
        System.out.println(realMessage);
        if (!TelegramOnebotAdapter.config.getOnebot().isUseArray()) {
            realMessage = TelegramToOnebot.stringMessageToArray(messageStr);
        }

        JsonArray messageArray = JsonParser.parseString(realMessage).getAsJsonArray();

        LanguageCode languageCode = LanguageCode.ZH_HANS;

        StringBuilder sb = new StringBuilder();
        SendPhoto photo = null;
        SendAudio audio = null;
        String convertedAudioPath = null;
        ReplyParameters replyParameters = null;

        for(JsonElement m : messageArray) {
            Messages.BaseMessage baseMessage = GSON.fromJson(m.toString(), Messages.BaseMessage.class);


            switch (baseMessage.getType()) {
                case "at":
                    Long userId = baseMessage.getData().getQq();
                    String username, firstName;

                    if (isGroup) {
                        User user = TelegramApi.bot.execute(new GetChatMember(chatId, userId)).chatMember().user();
                        languageCode = LanguageCode.parseLanguageCode(user.languageCode());
                        username = user.username();
                        firstName = user.firstName();
                    } else {
                        ChatFullInfo fullInfo = TelegramApi.bot.execute(new GetChat(chatId)).chat();
                        userId = chatId;
                        username = fullInfo.username();
                        firstName = fullInfo.firstName();
                    }

                    sb.append(username != null ? "@" + username : "<a href=\"tg://user?id=" + userId + "\">" + firstName + "</a>");
                    break;
                case "text":
                    String message = baseMessage.getData().getText();
                    if (!message.startsWith("html://")) {
                        message = message
                                .replace("&", "&amp;")
                                .replace("<", "&lt;")
                                .replace(">", "&gt;")
                                .replace("\"", "&quot;");
                    } else {
                        message = message.substring(7);
                    }
                    if (TelegramOnebotAdapter.config.getTelegram().getBot().isUseTranslator() && !languageCode.equals(LanguageCode.ZH_HANS)) {
                        message = Translator.Trans(languageCode, message);
                    }
                    sb.append(message);
                    break;
                case "image":
                    String imageFilePath = baseMessage.getData().getFile();
                    if (imageFilePath.startsWith("http")) {
                        photo = new SendPhoto(chatId, imageFilePath);
                    } else if(imageFilePath.startsWith("base64://")) {
                        byte[] bytes = Base64.getDecoder().decode(imageFilePath.substring(9));
                        photo = new SendPhoto(chatId, bytes);
                    } else {
                        File file = new File(imageFilePath.replaceFirst("^file://", ""));
                        photo = new SendPhoto(chatId, file);
                    }
                    break;
                case "reply":
                    replyParameters = new ReplyParameters(baseMessage.getData().getId());
                    break;
                case "record":
                    if (TelegramOnebotAdapter.config.getSpring().getFfmpegPath().isEmpty()) {
                        sb.append("[未配置ffmpeg，无法发送语音消息]");
                        break;
                    }
                    String recordFilePath = baseMessage.getData().getFile();
                    if (recordFilePath == null || recordFilePath.isEmpty()) {
                        sb.append("[语音消息文件路径为空]");
                        break;
                    }
                    convertedAudioPath = AudioConverter.convertToTelegramAudio(recordFilePath);
                    if (convertedAudioPath != null) {
                        File audioFile = new File(convertedAudioPath);
                        if (audioFile.exists()) {
                            audio = new SendAudio(chatId, audioFile);
                        } else {
                            sb.append("[音频文件不存在]");
                        }
                    } else {
                        sb.append("[无法转换语音消息]");
                    }

            }
        }

        String text = sb.toString();
        SendResponse response;

        if (photo != null) {
            photo.caption(text);
            photo.parseMode(ParseMode.HTML);
            if (replyParameters != null) {
                photo.replyParameters(replyParameters);
            }

            response = TelegramApi.bot.execute(photo);
        } else if (audio != null) {
            audio.caption(text);
            audio.parseMode(ParseMode.HTML);
            if (replyParameters != null) {
                audio.replyParameters(replyParameters);
            }

            response = TelegramApi.bot.execute(audio);

            try {
                if (convertedAudioPath != null) {
                    Files.deleteIfExists(Paths.get(convertedAudioPath));
                }
            } catch (Exception e) {
                log.warn("清理临时音频文件失败: {}", e.getMessage());
            }

        } else {
            SendMessage request = new SendMessage(chatId, text);
            request.parseMode(ParseMode.HTML);
            if (replyParameters != null) {
                request.replyParameters(replyParameters);
            }

            response = TelegramApi.bot.execute(request);
        }



        if (!response.isOk()) {
            JsonObject obj = new JsonObject();
            obj.addProperty("error_code", response.description());
            int messageId = TelegramApi.bot.execute(new SendMessage(chatId, "发送失败: " + response.description())).message().messageId();

            new Thread(() -> {
                try {
                    Thread.sleep(10000);
                    TelegramApi.bot.execute(new DeleteMessage(chatId, messageId));
                } catch (Exception e) {
                    log.error("删除失败", e);
                }
            }).start();

            log.info("发送消息至 Onebot --> {}", obj);
            return new TextMessage(data(echo, "", 1404, "failed", "", null).toString());
        } else {
            TelegramToOnebot.messageIdToChatId.put(response.message().messageId(), isGroup ? -Math.abs(chatId) : Math.abs(chatId));

            JsonObject obj = new JsonObject();
            obj.addProperty("message_id", response.message().messageId());
            JsonObject object = data(echo, "", 0, "ok", "", obj);
            log.info("发送消息至 Onebot --> {}", object);
            return new TextMessage(object.toString());
        }
    }




    public static MemberInfo getChatMember(long groupId, long memberId) {
        ChatMember chat = TelegramApi.bot.execute(new GetChatMember(groupId, memberId)).chatMember();
        if (chat == null) {
            Group group = HibernateFactory.selectOne(Group.class, groupId);
            GetChatResponse response = TelegramApi.bot.execute(new GetChat(groupId));
            if (response.chat() == null) {
                HibernateFactory.delete(group);
                return null;
            }
            return null;
        }
        String title = "", status = chat.status().toString();
        String username = chat.user().username();
        if (username == null) {
            username = chat.user().firstName();
        }
        if (chat.canPromoteMembers()) {
            status="creator";
        }
        return new MemberInfo(Math.abs(groupId), memberId, username, chat.user().firstName(), "unknown", 0, "虚拟地区", 0, 0, "0",levelConverter(status),false , title,0 ,chat.canChangeInfo());
    }


    private static String levelConverter(String memberStatus) {
        return switch (memberStatus) {
            case "creator" -> "owner";
            case "administrator" -> "admin";
            default -> "member";
        };
    }

    private static JsonObject data(String echo) {
        return JsonParser.parseString(new Data(echo).toString()).getAsJsonObject();
    }

    private static JsonObject data(String echo, boolean status) {
        return JsonParser.parseString(new Data(echo, status).toString()).getAsJsonObject();
    }

    private static JsonObject data(String echo, String message, int retcode, String status, String wording) {
        return JsonParser.parseString(new Data(echo, message, retcode, status, wording).toString()).getAsJsonObject();
    }

    private static JsonObject data(String echo, String message, int retcode, String status, String wording, JsonElement data) {
        JsonObject obj = JsonParser.parseString(new Data(echo, message, retcode, status, wording).toString()).getAsJsonObject();
        if (data != null) {
            obj.add("data", data);
        } else {
            obj.add("data", JsonNull.INSTANCE);
        }
        return obj;
    }
}
