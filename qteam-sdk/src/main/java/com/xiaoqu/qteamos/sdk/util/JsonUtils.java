/*
 * Copyright (c) 2023-2025 XiaoQuTeam. All rights reserved.
 * QTeamOS is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 *          http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND,
 * EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT,
 * MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */

/**
 * JSON工具类
 * 提供JSON序列化和反序列化功能
 *
 * @author yangqijun
 * @date 2025-05-06
 * @since 1.0.0
 */
package com.xiaoqu.qteamos.sdk.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Type;
import java.text.DateFormat;
import java.util.List;
import java.util.Map;

/**
 * JSON工具类
 * 提供JSON数据的序列化和反序列化方法
 * 
 * @author yangqijun
 * @date 2024-07-20
 * @since 1.0.0
 */
public class JsonUtils {
    
    private static final Logger log = LoggerFactory.getLogger(JsonUtils.class);
    
    /**
     * Gson实例
     */
    private static final Gson GSON;
    
    /**
     * 格式化输出的Gson实例
     */
    private static final Gson PRETTY_GSON;
    
    static {
        // 创建基本Gson实例
        GSON = new GsonBuilder()
                .setDateFormat("yyyy-MM-dd HH:mm:ss")
                .serializeNulls()
                .create();
        
        // 创建格式化输出的Gson实例
        PRETTY_GSON = new GsonBuilder()
                .setDateFormat("yyyy-MM-dd HH:mm:ss")
                .serializeNulls()
                .setPrettyPrinting()
                .create();
    }
    
    /**
     * 对象转JSON字符串
     * 
     * @param obj 对象
     * @return JSON字符串
     */
    public static String toJson(Object obj) {
        if (obj == null) {
            return null;
        }
        try {
            return GSON.toJson(obj);
        } catch (Exception e) {
            log.error("对象转JSON字符串失败: {}", e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * 对象转格式化的JSON字符串
     * 
     * @param obj 对象
     * @return 格式化的JSON字符串
     */
    public static String toPrettyJson(Object obj) {
        if (obj == null) {
            return null;
        }
        try {
            return PRETTY_GSON.toJson(obj);
        } catch (Exception e) {
            log.error("对象转格式化JSON字符串失败: {}", e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * JSON字符串转对象
     * 
     * @param json JSON字符串
     * @param clazz 对象类型
     * @param <T> 对象泛型
     * @return 对象
     */
    public static <T> T fromJson(String json, Class<T> clazz) {
        if (ValidationUtils.isEmpty(json)) {
            return null;
        }
        try {
            return GSON.fromJson(json, clazz);
        } catch (JsonSyntaxException e) {
            log.error("JSON字符串转对象失败: {}", e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * JSON字符串转复杂对象
     * 
     * @param json JSON字符串
     * @param typeToken 类型令牌
     * @param <T> 对象泛型
     * @return 对象
     */
    public static <T> T fromJson(String json, TypeToken<T> typeToken) {
        if (ValidationUtils.isEmpty(json)) {
            return null;
        }
        try {
            return GSON.fromJson(json, typeToken.getType());
        } catch (JsonSyntaxException e) {
            log.error("JSON字符串转复杂对象失败: {}", e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * JSON字符串转List
     * 
     * @param json JSON字符串
     * @param clazz 元素类型
     * @param <T> 元素泛型
     * @return List
     */
    public static <T> List<T> fromJsonToList(String json, Class<T> clazz) {
        if (ValidationUtils.isEmpty(json)) {
            return null;
        }
        try {
            Type type = TypeToken.getParameterized(List.class, clazz).getType();
            return GSON.fromJson(json, type);
        } catch (JsonSyntaxException e) {
            log.error("JSON字符串转List失败: {}", e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * JSON字符串转Map
     * 
     * @param json JSON字符串
     * @return Map
     */
    public static Map<String, Object> fromJsonToMap(String json) {
        if (ValidationUtils.isEmpty(json)) {
            return null;
        }
        try {
            Type type = new TypeToken<Map<String, Object>>() {}.getType();
            return GSON.fromJson(json, type);
        } catch (JsonSyntaxException e) {
            log.error("JSON字符串转Map失败: {}", e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * JSON字符串转JsonElement
     * 
     * @param json JSON字符串
     * @return JsonElement
     */
    public static JsonElement fromJsonToJsonElement(String json) {
        if (ValidationUtils.isEmpty(json)) {
            return null;
        }
        try {
            return JsonParser.parseString(json);
        } catch (JsonSyntaxException e) {
            log.error("JSON字符串转JsonElement失败: {}", e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * 对象转换为另一个对象
     * 
     * @param obj 源对象
     * @param clazz 目标对象类型
     * @param <T> 目标对象泛型
     * @return 目标对象
     */
    public static <T> T convertObject(Object obj, Class<T> clazz) {
        if (obj == null) {
            return null;
        }
        try {
            String json = toJson(obj);
            return fromJson(json, clazz);
        } catch (Exception e) {
            log.error("对象转换失败: {}", e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * 获取Gson实例
     * 
     * @return Gson实例
     */
    public static Gson getGson() {
        return GSON;
    }
} 