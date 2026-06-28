package com.hotel.checkin.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hotel.checkin.model.entity.Guest;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface GuestMapper extends BaseMapper<Guest> {

    @Select("SELECT * FROM chk_guest WHERE checkin_id = #{checkinId}")
    List<Guest> selectByCheckinId(@Param("checkinId") String checkinId);

    @Select("SELECT * FROM chk_guest WHERE id_card_hash = #{hash}")
    Guest selectByIdCardHash(@Param("hash") String hash);
}
