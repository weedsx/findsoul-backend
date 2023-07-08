package com.weeds.findsoul.model.query;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * @author weeds
 */
@Data
public class AddTeamQuery implements Serializable {

    private static final long serialVersionUID = 8227674493594762551L;

    /**
     * 队伍名称
     */
    private String name;

    /**
     * 描述
     */
    private String description;

    /**
     * 最大人数
     */
    private Integer maxNum;

    /**
     * 过期时间
     */
    private Date expireTime;

    /**
     * 创建人用户id
     */
    private Long creatorId;

    /**
     * 0 - 公开，1 - 私有，2 - 加密
     */
    private Integer status;

    /**
     * 密码
     */
    private String password;
}
