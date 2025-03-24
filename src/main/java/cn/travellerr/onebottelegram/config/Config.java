package cn.travellerr.onebottelegram.config;

import cn.chahuyun.hibernateplus.DriveType;
import lombok.*;
import org.h2.value.DataType;
import org.springframework.context.annotation.Configuration;


@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@ConfigEntity(name = "config", filePath = "./")
public class Config {

    private telegram telegram;
    private onebot onebot;
    private spring spring;


    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class telegram {

        private Config.telegram.bot bot;

        @Data
        @Builder
        @AllArgsConstructor
        @NoArgsConstructor
        public static class bot {
            private String token;
            private String username;

        }
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class onebot {
            public String ip;
            public String path;
            public int port;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class spring {
        private Config.spring.jackson jackson;
        private Config.spring.database database;

        @Data
        @Builder
        @AllArgsConstructor
        @NoArgsConstructor
        public static class jackson {
            private String dateformat;
            private String timezone;
        }

        @Data
        @Builder
        @AllArgsConstructor
        @NoArgsConstructor
        public static class database {
            private DriveType dataType;
            private String mysqlUrl;
            private String mysqlUser;
            private String mysqlPassword;
        }
    }
}
