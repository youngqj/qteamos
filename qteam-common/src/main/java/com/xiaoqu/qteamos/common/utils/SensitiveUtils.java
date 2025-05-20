package com.xiaoqu.qteamos.common.utils;

import cn.hutool.core.util.DesensitizedUtil;
import cn.hutool.core.util.StrUtil;

/**
 * 数据脱敏工具类
 */
public class SensitiveUtils {

    /**
     * 手机号脱敏
     * 例如：13812345678 -> 138****5678
     */
    public static String maskPhone(String phone) {
        return DesensitizedUtil.mobilePhone(phone);
    }

    /**
     * 邮箱脱敏
     * 例如：test@example.com -> t***@example.com
     */
    public static String maskEmail(String email) {
        return DesensitizedUtil.email(email);
    }

    /**
     * 身份证号脱敏
     * 例如：330102199901011234 -> 330102**********34
     */
    public static String maskIdCard(String idCard) {
        return DesensitizedUtil.idCardNum(idCard, 6, 4);
    }

    /**
     * 银行卡号脱敏
     * 例如：6222021234567890123 -> 622202******0123
     */
    public static String maskBankCard(String bankCard) {
        return DesensitizedUtil.bankCard(bankCard);
    }

    /**
     * 中文姓名脱敏
     * 例如：张三 -> 张*
     */
    public static String maskChineseName(String name) {
        return DesensitizedUtil.chineseName(name);
    }

    /**
     * 自定义脱敏
     * 保留前后指定位数，中间用*代替
     */
    public static String mask(String str, int front, int end) {
        if (StrUtil.isBlank(str)) {
            return str;
        }
        int length = str.length();
        if (front + end > length) {
            return str;
        }
        String prefix = str.substring(0, front);
        String suffix = str.substring(length - end);
        return prefix + StrUtil.repeat('*', length - front - end) + suffix;
    }

    /**
     * 密码脱敏
     * 全部替换为*
     */
    public static String maskPassword(String password) {
        if (StrUtil.isBlank(password)) {
            return password;
        }
        return StrUtil.repeat('*', password.length());
    }
} 