package com.weeds.findsoul.model.query;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * @author weeds
 */
@Data
public class TeamUpdateQuery implements Serializable {
    private static final long serialVersionUID = -3808940852981564639L;
    /**
     * id
     */
    private Long id;

    /**
     * 队伍名称
     */
    private String name;

    /**
     * 描述
     */
    private String description;

    /**
     * 过期时间
     */
    private Date expireTime;

    /**
     * 0 - 公开，1 - 私有，2 - 加密
     */
    private Integer status;

    /**
     * 密码
     */
    private String password;
}
