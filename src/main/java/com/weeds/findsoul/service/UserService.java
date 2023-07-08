package com.weeds.findsoul.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.weeds.findsoul.model.bo.UserBo;
import com.weeds.findsoul.model.entity.User;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;

/**
 * 用户的Service
 *
 * @author weeds
 * @description 针对表【user(用户)】的数据库操作Service
 */
public interface UserService extends IService<User> {
    /**
     * 用户注册
     *
     * @param userAccount   账号
     * @param password      密码
     * @param checkPassword 校验密码
     * @return 新用户id
     */
    long userRegister(String userAccount, String password, String checkPassword);

    /**
     * 用户登录
     *
     * @param userAccount 账号
     * @param password    密码
     * @return 信息脱敏的用户
     */
    User userLogin(String userAccount, String password, HttpServletRequest request);

    /**
     * 获取脱敏用户
     *
     * @param originUser 原用户
     * @return 脱敏用户
     */
    User getSafeUser(User originUser);

    /**
     * 退出登录，删除session
     *
     * @param request 请求
     * @return 1 | 0
     */
    int userLogout(HttpServletRequest request);

    /**
     * 根据标签列表查询符合所有标签用户
     *
     * @param tags 标签列表
     * @return 用户列表
     */
    List<UserBo> searchUsersByTags(List<String> tags);

    /**
     * 获取当前登录用户的信息
     *
     * @param request 请求
     * @return 脱敏用户
     */
    User getLoginUser(HttpServletRequest request);

    /**
     * 查看是否为管理员
     *
     * @param request 请求
     * @return 是否为管理员
     */
    boolean isAdmin(HttpServletRequest request);

    /**
     * 更新用户
     *
     * @param user    要更新的用户
     * @param request 请求
     * @return 改变的数据个数
     */
    int updateUser(User user, HttpServletRequest request);

    /**
     * 获取脱敏的用户DTO
     *
     * @param originUser
     * @return
     */
    UserBo getSafeUserBo(User originUser);

    /**
     * 将原用户的 json 格式的 tags 转为 ArrayList<String> 的 tags
     *
     * @param safeUserList 原用户列表
     * @return UserDto列表
     */
    ArrayList<UserBo> getUserBoList(List<User> safeUserList);

    /**
     * 获取不同用户的推荐用户列表
     * 做了缓存
     *
     * @param currentPage 页码
     * @param pageSize    页大小
     * @param request     请求
     * @return 用户列表
     */
    Page<UserBo> getRecommendUsers(long currentPage, long pageSize, HttpServletRequest request);

    /**
     * 根据标签获取最匹配的用户
     *
     * @param num       匹配用户个数
     * @param loginUser 请求
     * @return List<UserBo>
     */
    List<UserBo> getMatchUsers(long num, User loginUser);
}
