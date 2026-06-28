package com.hotel.room.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hotel.room.model.entity.RoomType;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface RoomTypeMapper extends BaseMapper<RoomType> {

    @Select("SELECT * FROM sys_room_type WHERE hotel_id = #{hotelId} AND status = 1 ORDER BY sort_order")
    List<RoomType> selectByHotelId(String hotelId);

    @Select("SELECT * FROM sys_room_type WHERE hotel_id = #{hotelId} AND room_type_code = #{typeCode} AND status = 1")
    RoomType selectByHotelAndCode(String hotelId, String typeCode);
}
