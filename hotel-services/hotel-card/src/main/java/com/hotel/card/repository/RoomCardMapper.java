package com.hotel.card.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hotel.card.model.entity.RoomCard;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface RoomCardMapper extends BaseMapper<RoomCard> {

    @Select("SELECT * FROM crd_card WHERE checkin_id = #{checkinId} AND card_status != 4 ORDER BY created_time DESC")
    List<RoomCard> selectByCheckinId(@Param("checkinId") String checkinId);

    @Select("SELECT * FROM crd_card WHERE room_id = #{roomId} AND card_status = 1 LIMIT 1")
    RoomCard selectActiveByRoom(@Param("roomId") String roomId);

    @Select("SELECT * FROM crd_card WHERE card_no = #{cardNo}")
    RoomCard selectByCardNo(@Param("cardNo") String cardNo);

    @Update("UPDATE crd_card SET card_status = #{toStatus}, updated_time = NOW(), version = version + 1 "
            + "WHERE card_id = #{cardId} AND card_status = #{fromStatus}")
    int updateStatus(@Param("cardId") String cardId, @Param("toStatus") Integer toStatus,
                     @Param("fromStatus") Integer fromStatus);

    @Update("UPDATE crd_card SET open_count = open_count + 1, last_open_time = NOW(), updated_time = NOW() "
            + "WHERE card_id = #{cardId}")
    int incrementOpenCount(@Param("cardId") String cardId);

    @Update("UPDATE crd_card SET valid_to = #{validTo}, card_type = 3, updated_time = NOW() "
            + "WHERE card_id = #{cardId}")
    int extendValidity(@Param("cardId") String cardId, @Param("validTo") java.time.LocalDateTime validTo);
}
