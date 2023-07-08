package com.weeds.findsoul.task;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.weeds.findsoul.model.bo.UserBo;
import com.weeds.findsoul.model.entity.User;
import com.weeds.findsoul.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.weeds.findsoul.constant.RedisKeyConstant.DO_CACHE_RECOMMEND_USERS_LOCK;
import static com.weeds.findsoul.constant.RedisKeyConstant.RECOMMEND_USERS_PREHEAT;

/**
 * @author weeds
 */
@Component
@Slf4j
public class PreCacheTask {
    private final List<Long> vipUserIdList = Arrays.asList(1663454129109958657L, 1659908167863115777L);
    @Resource
    private UserService userService;
    @Resource
    private RedisTemplate<String, Object> redisTemplate;
    @Resource
    private RedissonClient redissonClient;

    /**
     * 每天零点预热 RecommendUsers 的缓存
     * 预热不同用户的 RecommendUsers 缓存
     */
    @Scheduled(cron = "0 0 0 * * *") // 秒 分 时 日 月 年
    public void doCacheRecommendUsers() {
        RLock lock = redissonClient.getLock(DO_CACHE_RECOMMEND_USERS_LOCK);
        try {
            // 多机环境下只有一个机器的定时任务线程能执行
            if (lock.tryLock(0, TimeUnit.MILLISECONDS)) {
                // 执行缓存预热
                ValueOperations<String, Object> opsForValue = redisTemplate.opsForValue();
                for (Long userId : vipUserIdList) {
                    String redisKey = RECOMMEND_USERS_PREHEAT + userId;
                    Page<User> page = userService.page(new Page<>(1, 20));
                    List<User> userList = page.getRecords();
                    List<User> list = userList.stream()
                            .map(user -> userService.getSafeUser(user))
                            .collect(Collectors.toList());
                    ArrayList<UserBo> userDtoList = userService.getUserBoList(list);
                    // 写缓存
                    try {
                        opsForValue.set(redisKey, userDtoList, 1000 * 60 * 10, TimeUnit.MILLISECONDS);
                    } catch (Exception e) {
                        log.info("redis set key error", e);
                    }
                }
            }
        } catch (InterruptedException e) {
            log.error("doCacheRecommendUsers error: ", e);
        } finally {
            // 只能释放自己的锁
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }

    }
}
