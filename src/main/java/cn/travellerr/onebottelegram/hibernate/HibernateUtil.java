package cn.travellerr.onebottelegram.hibernate;

import cn.chahuyun.hibernateplus.Configuration;
import cn.chahuyun.hibernateplus.DriveType;
import cn.chahuyun.hibernateplus.HibernatePlusService;
import cn.travellerr.onebottelegram.OnebotTelegramApplication;
import cn.travellerr.onebottelegram.config.Config;

import java.io.File;
import java.nio.file.Path;

public class HibernateUtil {
    /**
     * Hibernate初始化
     *
     * @param app 插件
     * @author Moyuyanli
     * @date 2022/7/30 23:04
     */
    public static void init(OnebotTelegramApplication app) {
        Config config = OnebotTelegramApplication.INSTANCE.config;

        Configuration configuration = HibernatePlusService.createConfiguration(app.getClass());
        configuration.setPackageName("cn.travellerr.onebottelegram.hibernate.entity");

        DriveType dataType = config.getSpring().getDatabase().getDataType();
        configuration.setDriveType(dataType);
        Path dataFolderPath = Path.of("./");
        switch (dataType) {
            case MYSQL:
                configuration.setAddress(config.getSpring().getDatabase().getMysqlUrl());
                configuration.setUser(config.getSpring().getDatabase().getMysqlUser());
                configuration.setPassword(config.getSpring().getDatabase().getMysqlPassword());
                break;
            case H2:
                configuration.setAddress(dataFolderPath.resolve("TelegramData.h2").toString());
                break;
            case SQLITE:
                configuration.setAddress(dataFolderPath.resolve("TelegramData").toString());
                break;
        }

        HibernatePlusService.loadingService(configuration);
    }

}
