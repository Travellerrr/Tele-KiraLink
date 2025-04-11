package cn.travellerr.onebottelegram.onebotWebsocket.onebotSerialize;

import cn.chahuyun.hibernateplus.HibernateFactory;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.travellerr.onebotApi.*;
import cn.travellerr.onebottelegram.TelegramOnebotAdapter;
import cn.travellerr.onebottelegram.converter.TelegramToOnebot;
import cn.travellerr.onebottelegram.hibernate.entity.Group;
import cn.travellerr.onebottelegram.telegramApi.TelegramApi;
import com.pengrad.telegrambot.model.ChatFullInfo;
import com.pengrad.telegrambot.model.ChatMember;
import com.pengrad.telegrambot.model.request.ReplyParameters;
import com.pengrad.telegrambot.request.SendMessage;
import com.pengrad.telegrambot.request.*;
import com.pengrad.telegrambot.response.GetChatResponse;
import com.pengrad.telegrambot.response.SendResponse;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.File;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import static org.reflections.Reflections.log;

public class OnebotAction {
    public static void handleAction(WebSocketSession session, String payload) {
        JSONObject jsonObject = new JSONObject(payload);
        String action = jsonObject.getStr("action");
        int echo = jsonObject.getInt("echo");
        try {
        long groupId, userId;
        int msgId;
        String message, title, groupName;
        JSONObject params = jsonObject.getJSONObject("params");

        switch (action) {
            case "get_version_info":
                session.sendMessage(message(echo, new GetVersionInfo("TelegramAdapter", TelegramOnebotAdapter.VERSION, "v11")));
                log.info("发送消息至 Onebot --> {}", new GetVersionInfo("TelegramAdapter", TelegramOnebotAdapter.VERSION, "v11"));
                break;
            case "get_login_info":
                session.sendMessage(message(echo, new GetLoginInfo(TelegramApi.getMeResponse.user().id(), TelegramApi.getMeResponse.user().username())));
                log.info("发送消息至 Onebot --> {}", new GetLoginInfo(TelegramApi.getMeResponse.user().id(), TelegramApi.getMeResponse.user().username()));
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
            case "set_group_special_title":
                groupId = -params.getLong("group_id");
                userId = params.getLong("user_id");
                title = params.getStr("special_title");
                TelegramApi.bot.execute(new PromoteChatMember(groupId, userId).canChangeInfo(false).canDeleteMessages(false).canInviteUsers(false).canRestrictMembers(false).canPinMessages(false).canPromoteMembers(false));
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
                TelegramApi.bot.execute(new BanChatMember(groupId, userId).untilDate(0));
                break;
            case "set_group_admin":
                groupId = -params.getLong("group_id");
                userId = params.getLong("user_id");
                TelegramApi.bot.execute(new PromoteChatMember(groupId, userId).canChangeInfo(false).canDeleteMessages(false).canInviteUsers(false).canRestrictMembers(false).canPinMessages(false).canPromoteMembers(false));
                break;
            case "get_avatar":
                userId = params.getLong("user_id");
                JSONObject obj = new JSONObject().set("echo", echo).set("message", "https://avatars.githubusercontent.com/u/139743802?v=4&size=256").set("retcode", 0).set("user_id", userId);
                session.sendMessage(new TextMessage(obj.toString()));
                break;
            default:
                log.error("未知的 OneBot 消息: {}", action);
                session.sendMessage(new TextMessage(new JSONObject(new Data(echo, "", 0, "failed", "")).set("data", new JSONObject().set("error_code", "unknown_action")).toString()));
        }
        } catch (Exception e) {
            log.error("处理 OneBot 消息失败", e);
        }
    }

    private static WebSocketMessage<?> getGroupInfo(int echo, long groupId) {
        ChatFullInfo info = TelegramApi.bot.execute(new GetChat(groupId)).chat();
        int count = TelegramApi.bot.execute(new GetChatMemberCount(groupId)).count();
        JSONObject object = new JSONObject(new Data(echo));
        object.set("data", new GroupInfo(Math.abs(groupId), info.title(), count, 2000));

        return new TextMessage(object.toString());
    }


