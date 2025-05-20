package com.xiaoqu.qteamos.common.utils;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public class DateUtils {
    private static final String DEFAULT_TIME_ZONE = "Asia/Shanghai";
    private static final String DEFAULT_DATE_FORMAT = "yyyy-MM-dd HH:mm:ss";

    /**
     * 将LocalDateTime转换为时间戳
     *
     * @param dateTime 日期时间
     * @param timeZone 时区
     * @return 时间戳
     */
    public static long toTimestamp(LocalDateTime dateTime, String timeZone) {
        if (dateTime == null) {
            return 0L;
        }
        ZoneId zoneId = ZoneId.of(timeZone);
        return dateTime.atZone(zoneId).toInstant().toEpochMilli();
    }

    /**
     * 将LocalDateTime转换为时间戳（使用默认时区）
     *
     * @param dateTime 日期时间
     * @return 时间戳
     */
    public static long toTimestamp(LocalDateTime dateTime) {
        return toTimestamp(dateTime, DEFAULT_TIME_ZONE);
    }

    /**
     * 将时间戳转换为LocalDateTime
     *
     * @param timestamp 时间戳
     * @param timeZone 时区
     * @return LocalDateTime
     */
    public static LocalDateTime toLocalDateTime(long timestamp, String timeZone) {
        ZoneId zoneId = ZoneId.of(timeZone);
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(timestamp), zoneId);
    }

    /**
     * 将时间戳转换为LocalDateTime（使用默认时区）
     *
     * @param timestamp 时间戳
     * @return LocalDateTime
     */
    public static LocalDateTime toLocalDateTime(long timestamp) {
        return toLocalDateTime(timestamp, DEFAULT_TIME_ZONE);
    }

    /**
     * 将LocalDateTime格式化为字符串
     *
     * @param dateTime 日期时间
     * @param pattern 格式
     * @return 格式化后的字符串
     */
    public static String format(LocalDateTime dateTime, String pattern) {
        if (dateTime == null) {
            return null;
        }
        return dateTime.format(DateTimeFormatter.ofPattern(pattern));
    }

    /**
     * 将LocalDateTime格式化为字符串（使用默认格式）
     *
     * @param dateTime 日期时间
     * @return 格式化后的字符串
     */
    public static String format(LocalDateTime dateTime) {
        return format(dateTime, DEFAULT_DATE_FORMAT);
    }

    /**
     * 将字符串解析为LocalDateTime
     *
     * @param dateStr 日期字符串
     * @param pattern 格式
     * @return LocalDateTime
     */
    public static LocalDateTime parse(String dateStr, String pattern) {
        if (dateStr == null) {
            return null;
        }
        return LocalDateTime.parse(dateStr, DateTimeFormatter.ofPattern(pattern));
    }

    /**
     * 将字符串解析为LocalDateTime（使用默认格式）
     *
     * @param dateStr 日期字符串
     * @return LocalDateTime
     */
    public static LocalDateTime parse(String dateStr) {
        return parse(dateStr, DEFAULT_DATE_FORMAT);
    }
} 