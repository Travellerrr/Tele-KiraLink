package cn.travellerr.onebottelegram.telegramApi;

import cn.travellerr.onebottelegram.OnebotTelegramApplication;
import cn.travellerr.onebottelegram.converter.TelegramToOnebot;
import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.UpdatesListener;
import com.pengrad.telegrambot.request.GetMe;
import com.pengrad.telegrambot.request.SendMessage;
import com.pengrad.telegrambot.response.GetMeResponse;
import okhttp3.Dns;
import okhttp3.OkHttpClient;
import org.springframework.stereotype.Component;

import java.net.InetSocketAddress;
import java.net.Proxy;


@Component
public class TelegramApi {


    private static final String token = OnebotTelegramApplication.config.getTelegram().getBot().getToken();

    private static final String username = OnebotTelegramApplication.config.getTelegram().getBot().getUsername();

    public static GetMeResponse getMeResponse;

    public static TelegramBot bot;

    public static void init() {
        bot = new TelegramBot(token);
        Proxy proxy = new Proxy(Proxy.Type.SOCKS, new InetSocketAddress("127.0.0.1", 7890));
        bot = new TelegramBot.Builder(token).okHttpClient(new OkHttpClient.Builder().proxy(proxy).hostnameVerifier((hostname, session) -> true).build())/*.apiUrl("https://telegrambotapi.hoshiran.tech/bot")*/.build();
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
