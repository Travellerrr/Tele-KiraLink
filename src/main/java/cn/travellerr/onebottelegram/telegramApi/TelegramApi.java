package cn.travellerr.onebottelegram.telegramApi;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.travellerr.onebottelegram.config.Config;
import cn.travellerr.onebottelegram.converter.TelegramToOnebot;
import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.UpdatesListener;
import com.pengrad.telegrambot.model.*;
import com.pengrad.telegrambot.request.EditMessageText;
import com.pengrad.telegrambot.request.GetMe;
import com.pengrad.telegrambot.request.SetWebhook;
import com.pengrad.telegrambot.response.GetMeResponse;
import com.pengrad.telegrambot.utility.BotUtils;
import io.micrometer.common.util.StringUtils;
import okhttp3.Credentials;
import okhttp3.OkHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.File;
import java.net.Authenticator;
import java.net.InetSocketAddress;
import java.net.PasswordAuthentication;
import java.net.Proxy;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;

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
            while (!flag) {
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
                if (!flag) {
                    log.info("Telegram bot 信息获取失败超过最大次数(5/5), 60秒后重试");
                    try {
                        Thread.sleep(60000);
//                        Thread.sleep(1000);
                    } catch (InterruptedException e1) {
                        log.error("Telegram bot 信息获取中断: " + e1.getMessage());
                    }
                } else {
                    log.info("Telegram bot 信息获取成功");
                }
            }
        }

        log.info("Telegram bot 信息: " + getMeResponse);


        if (config.getTelegram().getWebhook().isUseWebhook()) {
            SetWebhook setWebhook = new SetWebhook()
                    .url(config.getTelegram().getWebhook().getUrl() + ":" + config.getTelegram().getWebhook().getPort() + "/api/telegram/webhook")
                    .certificate(new File(config.getTelegram().getWebhook().getCertPath()))
                    .secretToken(config.getTelegram().getWebhook().getSecret());

            String response = bot.execute(setWebhook).description();
            log.info("Telegram bot Webhook 设置成功: " + config.getTelegram().getWebhook().getUrl() + "/api/telegram/webhook");
            log.info("Telegram bot Webhook 设置返回: " + response);
            return;
        }

        log.info("Telegram bot Webhook 未启用, 使用长轮询模式");


        bot.setUpdatesListener(updates -> {
            new Thread(() -> updates.forEach(update -> {
                System.out.println(update.toString());
                Update newUpdate = callbackToMessage(update);
                TelegramToOnebot.forwardToOnebot(newUpdate);
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

    private static Update callbackToMessage(Update update) {
        if (update.callbackQuery() != null) {
            JSONObject object = new JSONObject(BotUtils.toJson(update));

            CallbackQuery callbackQuery = update.callbackQuery();

            object.set("callback_query", null);
            int message_id = callbackQuery.id().hashCode();
            User user = callbackQuery.from();
            Chat chat = callbackQuery.maybeInaccessibleMessage().chat();
            long date = new Date().getTime() / 1000;
            String text = callbackQuery.data();
            List<MessageEntity> entities = List.of(new MessageEntity[]{
                    new MessageEntity(MessageEntity.Type.bot_command, 0, text.length())
            });

            JSONObject messageObject = new JSONObject(new Message());
            messageObject.set("message_id", message_id);
            messageObject.set("from", new JSONObject(BotUtils.toJson(user)));
            messageObject.set("chat", new JSONObject(BotUtils.toJson(chat)));
            messageObject.set("date", date);
            messageObject.set("text", config.getCommand().getPrefix()+text);
            messageObject.set("entities", new JSONArray(BotUtils.toJson(entities)));

            object.set("message", messageObject);

            EditMessageText editMessageText = new EditMessageText(callbackQuery.maybeInaccessibleMessage().chat().id(), callbackQuery.maybeInaccessibleMessage().messageId(), "正在执行 " + text + " 指令...").replyMarkup(TelegramToOnebot.buildMenuButtons());
            bot.execute(editMessageText);


            return BotUtils.parseUpdate(object.toString());
        } else {
            return update;
        }
    }


}
