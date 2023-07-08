package com.weeds.findsoul.model.enums;

/**
 * @author weeds
 */
public enum TeamStatusEum {
    PUBLIC(0, "公开"),
    PRIVATE(1, "私有"),
    SECRET(2, "加密");
    private final int status;
    private final String msg;

    TeamStatusEum(int status, String msg) {
        this.status = status;
        this.msg = msg;
    }

    public static TeamStatusEum getEumByValue(int value) {
        for (TeamStatusEum teamStatusEum : TeamStatusEum.values()) {
            if (teamStatusEum.getStatus() == value) {
                return teamStatusEum;
            }
        }
        return null;
    }

    public int getStatus() {
        return status;
    }

    public String getMsg() {
        return msg;
    }
}
