package cn.travellerr.onebottelegram.telegramApi;

import cn.travellerr.onebottelegram.config.Config;
import cn.travellerr.onebottelegram.converter.TelegramToOnebot;
import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.UpdatesListener;
import com.pengrad.telegrambot.request.GetMe;
import com.pengrad.telegrambot.response.GetMeResponse;
import io.micrometer.common.util.StringUtils;
import okhttp3.Credentials;
import okhttp3.OkHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.net.Authenticator;
import java.net.InetSocketAddress;
import java.net.PasswordAuthentication;
import java.net.Proxy;
import java.nio.charset.StandardCharsets;

import static cn.travellerr.onebottelegram.TelegramOnebotAdapter.config;


@Component
public class TelegramApi {


    private static final String token = config.getTelegram().getBot().getToken();

    private static final String username = config.getTelegram().getBot().getUsername();

    public static GetMeResponse getMeResponse;

    public static TelegramBot bot;

    private static final Logger log = LoggerFactory.getLogger(TelegramApi.class);

    public static void init() {

        createBot();

        log.info("Telegram bot 开始运行: " + username);

        try {
            getMeResponse = bot.execute(new GetMe());
        } catch (Exception e) {
            log.error("Telegram bot 信息获取失败: " + e.getMessage());
            boolean flag = false;
            for (int i = 1; i <= 5; i++) {
                try {

                    log.info("尝试重新获取 Telegram bot 信息: " + i + "/5");
                    getMeResponse = bot.execute(new GetMe());
                    flag = true;
                    break;
                } catch (Exception e1) {
                    log.error("Telegram bot 信息获取失败: " + e1.getMessage());
                }
            }
            if (flag) {
                log.info("Telegram bot 信息获取成功");
            } else {
                log.error("Telegram bot 信息获取失败");
                System.exit(-1);
            }
        }

        log.info("Telegram bot 信息: " + getMeResponse);


        bot.setUpdatesListener(updates -> {
            new Thread(() -> updates.forEach(update -> {
                System.out.println(update);
                TelegramToOnebot.forwardToOnebot(update);
            })).start();
            return UpdatesListener.CONFIRMED_UPDATES_ALL;
        }, Throwable::fillInStackTrace);
    }

    private static void createBot() {
        bot = new TelegramBot(token);

        Config.telegram.bot.proxy proxy = config.getTelegram().getBot().getProxy();

        if (proxy.getPort() != -1) {
            Proxy.Type proxyType = switch (proxy.getType()) {
                case "SOCKS5", "SOCKS4", "VMESS", "SOCKS" -> Proxy.Type.SOCKS;
                case "HTTP", "HTTPS" -> Proxy.Type.HTTP;
                default -> {
                    log.error("代理类型错误");
                    yield Proxy.Type.DIRECT;
                }
            };

            OkHttpClient.Builder clientBuilder = new OkHttpClient.Builder()
                    .hostnameVerifier(SSLSocketClient.getHostnameVerifier())
                    .sslSocketFactory(SSLSocketClient.getSSLSocketFactory(), SSLSocketClient.getX509TrustManager())
                    .proxy(new Proxy(proxyType, new InetSocketAddress(proxy.getHost(), proxy.getPort())));

            if (StringUtils.isNotEmpty(proxy.getUsername()) && StringUtils.isNotEmpty(proxy.getSecret())) {
                clientBuilder.proxyAuthenticator((route, response) -> {
                    String credential = Credentials.basic(proxy.getUsername().strip(), proxy.getSecret().strip(), StandardCharsets.UTF_8);
                    return response.request().newBuilder().header("Proxy-Authorization", credential).build();
                });

                Authenticator.setDefault(new Authenticator() {
                    @Override
                    protected PasswordAuthentication getPasswordAuthentication() {
                        return new PasswordAuthentication(proxy.getUsername().strip(), proxy.getSecret().strip().toCharArray());
                    }
                });

                log.info("代理已启用: " + proxy.getHost() + ":" + proxy.getPort());
            }

            bot = new TelegramBot.Builder(token).okHttpClient(clientBuilder.build()).build();
        } else {
            bot = new TelegramBot.Builder(token).okHttpClient(new OkHttpClient.Builder().hostnameVerifier((hostname, session) -> true).build()).build();
        }
    }


}
