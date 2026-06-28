package com.hotel.member.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hotel.member.model.entity.MemberLevelConfig;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface MemberLevelMapper extends BaseMapper<MemberLevelConfig> {

    @Select("SELECT * FROM mem_level WHERE level_type = #{levelType} AND status = 1")
    MemberLevelConfig selectByType(@Param("levelType") Integer levelType);

    @Select("SELECT * FROM mem_level WHERE min_points <= #{points} AND (max_points IS NULL OR max_points >= #{points}) "
            + "AND status = 1 ORDER BY level_type DESC LIMIT 1")
    MemberLevelConfig selectByPoints(@Param("points") Long points);
}
