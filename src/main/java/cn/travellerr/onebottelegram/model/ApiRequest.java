package cn.travellerr.onebottelegram.model;

import com.google.gson.JsonArray;
import com.google.gson.annotations.SerializedName;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

public class ApiRequest {
    @AllArgsConstructor
    @NoArgsConstructor
    @Getter
    @Setter
    public static class BaseApiRequest {
        private String action = "";
        private Params params = new Params();
        private String echo = "0";
    }

    @AllArgsConstructor
    @NoArgsConstructor
    @Getter
    @Setter
    public static class Params {
        @SerializedName("user_id")
        Long userId = -1L;

        @SerializedName("group_id")
        Long groupId = -1L;

        @SerializedName("message_id")
        Integer messageId = -1;

        @SerializedName("duration")
        Integer duration;

        @SerializedName("message")
        JsonArray message;

        @SerializedName("group_name")
        String groupName = "";

        @SerializedName("special_title")
        String specialTitle;
    }


}

