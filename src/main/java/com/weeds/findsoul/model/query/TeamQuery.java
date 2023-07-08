package com.weeds.findsoul.model.query;

import com.weeds.findsoul.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;
import java.util.List;

/**
 * @author weeds
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class TeamQuery extends PageRequest {
    private static final long serialVersionUID = 3854630969554725517L;
    /**
     * id
     */
    private Long id;

    /**
     * idList
     */
    private List<Long> idList;

    /**
     * 搜索关键字
     */
    private String searchText;

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
