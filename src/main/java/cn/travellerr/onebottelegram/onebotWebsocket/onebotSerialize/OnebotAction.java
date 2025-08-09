package cn.travellerr.onebottelegram.onebotWebsocket.onebotSerialize;

import cn.chahuyun.hibernateplus.HibernateFactory;
import cn.hutool.core.date.DateUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.travellerr.onebotApi.*;
import cn.travellerr.onebottelegram.TelegramOnebotAdapter;
import cn.travellerr.onebottelegram.converter.LanguageCode;
import cn.travellerr.onebottelegram.converter.TelegramToOnebot;
import cn.travellerr.onebottelegram.converter.Translator;
import cn.travellerr.onebottelegram.hibernate.entity.Group;
import cn.travellerr.onebottelegram.telegramApi.TelegramApi;
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
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.List;

import static org.reflections.Reflections.log;

public class OnebotAction {
    public static void handleAction(WebSocketSession session, String payload) {
        JSONObject jsonObject = new JSONObject(payload);
        String action = jsonObject.getStr("action");
        String echo = jsonObject.getStr("echo", "0");
        try {
        long groupId, userId;
        int msgId;
        String message, title, groupName;
        JSONObject params = jsonObject.getJSONObject("params");

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
                groupId = -Math.abs(params.getLong("group_id"));
                session.sendMessage(getGroupMemberList(echo, groupId));
                break;
            case "get_group_info":
                groupId = -Math.abs(params.getLong("group_id"));
                session.sendMessage(getGroupInfo(echo, groupId));
                break;
            case "get_group_member_info":
                groupId = -params.getLong("group_id");
                userId = params.getLong("user_id");
                session.sendMessage(getGroupMemberInfo(echo, groupId, userId));
                break;
            case "get_msg":
                msgId = params.getInt("message_id");
                session.sendMessage(getMsg(echo, msgId));
                break;
            case "set_group_ban":
                groupId = -params.getLong("group_id");
                userId = params.getLong("user_id");
                int duration = params.getInt("duration", 0) == 0 ? 0 : Math.max(params.getInt("duration"), 30);
                session.sendMessage(setGroupBan(groupId, userId, duration));
                break;
            case "delete_msg":
                msgId = params.getInt("message_id");
                session.sendMessage(deleteMessage(echo, msgId));
                break;
            case "send_group_msg":
                groupId = -params.getLong("group_id");
                message = params.getStr("message");
                session.sendMessage(sendMessage(echo, groupId, message, true));
                break;
            case "send_private_msg":
                userId = params.getLong("user_id");
                message = params.getStr("message");
                session.sendMessage(sendMessage(echo, userId, message, false));
                break;
            case "send_msg":
                userId = params.getLong("user_id", -1L);
                groupId = -params.getLong("group_id", -1L);
                message = params.getStr("message");
                boolean isPrivate = userId != -1L;
                long targetId = isPrivate ? userId : groupId;
                session.sendMessage(sendMessage(echo, targetId, message, !isPrivate));
                break;
            case "set_group_special_title":
                groupId = -params.getLong("group_id");
                userId = params.getLong("user_id");
                title = params.getStr("special_title");
                TelegramApi.bot.execute(new SendChatAction(groupId, ChatAction.typing));
                TelegramApi.bot.execute(new PromoteChatMember(groupId, userId).canManageChat(true));
                TelegramApi.bot.execute(new SetChatAdministratorCustomTitle(groupId, userId, title));
                break;
            case "set_group_name":
                groupId = -params.getLong("group_id");
                groupName = params.getStr("group_name");
                TelegramApi.bot.execute(new SetChatTitle(groupId, groupName));
                break;
            case "set_group_kick":
                groupId = -params.getLong("group_id");
                userId = params.getLong("user_id");
                BaseResponse response = TelegramApi.bot.execute(new BanChatMember(groupId, userId).untilDate(30));
                System.out.println(response.description());
                break;
            case "set_group_admin":
                groupId = -params.getLong("group_id");
                userId = params.getLong("user_id");
                TelegramApi.bot.execute(new PromoteChatMember(groupId, userId).canManageChat(true));
                break;
            case "get_avatar":
                userId = params.getLong("user_id");
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

                JSONObject obj = new JSONObject(new Data(echo, true)).set("message", avatarId);
                session.sendMessage(new TextMessage(obj.toString()));
                break;
            default:
                log.error("未知的 OneBot 消息: {}", action);
                session.sendMessage(new TextMessage(new JSONObject(new Data(echo, "", 1404, "failed", "")).set("data", null).toString()));
                break;
        }
        } catch (Exception e) {
            log.error("处理 OneBot 消息失败", e);
        }
    }

    private static WebSocketMessage<?> getMsg(String echo, int msgId) {
        JSONObject msg;
        try {
            msg = HibernateFactory.selectOne(cn.travellerr.onebottelegram.hibernate.entity.Message.class, msgId)
                    .getMessage();
        } catch (NullPointerException e) {
            log.error("获取消息失败: {}", e.getMessage());
            return new TextMessage(new JSONObject(new Data(echo, "", 1404, "failed", "")).set("data", null).toString());
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
        msg.set("real_id", msg.get("message_id"));
        return new TextMessage(new JSONObject(new Data(echo, "", 0, "ok", "")).set("data", msg).toString());

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

        return new TextMessage(new JSONObject(new Data("0", status)).toString());
    }


    private static WebSocketMessage<?> getGroupInfo(String echo, long groupId) {
        ChatFullInfo info = TelegramApi.bot.execute(new GetChat(groupId)).chat();
        int count = TelegramApi.bot.execute(new GetChatMemberCount(groupId)).count();
        JSONObject object = new JSONObject(new Data(echo));
        object.set("data", new GroupInfo(Math.abs(groupId), info.title(), count, 2000));

        return new TextMessage(object.toString());
    }


    private static TextMessage message(String echo, Object message) {
        JSONObject object = new JSONObject(new Data(echo));
        JSONObject messages = new JSONObject(message);
        object.set("data", messages);
        log.info("发送消息至 Onebot --> {}", object);
        return new TextMessage(object.toString());
    }

    private static TextMessage deleteMessage(String echo, int msgId) {
        long chatId = -Math.abs(TelegramToOnebot.messageIdToChatId.get(msgId));
        TelegramApi.bot.execute(new DeleteMessage(chatId, Math.toIntExact(msgId)));
        JSONObject object = new JSONObject(new Data(echo)).set("data", new JSONArray());

        log.info("发送消息至 Onebot --> {}", object);
        return new TextMessage(object.toString());
    }

    private static TextMessage getFriendList(String echo) {
        JSONObject object = new JSONObject(new Data(echo));
        Friend friend = new Friend(0, "Tra", "Tra");
        List<Friend> friends = List.of(friend);
        JSONArray messages = new JSONArray(friends.toArray());
        object.set("data", messages);
        log.info("发送消息至 Onebot --> {}", object);
        return new TextMessage(object.toString());
    }

    public static TextMessage getGroupList(String echo) {
        JSONObject object = new JSONObject(new Data(echo));
        List<Group> groupList = HibernateFactory.selectList(Group.class);


        if (groupList == null) {
            object.set("data", new JSONObject(new GetGroupList(List.of())));
            return new TextMessage(object.toString());
        }


        List<GroupInfo> groupInfoList = new ArrayList<>();

        for (Group group : groupList) {
            groupInfoList.add(new GroupInfo(-group.getGroupId(), group.getGroupName(), group.getMemberCount(), group.getMaxMemberCount()));
        }

        JSONArray messages = new JSONArray(groupInfoList.toArray());

        object.set("data", messages);
        log.info("发送消息至 Onebot --> {}", object);
        return new TextMessage(object.toString());

    }

    private static TextMessage getGroupMemberList(String echo, long groupId) {
        JSONObject object = new JSONObject(new Data(echo));
        Group group = HibernateFactory.selectOne(Group.class, groupId);
        if (group == null) {
            object.set("data", new JSONObject(new GetGroupMemberListResponse(List.of())));
            return new TextMessage(object.toString());
        }

        List<Long> membersIdList = group.getMembersIdList();
        List<MemberInfo> memberInfoList = new ArrayList<>();
        for (Long memberId : membersIdList) {
            memberInfoList.add(getChatMember(groupId, memberId));
        }

        JSONArray messages = new JSONArray(memberInfoList.toArray());
        object.set("data", messages);
        log.info("发送消息至 Onebot --> {}", object);
        return new TextMessage(object.toString());
    }

    private static TextMessage getGroupMemberInfo(String echo, long groupId, long memberId) {
        JSONObject object = new JSONObject(new Data(echo));

        getChatMember(groupId, memberId);
        MemberInfo memberInfo = getChatMember(groupId, memberId);


        object.set("data", memberInfo);
        log.info("发送消息至 Onebot --> {}", object);
        return new TextMessage(object.toString());
    }

    public static TextMessage sendMessage(String echo, long chatId, String messageStr, boolean isGroup) {
        String realMessage = messageStr;
        if (!TelegramOnebotAdapter.config.getOnebot().isUseArray()) {
            realMessage = TelegramToOnebot.stringMessageToArray(messageStr);
        }
        JSONArray messageArray = new JSONArray(realMessage);

        LanguageCode languageCode = LanguageCode.ZH_HANS;

        StringBuilder sb = new StringBuilder();
        SendPhoto photo = null;
        ReplyParameters replyParameters = null;

        for(Object m : messageArray) {

            JSONObject msg = (JSONObject) m;

            switch (msg.getStr("type")) {
                case "at":
                    Long userId = msg.getJSONObject("data").getLong("qq");
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
                    String message = msg.getJSONObject("data").getStr("text");
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
                    String filePath = msg.getJSONObject("data").getStr("file");
                    if (filePath.startsWith("http")) {
                        photo = new SendPhoto(chatId, filePath);
                    } else if(filePath.startsWith("base64://")) {
                        byte[] bytes = Base64.getDecoder().decode(filePath.substring(9));
                        photo = new SendPhoto(chatId, bytes);
                    }
                    else {
                        File file = new File(filePath.replaceFirst("^file://", ""));
                        photo = new SendPhoto(chatId, file);
                    }
                    break;
                case "reply":
                    replyParameters = new ReplyParameters(Integer.valueOf(msg.getJSONObject("data").getStr("id")));
                    break;
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
        } else {
            SendMessage request = new SendMessage(chatId, text);
            request.parseMode(ParseMode.HTML);
            if (replyParameters != null) {
                request.replyParameters(replyParameters);
            }

            response = TelegramApi.bot.execute(request);
        }


        if (!response.isOk()) {
            JSONObject obj = new JSONObject().set("error_code", response.description());
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
            return new TextMessage(new JSONObject(new Data(echo, "", 1404, "failed", "")).set("data", null).toString());
        } else {
            TelegramToOnebot.messageIdToChatId.put(response.message().messageId(), chatId);
            JSONObject obj = new JSONObject().set("message_id", response.message().messageId());

            JSONObject object = new JSONObject(new Data(echo, "", 0, "ok", "")).set("data", obj);
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
}
