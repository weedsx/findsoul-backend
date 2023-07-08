package com.weeds.findsoul.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.weeds.findsoul.common.BaseResponse;
import com.weeds.findsoul.common.ErrorCode;
import com.weeds.findsoul.common.ResponseUtil;
import com.weeds.findsoul.exception.BusinessException;
import com.weeds.findsoul.model.bo.UserBo;
import com.weeds.findsoul.model.entity.User;
import com.weeds.findsoul.model.query.UserLoginQuery;
import com.weeds.findsoul.model.query.UserRegisterQuery;
import com.weeds.findsoul.service.UserService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * @author weeds
 */
@RestController
@RequestMapping("/user")
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:5173"})
public class UserController {
    @Resource
    private UserService userService;

    @PostMapping("/register")
    public BaseResponse<Long> userRegister(@RequestBody UserRegisterQuery registerRequest) {
        if (registerRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        String userAccount = registerRequest.getUserAccount();
        String userPassword = registerRequest.getUserPassword();
        String checkPassword = registerRequest.getCheckPassword();
        if (StringUtils.isAnyBlank(userAccount, userPassword, checkPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        long userId = userService.userRegister(userAccount, userPassword, checkPassword);
        return ResponseUtil.success(userId);
    }

    @PostMapping("/login")
    public BaseResponse<User> userLogin(
            @RequestBody UserLoginQuery loginRequest,
            HttpServletRequest request) {
        if (loginRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        String userAccount = loginRequest.getUserAccount();
        String password = loginRequest.getUserPassword();
        if (StringUtils.isAnyBlank(userAccount, password)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User user = userService.userLogin(userAccount, password, request);
        return ResponseUtil.success(user);
    }

    @PostMapping("/logout")
    public BaseResponse<Integer> userLogout(HttpServletRequest request) {
        if (request == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        int flag = userService.userLogout(request);
        return ResponseUtil.success(flag);
    }

    @GetMapping("/current")
    public BaseResponse<User> getCurrentUser(HttpServletRequest request) {
        Optional.ofNullable(request).orElseThrow(() -> new BusinessException(ErrorCode.PARAMS_ERROR));
        User safeUser = userService.getLoginUser(request);
        return ResponseUtil.success(safeUser);
    }

    @GetMapping("/search")
    public BaseResponse<List<User>> searchUsers(String userName, HttpServletRequest request) {
        // 鉴权，只有管理员才能查询
        if (!userService.isAdmin(request)) {
            throw new BusinessException(ErrorCode.NO_ACCESS);
        }
        LambdaQueryWrapper<User> wrapper = new QueryWrapper<User>().lambda();
        wrapper.like(StringUtils.isNotBlank(userName),
                User::getUserName, userName);
        List<User> userList = userService.list(wrapper);
        // 用户脱敏
        List<User> list = userList.stream().map(user -> userService.getSafeUser(user))
                .collect(Collectors.toList());
        return ResponseUtil.success(list);
    }

    @GetMapping("/recommend")
    public BaseResponse<Page<UserBo>> recommendUsers(
            @RequestParam(defaultValue = "1") long currentPage,
            @RequestParam(defaultValue = "10") long pageSize,
            HttpServletRequest request) {
        Page<UserBo> userDtoPage = userService.getRecommendUsers(currentPage, pageSize, request);
        return ResponseUtil.success(userDtoPage);
    }

    @PostMapping("/delete")
    public BaseResponse<Boolean> deleteUser(@RequestBody long userId, HttpServletRequest request) {
        // 鉴权，只有管理员才能删除
        if (!userService.isAdmin(request)) {
            throw new BusinessException(ErrorCode.NO_ACCESS);
        }
        if (userId < 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        boolean removed = userService.removeById(userId);
        return ResponseUtil.success(removed);
    }

    @GetMapping("/search/tags")
    public BaseResponse<List<UserBo>> searchUsersByTags(
            @RequestParam(required = false) List<String> tagList) {
        if (tagList == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "Invalid tag list");
        }
        List<UserBo> userList = userService.searchUsersByTags(tagList);
        return ResponseUtil.success(userList);
    }

    @PostMapping("/update")
    public BaseResponse<Integer> updateUser(@RequestBody User user, HttpServletRequest request) {
        Optional.ofNullable(user).orElseThrow(() -> new BusinessException(ErrorCode.PARAMS_ERROR));
        // 如果要更新的用户 id 为空直接报错
        Optional.ofNullable(user.getId()).orElseThrow(() -> new BusinessException(ErrorCode.PARAMS_ERROR));
        int flag = userService.updateUser(user, request);
        return ResponseUtil.success(flag);
    }

    @GetMapping("/match")
    public BaseResponse<List<UserBo>> matchUsers(long num, HttpServletRequest request) {
        if (num < 0 || num > 20) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "匹配数目为1~20");
        }
        User loginUser = userService.getLoginUser(request);
        List<UserBo> userBoList = userService.getMatchUsers(num, loginUser);
        return ResponseUtil.success(userBoList);
    }
}
