package cn.travellerr.onebottelegram.converter;

import lombok.AllArgsConstructor;
import lombok.Getter;

import javax.annotation.Nullable;
import java.util.Locale;

@Getter
@AllArgsConstructor
public enum LanguageCode {
    AUTO("auto-detect"),
    ZH("zh"),
    ZH_HANS("zh-hans"),
    EN("en"),
    JA("ja"),
    FR("fr"),
    DE("de"),
    RU("ru");

    private final String code;

    public static LanguageCode parseLanguageCode(@Nullable String code) {
        if (code == null) {
            return EN;
        }
        for (LanguageCode languageCode : LanguageCode.values()) {
            if (languageCode.getCode().equals(code.toLowerCase(Locale.ROOT))) {
                return languageCode;
            }
        }
        return EN;
    }
}
