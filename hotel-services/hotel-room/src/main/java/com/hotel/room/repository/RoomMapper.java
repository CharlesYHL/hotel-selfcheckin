package com.hotel.room.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hotel.room.model.entity.Room;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface RoomMapper extends BaseMapper<Room> {

    @Select("SELECT * FROM rom_room WHERE hotel_id = #{hotelId} AND room_type_id = #{roomTypeId} AND room_status = 1 AND is_active = 1 ORDER BY sort_order, room_no")
    List<Room> selectAvailable(@Param("hotelId") String hotelId, @Param("roomTypeId") String roomTypeId);

    @Select("SELECT COUNT(*) FROM rom_room WHERE hotel_id = #{hotelId} AND room_type_id = #{roomTypeId} AND room_status = 1 AND is_active = 1")
    int countAvailable(@Param("hotelId") String hotelId, @Param("roomTypeId") String roomTypeId);

    @Update("UPDATE rom_room SET room_status = #{status}, updated_time = NOW() WHERE room_id = #{roomId} AND room_status = #{expectedStatus}")
    int updateStatusWithCheck(@Param("roomId") String roomId, @Param("status") int status, @Param("expectedStatus") int expectedStatus);

    @Select("SELECT * FROM rom_room WHERE hotel_id = #{hotelId} AND room_status = #{status} AND is_active = 1")
    List<Room> selectByStatus(@Param("hotelId") String hotelId, @Param("status") int status);

    @Select("SELECT room_type_id, COUNT(*) as cnt FROM rom_room WHERE hotel_id = #{hotelId} AND room_status = 1 AND is_active = 1 GROUP BY room_type_id")
    List<java.util.Map<String, Object>> countAvailableGroupByType(@Param("hotelId") String hotelId);
}
