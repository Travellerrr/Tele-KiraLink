package cn.travellerr.onebottelegram;

import cn.travellerr.onebottelegram.command.CommandHandler;
import cn.travellerr.onebottelegram.config.Config;
import cn.travellerr.onebottelegram.config.ConfigGenerator;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.ApplicationListener;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.util.Collections;

@SpringBootApplication
@ConfigurationPropertiesScan
public class TelegramOnebotAdapter {

    public static final String VERSION = "0.0.6";
    public static TelegramOnebotAdapter INSTANCE = new TelegramOnebotAdapter();
    public static Config config;
    public static long startTime = System.currentTimeMillis();

    public static void main(String[] args) {
        SpringApplication springApplication = new SpringApplication(TelegramOnebotAdapter.class);
        springApplication.setBanner((environment, sourceClass, out) -> {
            out.println("""
                      _____      _                                    \s
                     |_   _|___ | |  ___   __ _  _ __  __ _  _ __ ___ \s
                       | | / _ \\| | / _ \\ / _` || '__|/ _` || '_ ` _ \\\s
                       | ||  __/| ||  __/| (_| || |  | (_| || | | | | |
                       |_| \\___||_| \\___| \\__, ||_|   \\__,_||_| |_| |_|
                                          |___/                       \s
                         _        _                _                  \s
                        / \\    __| |  __ _  _ __  | |_  ___  _ __     \s
                       / _ \\  / _` | / _` || '_ \\ | __|/ _ \\| '__|    \s
                      / ___ \\| (_| || (_| || |_) || |_|  __/| |       \s
                     /_/   \\_\\\\__,_| \\__,_|| .__/  \\__|\\___||_|       \s
                                           |_|                     \s
                    """);
            out.println("Telegram-Onebot-Adapter v" + VERSION);
        });
        springApplication.addListeners((ApplicationListener<ApplicationEnvironmentPreparedEvent>) event -> {
            config = ConfigGenerator.loadConfig();
            ConfigurableEnvironment environment = event.getEnvironment();
            environment.getPropertySources().addFirst(new MapPropertySource("customPort", Collections.singletonMap("server.port", config.getOnebot().getPort())));
        });


        springApplication.run(args);

        CommandHandler.startCommandConsole();
    }

}
