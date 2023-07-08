package com.weeds.findsoul.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.weeds.findsoul.model.entity.User;
import org.apache.ibatis.annotations.Mapper;

/**
* @author weeds
* @description 针对表【user(用户)】的数据库操作Mapper
* @Entity com.weeds.findsoul.model.entity.User
*/
@Mapper
public interface UserMapper extends BaseMapper<User> {

}




