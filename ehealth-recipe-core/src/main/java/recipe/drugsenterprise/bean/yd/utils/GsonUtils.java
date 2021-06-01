package recipe.drugsenterprise.bean.yd.utils;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * JSON工具类
 */
public class GsonUtils {

    //自定义Strig适配器,将null字符输出""
    private static final TypeAdapter<String> STRING = new TypeAdapter<String>(){
        public String read(JsonReader reader) throws IOException
        {
            if (reader.peek() == JsonToken.NULL)
            {
                reader.nextNull();
                return "";
            }
            return reader.nextString();
        }

        public void write(JsonWriter writer, String value) throws IOException
        {
            if (value == null)
            {
                // 在这里处理null改为空字符串
                writer.value("");
                return;
            }
            writer.value(value);
        }
    };

    //自定义Number适配器,将null字符输出0.00
    private static final TypeAdapter<Number> NUMBER = new TypeAdapter<Number>(){
        public Number read(JsonReader reader) throws IOException
        {
            if (reader.peek() == JsonToken.NULL)
            {
                reader.nextNull();
                return 0.00;
            }
            return reader.nextDouble();
        }

        public void write(JsonWriter writer, Number value) throws IOException
        {
            if (value == null)
            {
                // 在这里处理null改为0.00
                writer.value(0.00);
                return;
            }
            writer.value(value);
        }
    };

    private static Gson gson = new GsonBuilder()
            .setDateFormat("yyyy-MM-dd HH:mm:ss")
            .disableHtmlEscaping()
            .registerTypeAdapter(String.class, STRING)
            .registerTypeAdapter(Double.class, NUMBER)
            .serializeNulls()
            .create();

    /**
     * 对象转json字符串
     *
     * @param object
     * @return
     */
    public static String toJson(Object object) {
        return gson.toJson(object);
    }


    /**
     * json字符串解析成对象
     *
     * @param jsonData
     * @param entityType
     * @return
     */
    public static <T> T parseJson(String jsonData, Class<T> entityType) {
        return gson.fromJson(jsonData, entityType);
    }

    /**
     * JsonArray字符数组解析成对象
     *
     * @param jsonData
     * @return
     */
    public static <T> T parseJson(String jsonData, Type type) {
        return gson.fromJson(jsonData, type);
    }

    /**
     * JsonArray字符数组解析成对象
     *
     * @param jsonArrayData
     * @return
     */
    public static <T> List<T> parseJsonArray(String jsonArrayData) {
        Type required = new TypeToken<List<T>>() {
        }.getType();
        List<T> list = gson.fromJson(jsonArrayData, required);
        return list;
    }

    public static <T> List<T> parseJsonArray(String jsonArrayData, Type requiredParameterizedType) {
        List<T> list = gson.fromJson(jsonArrayData, requiredParameterizedType);
        return list;
    }

    public static JsonElement replaceKey(JsonElement source, Map<String, String> rep) {
        if (source == null || source.isJsonNull()) {
            return JsonNull.INSTANCE;
        }
        if (source.isJsonPrimitive()) {
            return source;
        }
        if (source.isJsonArray()) {
            JsonArray jsonArr = source.getAsJsonArray();
            JsonArray jsonArray = new JsonArray();
            Iterator<JsonElement> iterator = jsonArr.iterator();
            while(iterator.hasNext()){
                jsonArray.add(replaceKey(iterator.next(), rep));
            }
            return jsonArray;
        }
        if (source.isJsonObject()) {
            JsonObject jsonObj = source.getAsJsonObject();
            Iterator<Map.Entry<String, JsonElement>> iterator = jsonObj.entrySet().iterator();
            JsonObject newJsonObj = new JsonObject();
            while(iterator.hasNext()){
                Map.Entry<String, JsonElement> item = iterator.next();
                String key = item.getKey();
                JsonElement value = item.getValue();
                if (rep.containsKey(key)) {
                    String newKey = rep.get(key);
                    key = newKey;
                }
                newJsonObj.add(key, replaceKey(value, rep));
            }
            return newJsonObj;
        }
        return JsonNull.INSTANCE;
    }


}