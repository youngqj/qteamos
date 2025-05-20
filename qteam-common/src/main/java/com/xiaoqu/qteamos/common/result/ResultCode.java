/*
 * @Author: yangqijun youngqj@126.com
 * @Date: 2025-04-07 11:14:34
 * @LastEditors: yangqijun youngqj@126.com
 * @LastEditTime: 2025-04-07 18:08:05
 * @FilePath: /qelebase/qelebase-common/src/main/java/com/xiaoqu/qelebase/common/result/ResultCode.java
 * @Description: 
 * 
 * Copyright © Zhejiang Xiaoqu Information Technology Co., Ltd, All Rights Reserved. 
 */
package com.xiaoqu.qteamos.common.result;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 响应状态码枚举
 *
 * @author yangqijun@xiaoquio.com
 * @version 1.0.0
 * @copyright 浙江小趣信息技术有限公司
 */
@Getter
@AllArgsConstructor
public enum ResultCode {

    /**
     * 成功
     */
    SUCCESS(200, "操作成功"),

    /**
     * 失败
     */
    FAILED(500, "操作失败"),
    
    /**
     * 参数错误
     */
    VALIDATE_FAILED(404, "参数检验失败"),
    
    /**
     * 未授权
     */
    UNAUTHORIZED(401, "暂未登录或token已经过期"),
    
    /**
     * 拒绝访问
     */
    FORBIDDEN(403, "没有相关权限"),
    
    /**
     * 资源不存在
     */
    NOT_FOUND(404, "资源不存在"),
    
    /**
     * 用户名或密码错误
     */
    LOGIN_ERROR(1001, "用户名或密码错误"),
    
    /**
     * 用户名已存在
     */
    USERNAME_EXISTS(1002, "用户名已存在"),
    
    /**
     * 用户不存在
     */
    USER_NOT_FOUND(1003, "用户不存在"),
    
    /**
     * 刷新令牌无效
     */
    REFRESH_TOKEN_INVALID(1004, "刷新令牌无效"),
    PLUGIN_UNDER(2001,"插件维护中"),
    
    /**
     * 刷新令牌不匹配
     */
    REFRESH_TOKEN_MISMATCH(1005, "刷新令牌不匹配"),

    /**
     * 插件连接相关错误码 - 4000系列
     */
    PLUGIN_CONNECTION_FAILED(4000, "插件连接失败"),
    PLUGIN_CONNECTION_TIMEOUT(4001, "插件连接超时"),
    PLUGIN_CONNECTION_REFUSED(4002, "插件连接被拒绝"),
    PLUGIN_CONNECTION_RESET(4003, "插件连接被重置"),
    PLUGIN_CONNECTION_CLOSED(4004, "插件连接已关闭"),
    PLUGIN_CONNECTION_INTERRUPTED(4005, "插件连接被中断"),
    PLUGIN_CONNECTION_NETWORK_UNREACHABLE(3007, "网络不可达，无法连接插件"),
    PLUGIN_CONNECTION_HOST_UNREACHABLE(3008, "主机不可达，无法连接插件"),
    PLUGIN_CONNECTION_ADDRESS_INVALID(3009, "插件连接地址无效");

    private final Integer code;
    private final String message;

    public Integer getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
} 