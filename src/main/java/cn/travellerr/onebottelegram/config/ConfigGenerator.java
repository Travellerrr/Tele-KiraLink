package cn.travellerr.onebottelegram.config;

import cn.chahuyun.hibernateplus.DriveType;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.representer.Representer;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.reflections.Reflections.log;

public class ConfigGenerator {

    /**
     * 智能加载配置（自动合并新增字段）
     */
    public static Config loadConfig() {
        try {
            // 获取配置元信息
            ConfigEntity entity = Config.class.getAnnotation(ConfigEntity.class);
            Path configPath = Path.of(entity.filePath(), entity.name() + ".yml");

            // 生成默认配置模板
            Config defaultConfig = createDefaultConfig();

            // 如果配置文件不存在则初始化
            if (!Files.exists(configPath)) {
                Files.createDirectories(configPath.getParent());
                writeConfig(configPath, defaultConfig);
                log.warn("已生成默认配置文件，请修改后重启: {}", configPath);
                System.exit(0);
            }

            // 加载现有配置
            Config existingConfig = readConfig(configPath);

            Config mergedConfig = mergeConfigs(defaultConfig, existingConfig);
            // 自动合并新增字段

            writeConfig(configPath, mergedConfig);
            // 如果发现变化则保存

            return mergeConfigs(defaultConfig, existingConfig);
        } catch (Exception e) {
            log.error("配置加载失败", e);
            throw new IllegalStateException("配置加载失败", e);
        }
    }

    /**
     * 递归合并配置对象（保留用户配置，补充新增字段）
     */
    private static Config mergeConfigs(Config source, Config target) throws Exception {
        // 通过反射深度合并对象
        mergeObjects(source, target);
        return target;
    }

    private static void mergeObjects(Object source, Object target) throws Exception {
        Class<?> clazz = source.getClass();
        for (Field field : clazz.getDeclaredFields()) {
            field.setAccessible(true);

            Object sourceValue = field.get(source);
            Object targetValue = field.get(target);

            if (targetValue == null) {
                // 填充缺失字段
                field.set(target, sourceValue);
            } else if (isComplexType(field.getType())) {
                // 递归处理嵌套对象
                mergeObjects(sourceValue, targetValue);
            }
        }
    }

    private static boolean isComplexType(Class<?> type) {
        return !type.isPrimitive()
                && !type.equals(String.class)
                && !type.isEnum();
    }

    /**
     * 创建默认配置模板
     */
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
                                        .type("SOCKS5")
                                        .build()).build()).build())
                .onebot(Config.onebot.builder()
                        .ip("0.0.0.0")
                        .path("/ws")
                        .port(6700)
                        .build())
                .spring(Config.spring.builder()
                        .jackson(Config.spring.jackson.builder()
                                .dateformat("yyyy-MM-dd HH:mm:ss")
                                .timezone("Asia/Shanghai")
                                .build())
                        .database(Config.spring.database.builder()
                                .dataType(DriveType.MYSQL)
                                .mysqlUrl("jdbc:mysql://localhost:3306/onebot_telegram?useSSL=false&serverTimezone=Asia/Shanghai")
                                .mysqlUser("root")
                                .mysqlPassword("root")
                                .build())
                        .build())
                .build();
    }

    /**
     * 序列化配置到文件
     */
    private static void writeConfig(Path path, Config config) throws IOException {
        Yaml yaml = createYaml();
        try (Writer writer = Files.newBufferedWriter(path)) {
            yaml.dump(config, writer);
        }
    }

    /**
     * 从文件反序列化配置
     */
    private static Config readConfig(Path path) throws IOException {
        Yaml yaml = createYaml();
        try (Reader reader = Files.newBufferedReader(path)) {
            return yaml.loadAs(reader, Config.class);
        }
    }

    /**
     * 创建定制化的YAML处理器
     */
    private static Yaml createYaml() {
        DumperOptions options = new DumperOptions();
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        options.setIndent(2);
        options.setPrettyFlow(true);

        Representer representer = new Representer(options);
        representer.getPropertyUtils().setSkipMissingProperties(true);

        return new Yaml(representer, options);
    }
}