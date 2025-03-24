package cn.travellerr.onebottelegram.config;

import cn.chahuyun.hibernateplus.DriveType;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.support.ResourcePropertySource;
import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.nodes.Tag;
import org.yaml.snakeyaml.representer.Representer;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;

import static org.reflections.Reflections.log;


public class ConfigGenerator {
    public static Config loadConfig() {
        // 获取配置文件路径
        String filePath = Config.class.getAnnotation(ConfigEntity.class).filePath();
        // 获取配置文件名
        String fileName = Config.class.getAnnotation(ConfigEntity.class).name() + ".yml";

        // 创建配置文件对象
        File file = new File(filePath + File.separator + fileName);

        // 创建Yaml对象，设置Representer
        Yaml yaml = createYaml();

        try {
            // 如果配置文件不存在，则创建配置文件
            if (!file.exists()) {
                if (!new File(filePath).mkdirs()) {
                    log.error("创建配置文件失败");
                }
                // 创建配置对象
            Config newConfig = Config.builder()
                .telegram(
                    Config.telegram.builder()
                        .bot(
                            Config.telegram.bot.builder()
                                .token("your-telegram-token")
                                .username("your-telegram-username")
                                .build()
                        )
                        .build()
                )
                .onebot(
                    Config.onebot.builder()
                        .ip("your-onebot-ip")
                        .path("your-onebot-path")
                        .port(0)
                        .build()
                )
                .spring(
                    Config.spring.builder()
                        .jackson(
                            Config.spring.jackson.builder()
                                .dateformat("yyyy-MM-dd HH:mm:ss")
                                .timezone("Asia/Shanghai")
                                .build()
                        )
                        .database(
                            Config.spring.database.builder()
                                    .mysqlUser("")
                                    .mysqlUrl("")
                                    .mysqlPassword("")
                                    .dataType(DriveType.H2)
                                    .build()
                        )
                        .build()
                )
                .build();


                // 将配置对象写入配置文件
                yaml.dump(newConfig, new FileWriter(file));
                log.warn("请修改配置文件: " + file.getAbsolutePath());
                System.exit(0);
            }

            // 从配置文件中加载配置对象
            return yaml.loadAs(new FileReader(file), Config.class);

        } catch (Exception e) {
            log.error("加载配置文件失败: " + e.getMessage());
        }
        return new Config();
    }

    // 保存配置文件
    public static boolean saveConfig(Config config) {
        // 获取配置文件路径
        String filePath = config.getClass().getAnnotation(ConfigEntity.class).filePath();
        // 获取配置文件名
        String fileName = config.getClass().getAnnotation(ConfigEntity.class).name() + ".yml";

        // 创建配置文件对象
        File file = new File(filePath + File.separator + fileName);

        // 创建Yaml对象，设置Representer
        Yaml yaml = createYaml();

        try {
            // 将配置对象写入配置文件
            yaml.dump(config, new FileWriter(file));
            return true;

        } catch (Exception e) {
            log.error("保存配置文件失败: " + e.getMessage());
        }
        return false;
    }

    private static Yaml createYaml() {
        DumperOptions dumperOptions = new DumperOptions();
        dumperOptions.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        dumperOptions.setPrettyFlow(true);

        Representer representer = new Representer(dumperOptions);
        representer.addClassTag(Config.class, Tag.MAP);

        return new Yaml(representer, dumperOptions);
    }
}