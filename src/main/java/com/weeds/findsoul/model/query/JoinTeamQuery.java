package com.weeds.findsoul.model.query;

import lombok.Data;

import java.io.Serializable;

/**
 * @author weeds
 */
@Data
public class JoinTeamQuery implements Serializable {
    private static final long serialVersionUID = 6900841185576169312L;
    /**
     * 队伍id
     */
    private Long teamId;

    /**
     * 密码
     */
    private String password;
}
