package cn.travellerr.onebottelegram;

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
public class OnebotTelegramApplication {

    public static OnebotTelegramApplication INSTANCE = new OnebotTelegramApplication();

    public static final String VERSION = "0.0.2";

    public static Config config;

    public static void main(String[] args) {
        SpringApplication springApplication = new SpringApplication(OnebotTelegramApplication.class);
        springApplication.addListeners((ApplicationListener<ApplicationEnvironmentPreparedEvent>) event -> {
            config = ConfigGenerator.loadConfig();
            ConfigurableEnvironment environment = event.getEnvironment();
            environment.getPropertySources().addFirst(new MapPropertySource("customPort", Collections.singletonMap("server.port", config.getOnebot().port)));
        });


        springApplication.run(args);
    }

}
