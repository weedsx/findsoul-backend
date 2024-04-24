package com.weeds.findsoul.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.weeds.findsoul.common.ErrorCode;
import com.weeds.findsoul.exception.BusinessException;
import com.weeds.findsoul.mapper.UserMapper;
import com.weeds.findsoul.model.bo.UserBo;
import com.weeds.findsoul.model.entity.User;
import com.weeds.findsoul.service.UserService;
import com.weeds.findsoul.util.AlgorithmUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.DigestUtils;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.lang.reflect.Type;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.weeds.findsoul.constant.UserConstant.ADMIN_ROLE;
import static com.weeds.findsoul.constant.UserConstant.USER_LOGIN_STATE;

/**
 * @author weeds
 * @description 针对表【user(用户)】的数据库操作Service实现
 */
@Service
@Slf4j
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {
    /**
     * 密码混淆盐
     */
    private static final String SALT = "weeds";

    @Value("${spring.application.name}")
    private String projectName;

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    @Resource
    private Gson gson;

    @Override
    public long userRegister(String userAccount, String password, String checkPassword) {
        // 校验
        if (StringUtils.isAnyBlank(userAccount, password, checkPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数为空");
        }
        if (userAccount.length() < 4) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户名过短");
        }
        if (password.length() < 8 || checkPassword.length() < 8) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "密码过短");
        }
        // 账号不能有特殊字符
        String validPattern = "[`~!@#$%^&*()+=|{}':;',\\\\[\\\\].<>/?~！@#￥%……&*（）——+|{}【】‘；：”“’。，、？]";
        if (userAccount.matches(validPattern)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号请勿包含特殊字符");
        }
        // 密码和校验密码相同
        if (!password.equals(checkPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "两次输入的密码不同");
        }
        // 账号不能重复，查询数据库放在最后
        QueryWrapper<User> wrapper = new QueryWrapper<>();
        wrapper.eq("user_account", userAccount);
        long count = this.count(wrapper);
        if (count > 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号重复");
        }
        // 密码加密
        String encryptPassword = DigestUtils.md5DigestAsHex((SALT + password).getBytes());
        User user = new User();
        user.setUserAccount(userAccount);
        user.setUserPassword(encryptPassword);
        boolean save = this.save(user);
        if (!save) {
            return -1;
        }
        return user.getId();
    }

    @Override
    public User userLogin(String userAccount, String password, HttpServletRequest request) {
        // 校验
        if (StringUtils.isAnyBlank(userAccount, password)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数为空");
        }
        if (userAccount.length() < 4) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户名过短");
        }
        if (password.length() < 8) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "密码过短");
        }
        // 密码加密并与数据库中的密文比较
        String encryptPassword = DigestUtils.md5DigestAsHex((SALT + password).getBytes());
        QueryWrapper<User> wrapper = new QueryWrapper<>();
        wrapper.eq("user_account", userAccount)
                .eq("user_password", encryptPassword);
        User user = this.getOne(wrapper);
        if (user == null) {
            log.info("user login failed, userAccount cannot match userPassword");
            throw new BusinessException(ErrorCode.NULL_ERROR, "账号密码错误");
        }
        User safeUser = getSafeUser(user);
        // 记录用户的登录态
        request.getSession().setAttribute(USER_LOGIN_STATE, safeUser);

        return safeUser;
    }

    @Override
    public User getSafeUser(User originUser) {
        if (originUser == null) {
            return null;
        }
        // 用户信息脱敏
        User safeUser = new User();
        safeUser.setId(originUser.getId());
        safeUser.setUserName(originUser.getUserName());
        safeUser.setUserAccount(originUser.getUserAccount());
        safeUser.setAvatarUrl(originUser.getAvatarUrl());
        safeUser.setGender(originUser.getGender());
        safeUser.setPhone(originUser.getPhone());
        safeUser.setEmail(originUser.getEmail());
        safeUser.setUserStatus(originUser.getUserStatus());
        safeUser.setCreateTime(originUser.getCreateTime());
        safeUser.setUserRole(originUser.getUserRole());
        safeUser.setPlanetCode(originUser.getPlanetCode());
        safeUser.setTags(originUser.getTags());
        return safeUser;
    }

    @Override
    public int userLogout(HttpServletRequest request) {
        request.getSession().removeAttribute(USER_LOGIN_STATE);
        return 1;
    }

    @Override
    public List<UserBo> searchUsersByTags(List<String> tags) {
        if (CollectionUtils.isEmpty(tags)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请至少选择一个标签");
        }

        // 使用内存筛选
//        List<User> safeUserList = searchUsersByTagsBySQL(tags);
        List<User> safeUserList = searchUsersByTagsByMemory(tags);

        return getUserBoList(safeUserList);
    }

    @Override
    public UserBo getSafeUserBo(User originUser) {
        User safeUser = this.getSafeUser(originUser);
        Type type = new TypeToken<ArrayList<String>>() {
        }.getType();
        String tagsJson = safeUser.getTags();
        UserBo userDto = new UserBo();
        if (StringUtils.isNotBlank(tagsJson)) {
            ArrayList<String> tagsList = gson.fromJson(tagsJson, type);
            BeanUtils.copyProperties(safeUser, userDto);
            userDto.setTags(tagsList);
        }
        return userDto;
    }

    @Override
    public ArrayList<UserBo> getUserBoList(List<User> safeUserList) {
        Type type = new TypeToken<ArrayList<String>>() {
        }.getType();
        ArrayList<UserBo> userDtoList = new ArrayList<>();
        for (User user : safeUserList) {
            String tagsJson = user.getTags();
            if (StringUtils.isNotBlank(tagsJson)) {
                ArrayList<String> tagsList = gson.fromJson(tagsJson, type);
                UserBo userDto = new UserBo();
                BeanUtils.copyProperties(user, userDto);
                userDto.setTags(tagsList);
                userDtoList.add(userDto);
            }
        }
        return userDtoList;
    }

    @Override
    public Page<UserBo> getRecommendUsers(long currentPage, long pageSize, HttpServletRequest request) {
        User loginUser = this.getLoginUser(request);
        String redisKey = String.format("%s:user:getRecommendUsers:%s", projectName, loginUser.getId());
        ValueOperations<String, Object> redisOperations = redisTemplate.opsForValue();
        // 先查 redis 缓存
        Page<UserBo> redisUserPage = (Page<UserBo>) redisOperations.get(redisKey);
        if (redisUserPage != null) {
            return redisUserPage;
        }
        Page<User> userPage = new Page<>(currentPage, pageSize);
        Page<User> page = this.page(userPage);
        List<User> userList = page.getRecords();
        // 用户脱敏、封装 UserDto
        List<User> list = userList.stream()
                .map(this::getSafeUser)
                .collect(Collectors.toList());
        ArrayList<UserBo> userDtoList = this.getUserBoList(list);
        // 重新构建 UserDto 的 Page
        Page<UserBo> userDtoPage = new Page<>();
        BeanUtils.copyProperties(page, userDtoPage);
        userDtoPage.setRecords(userDtoList);
        // 存入缓存
        try {
            // try catch 一下，如果出现错误记录日志
            // 一定要加过期时间
            redisOperations.set(redisKey, userDtoPage, 1000 * 60 * 10, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            log.info("redis set key error", e);
        }
        return userDtoPage;
    }

    @Override
    public List<UserBo> getMatchUsers(long num, User loginUser) {
        // 先查询所有用户，只取 id 和 tags
        // 并剔除 tags 为空的，包括 [] 和 NULL
        LambdaQueryWrapper<User> lambdaQW = new LambdaQueryWrapper<>();
        lambdaQW.select(User::getId, User::getTags)
                // 剔除自己
                .ne(User::getId, loginUser.getId())
                .ne(User::getTags, "[]")
                .isNotNull(User::getTags);
        List<User> userList = this.list(lambdaQW);
        // 初步满足要求的用户
        String currUserTags = loginUser.getTags();
        List<String> currTagList = gson.fromJson(currUserTags, new TypeToken<List<String>>() {
        }.getType());
        // 记录：用户 - 匹配度
        List<Pair<User, Integer>> pairList = new ArrayList<>();
        for (User user : userList) {
            String tags = user.getTags();
            List<String> tagList = gson.fromJson(tags, new TypeToken<List<String>>() {
            }.getType());
            // 标签匹配相似度计算
            int distance = AlgorithmUtils.minDistance(currTagList, tagList);
            pairList.add(Pair.of(user, distance));
        }
        // pairList 按相似度由小到大排序，并截取前 num 个 User
        List<Pair<User, Integer>> sortedPairList = pairList.stream()
                .sorted(Comparator.comparingInt(Pair::getRight))
                .limit(num)
                .collect(Collectors.toList());
        // 相似度已经由小到大排序的用户 id
        List<Long> idList = sortedPairList.stream()
                .map(pair -> pair.getLeft().getId())
                .collect(Collectors.toList());
        // 按最相似用户的 id 查出完整用户信息
        lambdaQW = new LambdaQueryWrapper<>();
        lambdaQW.in(User::getId, idList);
        Map<Long, List<User>> idUserMap = this.list(lambdaQW)
                // 按照 id 进行归约
                .stream()
                .map(this::getSafeUser)
                .collect(Collectors.groupingBy(User::getId));
        List<User> resultUserList = new ArrayList<>();
        for (Long id : idList) {
            resultUserList.add(idUserMap.get(id).get(0));
        }
        // 脱敏、规整
        return resultUserList.stream()
                .map(this::getSafeUserBo)
                .collect(Collectors.toList());
    }

    @Override
    public User getLoginUser(HttpServletRequest request) {
        Optional.ofNullable(request).orElseThrow(() -> new BusinessException(ErrorCode.PARAMS_ERROR));

        User sessionUser = (User) request.getSession().getAttribute(USER_LOGIN_STATE);
        Optional.ofNullable(sessionUser).orElseThrow(() -> new BusinessException(ErrorCode.NULL_ERROR, "会话已过期"));

        User user = this.getById(sessionUser.getId());
        Optional.ofNullable(user).orElseThrow(() -> new BusinessException(ErrorCode.NULL_ERROR));
        return this.getSafeUser(user);
    }

    @Override
    public boolean isAdmin(HttpServletRequest request) {
        // 鉴权，看是不是管理员
        User user = (User) request.getSession().getAttribute(USER_LOGIN_STATE);
        if (user == null) {
            return false;
        }
        return user.getUserRole() == ADMIN_ROLE;
    }

    @Override
    public int updateUser(User user, HttpServletRequest request) {
        long userId = user.getId();
        long loginUserId = getLoginUser(request).getId();


        // 如果不是管理员并且改的还不是登录的用户就抛异常
        if (!isAdmin(request) && userId != loginUserId) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        // 先查看一下要修改的用户原来的信息
        User oldUser = baseMapper.selectById(userId);
        Optional.ofNullable(oldUser).orElseThrow(() -> new BusinessException(ErrorCode.NULL_ERROR));
        return baseMapper.updateById(user);
    }

    /**
     * 使用内存筛选满足所有标签的用户
     *
     * @param tags 标签列表
     * @return 满足所有标签的脱敏用户列表
     */
    private List<User> searchUsersByTagsByMemory(List<String> tags) {
        // 内存筛选
        QueryWrapper<User> wrapper = new QueryWrapper<>();
        wrapper.like("tags", tags.get(0));
        List<User> userList = this.list(wrapper);
        return userList.stream().filter(user -> {
            String tagsJson = user.getTags();
            Set<String> tagSet = gson.fromJson(tagsJson, new TypeToken<Set<String>>() {
            }.getType());
            tagSet = Optional.ofNullable(tagSet).orElse(new HashSet<>());
            for (String tag : tags) {
                if (tagSet.contains(tag)) {
                    return true;
                }
            }
            return false;
        }).map(this::getSafeUser).collect(Collectors.toList());
    }

    /**
     * 使用SQL查询满足所有标签的用户
     *
     * @param tags 标签列表
     * @return 满足所有标签的脱敏用户列表
     */
    private List<User> searchUsersByTagsBySQL(List<String> tags) {
        // SQL查询
        QueryWrapper<User> wrapper = new QueryWrapper<>();
        for (String tag : tags) {
            wrapper.like("tags", tag);
        }
        List<User> userList = this.list(wrapper);
        return userList.stream()
                .map(this::getSafeUser)
                .collect(Collectors.toList());
    }

}




