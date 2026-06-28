package com.hotel.member.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hotel.member.model.entity.PointsLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface PointsLogMapper extends BaseMapper<PointsLog> {

    @Select("SELECT * FROM mem_points_log WHERE member_id = #{memberId} ORDER BY created_time DESC LIMIT #{limit}")
    List<PointsLog> selectByMemberId(@Param("memberId") String memberId, @Param("limit") int limit);

    @Select("SELECT * FROM mem_points_log WHERE member_id = #{memberId} AND expire_date <= CURDATE() "
            + "AND points > 0 ORDER BY created_time")
    List<PointsLog> selectExpiringPoints(@Param("memberId") String memberId);

    @Select("SELECT COALESCE(SUM(points), 0) FROM mem_points_log WHERE member_id = #{memberId} "
            + "AND business_type = #{businessType} AND created_time >= #{since}")
    Long sumPointsByType(@Param("memberId") String memberId, @Param("businessType") Integer businessType,
                         @Param("since") java.time.LocalDateTime since);
}
