package com.weeds.findsoul.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.weeds.findsoul.common.BaseResponse;
import com.weeds.findsoul.common.ErrorCode;
import com.weeds.findsoul.common.ResponseUtil;
import com.weeds.findsoul.exception.BusinessException;
import com.weeds.findsoul.model.entity.Team;
import com.weeds.findsoul.model.entity.User;
import com.weeds.findsoul.model.entity.UserTeam;
import com.weeds.findsoul.model.query.*;
import com.weeds.findsoul.model.vo.TeamUserVO;
import com.weeds.findsoul.service.TeamService;
import com.weeds.findsoul.service.UserService;
import com.weeds.findsoul.service.UserTeamService;
import org.springframework.beans.BeanUtils;
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
@RequestMapping("/team")
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:5173"})
public class TeamController {
    @Resource
    private TeamService teamService;

    @Resource
    private UserService userService;

    @Resource
    private UserTeamService userTeamService;

    @PostMapping("/add")
    public BaseResponse<Long> addTeam(@RequestBody AddTeamQuery addTeamQuery, HttpServletRequest request) {
        Optional.ofNullable(addTeamQuery).orElseThrow(() -> new BusinessException(ErrorCode.PARAMS_ERROR, "team is null"));
        Team team = new Team();
        BeanUtils.copyProperties(addTeamQuery, team);
        long teamId = teamService.addTeam(team, request);
        return ResponseUtil.success(teamId);
    }

    @PostMapping("/delete")
    public BaseResponse<Boolean> deleteTeam(
            @RequestBody long teamId,
            HttpServletRequest request) {
        if (teamId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "teamId is illegal");
        }
        User loginUser = userService.getLoginUser(request);
        boolean remove = teamService.removeTeam(teamId, loginUser);
        if (!remove) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "team save failed");
        }
        return ResponseUtil.success(true);
    }

    @PostMapping("/update")
    public BaseResponse<Boolean> updateTeam(
            @RequestBody TeamUpdateQuery teamUpdateQuery,
            HttpServletRequest request) {
        Optional.ofNullable(teamUpdateQuery).orElseThrow(() -> new BusinessException(ErrorCode.PARAMS_ERROR, "team is null"));
        // getLoginUser 已经判空
        User loginUser = userService.getLoginUser(request);
        boolean update = teamService.updateTeam(teamUpdateQuery, loginUser);
        if (!update) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "team save failed");
        }
        return ResponseUtil.success(true);
    }

    @PostMapping("/list")
    public BaseResponse<List<TeamUserVO>> getTeamList(
            TeamQuery teamQuery,
            HttpServletRequest request) {
        Optional.ofNullable(teamQuery).orElseThrow(() -> new BusinessException(ErrorCode.PARAMS_ERROR));
        boolean isAdmin = userService.isAdmin(request);
        // 1、根据条件查出用户
        List<TeamUserVO> teamUserVOList = teamService.teamList(teamQuery, isAdmin);
        List<TeamUserVO> result;
        try { // 让未登录的用户也可以访问
            User loginUser = userService.getLoginUser(request);
            // 2、设置当前用户是否已经加入这些队伍
            result = teamService.setHasJoined(teamUserVOList, loginUser);
            // 3、设置每个队伍的当前人数
            result = teamService.setHasJoinedNum(result);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return ResponseUtil.success(result);
    }

    @PostMapping("/page")
    public BaseResponse<Page<Team>> getTeamPage(@RequestBody TeamQuery teamQuery) {
        Optional.ofNullable(teamQuery).orElseThrow(() -> new BusinessException(ErrorCode.PARAMS_ERROR, "term is null"));
        Team team = new Team();
        BeanUtils.copyProperties(teamQuery, team);
        QueryWrapper<Team> wrapper = new QueryWrapper<>(team);
        Page<Team> teamPage = new Page<>(teamQuery.getCurrentPage(), teamQuery.getPageSize());
        Page<Team> page = teamService.page(teamPage, wrapper);
        Optional.ofNullable(page.getRecords()).orElseThrow(() -> new BusinessException(ErrorCode.NULL_ERROR));
        return ResponseUtil.success(page);
    }

    @PostMapping("/join")
    public BaseResponse<Boolean> joinTeam(
            @RequestBody JoinTeamQuery joinTeamQuery,
            HttpServletRequest request) {
        Optional.ofNullable(joinTeamQuery).orElseThrow(() -> new BusinessException(ErrorCode.PARAMS_ERROR));
        User loginUser = userService.getLoginUser(request);
        boolean result = teamService.joinTeam(joinTeamQuery, loginUser);
        return ResponseUtil.success(result);
    }

    @PostMapping("/quit")
    public BaseResponse<Boolean> quitTeam(
            @RequestBody QuitTeamQuery quitTeamQuery,
            HttpServletRequest request) {
        Optional.ofNullable(quitTeamQuery).orElseThrow(() -> new BusinessException(ErrorCode.PARAMS_ERROR));
        User loginUser = userService.getLoginUser(request);
        boolean result = teamService.quitTeam(quitTeamQuery, loginUser);
        return ResponseUtil.success(result);
    }

    /**
     * 查询用户创建的队伍
     *
     * @param request 请求
     * @return BaseResponse<List < TeamUserVO>>
     */
    @PostMapping("/list/my/created")
    public BaseResponse<List<TeamUserVO>> getMyCreatedTeamList(HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        // 自己 new TeamQuery 防止被外部利用
        TeamQuery teamQuery = new TeamQuery() {{
            setCreatorId(loginUser.getId());
        }};
        List<TeamUserVO> teamUserVOList = teamService.teamList(teamQuery, true);
        List<TeamUserVO> result = teamService.setHasJoined(teamUserVOList, loginUser);
        result = teamService.setHasJoinedNum(result);
        return ResponseUtil.success(result);
    }

    /**
     * 查询用户加入的队伍
     *
     * @param request 请求
     * @return BaseResponse<List < TeamUserVO>>
     */
    @PostMapping("/list/my/joined")
    public BaseResponse<List<TeamUserVO>> getMyJoinedTeamList(HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        LambdaQueryWrapper<UserTeam> lambdaQW = new QueryWrapper<UserTeam>().lambda();
        lambdaQW.eq(UserTeam::getUserId, loginUser.getId());
        List<UserTeam> list = userTeamService.list(lambdaQW);
        List<Long> teamIdList = list.stream()
                .map(UserTeam::getTeamId)
                .distinct() // 筛选，通过流所生成元素的hashCode() 和equals() 去除重复元素
                .collect(Collectors.toList());
        // 自己 new TeamQuery 防止被外部利用
        TeamQuery teamQuery = new TeamQuery() {{
            setIdList(teamIdList);
        }};
        List<TeamUserVO> teamUserVOList = teamService.teamList(teamQuery, true);
        List<TeamUserVO> result = teamService.setHasJoined(teamUserVOList, loginUser);
        result = teamService.setHasJoinedNum(result);
        return ResponseUtil.success(result);
    }

    @GetMapping("/get")
    public BaseResponse<TeamUserVO> getTeamById(Long id) {
        Optional.ofNullable(id).orElseThrow(() -> new BusinessException(ErrorCode.PARAMS_ERROR));
        Team team = teamService.getById(id);
        Optional.ofNullable(team).orElseThrow(() -> new BusinessException(ErrorCode.NULL_ERROR));
        TeamUserVO teamUserVO = teamService.getTeamVo(team);
        return ResponseUtil.success(teamUserVO);
    }

}