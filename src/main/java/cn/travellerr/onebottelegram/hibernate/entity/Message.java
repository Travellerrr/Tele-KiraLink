package cn.travellerr.onebottelegram.hibernate.entity;

import cn.hutool.json.JSONObject;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.pengrad.telegrambot.model.Chat;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Table(name = "Message")
@Entity
public class Message {
    @Id
    private int messageId;

    private long contactId;

    @Builder.Default
    private Date createTime = new Date();

    @Enumerated(EnumType.STRING)
    private Chat.Type contactType;

    /**
     * 规定此message为array格式的onebotV11 json字符串
     */
    private String message;

    public Message(com.pengrad.telegrambot.model.Message message, String array) {
        this.messageId = message.messageId();
        this.contactId = message.chat().id();
        this.contactType = message.chat().type();
        this.createTime = new Date();
        this.message = array;
    }

    public JsonObject getMessage() {
        return JsonParser.parseString(this.message).getAsJsonObject();
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public void setMessage(JSONObject message) {
        this.message = message.toString();
    }

    public String getMessageString() {
        return this.message;
    }
}
