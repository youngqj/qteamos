/*
 * @Author: yangqijun youngqj@126.com
 * @Date: 2025-04-07 11:14:05
 * @LastEditors: yangqijun youngqj@126.com
 * @LastEditTime: 2025-04-07 18:06:24
 * @FilePath: /qelebase/qelebase-common/src/main/java/com/xiaoqu/qelebase/common/utils/JsonUtils.java
 * @Description: 
 * 
 * Copyright © Zhejiang Xiaoqu Information Technology Co., Ltd, All Rights Reserved. 
 */
package com.xiaoqu.qteamos.common.utils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.lang.reflect.Type;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

/**
 * JSON工具类
 *
 * @author yangqijun@xiaoquio.com
 * @version 1.0.0
 * @copyright 浙江小趣信息技术有限公司
 */
@Component
public class JsonUtils {

    private static String timeZone = "Asia/Shanghai";
    private static String dateFormat = "yyyy-MM-dd HH:mm:ss";

    @Value("${spring.gson.time-zone:Asia/Shanghai}")
    public void setTimeZone(String timeZone) {
        JsonUtils.timeZone = timeZone;
    }
    
    @Value("${spring.gson.date-format:yyyy-MM-dd HH:mm:ss}")
    public void setDateFormat(String dateFormat) {
        JsonUtils.dateFormat = dateFormat;
    }

    private static final Gson GSON = new GsonBuilder()
            .setDateFormat(dateFormat)
            .registerTypeAdapter(java.util.Date.class, new com.google.gson.JsonSerializer<java.util.Date>() {
                @Override
                public com.google.gson.JsonElement serialize(java.util.Date src, Type typeOfSrc, com.google.gson.JsonSerializationContext context) {
                    DateFormat df = new SimpleDateFormat(dateFormat);
                    df.setTimeZone(TimeZone.getTimeZone(timeZone));
                    return new com.google.gson.JsonPrimitive(df.format(src));
                }
            })
            .registerTypeAdapter(java.util.Date.class, new com.google.gson.JsonDeserializer<java.util.Date>() {
                @Override
                public java.util.Date deserialize(com.google.gson.JsonElement json, Type typeOfT, com.google.gson.JsonDeserializationContext context) throws com.google.gson.JsonParseException {
                    try {
                        DateFormat df = new SimpleDateFormat(dateFormat);
                        df.setTimeZone(TimeZone.getTimeZone(timeZone));
                        return df.parse(json.getAsString());
                    } catch (java.text.ParseException e) {
                        throw new com.google.gson.JsonParseException(e);
                    }
                }
            })
            .disableHtmlEscaping()
            .create();

    /**
     * 对象转JSON字符串
     *
     * @param object 对象
     * @return JSON字符串
     */
    public static String toJson(Object object) {
        return GSON.toJson(object);
    }

    /**
     * JSON字符串转对象
     *
     * @param json  JSON字符串
     * @param clazz 目标类型
     * @param <T>   泛型
     * @return 目标对象
     */
    public static <T> T fromJson(String json, Class<T> clazz) {
        return GSON.fromJson(json, clazz);
    }

    /**
     * JSON字符串转List
     *
     * @param json  JSON字符串
     * @param clazz 目标类型
     * @param <T>   泛型
     * @return 目标List
     */
    public static <T> List<T> fromJsonList(String json, Class<T> clazz) {
        Type type = TypeToken.getParameterized(List.class, clazz).getType();
        return GSON.fromJson(json, type);
    }

    /**
     * JSON字符串转Map
     *
     * @param json JSON字符串
     * @return Map对象
     */
    public static Map<String, Object> fromJsonMap(String json) {
        Type type = new TypeToken<Map<String, Object>>() {}.getType();
        return GSON.fromJson(json, type);
    }
} 