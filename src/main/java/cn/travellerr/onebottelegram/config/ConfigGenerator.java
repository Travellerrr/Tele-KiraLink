package cn.travellerr.onebottelegram.config;

import cn.chahuyun.hibernateplus.DriveType;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.nodes.Tag;
import org.yaml.snakeyaml.representer.Representer;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
                log.info("配置文件加载成功");
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

    private static Config mergeConfigs(Config defaultConfig, Config existingConfig) {
        // 将配置对象转换为Map结构
        Map<String, Object> defaultMap = convertToMap(defaultConfig);
        Map<String, Object> existingMap = convertToMap(existingConfig);

        // 递归合并Map
        Map<String, Object> mergedMap = mergeMaps(defaultMap, existingMap);

        // 将合并后的Map转回Config对象
        return createYaml().loadAs(createYaml().dump(mergedMap), Config.class);
    }

    /**
     * 将Config对象转为可修改的Map（解决不可变集合问题）
     */
    private static Map<String, Object> convertToMap(Object obj) {
        Yaml yaml = new Yaml();
        String yamlStr = yaml.dumpAsMap(obj);
        return yaml.load(yamlStr);
    }

    /**
     * 递归合并两个Map结构
     */
    @SuppressWarnings("unchecked")
    private static Map<String, Object> mergeMaps(Map<String, Object> defaultMap, Map<String, Object> existingMap) {
        Map<String, Object> result = new LinkedHashMap<>(existingMap);

        defaultMap.forEach((key, defaultValue) -> {
            Object existingValue = result.get(key);

            if (existingValue == null) {
                // 添加新字段
                result.put(key, defaultValue);
            } else if (defaultValue instanceof Map && existingValue instanceof Map) {
                // 递归合并嵌套Map
                result.put(key, mergeMaps(
                        (Map<String, Object>) defaultValue,
                        (Map<String, Object>) existingValue
                ));
            } else if (defaultValue instanceof List && existingValue instanceof List) {
                // 合并List策略：保留现有元素，添加默认中不存在的新元素
                List<Object> mergedList = mergeLists(
                        (List<Object>) defaultValue,
                        (List<Object>) existingValue
                );
                result.put(key, mergedList);
            }
            // 其他类型保持现有值
        });

        return result;
    }

    /**
     * 合并List策略示例（根据需求调整）
     */
    private static List<Object> mergeLists(List<Object> defaultList, List<Object> existingList) {
        List<Object> result = new ArrayList<>(existingList);

        // 添加默认列表中不存在的新元素
        defaultList.stream()
                .filter(item -> !result.contains(item))
                .forEach(result::add);

        return result;
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
                                        .build()).build())
                        .webhook(Config.telegram.webhook.builder()
                                .certPath("")
                                .secret("")
                                .port(6700)
                                .url("127.0.0.1")
                                .useWebhook(false)
                                .build()).build())
                .onebot(Config.onebot.builder()
                        .ip("0.0.0.0")
                        .path("/ws")
                        .port(6700)
                        .useArray(true)
                        .banGroupUser(true)
                        .groupUserWarning("")
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
                .command(Config.command.builder()
                        .prefix("/")
                        .commandMap(Map.of())
                        .menu(Map.of())
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