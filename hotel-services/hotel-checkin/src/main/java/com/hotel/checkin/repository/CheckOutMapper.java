package com.hotel.checkin.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hotel.checkin.model.entity.CheckOut;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface CheckOutMapper extends BaseMapper<CheckOut> {

    @Select("SELECT * FROM chk_checkout WHERE checkin_id = #{checkinId}")
    CheckOut selectByCheckinId(@Param("checkinId") String checkinId);
}
