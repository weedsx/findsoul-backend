package com.weeds.findsoul.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.weeds.findsoul.model.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;

/**
 * @author weeds
 */
@SpringBootTest
public class MapperTest {
    @Resource
    private UserService userService;

    @Test
    void test() {
        LambdaQueryWrapper<User> lambdaQW = new QueryWrapper<User>().lambda();
        lambdaQW.isNotNull(User::getTags);
        Page<User> page = userService.page(new Page<>(1, 20), lambdaQW);
        page.getRecords().forEach(System.out::println);
    }

}
