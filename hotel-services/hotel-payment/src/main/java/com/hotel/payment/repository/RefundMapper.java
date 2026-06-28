package com.hotel.payment.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hotel.payment.model.entity.Refund;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface RefundMapper extends BaseMapper<Refund> {

    @Select("SELECT * FROM pay_refund WHERE refund_no = #{refundNo}")
    Refund selectByRefundNo(@Param("refundNo") String refundNo);

    @Select("SELECT * FROM pay_refund WHERE payment_id = #{paymentId} ORDER BY created_time DESC")
    java.util.List<Refund> selectByPaymentId(@Param("paymentId") String paymentId);
}
