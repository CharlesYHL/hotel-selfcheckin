package com.hotel.payment.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hotel.payment.model.entity.Payment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface PaymentMapper extends BaseMapper<Payment> {

    @Select("SELECT * FROM pay_payment WHERE payment_no = #{paymentNo}")
    Payment selectByPaymentNo(@Param("paymentNo") String paymentNo);

    @Select("SELECT * FROM pay_payment WHERE order_id = #{orderId} ORDER BY created_time DESC")
    java.util.List<Payment> selectByOrderId(@Param("orderId") String orderId);

    @Update("UPDATE pay_payment SET status = #{status}, trade_no = #{tradeNo}, pay_time = NOW(), updated_time = NOW() WHERE payment_id = #{paymentId} AND status = #{expectedStatus}")
    int updatePaid(@Param("paymentId") String paymentId, @Param("status") int status,
                   @Param("tradeNo") String tradeNo, @Param("expectedStatus") int expectedStatus);

    @Update("UPDATE pay_payment SET status = #{status}, refund_id = #{refundId}, refund_amount = #{refundAmount}, refund_time = #{refundTime}, refund_reason = #{refundReason}, updated_time = NOW() WHERE payment_id = #{paymentId} AND status = #{expectedStatus}")
    int updateRefunded(@Param("paymentId") String paymentId, @Param("status") int status,
                       @Param("refundId") String refundId, @Param("refundAmount") java.math.BigDecimal refundAmount,
                       @Param("refundTime") java.time.LocalDateTime refundTime,
                       @Param("refundReason") String refundReason, @Param("expectedStatus") int expectedStatus);
}
