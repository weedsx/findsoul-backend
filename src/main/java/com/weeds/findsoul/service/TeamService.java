package com.weeds.findsoul.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.weeds.findsoul.model.entity.Team;
import com.weeds.findsoul.model.entity.User;
import com.weeds.findsoul.model.query.JoinTeamQuery;
import com.weeds.findsoul.model.query.QuitTeamQuery;
import com.weeds.findsoul.model.query.TeamQuery;
import com.weeds.findsoul.model.query.TeamUpdateQuery;
import com.weeds.findsoul.model.vo.TeamUserVO;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * @author weeds
 */
public interface TeamService extends IService<Team> {
    /**
     * 创建队伍
     *
     * @param team    队伍
     * @param request 请求
     * @return 队伍id
     */
    long addTeam(Team team, HttpServletRequest request);

    /**
     * 根据条件查询队伍
     *
     * @param teamQuery 条件
     * @param isAdmin   是否为管理员
     * @return 队伍列表
     */
    List<TeamUserVO> teamList(TeamQuery teamQuery, boolean isAdmin);

    /**
     * 队伍更新
     *
     * @param teamUpdateQuery 更新条件
     * @param loginUser       用户
     * @return boolean
     */
    boolean updateTeam(TeamUpdateQuery teamUpdateQuery, User loginUser);

    /**
     * 加入队伍
     *
     * @param joinTeamQuery 参数
     * @param loginUser     当前用户
     * @return boolean
     */
    boolean joinTeam(JoinTeamQuery joinTeamQuery, User loginUser);

    /**
     * 退出队伍
     *
     * @param quitTeamQuery 条件
     * @param loginUser     当前用户
     * @return boolean
     */
    boolean quitTeam(QuitTeamQuery quitTeamQuery, User loginUser);

    /**
     * 队长解散队伍
     *
     * @param teamId teamId
     * @param loginUser loginUser
     * @return boolean
     */
    boolean removeTeam(long teamId, User loginUser);

    /**
     * 设置当前用户是否已经加入这些队伍
     * @param teamUserVOList 队伍列表
     * @param loginUser 当前用户
     * @return 队伍列表
     */
    List<TeamUserVO> setHasJoined(List<TeamUserVO> teamUserVOList, User loginUser);

    /**
     * getTeamById
     * @param team team
     * @return TeamUserVO
     */
    TeamUserVO getTeamVo(Team team);

    /**
     * 设置队伍的当前人数
     * @param teamUserVOList teamUserVOList
     * @return List<TeamUserVO>
     */
    List<TeamUserVO> setHasJoinedNum(List<TeamUserVO> teamUserVOList);
}