package com.weeds.findsoul.common;

import lombok.Data;

import java.io.Serializable;

/**
 * @author weeds
 */
@Data
public class BaseResponse<T> implements Serializable {
    private static final long serialVersionUID = -3570878497896063867L;
    private int code;
    private T data;
    private String message;
    private String description;

    public BaseResponse(int code, T data, String message, String description) {
        this.code = code;
        this.data = data;
        this.message = message;
        this.description = description;
    }

    public BaseResponse(int code, T data, String message) {
        this(code, data, message, "");
    }

    public BaseResponse(int code, T data) {
        this(code, data, "", "");
    }

}
