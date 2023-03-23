package github.kasuminova.serverhelperbc.util;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import github.kasuminova.network.message.whitelist.FullWhiteListInfo;

import java.io.Reader;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class MapSerializeUtil {

    /**
     * 解决 gson 解析 long 自动转为科学计数的问题
     */
    public static Gson getMapGson(boolean concurrent) {
        return new GsonBuilder().registerTypeAdapter(Map.class, (JsonDeserializer<Map<?, ?>>) (json, typeOfT, context) -> {
            Map<String, Object> resultMap = concurrent ? new ConcurrentHashMap<>(100) : new HashMap<>(100);
            JsonObject jsonObject = json.getAsJsonObject();
            Set<Map.Entry<String, JsonElement>> entrySet = jsonObject.entrySet();
            for (Map.Entry<String, JsonElement> entry : entrySet) {
                resultMap.put(entry.getKey(), entry.getValue());
            }
            return resultMap;
        }).setLongSerializationPolicy(LongSerializationPolicy.STRING).create();
    }

    /**
     * 把json字符串解析成为map
     */
    public static Map<String, FullWhiteListInfo> parseToMap(Reader reader, boolean concurrent) {
        Gson gson = getMapGson(concurrent);
        Type type = concurrent
                ? new TypeToken<ConcurrentHashMap<String, FullWhiteListInfo>>() {}.getType()
                : new TypeToken<HashMap<String, FullWhiteListInfo>>() {}.getType();
        return gson.fromJson(reader, type);
    }
}
