package com.hotel.card.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hotel.card.model.entity.CardLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface CardLogMapper extends BaseMapper<CardLog> {

    @Select("SELECT * FROM crd_card_log WHERE card_id = #{cardId} ORDER BY created_time DESC")
    List<CardLog> selectByCardId(@Param("cardId") String cardId);

    @Select("SELECT * FROM crd_card_log WHERE room_id = #{roomId} AND action_type = 2 ORDER BY created_time DESC LIMIT 50")
    List<CardLog> selectOpenLogsByRoom(@Param("roomId") String roomId);
}
