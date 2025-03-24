package cn.travellerr.onebottelegram.config;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface ConfigEntity {
    String name();
    String filePath();
}