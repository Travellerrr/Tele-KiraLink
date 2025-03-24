package cn.travellerr.onebottelegram.onebotWebsocket.onebotSerialize;

import cn.chahuyun.hibernateplus.HibernateFactory;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.travellerr.onebotApi.*;
import cn.travellerr.onebottelegram.hibernate.entity.Group;
import cn.travellerr.onebottelegram.telegramApi.TelegramApi;
import com.pengrad.telegrambot.model.ChatMember;
import com.pengrad.telegrambot.model.UserProfilePhotos;
import com.pengrad.telegrambot.request.GetChatMember;
import com.pengrad.telegrambot.request.GetUserProfilePhotos;
import com.pengrad.telegrambot.request.SendMessage;
import com.pengrad.telegrambot.response.SendResponse;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.reflections.Reflections.log;

public class OnebotAction {
    public static void handleAction(WebSocketSession session, String payload) {
        System.out.println(payload);
        JSONObject jsonObject = new JSONObject(payload);
        String action = jsonObject.getStr("action");
        int echo = jsonObject.getInt("echo");
        try {
            switch (action) {
                case "get_version_info":
                    session.sendMessage(message(echo, new GetVersionInfo("TelegramAdapter", "1.0.0", "v11")));
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
                    long groupId = jsonObject.getJSONObject("params").getLong("group_id");
                    session.sendMessage(getGroupMemberList(echo, groupId));
                    break;
                case "get_group_member_info":
                    long groupId1 = jsonObject.getJSONObject("params").getLong("group_id");
                    long memberId = jsonObject.getJSONObject("params").getLong("user_id");
                    session.sendMessage(getGroupMemberInfo(echo, groupId1, memberId));
                    break;
                case "send_group_msg":
                    long groupId2 = jsonObject.getJSONObject("params").getLong("group_id");
                    String message = jsonObject.getJSONObject("params").getStr("message");
                    session.sendMessage(sendGroupMessage(echo, groupId2, message));
                    break;
                case "get_avatar":
                    long userId = jsonObject.getJSONObject("params").getLong("user_id");
                    break;



            }
        } catch (Exception e) {
            log.error("处理 OneBot 消息失败", e);
        }
    }


    private static TextMessage message(int echo, Object message) {
        JSONObject object = new JSONObject(new Data(echo));
        JSONObject messages = new JSONObject(message);
        object.set("data", messages);
        System.out.println(object);
        return new TextMessage(object.toString());
    }

    private static TextMessage getFriendList(int echo) {
        JSONObject object = new JSONObject(new Data(echo));
        Friend friend = new Friend(0, "Tra", "Tra");
        List<Friend> friends = List.of(friend);
        JSONArray messages = new JSONArray(friends.toArray());
        object.set("data", messages);
        System.out.println(object);
        return new TextMessage(object.toString());
    }

    private static TextMessage getGroupList(int echo) {
        JSONObject object = new JSONObject(new Data(echo));
        List<Group> groupList = HibernateFactory.selectList(Group.class);


        if (groupList == null) {
            object.set("data", new JSONObject(new GetGroupList(List.of())));
            return new TextMessage(object.toString());
        }


        List<GroupInfo> groupInfoList = new ArrayList<>();

        for (Group group : groupList) {
            groupInfoList.add(new GroupInfo(group.getGroupId(), group.getGroupName(), group.getMemberCount(), group.getMaxMemberCount()));
        }

        JSONArray messages = new JSONArray(groupInfoList.toArray());

        object.set("data", messages);
        System.out.println(object);
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
        System.out.println(object);
        return new TextMessage(object.toString());
    }

    private static TextMessage getGroupMemberInfo(int echo, long groupId, long memberId) {
        JSONObject object = new JSONObject(new Data(echo));
        Group group = HibernateFactory.selectOne(Group.class, groupId);
        if (group == null) {
            object.set("data", new JSONObject(new GetGroupMemberListResponse(List.of())));
            return new TextMessage(object.toString());
        }

        getChatMember(groupId, memberId);
        MemberInfo memberInfo = getChatMember(groupId, memberId);


        object.set("data", memberInfo);
        System.out.println(object);
        return new TextMessage(object.toString());
    }

    private static TextMessage sendGroupMessage(int echo, long groupId, String messageStr) {
        System.out.println("消息"+messageStr);
        JSONArray messageArray = new JSONArray(messageStr);
        JSONObject message = messageArray.getJSONObject(0);

        StringBuilder sb = new StringBuilder();

        messageArray.forEach(m -> {
            JSONObject msg = (JSONObject) m;

            if (msg.getStr("type").equals("at")) {
                ChatMember member = TelegramApi.bot.execute(new GetChatMember(groupId, msg.getJSONObject("data").getLong("qq"))).chatMember();
                sb.append("@").append(member.user().username());
            } else
            if (msg.getStr("type").equals("text")) {
                sb.append(msg.getJSONObject("data").getStr("text"));
            }
        });

        String text = sb.toString();
        SendMessage request = new SendMessage(groupId, text);
        SendResponse response = TelegramApi.bot.execute(request);

        if (!response.isOk()) {
            return new TextMessage(new JSONObject(new Data(echo, response.description(), 0, "failed", "")).toString());
        } else {
            return new TextMessage(new JSONObject(new Data(echo, "{\"message_id\": "+response.message().messageId()+"}", 0, "ok", "")).toString());
        }
    }

/*    private static TextMessage getAvatar(long userId) {
        UserProfilePhotos photos = TelegramApi.bot.execute(new GetUserProfilePhotos(userId)).photos();

        String fileId = photos.photos()[0][0].fileId();
    }*/

    private static MemberInfo getChatMember(long groupId, long memberId) {
        ChatMember chat = TelegramApi.bot.execute(new GetChatMember(groupId, memberId)).chatMember();
        String title = chat.customTitle();
        if (title == null) {
            title = "";
        }
        return new MemberInfo(groupId, memberId, chat.user().username(), chat.user().firstName(), "unknown", 0, "虚拟地区", 0, 0, "0",levelConverter(String.valueOf(chat.status())),false , title,0 ,chat.canChangeInfo());
    }

    private static String levelConverter(String memberStatus) {
        return switch (memberStatus) {
            case "creator" -> "owner";
            case "administrator" -> "admin";
            default -> "member";
        };
    }
}
