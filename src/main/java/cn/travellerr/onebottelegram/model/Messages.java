package cn.travellerr.onebottelegram.model;

import com.google.gson.JsonElement;
import com.google.gson.annotations.SerializedName;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

import static cn.travellerr.onebottelegram.onebotWebsocket.onebotSerialize.OnebotAction.GSON;


public class Messages {
    @Getter
    @Setter
    public static class BaseMessage implements Serializable {
        @SerializedName("type")
        String type;

        @SerializedName("data")
        Data data;

        public JsonElement toJson() {
            return GSON.toJsonTree(this);
        }

    }

    @Getter
    @Setter
    public static class Text extends BaseMessage {
        public Text() {
            this.type = "text";
        }

        public Text(String text) {
            this.type = "text";
            this.data = Data.builder().text(text).build();
        }
    }

    @Getter
    @Setter
    public static class Image extends BaseMessage {
        public Image() {
            this.type = "image";
        }

        public Image(String file) {
            this.type = "image";
            this.data = Data.builder().file(file).build();
        }
    }

    @Getter
    @Setter
    public static class Record extends BaseMessage {
        public Record() {
            this.type = "record";
        }

        public Record(String file) {
            this.type = "record";
            this.data = Data.builder().file(file).build();
        }
    }

    @Getter
    @Setter
    public static class At extends BaseMessage {
        public At() {
            this.type = "at";
        }

        public At(Long qq) {
            this.type = "at";
            this.data = Data.builder().qq(qq).build();
        }
    }

    @Getter
    @Setter
    public static class Reply extends BaseMessage {
        public Reply() {
            this.type = "reply";
        }

        public Reply(Integer id) {
            this.type = "reply";
            this.data = Data.builder().id(id).build();
        }
    }

    @Builder
    @Getter
    @Setter
    public static class Data {
        @SerializedName("text")
        String text;

        @SerializedName("file")
        String file;

        @SerializedName("qq")
        Long qq;

        @SerializedName("id")
        Integer id;
    }
}