package com.weeds.findsoul.model.query;

import lombok.Data;

import java.io.Serializable;

/**
 * 用户注册请求参数
 *
 * @author weeds
 */
@Data
public class UserRegisterQuery implements Serializable {
    private static final long serialVersionUID = -8927793227458252627L;
    private String userAccount;
    private String userPassword;
    private String checkPassword;
}
