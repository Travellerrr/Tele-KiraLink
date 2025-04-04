package cn.travellerr.onebottelegram.config;

import cn.chahuyun.hibernateplus.DriveType;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.nodes.Tag;
import org.yaml.snakeyaml.representer.Representer;

import java.io.*;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.reflections.Reflections.log;

public class ConfigGenerator {

    public static Config loadConfig() {
        try {
            ConfigEntity entity = Config.class.getAnnotation(ConfigEntity.class);
            Path configPath = Path.of(entity.filePath(), entity.name() + ".yml");

            Config defaultConfig = createDefaultConfig();

            if (!Files.exists(configPath)) {
                Files.createDirectories(configPath.getParent());
                writeConfig(configPath, defaultConfig);
                log.warn("生成默认配置文件，请修改并重启: {}", configPath);
                System.exit(0);
            }

            Config existingConfig = readConfig(configPath);
            Config mergedConfig = mergeConfigs(defaultConfig, deepCopy(existingConfig));

            if (!mergedConfig.toString().equals(existingConfig.toString())) {
                writeConfig(configPath, mergedConfig);
                log.info("更新的配置文件");
                return mergedConfig;
            } else {
                log.info("配置文件无更改");
                return existingConfig;
            }
        } catch (Exception e) {
            log.error("加载配置失败", e);
            throw new IllegalStateException("加载配置失败", e);
        }
    }

    private static Config deepCopy(Config config) throws IOException, ClassNotFoundException {
        try (ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
             ObjectOutputStream out = new ObjectOutputStream(byteOut)) {
            out.writeObject(config);
            try (ByteArrayInputStream byteIn = new ByteArrayInputStream(byteOut.toByteArray());
                 ObjectInputStream in = new ObjectInputStream(byteIn)) {
                return (Config) in.readObject();
            }
        }
    }

    private static Config mergeConfigs(Config source, Config target) throws Exception {
        mergeObjects(source, target);
        return target;
    }

    private static void mergeObjects(Object source, Object target) throws Exception {
        for (Field field : source.getClass().getDeclaredFields()) {
            field.setAccessible(true);
            Object sourceValue = field.get(source);
            Object targetValue = field.get(target);

            if (targetValue == null) {
                field.set(target, sourceValue);
            } else if (isComplexType(field.getType())) {
                mergeObjects(sourceValue, targetValue);
            }
        }
    }

    private static boolean isComplexType(Class<?> type) {
        return !type.isPrimitive() && !type.equals(String.class) && !type.isEnum();
    }

    private static Config createDefaultConfig() {
        return Config.builder()
                .telegram(Config.telegram.builder()
                        .bot(Config.telegram.bot.builder()
                                .token("123456:ABC-DEF1234ghIkl-zyx57W2v1u123ew11")
                                .username("bot_username")
                                .proxy(Config.telegram.bot.proxy.builder()
                                        .host("127.0.0.1")
                                        .port(-1)
                                        .secret("")
                                        .username("")
                                        .type("DIRECT")
                                        .build()).build()).build())
                .onebot(Config.onebot.builder()
                        .ip("0.0.0.0")
                        .path("/ws")
                        .port(6700)
                        .useArray(true)
                        .build())
                .spring(Config.spring.builder()
                        .jackson(Config.spring.jackson.builder()
                                .dateformat("yyyy-MM-dd HH:mm:ss")
                                .timezone("Asia/Shanghai")
                                .build())
                        .database(Config.spring.database.builder()
                                .dataType(DriveType.MYSQL)
                                .mysqlUrl("jdbc:mysql://localhost:3306")
                                .mysqlUser("root")
                                .mysqlPassword("root")
                                .build())
                        .build())
                .build();
    }

    private static void writeConfig(Path path, Config config) throws IOException {
        Yaml yaml = createYaml();
        try (Writer writer = Files.newBufferedWriter(path)) {
            yaml.dump(config, writer);
        }
    }

    private static Config readConfig(Path path) throws IOException {
        Yaml yaml = createYaml();
        try (Reader reader = Files.newBufferedReader(path)) {
            return yaml.loadAs(reader, Config.class);
        }
    }

    private static Yaml createYaml() {
        DumperOptions options = new DumperOptions();
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        options.setIndent(2);
        options.setPrettyFlow(true);

        Representer representer = new Representer(options);
        representer.getPropertyUtils().setSkipMissingProperties(true);
        representer.addClassTag(Config.class, Tag.MAP);

        return new Yaml(representer, options);
    }
}