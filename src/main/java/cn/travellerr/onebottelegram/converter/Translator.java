package cn.travellerr.onebottelegram.converter;

import cn.travellerr.onebotApi.TranslateResponseItem;
import cn.travellerr.onebotApi.Translation;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Translator {

    private static final Logger logger = LoggerFactory.getLogger(Translator.class);

    public static String Trans(LanguageCode to, String text) {
        LanguageCode from = LanguageCode.AUTO;
        OkHttpClient okHttpClient = new OkHttpClient();
        RequestBody body = RequestBody.create(
                ("fromLang="+from.getCode()+"&to="+to.getCode()+"&text="+text+"&tryFetchingGenderDebiasedTranslations=true&token=4hGHCk1A8zGjT-iDmwfYR8qgW8JyDcGL&key=1744467356960").getBytes()
        );
        Request request = new Request.Builder()
                .url("https://cn.bing.com/ttranslatev3?isVertical=1&IG=339F15526F524081823C28726EFC8796&IID=translator.5026")
                .addHeader("content-type", "application/x-www-form-urlencoded; charset=UTF-8")
                .addHeader("user-agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/135.0.0.0 Safari/537.36")
                .post(body).build();


        try (Response response = okHttpClient.newCall(request).execute()) {
            if (response.body() == null) {
                throw new RuntimeException("Response body is null");
            }
            try {
                String responseBody = response.body().string();
                System.out.println(responseBody);
                TranslateResponseItem responseItems = TranslateResponseItem.Companion.parse(responseBody.substring(1, responseBody.length() - 1));
                Translation translation = responseItems.getTranslations().get(0);

                return translation.getText();
            } catch (Exception e) {
                logger.error("解析翻译结果失败！", e);
                return text;
            }
        } catch (Exception e) {
            logger.error("出错了！", e);
        }
        return text;
    }
}
