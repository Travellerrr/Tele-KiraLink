package cn.travellerr.onebottelegram.onebotWebsocket;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.server.standard.ServletServerContainerFactoryBean;

import static cn.travellerr.onebottelegram.OnebotTelegramApplication.config;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    @Autowired
    private OneBotWebSocketHandler handler;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(handler, config.getOnebot().path)
                .setAllowedOrigins("*");
    }

    @Bean
    public ServletServerContainerFactoryBean createWebSocketContainer() {
        ServletServerContainerFactoryBean container = new ServletServerContainerFactoryBean();
        // 在此处设置bufferSize
        container.setMaxTextMessageBufferSize(102400000);
        container.setMaxBinaryMessageBufferSize(102400000);
        container.setMaxSessionIdleTimeout(15 * 60000L);
        return container;
    }
}