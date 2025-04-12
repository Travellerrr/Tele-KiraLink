package cn.travellerr.onebottelegram.webui.entity;

import cn.hutool.core.codec.Base64Encoder;
import cn.hutool.core.date.BetweenFormatter;
import cn.hutool.core.date.DateUtil;
import cn.travellerr.onebottelegram.TelegramOnebotAdapter;
import com.pengrad.telegrambot.response.GetMeResponse;
import lombok.Data;

import java.io.File;
import java.nio.file.Files;
import java.util.Date;

@Data
public class BotInfo {
    private String firstName;
    private String name;
    private String uptime;
    private String version;
    private String avatarUrl;
    private int latency;

    public BotInfo(GetMeResponse getMe, String avatarUrl, boolean withAvatar) {
        String avatar;
        try {
            File file = new File(avatarUrl);
            if (file.exists()) {
                avatar = "data:image/png;base64," + Base64Encoder.encode(Files.readAllBytes(file.toPath()));
            } else {
                throw new Exception("File not found");
            }
        } catch (Exception e) {
            avatar = avatarUrl;
        }

        this.firstName = getMe.user().firstName();
        this.name = "@"+getMe.user().username();
        this.avatarUrl = withAvatar ? avatar : "";
        this.uptime = DateUtil.formatBetween(new Date(TelegramOnebotAdapter.startTime), new Date(), BetweenFormatter.Level.SECOND);
        this.version = "OneBot v11";
        this.latency = 0; // Placeholder for latency, implement actual logic if needed
    }
}
