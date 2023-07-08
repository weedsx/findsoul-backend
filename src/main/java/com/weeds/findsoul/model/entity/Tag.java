package com.weeds.findsoul.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 标签
 *
 * @author weeds
 */
@TableName(value = "tag")
@Data
public class Tag implements Serializable {
    /**
     * id
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 标签名
     */
    @TableField(value = "tag_name")
    private String tagName;

    /**
     * 上传标签的用户的id
     */
    @TableField(value = "user_id")
    private Long userId;

    /**
     * 是否是父标签 0 - 不是父标签 1 - 是父标签
     */
    @TableField(value = "is_parent")
    private Long isParent;

    /**
     * 父标签id
     */
    @TableField(value = "parent_id")
    private Long parentId;

    /**
     * 创建时间
     */
    @TableField(value = "create_time")
    private Date createTime;

    /**
     *
     */
    @TableField(value = "update_time")
    private Date updateTime;

    /**
     * 是否删除
     */
    @TableField(value = "is_delete")
    @TableLogic
    private Integer isDelete;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}