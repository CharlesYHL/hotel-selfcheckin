package com.hotel.checkin.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hotel.checkin.model.entity.Extension;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ExtensionMapper extends BaseMapper<Extension> {

    @Select("SELECT * FROM chk_extension WHERE checkin_id = #{checkinId} ORDER BY created_time DESC")
    List<Extension> selectByCheckinId(@Param("checkinId") String checkinId);
}
