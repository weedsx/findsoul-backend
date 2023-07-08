package com.weeds.findsoul.common;

/**
 * @author weeds
 */
public class ResponseUtil {
    /**
     * 成功
     *
     * @param data 数据
     * @param <T>  数据类型
     * @return 成功的BaseResponse
     */
    public static <T> BaseResponse<T> success(T data) {
        return new BaseResponse<>(
                ErrorCode.SUCCESS.getCode(),
                data,
                ErrorCode.SUCCESS.getMessage());
    }

    /**
     * 错误
     *
     * @param errorCode 错误枚举
     * @return 错误枚举的BaseResponse
     */
    public static <T> BaseResponse<T> error(ErrorCode errorCode) {
        return new BaseResponse<>(
                errorCode.getCode(),
                null,
                errorCode.getMessage(),
                errorCode.getDescription());
    }

    /**
     * 失败
     *
     * @param code
     * @param message
     * @param description
     * @return
     */
    public static <T> BaseResponse<T> error(int code, String message, String description) {
        return new BaseResponse<>(code, null, message, description);
    }

    /**
     * 失败
     *
     * @param errorCode
     * @return
     */
    public static <T> BaseResponse<T> error(ErrorCode errorCode, String message, String description) {
        return new BaseResponse<>(errorCode.getCode(), null, message, description);
    }

    /**
     * 失败
     *
     * @param errorCode
     * @return
     */
    public static <T> BaseResponse<T> error(ErrorCode errorCode, String description) {
        return new BaseResponse<>(errorCode.getCode(), null, errorCode.getMessage(), description);
    }

    private ResponseUtil() {
    }
}
