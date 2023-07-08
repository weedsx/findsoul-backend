package com.weeds.findsoul.service;

import com.weeds.findsoul.model.bo.UserBo;
import com.weeds.findsoul.model.entity.User;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;
import java.util.Arrays;
import java.util.List;

/**
 * @author weeds
 */
@SpringBootTest
public class UserServiceTest {
    @Resource
    private UserService userService;

    @Test
    public void testAddUser() {
        User user = new User();
        user.setUserName("weeds");
        user.setUserAccount("weeds");
        user.setAvatarUrl("https://cdn.nlark.com/yuque/0/2023/jpeg/21359433/1680538677691-avatar/fd1822ac-2ae4-4a83-a07a-c5d5265af8c4.jpeg?x-oss-process=image%2Fresize%2Cm_fill%2Cw_328%2Ch_328%2Fformat%2Cpng");
        user.setGender(0);
        user.setUserPassword("xxx");
        user.setPhone("123");
        user.setEmail("123@126.com");
        System.out.println(userService.save(user));
//        System.out.println(1);
    }

    @Test
    public void userRegister() {
        String userAccount = "yupi";
        String userPassword = "";
        String checkPassword = "123456";
        long result = userService.userRegister(userAccount, userPassword, checkPassword);
        Assertions.assertEquals(-1, result);
        userAccount = "yu";
        result = userService.userRegister(userAccount, userPassword, checkPassword);
        Assertions.assertEquals(-1, result);
        userAccount = "yupi";
        userPassword = "123456";
        result = userService.userRegister(userAccount, userPassword, checkPassword);
        Assertions.assertEquals(-1, result);
        userAccount = "yu pi";
        userPassword = "12345678";
        result = userService.userRegister(userAccount, userPassword, checkPassword);
        Assertions.assertEquals(-1, result);
        checkPassword = "123456789";
        result = userService.userRegister(userAccount, userPassword, checkPassword);
        Assertions.assertEquals(-1, result);
        userAccount = "dogYupi";
        checkPassword = "12345678";
        result = userService.userRegister(userAccount, userPassword, checkPassword);
        Assertions.assertEquals(-1, result);
        userAccount = "yupi";
        result = userService.userRegister(userAccount, userPassword, checkPassword);
        Assertions.assertEquals(-1, result);
    }

    @Test
    public void userSearchUsersByTags() {
        List<String> tagList = Arrays.asList("Python", "前端");
        List<UserBo> users = userService.searchUsersByTags(tagList);
        Assertions.assertNotNull(users);
    }

}