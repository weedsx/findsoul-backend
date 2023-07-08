package com.weeds.findsoul.model.vo;

import com.weeds.findsoul.model.bo.UserBo;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * @author weeds
 */
@Data
public class TeamUserVO implements Serializable {
    private static final long serialVersionUID = 2202206685892291661L;
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

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 创建人
     */
    private UserBo creator;

    /**
     * 是否已加入队伍
     */
    private Boolean hasJoined = false;

    /**
     * 队伍当前人数
     */
    private Long hasJoinedNum;
}