    private static TextMessage message(int echo, Object message) {
        JSONObject object = new JSONObject(new Data(echo));
        JSONObject messages = new JSONObject(message);
        object.set("data", messages);
        return new TextMessage(object.toString());
    }

    private static TextMessage deleteMessage(int echo, int msgId) {
        long chatId = -Math.abs(TelegramToOnebot.messageIdToChatId.get(msgId));
        TelegramApi.bot.execute(new DeleteMessage(chatId, Math.toIntExact(msgId)));
        JSONObject object = new JSONObject(new Data(echo)).set("data", new JSONArray());

        log.info("发送消息至 Onebot --> {}", object);
        return new TextMessage(object.toString());
    }

    private static TextMessage getFriendList(int echo) {
        JSONObject object = new JSONObject(new Data(echo));
        Friend friend = new Friend(0, "Tra", "Tra");
        List<Friend> friends = List.of(friend);
        JSONArray messages = new JSONArray(friends.toArray());
        object.set("data", messages);
        log.info("发送消息至 Onebot --> {}", object);
        return new TextMessage(object.toString());
    }

    public static TextMessage getGroupList(int echo) {
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

    private static TextMessage getGroupMemberList(int echo, long groupId) {
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

    private static TextMessage getGroupMemberInfo(int echo, long groupId, long memberId) {
        JSONObject object = new JSONObject(new Data(echo));

        getChatMember(groupId, memberId);
        MemberInfo memberInfo = getChatMember(groupId, memberId);


        object.set("data", memberInfo);
        log.info("发送消息至 Onebot --> {}", object);
        return new TextMessage(object.toString());
    }

    public static TextMessage sendMessage(int echo, long chatId, String messageStr, boolean isGroup) {
        String realMessage = messageStr;
        if (!TelegramOnebotAdapter.config.getOnebot().isUseArray()) {
            realMessage = TelegramToOnebot.stringMessageToArray(messageStr);
        }
        JSONArray messageArray = new JSONArray(realMessage);

        StringBuilder sb = new StringBuilder();
        SendPhoto photo = null;
        ReplyParameters replyParameters = null;

        for(Object m : messageArray) {

            JSONObject msg = (JSONObject) m;

            switch (msg.getStr("type")) {
                case "at":
                    String username;
                    if (isGroup) {
                        username = TelegramApi.bot.execute(new GetChatMember(chatId, msg.getJSONObject("data").getLong("qq"))).chatMember().user().username();
                    } else {
                        username = TelegramApi.bot.execute(new GetChat(chatId)).chat().username();
                    }
                    sb.append("@").append(username);
                    break;
                case "text":
                    sb.append(msg.getJSONObject("data").getStr("text"));
                    break;
                case "image":
                    if (msg.getJSONObject("data").getStr("file").startsWith("file://")) {
                        File file = new File(msg.getJSONObject("data").getStr("file").substring(7));
                        photo = new SendPhoto(chatId, file);
                    } else if(msg.getJSONObject("data").getStr("file").startsWith("base64://")) {
                        byte[] bytes = Base64.getDecoder().decode(msg.getJSONObject("data").getStr("file").substring(9));
                        photo = new SendPhoto(chatId, bytes);
                    }

                    else {
                        sb.append("[图片消息, 暂不支持传输]");
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

            if (replyParameters != null) {
                photo.replyParameters(replyParameters);
            }

            response = TelegramApi.bot.execute(photo);
        } else {
            SendMessage request = new SendMessage(chatId, text);

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
            return new TextMessage(new JSONObject(new Data(echo, "", 0, "failed", "")).set("data", obj).toString());
        } else {
            TelegramToOnebot.messageIdToChatId.put(response.message().messageId(), Math.abs(chatId));
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
        String title = "";
        String username = chat.user().username();
        if (username == null) {
            username = chat.user().firstName();
        }
        return new MemberInfo(Math.abs(groupId), memberId, username, chat.user().firstName(), "unknown", 0, "虚拟地区", 0, 0, "0",levelConverter(String.valueOf(chat.status())),false , title,0 ,chat.canChangeInfo());
    }


    private static String levelConverter(String memberStatus) {
        return switch (memberStatus) {
            case "creator" -> "owner";
            case "administrator" -> "admin";
            default -> "member";
        };
    }
}
