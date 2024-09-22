package github.kasuminova.serverhelperbc.hitokoto;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import github.kasuminova.serverhelperbc.ServerHelperBC;
import io.netty.util.internal.ThrowableUtil;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

public class HitokotoAPI {
    public static final String API_URL = "https://v1.hitokoto.cn/";

    public static String hitokotoCache = null;

    private static long lastGetTimestamp = -1;
    private static AtomicBoolean getting = new AtomicBoolean(false);

    private static final Gson DESERIALIZER = new GsonBuilder()
            .registerTypeHierarchyAdapter(HitokotoResult.class, new HitokotoDeserializer())
            .create();

    public static String getHitokotoCache() {
        if (hitokotoCache == null || hitokotoCache.isEmpty() || lastGetTimestamp != -1 && System.currentTimeMillis() - lastGetTimestamp > 5 * 1000 && !getting.get()) {
            getting.set(true);
            CompletableFuture.runAsync(() -> {
                try {
                    getRandomHitokoto();
                } catch (Exception e) {
                    ServerHelperBC.logger.warn(ThrowableUtil.stackTraceToString(e));
                }
                getting.set(false);
            });
            lastGetTimestamp = System.currentTimeMillis();
        }

        return hitokotoCache == null ? "" : hitokotoCache;
    }

    public static String getRandomHitokoto() {
        String jsonStr;
        try {
            jsonStr = getStringFromURL(API_URL);
        } catch (IOException e) {
            return "";
        }

        if (jsonStr == null || jsonStr.isEmpty()) {
            return "";
        }

        HitokotoResult hitokoto;
        try {
            hitokoto = DESERIALIZER.fromJson(jsonStr, HitokotoResult.class);
        } catch (Exception e) {
            return "";
        }

        if (hitokoto == null) {
            return "";
        }

        String assembled = assembleHitokoto(hitokoto);
        if (!assembled.isEmpty()) {
            hitokotoCache = assembled;
        }
        lastGetTimestamp = System.currentTimeMillis();
        return assembled;
    }

    public static String assembleHitokoto(HitokotoResult result) {
        String hitokoto = result.getHitokoto();
        String fromWho = result.getFromWho();
        if (fromWho.isEmpty()) {
            fromWho = result.getFrom();
            if (fromWho.isEmpty()) {
                fromWho = result.getCreator();
            }
        }

        if (hitokoto != null && fromWho != null) {
            return hitokoto + " —— " + fromWho;
        }

        return "";
    }

    public static String getStringFromURL(String urlStr) throws IOException {
        try {
            URL url = new URL(urlStr);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.connect();
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8));

            String line;
            StringBuilder stringBuilder = new StringBuilder();
            while ((line = reader.readLine()) != null) {
                stringBuilder.append(line);
            }

            reader.close();
            connection.disconnect();
            return stringBuilder.toString();
        } catch (MalformedURLException e) {
            return null;
        }
    }
}
