package com.hotel.member.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hotel.member.model.entity.Member;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface MemberMapper extends BaseMapper<Member> {

    @Select("SELECT * FROM mem_member WHERE phone = #{phone} AND status = 1 LIMIT 1")
    Member selectByPhone(@Param("phone") String phone);

    @Select("SELECT * FROM mem_member WHERE openid = #{openid} LIMIT 1")
    Member selectByOpenid(@Param("openid") String openid);

    @Select("SELECT * FROM mem_member WHERE member_no = #{memberNo}")
    Member selectByMemberNo(@Param("memberNo") String memberNo);

    @Update("UPDATE mem_member SET available_points = available_points + #{points}, "
            + "total_points = total_points + #{points}, total_consume = total_consume + #{amount}, "
            + "total_stay = total_stay + #{stay}, total_nights = total_nights + #{nights}, "
            + "last_hotel_id = #{hotelId}, last_stay_date = CURDATE(), updated_time = NOW() "
            + "WHERE member_id = #{memberId}")
    int updateAfterStay(@Param("memberId") String memberId, @Param("points") Long points,
                        @Param("amount") java.math.BigDecimal amount,
                        @Param("stay") Integer stay, @Param("nights") Integer nights,
                        @Param("hotelId") String hotelId);

    @Update("UPDATE mem_member SET level_id = #{levelId}, level_name = #{levelName}, "
            + "updated_time = NOW() WHERE member_id = #{memberId}")
    int updateLevel(@Param("memberId") String memberId,
                    @Param("levelId") String levelId, @Param("levelName") String levelName);

    @Update("UPDATE mem_member SET available_points = available_points - #{points}, "
            + "updated_time = NOW() WHERE member_id = #{memberId} AND available_points >= #{points}")
    int deductPoints(@Param("memberId") String memberId, @Param("points") Long points);
}
