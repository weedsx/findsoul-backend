package com.weeds.findsoul.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 队伍
 *
 * @TableName team
 */
@TableName(value = "team")
@Data
public class Team implements Serializable {
    @TableField(exist = false)
    private static final long serialVersionUID = 3689869564884334945L;
    /**
     * id
     */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 队伍名称
     */
    @TableField(value = "name")
    private String name;

    /**
     * 描述
     */
    @TableField(value = "description")
    private String description;

    /**
     * 最大人数
     */
    @TableField(value = "max_num")
    private Integer maxNum;

    /**
     * 过期时间
     */
    @TableField(value = "expire_time")
    private Date expireTime;

    /**
     * 创建人用户id
     */
    @TableField(value = "creator_id")
    private Long creatorId;

    /**
     * 0 - 公开，1 - 私有，2 - 加密
     */
    @TableField(value = "status")
    private Integer status;

    /**
     * 密码
     */
    @TableField(value = "password")
    private String password;

    /**
     * 创建时间
     */
    @TableField(value = "create_time")
    private Date createTime;

    /**
     * 更新时间
     */
    @TableField(value = "update_time")
    private Date updateTime;

    /**
     * 是否删除
     */
    @TableField(value = "is_delete")
    @TableLogic
    private Integer isDelete;
}