package com.hotel.order.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hotel.order.model.entity.Order;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface OrderMapper extends BaseMapper<Order> {
}
