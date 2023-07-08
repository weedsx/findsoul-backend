package com.weeds.findsoul.model.query;

import lombok.Data;

import java.io.Serializable;

/**
 * 用户登录请求参数
 *
 * @author weeds
 */
@Data
public class UserLoginQuery implements Serializable {
    private static final long serialVersionUID = -6616503136747793076L;
    private String userAccount;
    private String userPassword;
}
