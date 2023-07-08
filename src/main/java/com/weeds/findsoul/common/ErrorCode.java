package com.weeds.findsoul.common;

/**
 * @author weeds
 */
public enum ErrorCode {
    SUCCESS(200, "ok", ""),
    PARAMS_ERROR(40000, "请求参数错误", ""),
    PARAMS_NULL_ERROR(40001, "请求参数为空", ""),
    NOT_LOGIN(40100, "未登录", ""),
    NO_ACCESS(40101, "无权限", ""),
    NULL_ERROR(50001, "数据为空", ""),
    SYSTEM_ERROR(50000, "系统内部错误", ""),
    DB_ERROR(50002, "数据库错误", "");

    private final int code;
    /**
     * 状态码信息
     */
    private final String message;

    /**
     * 状态码描述（详情）
     */
    private final String description;

    ErrorCode(int code, String message, String description) {
        this.code = code;
        this.message = message;
        this.description = description;
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    public String getDescription() {
        return description;
    }
}
