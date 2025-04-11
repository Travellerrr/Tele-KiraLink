package cn.travellerr.onebottelegram.webui.entity;

import cn.hutool.core.date.BetweenFormatter;
import cn.hutool.core.date.DateUtil;
import cn.travellerr.onebottelegram.TelegramOnebotAdapter;
import com.pengrad.telegrambot.response.GetMeResponse;
import lombok.Data;

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
        this.firstName = getMe.user().firstName();
        this.name = "@"+getMe.user().username();
        this.avatarUrl = withAvatar ? avatarUrl : "";
        this.uptime = DateUtil.formatBetween(new Date(TelegramOnebotAdapter.startTime), new Date(), BetweenFormatter.Level.SECOND);
        this.version = "OneBot v11";
        this.latency = 0; // Placeholder for latency, implement actual logic if needed
    }
}
