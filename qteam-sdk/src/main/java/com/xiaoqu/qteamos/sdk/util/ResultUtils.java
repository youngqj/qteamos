package com.xiaoqu.qteamos.sdk.util;

import java.util.List;

/**
 * 统一返回结果工具类
 * 提供与 QTeamOS 系统一致的返回结果格式
 * 
 * @author yangqijun
 * @date 2024-07-20
 * @since 1.0.0
 */
public class ResultUtils {
    
    /**
     * 通用返回结果类
     * 
     * @param <T> 数据类型
     */
    public static class ApiResult<T> {
        /**
         * 状态码
         */
        private Integer code;
        
        /**
         * 消息
         */
        private String message;
        
        /**
         * 数据
         */
        private T data;
        
        public ApiResult() {
        }
        
        public ApiResult(Integer code, String message, T data) {
            this.code = code;
            this.message = message;
            this.data = data;
        }
        
        public Integer getCode() {
            return code;
        }
        
        public void setCode(Integer code) {
            this.code = code;
        }
        
        public String getMessage() {
            return message;
        }
        
        public void setMessage(String message) {
            this.message = message;
        }
        
        public T getData() {
            return data;
        }
        
        public void setData(T data) {
            this.data = data;
        }
    }
    
    /**
     * 分页结果类
     * 
     * @param <T> 数据类型
     */
    public static class PageResult<T> {
        /**
         * 记录列表
         */
        private List<T> records;
        
        /**
         * 页码
         */
        private long page;
        
        /**
         * 每页大小
         */
        private long pageSize;
        
        /**
         * 总记录数
         */
        private long total;
        
        public PageResult() {
        }
        
        public PageResult(List<T> records, long page, long pageSize, long total) {
            this.records = records;
            this.page = page;
            this.pageSize = pageSize;
            this.total = total;
        }
        
        public List<T> getRecords() {
            return records;
        }
        
        public void setRecords(List<T> records) {
            this.records = records;
        }
        
        public long getPage() {
            return page;
        }
        
        public void setPage(long page) {
            this.page = page;
        }
        
        public long getPageSize() {
            return pageSize;
        }
        
        public void setPageSize(long pageSize) {
            this.pageSize = pageSize;
        }
        
        public long getTotal() {
            return total;
        }
        
        public void setTotal(long total) {
            this.total = total;
        }
    }
    
    /**
     * 响应状态码枚举
     */
    public static final class ResultCode {
        /**
         * 成功
         */
        public static final int SUCCESS = 200;
        
        /**
         * 失败
         */
        public static final int FAILED = 500;
        
        /**
         * 参数错误
         */
        public static final int VALIDATE_FAILED = 400;
        
        /**
         * 未授权
         */
        public static final int UNAUTHORIZED = 401;
        
        /**
         * 拒绝访问
         */
        public static final int FORBIDDEN = 403;
        
        /**
         * 资源不存在
         */
        public static final int NOT_FOUND = 404;
        
        /**
         * 用户名或密码错误
         */
        public static final int LOGIN_ERROR = 1001;
        
        /**
         * 用户名已存在
         */
        public static final int USERNAME_EXISTS = 1002;
        
        /**
         * 用户不存在
         */
        public static final int USER_NOT_FOUND = 1003;
        
        /**
         * 刷新令牌无效
         */
        public static final int REFRESH_TOKEN_INVALID = 1004;
        
        /**
         * 插件维护中
         */
        public static final int PLUGIN_UNDER = 2001;
        
        private ResultCode() {
            // 防止实例化
        }
    }
    
    /**
     * 构建成功响应
     * 
     * @return 响应结果
     */
    public static <T> ApiResult<T> success() {
        return success(null);
    }
    
    /**
     * 构建成功响应
     * 
     * @param data 数据
     * @return 响应结果
     */
    public static <T> ApiResult<T> success(T data) {
        return new ApiResult<>(ResultCode.SUCCESS, "操作成功", data);
    }
    
    /**
     * 构建成功响应
     * 
     * @param data 数据
     * @param message 消息
     * @return 响应结果
     */
    public static <T> ApiResult<T> success(T data, String message) {
        return new ApiResult<>(ResultCode.SUCCESS, message, data);
    }
    
    /**
     * 构建失败响应
     * 
     * @param message 错误消息
     * @return 响应结果
     */
    public static <T> ApiResult<T> failed(String message) {
        return new ApiResult<>(ResultCode.FAILED, message, null);
    }
    
    /**
     * 构建失败响应
     * 
     * @param code 错误码
     * @param message 错误消息
     * @return 响应结果
     */
    public static <T> ApiResult<T> failed(int code, String message) {
        return new ApiResult<>(code, message, null);
    }
    
    /**
     * 构建失败响应
     * 
     * @param code 错误码
     * @param message 错误消息
     * @param data 错误数据
     * @return 响应结果
     */
    public static <T> ApiResult<T> failed(int code, String message, T data) {
        return new ApiResult<>(code, message, data);
    }
    
    /**
     * 构建参数错误响应
     * 
     * @param message 错误消息
     * @return 响应结果
     */
    public static <T> ApiResult<T> validateFailed(String message) {
        return new ApiResult<>(ResultCode.VALIDATE_FAILED, message, null);
    }
    
    /**
     * 构建未授权响应
     * 
     * @return 响应结果
     */
    public static <T> ApiResult<T> unauthorized() {
        return new ApiResult<>(ResultCode.UNAUTHORIZED, "暂未登录或token已经过期", null);
    }
    
    /**
     * 构建拒绝访问响应
     * 
     * @return 响应结果
     */
    public static <T> ApiResult<T> forbidden() {
        return new ApiResult<>(ResultCode.FORBIDDEN, "没有相关权限", null);
    }
    
    /**
     * 构建资源不存在响应
     * 
     * @return 响应结果
     */
    public static <T> ApiResult<T> notFound() {
        return new ApiResult<>(ResultCode.NOT_FOUND, "资源不存在", null);
    }
    
    /**
     * 构建分页结果
     * 
     * @param list 数据列表
     * @param page 页码
     * @param pageSize 每页大小
     * @param total 总记录数
     * @return 分页结果
     */
    public static <T> PageResult<T> buildPageResult(List<T> list, long page, long pageSize, long total) {
        return new PageResult<>(list, page, pageSize, total);
    }
    
    /**
     * 构建分页成功响应
     * 
     * @param pageResult 分页结果
     * @return 响应结果
     */
    public static <T> ApiResult<PageResult<T>> pageSuccess(PageResult<T> pageResult) {
        return success(pageResult);
    }
    
    /**
     * 构建分页成功响应
     * 
     * @param list 数据列表
     * @param page 页码
     * @param pageSize 每页大小
     * @param total 总记录数
     * @return 响应结果
     */
    public static <T> ApiResult<PageResult<T>> pageSuccess(List<T> list, long page, long pageSize, long total) {
        PageResult<T> pageResult = buildPageResult(list, page, pageSize, total);
        return success(pageResult);
    }
} 