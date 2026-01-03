package com.dms.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.dms.entity.Friend;
import org.apache.ibatis.annotations.Mapper;

/**
 * 好友Mapper
 */
@Mapper
public interface FriendMapper extends BaseMapper<Friend> {
}

