package com.weeds.findsoul.exception;

import com.weeds.findsoul.common.BaseResponse;
import com.weeds.findsoul.common.ErrorCode;
import com.weeds.findsoul.common.ResponseUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理
 *
 * @author weeds
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {
    @ExceptionHandler(BusinessException.class)
    public BaseResponse<Object> businessExceptionHandler(BusinessException e) {
        log.error("BusinessException：" + e.getMessage(), e);
        return ResponseUtil.error(e.getCode(), e.getMessage(), e.getDescription());
    }

    @ExceptionHandler(RuntimeException.class)
    public BaseResponse<Object> runtimeExceptionHandler(RuntimeException e) {
        log.error("BusinessException：" + e.getMessage(), e);
        return ResponseUtil.error(ErrorCode.SYSTEM_ERROR, e.getMessage(), "");
    }
}
