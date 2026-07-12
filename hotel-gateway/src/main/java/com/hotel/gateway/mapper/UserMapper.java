package com.hotel.gateway.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hotel.gateway.model.entity.User;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMapper extends BaseMapper<User> {
}