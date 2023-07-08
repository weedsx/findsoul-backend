package com.weeds.findsoul.constant;

/**
 * redis key 常量
 *
 * @author weeds
 */
public interface RedisKeyConstant {
    /**
     * 分布式锁的key
     */
    String DO_CACHE_RECOMMEND_USERS_LOCK = "find-soul-backend:PreCacheTask:doCacheRecommendUsers:lock";

    /**
     * 推荐用户缓存预热的key
     */
    String RECOMMEND_USERS_PREHEAT = "find-soul-backend:PreCacheTask:doCacheRecommendUsers:preheat";

    /**
     * 用户加入队伍分布式锁的key
     */
    String JOIN_TEAM_LOCK = "find-soul-backend:TeamService:joinTeam:lock";

}
