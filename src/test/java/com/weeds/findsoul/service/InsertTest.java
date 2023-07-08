package com.weeds.findsoul.service;

import com.weeds.findsoul.model.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.util.StopWatch;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author weeds
 */
@SpringBootTest
public class InsertTest {
    @Resource
    private UserService userService;
    /**
     * 线程池
     */
    ExecutorService executorService = Executors.newFixedThreadPool(20);

    @Test
    public void testInsert() {
        StopWatch stopWatch = new StopWatch();

        int INSERT_NUM = 50000;
        int BATCH_SIZE = 5000;

        ArrayList<User> userList = new ArrayList<>();
        stopWatch.start();
        for (int i = 0; i < INSERT_NUM; i++) {
            User user = new User();
            user.setUserName("fakeUser");
            user.setUserAccount("fake");
            user.setAvatarUrl("https://pic.code-nav.cn/user_avatar/1630910275517513729/MZmv9O2u-1680538676982.jpg");
            user.setGender(0);
            user.setUserPassword("12345678");
            user.setPhone("123");
            user.setEmail("123@qq.com");
            user.setTags("[]");
            user.setUserStatus(0);
            user.setUserRole(0);
            user.setPlanetCode("11111111");
            userList.add(user);
        }
        userService.saveBatch(userList, BATCH_SIZE);
        stopWatch.stop();
        System.out.println("耗时: " + stopWatch.getTotalTimeMillis() + " ms");
    }

    /**
     * 耗时: 2725 ms
     */
    @Test
    public void testAsyncInsert() {
        StopWatch stopWatch = new StopWatch();

        int INSERT_NUM = 100000;
        int BATCH_SIZE = 5000;
        int COUNT = 0;

        // 异步任务集合
        ArrayList<CompletableFuture<Void>> futureList = new ArrayList<>();

        stopWatch.start();
        for (int i = 0; i < INSERT_NUM / BATCH_SIZE; i++) {
            ArrayList<User> userList = new ArrayList<>();
            while (COUNT % BATCH_SIZE != 0 || COUNT == 0) {
                COUNT++;
                User user = new User();
                user.setUserName("fakeUser");
                user.setUserAccount("fake");
                user.setAvatarUrl("https://pic.code-nav.cn/user_avatar/1630910275517513729/MZmv9O2u-1680538676982.jpg");
                user.setGender(0);
                user.setUserPassword("12345678");
                user.setPhone("123");
                user.setEmail("123@qq.com");
                user.setTags("[]");
                user.setUserStatus(0);
                user.setUserRole(0);
                user.setPlanetCode("11111111");
                userList.add(user);
            }
            // 异步执行
            CompletableFuture<Void> runAsync = CompletableFuture.runAsync(() -> {
                System.out.println("threadName: " + Thread.currentThread().getName());
                userService.saveBatch(userList, BATCH_SIZE);
            }, executorService);
            // 加入任务集合
            futureList.add(runAsync);
        }
        // 等待所有异步任务结束
        CompletableFuture.allOf(futureList.toArray(new CompletableFuture[]{})).join();

        stopWatch.stop();
        System.out.println("耗时: " + stopWatch.getTotalTimeMillis() + " ms");
    }
}
