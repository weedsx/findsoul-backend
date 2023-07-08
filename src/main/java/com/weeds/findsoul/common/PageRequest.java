package com.weeds.findsoul.common;

import lombok.Data;

import java.io.Serializable;

/**
 * 通用分页请求参数类
 *
 * @author weeds
 */
@Data
public class PageRequest implements Serializable {
    private static final long serialVersionUID = 5810332412387100325L;
    /**
     * 当前页大小
     */
    protected long pageSize = 10;
    /**
     * 当前页
     */
    protected long currentPage = 1;
}
