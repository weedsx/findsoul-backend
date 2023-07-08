package com.weeds.findsoul.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.weeds.findsoul.common.ErrorCode;
import com.weeds.findsoul.exception.BusinessException;
import com.weeds.findsoul.mapper.TeamMapper;
import com.weeds.findsoul.model.bo.UserBo;
import com.weeds.findsoul.model.entity.Team;
import com.weeds.findsoul.model.entity.User;
import com.weeds.findsoul.model.entity.UserTeam;
import com.weeds.findsoul.model.enums.TeamStatusEum;
import com.weeds.findsoul.model.query.JoinTeamQuery;
import com.weeds.findsoul.model.query.QuitTeamQuery;
import com.weeds.findsoul.model.query.TeamQuery;
import com.weeds.findsoul.model.query.TeamUpdateQuery;
import com.weeds.findsoul.model.vo.TeamUserVO;
import com.weeds.findsoul.service.TeamService;
import com.weeds.findsoul.service.UserService;
import com.weeds.findsoul.service.UserTeamService;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.weeds.findsoul.constant.RedisKeyConstant.JOIN_TEAM_LOCK;
import static com.weeds.findsoul.constant.UserConstant.ADMIN_ROLE;

/**
 * @author weeds
 * @description 针对表【team(队伍)】的数据库操作Service实现
 * @createDate 2023-06-13 13:56:11
 */
