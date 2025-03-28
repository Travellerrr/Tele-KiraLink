package cn.travellerr.onebottelegram.telegramApi;

import cn.travellerr.onebottelegram.config.Config;
import cn.travellerr.onebottelegram.converter.TelegramToOnebot;
import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.UpdatesListener;
import com.pengrad.telegrambot.request.GetMe;
import com.pengrad.telegrambot.response.GetMeResponse;
import okhttp3.OkHttpClient;
import org.springframework.stereotype.Component;

import java.net.InetSocketAddress;
import java.net.Proxy;

import static cn.travellerr.onebottelegram.OnebotTelegramApplication.config;
import static org.reflections.Reflections.log;


@Component
public class TelegramApi {


    private static final String token = config.getTelegram().getBot().getToken();

    private static final String username = config.getTelegram().getBot().getUsername();

    public static GetMeResponse getMeResponse;

    public static TelegramBot bot;

    public static void init() {
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

            OkHttpClient.Builder clientBuilder = new OkHttpClient.Builder();
            clientBuilder.proxy(new Proxy(proxyType, new InetSocketAddress(proxy.getHost(), proxy.getPort())));

 //          TODO SOCKS账密代理

 /*         clientBuilder.hostnameVerifier(SSLSocketClient.getHostnameVerifier());
            clientBuilder.sslSocketFactory(SSLSocketClient.getSSLSocketFactory(), SSLSocketClient.getX509TrustManager());
            clientBuilder.proxy(new Proxy(proxyType, new InetSocketAddress(proxy.getHost(), proxy.getPort())));
            clientBuilder.dns(Dns.SYSTEM);

            if (StringUtils.isNotEmpty(proxy.getUsername()) && StringUtils.isNotEmpty(proxy.getSecret()) && proxyType.equals(Proxy.Type.SOCKS)) {
                clientBuilder.proxyAuthenticator((route, response) -> {
                    String credential = Credentials.basic(proxy.getUsername().strip(), proxy.getSecret().strip(), StandardCharsets.UTF_8);
                    return response.request().newBuilder().header("Authorization", credential).build();
                });

                 // 配置认证（需全局设置）
                Authenticator.setDefault(new Authenticator() {
                    @Override
                    protected PasswordAuthentication getPasswordAuthentication() {
                        if (getRequestorType() == RequestorType.PROXY) {
                            return new PasswordAuthentication(
                                    proxy.getUsername().strip(),
                                    proxy.getSecret().strip().toCharArray()
                            );
                        }
                        return null;
                    }
                });

                System.setProperty("java.net.socks.username", proxy.getUsername());

                System.setProperty("java.net.socks.password", proxy.getSecret());


                System.out.println("代理已启用: " + proxy.getHost() + ":" + proxy.getPort());
            }*/


            bot = new TelegramBot.Builder(token).okHttpClient(clientBuilder.build()).build();
        } else {
            bot = new TelegramBot.Builder(token).okHttpClient(new OkHttpClient.Builder().hostnameVerifier((hostname, session) -> true).build()).build();
        }

        System.out.println(token);
        System.out.println("Telegram bot 开始运行: " + username);

        getMeResponse = bot.execute(new GetMe());

        System.out.println("Telegram bot 信息: " + getMeResponse);

        bot.setUpdatesListener(updates -> {
            new Thread(() -> updates.forEach(update -> {
                System.out.println(update);
                TelegramToOnebot.forwardToOnebot(update);
            })).start();
            return UpdatesListener.CONFIRMED_UPDATES_ALL;
        }, Throwable::fillInStackTrace);
    }



}
