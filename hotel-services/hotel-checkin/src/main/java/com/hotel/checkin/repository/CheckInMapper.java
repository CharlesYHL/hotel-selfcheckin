package com.hotel.checkin.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hotel.checkin.model.entity.CheckIn;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface CheckInMapper extends BaseMapper<CheckIn> {

    @Select("SELECT * FROM chk_checkin WHERE order_id = #{orderId}")
    CheckIn selectByOrderId(@Param("orderId") String orderId);

    @Select("SELECT * FROM chk_checkin WHERE hotel_id = #{hotelId} AND status IN (1, 2) ORDER BY check_in_time DESC")
    List<CheckIn> selectActiveByHotel(@Param("hotelId") String hotelId);

    @Select("SELECT * FROM chk_checkin WHERE member_id = #{memberId} ORDER BY created_time DESC")
    List<CheckIn> selectByMemberId(@Param("memberId") String memberId);

    @Select("SELECT * FROM chk_checkin WHERE room_id = #{roomId} AND status = 1 LIMIT 1")
    CheckIn selectActiveByRoom(@Param("roomId") String roomId);

    @Update("UPDATE chk_checkin SET status = #{toStatus}, updated_time = NOW(), version = version + 1 "
            + "WHERE checkin_id = #{checkinId} AND status = #{fromStatus}")
    int updateStatus(@Param("checkinId") String checkinId, @Param("toStatus") Integer toStatus,
                     @Param("fromStatus") Integer fromStatus);

    @Update("UPDATE chk_checkin SET verify_status = #{verifyStatus}, verify_remark = #{remark}, updated_time = NOW() "
            + "WHERE checkin_id = #{checkinId}")
    int updateVerifyStatus(@Param("checkinId") String checkinId,
                           @Param("verifyStatus") Integer verifyStatus,
                           @Param("remark") String remark);
}
