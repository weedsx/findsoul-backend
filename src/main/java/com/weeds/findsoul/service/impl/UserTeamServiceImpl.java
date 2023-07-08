package com.weeds.findsoul.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.weeds.findsoul.mapper.UserTeamMapper;
import com.weeds.findsoul.model.entity.UserTeam;
import com.weeds.findsoul.service.UserTeamService;
import org.springframework.stereotype.Service;

/**
 * @author weeds
 * @description 针对表【user_team(用户队伍关系)】的数据库操作Service实现
 * @createDate 2023-06-13 13:59:30
 */
@Service
public class UserTeamServiceImpl extends ServiceImpl<UserTeamMapper, UserTeam>
        implements UserTeamService {

}