@Service
public class TeamServiceImpl extends ServiceImpl<TeamMapper, Team>
        implements TeamService {
    @Resource
    private UserService userService;

    @Resource
    private UserTeamService userTeamService;

    @Resource
    private RedissonClient redissonClient;


    @Override
    @Transactional(rollbackFor = RuntimeException.class)
    public long addTeam(Team team, HttpServletRequest request) {
        // 1. 请求参数是否为空？
        // 2. 是否登录，未登录不允许创建
        User loginUser = userService.getLoginUser(request);
        Optional.ofNullable(loginUser).orElseThrow(() -> new BusinessException(ErrorCode.NOT_LOGIN));
        // 3. 校验信息
        //    a. 队伍人数 > 1 且 <= 20
        int teamNum = Optional.ofNullable(team.getMaxNum()).orElse(0);
        if (teamNum < 1 || teamNum > 20) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "队伍人数不满足要求");
        }
        //    b. 队伍标题 <= 20
        String teamName = team.getName();
        if (StringUtils.isBlank(teamName) || teamName.length() > 20) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "队伍标题过长");
        }
        //    c. 描述 <= 512
        String description = team.getDescription();
        if (StringUtils.isBlank(description) || description.length() > 512) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "队伍描述信息过长");
        }
        //    d. status 是否公开（int）不传默认为 0（公开）
        team.setStatus(Optional.ofNullable(team.getStatus()).orElse(0));
        //    e. 如果 status 是加密状态，一定要有密码，且密码 <= 32
        TeamStatusEum teamStatusEum = TeamStatusEum.getEumByValue(team.getStatus());
        if (TeamStatusEum.SECRET.equals(teamStatusEum)) {
            String password = team.getPassword();
            if (StringUtils.isBlank(password) || password.length() > 32) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "队伍密码过长");
            }
        }
        //    f. 超时时间 > 当前时间
        if (new Date().after(team.getExpireTime())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "队伍超时异常");
        }
        //    g. 校验用户最多创建 5 个队伍
        Long creatorId = loginUser.getId();
        QueryWrapper<Team> wrapper = new QueryWrapper<>();
        wrapper.eq("creator_id", creatorId);
        long count = this.count(wrapper);
        if (count > 5) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "最多创建5个队伍");
        }
        //4. 插入队伍信息到队伍表
        team.setCreatorId(creatorId);
        boolean save = this.save(team);
        if (!save) {
            throw new BusinessException(ErrorCode.DB_ERROR, "创建队伍失败");
        }
        //5. 插入用户  => 队伍关系到关系表
        UserTeam userTeam = new UserTeam() {{
            setTeamId(team.getId());
            setUserId(creatorId);
            setJoinTime(new Date());
        }};
        boolean userTeamSave = userTeamService.save(userTeam);
        if (!userTeamSave) {
            throw new BusinessException(ErrorCode.DB_ERROR, "创建队伍失败");
        }
        return team.getId();
    }

    @Override
    public List<TeamUserVO> teamList(TeamQuery teamQuery, boolean isAdmin) {
        Optional.ofNullable(teamQuery).orElseThrow(() -> new BusinessException(ErrorCode.PARAMS_ERROR));
        QueryWrapper<Team> teamQW = new QueryWrapper<>();
        Long id = teamQuery.getId();
        teamQW.eq(id != null && id > 0,
                "id", id);
        List<Long> idList = teamQuery.getIdList();
        teamQW.in(CollectionUtils.isNotEmpty(idList),
                "id", idList);
        String searchText = teamQuery.getSearchText();
        teamQW.and(StringUtils.isNotBlank(searchText),
                qw -> qw.like("name", searchText).or().like("description", searchText));
        teamQW.like(StringUtils.isNotBlank(teamQuery.getName()),
                "name", teamQuery.getName());
        teamQW.like(StringUtils.isNotBlank(teamQuery.getDescription()),
                "description", teamQuery.getDescription());
        Integer maxNum = teamQuery.getMaxNum();
        teamQW.eq(maxNum != null && maxNum > 0,
                "max_num", maxNum);
        Long creatorId = teamQuery.getCreatorId();
        teamQW.eq(creatorId != null && creatorId > 0,
                "creator_id", creatorId);
        // 根据状态查询
        Integer status = teamQuery.getStatus();
        // 只有管理员才能查看私有队伍
        if (!isAdmin && status != null &&
                TeamStatusEum.PRIVATE.getStatus() == status) {
            throw new BusinessException(ErrorCode.NO_ACCESS);
        }
        // 状态为空就展示公开的和加密的
        if (status == null) {
            TeamStatusEum[] teamStatusEumArr = TeamStatusEum.values();
            List<Integer> statusList = Arrays.stream(teamStatusEumArr)
                    .filter(teamStatusEum -> !TeamStatusEum.PRIVATE.equals(teamStatusEum))
                    .map(TeamStatusEum::getStatus)
                    .collect(Collectors.toList());
            teamQW.in("status", statusList);
        } else {
            teamQW.eq(status != 1, "status", status);
        }

        // 不展示过期的队伍
        // and (expire_time > now() or expire_time == null)
        teamQW.and(qw -> qw.gt("expire_time", new Date()).or().isNull("expire_time"));
        List<Team> teamList = this.list(teamQW);
        if (CollectionUtils.isEmpty(teamList)) {
            throw new BusinessException(ErrorCode.NULL_ERROR, "无符合条件的队伍");
        }
        // 将数据库的队伍列表转为TeamUserVO，注意创建人脱敏
        List<TeamUserVO> teamUserVOList = new ArrayList<>();
        for (Team team : teamList) {
            Long teamCreatorId = team.getCreatorId();
            if (teamCreatorId == null) {
                continue;
            }
            User creator = userService.getById(teamCreatorId);
            TeamUserVO teamUserVO = new TeamUserVO();
            BeanUtils.copyProperties(team, teamUserVO);
            Optional.ofNullable(creator).ifPresent(user -> {
                UserBo safeUserDto = userService.getSafeUserBo(user);
                teamUserVO.setCreator(safeUserDto);
            });
            teamUserVOList.add(teamUserVO);
        }
        return teamUserVOList;
    }

    @Override
    public TeamUserVO getTeamVo(Team team) {
        Long teamCreatorId = team.getCreatorId();
        User user = userService.getById(teamCreatorId);
        UserBo safeUserBo = userService.getSafeUserBo(user);
        TeamUserVO teamUserVO = new TeamUserVO();
        BeanUtils.copyProperties(team, teamUserVO);
        teamUserVO.setCreator(safeUserBo);
        // hasJoined 肯定为 true
        teamUserVO.setHasJoined(true);
        return teamUserVO;
    }

    @Override
    public List<TeamUserVO> setHasJoinedNum(List<TeamUserVO> teamUserVOList) {
        // 3、设置每个队伍的当前人数
        List<Long> teamIdList = teamUserVOList.stream()
                .map(TeamUserVO::getId)
                .collect(Collectors.toList());
        LambdaQueryWrapper<UserTeam> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(UserTeam::getTeamId, teamIdList);
        List<UserTeam> userTeamList = userTeamService.list(wrapper);
        Map<Long, List<UserTeam>> teamIdUserTeamMap = userTeamList.stream()
                .collect(Collectors.groupingBy(UserTeam::getTeamId));
        for (TeamUserVO teamUserVO : teamUserVOList) {
            teamUserVO.setHasJoinedNum((long) teamIdUserTeamMap.get(teamUserVO.getId()).size());
        }
        return teamUserVOList;
    }

    @Override
    public boolean updateTeam(TeamUpdateQuery teamUpdateQuery, User loginUser) {
        Optional.ofNullable(teamUpdateQuery).orElseThrow(() -> new BusinessException(ErrorCode.PARAMS_ERROR));
        Long id = teamUpdateQuery.getId();
        Team oldTeam = getTeamByTeamId(id);
        // 只有管理员或者队伍的创建者可以修改
        if (loginUser.getUserRole() != ADMIN_ROLE &&
                !Objects.equals(oldTeam.getCreatorId(), id)) {
            throw new BusinessException(ErrorCode.NO_ACCESS);
        }
        // 如果队伍状态改为加密，必须要有密码
        TeamStatusEum teamStatusEum = TeamStatusEum.getEumByValue(teamUpdateQuery.getStatus());
        if (TeamStatusEum.SECRET.equals(teamStatusEum) &&
                StringUtils.isNotBlank(teamUpdateQuery.getPassword())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请设置密码");
        }
        // 名称和描述有就更新而且不能为空字符串
        String name = teamUpdateQuery.getName();
        if (name != null && StringUtils.isBlank(name)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "队伍名称请勿为空");
        }
        String description = teamUpdateQuery.getDescription();
        if (description != null && StringUtils.isBlank(description)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "队伍描述请勿为空");
        }
        Date expireTime = teamUpdateQuery.getExpireTime();
        if (expireTime != null && expireTime.after(new Date())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "过期时间不能大于当前时间");
        }

        Team updateTeam = new Team();
        BeanUtils.copyProperties(teamUpdateQuery, updateTeam);
        return this.updateById(updateTeam);
    }

    @Override
    public boolean joinTeam(JoinTeamQuery joinTeamQuery, User loginUser) {
        Long userId = loginUser.getId();
        Long teamId = joinTeamQuery.getTeamId();

        // 队伍必须存在
        Team team = getTeamByTeamId(teamId);
        // 不能加入自己的队伍（可以加入，万一是退出了再加，但不能重复加入）
//        if (Objects.equals(team.getCreatorId(), userId)) {
//            throw new BusinessException(ErrorCode.PARAMS_ERROR, "不能加入自己创建的队伍");
//        }
        // 只能加入未过期的队伍
        Date expireTime = team.getExpireTime();
        if (expireTime != null && new Date().after(expireTime)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "队伍已过期");
        }
        // 禁止加入私有的队伍
        if (TeamStatusEum.PRIVATE.equals(TeamStatusEum.getEumByValue(team.getStatus()))) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "不能加入私有的队伍");
        }
        // 如果加入的队伍是加密的，必须密码匹配才可以
        if (TeamStatusEum.SECRET.equals(TeamStatusEum.getEumByValue(team.getStatus()))) {
            String password = joinTeamQuery.getPassword();
            if (StringUtils.isBlank(password)) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "请设置密码");
            }
            if (!Objects.equals(password, team.getPassword())) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "密码错误");
            }
        }

        // 要查数据库的校验尽量放在后面
        // 分布式锁
        RLock lock = redissonClient.getLock(JOIN_TEAM_LOCK);
        try {
            while (true) {
                if (lock.tryLock(0, TimeUnit.MILLISECONDS)) {
                    // 用户最多加入 5 个队伍
                    QueryWrapper<UserTeam> wrapper = new QueryWrapper<>();
                    wrapper.eq("user_id", userId);
                    long count = userTeamService.count(wrapper);
                    if (count > 5) {
                        throw new BusinessException(ErrorCode.PARAMS_ERROR, "最多加入5个队伍");
                    }
                    // 不能重复加入已加入的队伍
                    wrapper.eq("team_id", teamId);
                    long repeatCount = userTeamService.count(wrapper);
                    if (repeatCount != 0) {
                        throw new BusinessException(ErrorCode.PARAMS_ERROR, "不能重复加入已加入的队伍");
                    }
                    // 只能加入未满的队伍
                    wrapper = new QueryWrapper<>();
                    wrapper.eq("team_id", teamId);
                    long teamSize = userTeamService.count(wrapper);
                    if (teamSize > team.getMaxNum()) {
                        throw new BusinessException(ErrorCode.PARAMS_ERROR, "队伍已满");
                    }
                    // 新增队伍 - 用户关联信息
                    UserTeam userTeam = new UserTeam() {{
                        setUserId(userId);
                        setTeamId(teamId);
                        setJoinTime(new Date());
                    }};
                    return userTeamService.save(userTeam);
                }
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            // 只能释放自己的锁
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean quitTeam(QuitTeamQuery quitTeamQuery, User loginUser) {
        Long teamId = quitTeamQuery.getTeamId();
        Long userId = loginUser.getId();
        Team team = getTeamByTeamId(teamId);
        // 校验是否已加入队伍
        LambdaQueryWrapper<UserTeam> lambdaQW = new QueryWrapper<UserTeam>().lambda();
        lambdaQW.eq(UserTeam::getTeamId, teamId)
                .eq(UserTeam::getUserId, userId);
        long alreadyJoined = userTeamService.count(lambdaQW);
        if (alreadyJoined == 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "不能退出已退出的队伍");
        }
        // 队伍人数
        lambdaQW = new LambdaQueryWrapper<>();
        lambdaQW.eq(UserTeam::getTeamId, teamId);
        long teamLeftCount = userTeamService.count(lambdaQW);
        // 专门删除UserTeam的条件，以便下面复用
        LambdaQueryWrapper<UserTeam> removeUserTeam = new LambdaQueryWrapper<>();
        removeUserTeam.eq(UserTeam::getTeamId, teamId)
                .eq(UserTeam::getUserId, userId);
        // 只剩一人，队伍解散
        if (teamLeftCount == 1) {
            boolean teamRemoved = this.removeById(teamId);
            if (!teamRemoved) {
                throw new BusinessException(ErrorCode.DB_ERROR, "退出失败");
            }
            return userTeamService.remove(removeUserTeam);
        } else {
            // 还有其他人，如果退出的是队长
            if (Objects.equals(userId, team.getCreatorId())) {
                // 权限转移给第二早加入的用户 - 先来后到
                lambdaQW = new LambdaQueryWrapper<>();
                lambdaQW.eq(UserTeam::getTeamId, teamId)
                        .orderByAsc(UserTeam::getJoinTime)
                        // 只取前两条
                        .last("limit 2");
                List<UserTeam> userTeamList = userTeamService.list(lambdaQW);
                if (CollectionUtils.isEmpty(userTeamList) || userTeamList.size() < 2) {
                    throw new BusinessException(ErrorCode.NULL_ERROR);
                }
                UserTeam userTeam = userTeamList.get(1);
                Long newLeaderId = userTeam.getUserId();
                Team newTeam = new Team() {{
                    setId(teamId);
                    setCreatorId(newLeaderId);
                }};
                boolean update = this.updateById(newTeam);
                if (!update) {
                    throw new BusinessException(ErrorCode.DB_ERROR, "退出失败");
                }
                return userTeamService.remove(removeUserTeam);
            } else {
                // 退出的是普通队员（非队长）
                boolean userTeamRemoved = userTeamService.remove(removeUserTeam);
                if (userTeamRemoved) {
                    return true;
                } else {
                    throw new BusinessException(ErrorCode.DB_ERROR, "退出失败");
                }
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean removeTeam(long teamId, User loginUser) {
        // 1. 校验请求参数
        // 2. 校验队伍是否存在
        Team team = getTeamByTeamId(teamId);
        Long tId = team.getId();
        // 3. 校验你是不是队伍的队长
        if (!Objects.equals(team.getCreatorId(), loginUser.getId())) {
            throw new BusinessException(ErrorCode.NO_ACCESS);
        }
        // 4. 移除所有加入队伍的关联信息
        LambdaQueryWrapper<UserTeam> lambdaQW = new QueryWrapper<UserTeam>().lambda();
        lambdaQW.eq(UserTeam::getTeamId, tId);
        boolean userTeamRemoved = userTeamService.remove(lambdaQW);
        if (!userTeamRemoved) {
            throw new BusinessException(ErrorCode.DB_ERROR);
        }
        // 5. 删除队伍
        return this.removeById(tId);
    }

    @Override
    public List<TeamUserVO> setHasJoined(List<TeamUserVO> teamUserVOList, User loginUser) {
        if (loginUser == null) {
            return teamUserVOList;
        }
        List<Long> teamIdList = teamUserVOList.stream()
                .map(TeamUserVO::getId)
                .collect(Collectors.toList());
        LambdaQueryWrapper<UserTeam> lambdaQW = new LambdaQueryWrapper<>();
        lambdaQW.eq(UserTeam::getUserId, loginUser.getId())
                .in(UserTeam::getTeamId, teamIdList);
        List<UserTeam> userTeamList = userTeamService.list(lambdaQW);
        Set<Long> teamIdSet = userTeamList.stream()
                .map(UserTeam::getTeamId)
                .collect(Collectors.toSet());
        for (TeamUserVO teamUserVO : teamUserVOList) {
            teamUserVO.setHasJoined(teamIdSet.contains(teamUserVO.getId()));
        }
        return teamUserVOList;
    }

    private Team getTeamByTeamId(Long teamId) {
        if (teamId == null || teamId < 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Team team = this.getById(teamId);
        Optional.ofNullable(team).orElseThrow(() -> new BusinessException(ErrorCode.NULL_ERROR));
        return team;
    }
}