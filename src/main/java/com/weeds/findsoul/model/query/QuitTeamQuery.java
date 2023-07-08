package com.weeds.findsoul.model.query;

import lombok.Data;

import java.io.Serializable;

/**
 * @author weeds
 */
@Data
public class QuitTeamQuery implements Serializable {
    private static final long serialVersionUID = -3128329999314935404L;
    /**
     * 队伍id
     */
    private Long teamId;
}
