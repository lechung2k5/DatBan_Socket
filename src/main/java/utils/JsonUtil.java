package utils;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.lang.reflect.Type;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
public class JsonUtil {
    private static final Gson gson = new GsonBuilder()
    .registerTypeAdapter(LocalDateTime.class, new LocalDateTimeAdapter())
    .registerTypeAdapter(LocalDate.class, new LocalDateAdapter())
    .registerTypeAdapter(LocalTime.class, new LocalTimeAdapter())
    .create();
    public static String toJson(Object obj) {
        return gson.toJson(obj);
    }
    public static <T> T fromJson(String json, Class<T> classOfT) {
        return gson.fromJson(json, classOfT);
    }
    public static <T> T fromJson(String json, Type typeOfT) {
        return gson.fromJson(json, typeOfT);
    }
    public static <T> java.util.List<T> fromJsonList(String json, Class<T> clazz) {
        Type typeOfT = com.google.gson.reflect.TypeToken.getParameterized(java.util.List.class, clazz).getType();
        return gson.fromJson(json, typeOfT);
    }
    /**
    * Chuyển đổi một object (thường là Map từ Gson) sang một class cụ thể.
    */
    public static <T> T convertValue(Object fromValue, Class<T> toValueType) {
        if (fromValue == null) return null;
        if (toValueType.isInstance(fromValue)) return toValueType.cast(fromValue);
        String json = toJson(fromValue);
        return fromJson(json, toValueType);
    }
}