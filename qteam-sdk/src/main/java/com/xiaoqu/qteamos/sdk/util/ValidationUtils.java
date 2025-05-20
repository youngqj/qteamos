package com.xiaoqu.qteamos.sdk.util;

import java.util.Collection;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * 数据验证工具类
 * 提供常见的数据验证方法
 * 
 * @author yangqijun
 * @date 2024-07-20
 * @since 1.0.0
 */
public class ValidationUtils {
    
    /**
     * 邮箱正则表达式
     */
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[a-zA-Z0-9_-]+@[a-zA-Z0-9_-]+(\\.[a-zA-Z0-9_-]+)+$");
    
    /**
     * 手机号正则表达式（中国大陆）
     */
    private static final Pattern MOBILE_PATTERN = Pattern.compile("^1[3-9]\\d{9}$");
    
    /**
     * URL正则表达式
     */
    private static final Pattern URL_PATTERN = Pattern.compile("^(https?|ftp)://[^\\s/$.?#].[^\\s]*$");
    
    /**
     * 身份证号正则表达式（18位）
     */
    private static final Pattern ID_CARD_PATTERN = Pattern.compile("^[1-9]\\d{5}(19|20)\\d{2}(0[1-9]|1[0-2])(0[1-9]|[12]\\d|3[01])\\d{3}[0-9Xx]$");
    
    /**
     * 判断字符串是否为空
     * 
     * @param str 字符串
     * @return 是否为空
     */
    public static boolean isEmpty(String str) {
        return str == null || str.trim().isEmpty();
    }
    
    /**
     * 判断字符串是否不为空
     * 
     * @param str 字符串
     * @return 是否不为空
     */
    public static boolean isNotEmpty(String str) {
        return !isEmpty(str);
    }
    
    /**
     * 判断字符串是否为空或仅包含空白字符
     * 
     * @param str 字符串
     * @return 是否为空或仅包含空白字符
     */
    public static boolean isBlank(String str) {
        return str == null || str.trim().isEmpty();
    }
    
    /**
     * 判断字符串是否不为空且不仅包含空白字符
     * 
     * @param str 字符串
     * @return 是否不为空且不仅包含空白字符
     */
    public static boolean isNotBlank(String str) {
        return !isBlank(str);
    }
    
    /**
     * 判断集合是否为空
     * 
     * @param collection 集合
     * @return 是否为空
     */
    public static boolean isEmpty(Collection<?> collection) {
        return collection == null || collection.isEmpty();
    }
    
    /**
     * 判断集合是否不为空
     * 
     * @param collection 集合
     * @return 是否不为空
     */
    public static boolean isNotEmpty(Collection<?> collection) {
        return !isEmpty(collection);
    }
    
    /**
     * 判断映射是否为空
     * 
     * @param map 映射
     * @return 是否为空
     */
    public static boolean isEmpty(Map<?, ?> map) {
        return map == null || map.isEmpty();
    }
    
    /**
     * 判断映射是否不为空
     * 
     * @param map 映射
     * @return 是否不为空
     */
    public static boolean isNotEmpty(Map<?, ?> map) {
        return !isEmpty(map);
    }
    
    /**
     * 判断数组是否为空
     * 
     * @param array 数组
     * @return 是否为空
     */
    public static boolean isEmpty(Object[] array) {
        return array == null || array.length == 0;
    }
    
    /**
     * 判断数组是否不为空
     * 
     * @param array 数组
     * @return 是否不为空
     */
    public static boolean isNotEmpty(Object[] array) {
        return !isEmpty(array);
    }
    
    /**
     * 判断字符串是否是邮箱格式
     * 
     * @param email 邮箱
     * @return 是否是邮箱格式
     */
    public static boolean isEmail(String email) {
        return isNotEmpty(email) && EMAIL_PATTERN.matcher(email).matches();
    }
    
    /**
     * 判断字符串是否是手机号格式（中国大陆）
     * 
     * @param mobile 手机号
     * @return 是否是手机号格式
     */
    public static boolean isMobile(String mobile) {
        return isNotEmpty(mobile) && MOBILE_PATTERN.matcher(mobile).matches();
    }
    
    /**
     * 判断字符串是否是URL格式
     * 
     * @param url URL
     * @return 是否是URL格式
     */
    public static boolean isUrl(String url) {
        return isNotEmpty(url) && URL_PATTERN.matcher(url).matches();
    }
    
    /**
     * 判断字符串是否是身份证号格式（18位）
     * 
     * @param idCard 身份证号
     * @return 是否是身份证号格式
     */
    public static boolean isIdCard(String idCard) {
        return isNotEmpty(idCard) && ID_CARD_PATTERN.matcher(idCard).matches();
    }
    
    /**
     * 判断字符串是否是数字
     * 
     * @param str 字符串
     * @return 是否是数字
     */
    public static boolean isNumber(String str) {
        if (isEmpty(str)) {
            return false;
        }
        try {
            Double.parseDouble(str);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }
    
    /**
     * 判断字符串是否是整数
     * 
     * @param str 字符串
     * @return 是否是整数
     */
    public static boolean isInteger(String str) {
        if (isEmpty(str)) {
            return false;
        }
        try {
            Integer.parseInt(str);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }
    
    /**
     * 判断对象是否为null
     * 
     * @param obj 对象
     * @return 是否为null
     */
    public static boolean isNull(Object obj) {
        return obj == null;
    }
    
    /**
     * 判断对象是否不为null
     * 
     * @param obj 对象
     * @return 是否不为null
     */
    public static boolean isNotNull(Object obj) {
        return obj != null;
    }
    
    /**
     * 判断字符串是否满足最小长度要求
     * 
     * @param str 字符串
     * @param minLength 最小长度
     * @return 是否满足最小长度要求
     */
    public static boolean hasMinLength(String str, int minLength) {
        return isNotEmpty(str) && str.length() >= minLength;
    }
    
    /**
     * 判断字符串是否满足最大长度要求
     * 
     * @param str 字符串
     * @param maxLength 最大长度
     * @return 是否满足最大长度要求
     */
    public static boolean hasMaxLength(String str, int maxLength) {
        return isNotEmpty(str) && str.length() <= maxLength;
    }
    
    /**
     * 判断字符串是否在指定长度范围内
     * 
     * @param str 字符串
     * @param minLength 最小长度
     * @param maxLength 最大长度
     * @return 是否在指定长度范围内
     */
    public static boolean hasLengthBetween(String str, int minLength, int maxLength) {
        return isNotEmpty(str) && str.length() >= minLength && str.length() <= maxLength;
    }
    
    /**
     * 判断数字是否在指定范围内
     * 
     * @param num 数字
     * @param min 最小值
     * @param max 最大值
     * @return 是否在指定范围内
     */
    public static boolean isBetween(int num, int min, int max) {
        return num >= min && num <= max;
    }
    
    /**
     * 判断数字是否在指定范围内
     * 
     * @param num 数字
     * @param min 最小值
     * @param max 最大值
     * @return 是否在指定范围内
     */
    public static boolean isBetween(long num, long min, long max) {
        return num >= min && num <= max;
    }
    
    /**
     * 判断数字是否在指定范围内
     * 
     * @param num 数字
     * @param min 最小值
     * @param max 最大值
     * @return 是否在指定范围内
     */
    public static boolean isBetween(double num, double min, double max) {
        return num >= min && num <= max;
    }
} 