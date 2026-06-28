package com.hotel.room.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hotel.room.model.entity.RoomPrice;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.util.List;

@Mapper
public interface RoomPriceMapper extends BaseMapper<RoomPrice> {

    @Select("SELECT * FROM rom_price WHERE hotel_id = #{hotelId} AND room_type_id = #{roomTypeId} AND start_date <= #{date} AND end_date >= #{date} AND status = 1 ORDER BY date_type, price_date")
    List<RoomPrice> selectByDate(@Param("hotelId") String hotelId, @Param("roomTypeId") String roomTypeId, @Param("date") LocalDate date);
}
