package ee.cyber.sdsb.asyncdb;

import java.lang.reflect.Type;
import java.util.Date;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

public class JsonUtils {

    public static Gson getSerializer() {
        GsonBuilder builder = new GsonBuilder();
        builder.registerTypeAdapter(Date.class, new DateSerializer());
        return builder.create();
    }

    public static String getStringPropertyValue(JsonObject jsonObject,
            String memberName) {
        JsonElement jsonElement = jsonObject.get(memberName);

        if (jsonElement == null) {
            return null;
        }
        return jsonElement.getAsString();
    }

    public static Date getDatePropertyValue(JsonObject jsonObject,
            String memberName) {
        JsonElement jsonElement = jsonObject.get(memberName);

        if (jsonElement == null) {
            return null;
        }

        return new Date(jsonElement.getAsLong());
    }

    public static int getIntPropertyValue(JsonObject jsonObject,
            String memberName) {
        JsonElement jsonElement = jsonObject.get(memberName);

        if (jsonElement == null) {
            return 0;
        }
        return jsonElement.getAsInt();
    }

    public static boolean getBooleanPropertyValue(JsonObject jsonObject,
            String memberName) {
        JsonElement jsonElement = jsonObject.get(memberName);

        if (jsonElement == null) {
            return false;
        }
        return jsonElement.getAsBoolean();
    }


    private static class DateSerializer implements JsonSerializer<Date> {
        @Override
        public JsonElement serialize(Date src, Type typeOfSrc,
                JsonSerializationContext context) {
            return new JsonPrimitive(src.getTime());
        }
    }
}
