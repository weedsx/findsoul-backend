package com.weeds.findsoul.service.impl;


import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.weeds.findsoul.mapper.TagMapper;
import com.weeds.findsoul.model.entity.Tag;
import com.weeds.findsoul.service.TagService;
import org.springframework.stereotype.Service;

/**
 * @author weeds
 * @description 针对表【tag(标签)】的数据库操作Service实现
 * @createDate 2023-06-01 18:21:47
 */
@Service
public class TagServiceImpl extends ServiceImpl<TagMapper, Tag>
        implements TagService {


}




