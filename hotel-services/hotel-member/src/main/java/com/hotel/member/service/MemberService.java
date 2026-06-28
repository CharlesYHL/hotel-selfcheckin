package com.hotel.member.service;

import com.hotel.common.core.BusinessException;
import com.hotel.member.model.dto.*;
import com.hotel.member.model.entity.Member;
import com.hotel.member.model.entity.MemberLevelConfig;
import com.hotel.member.model.entity.PointsLog;
import com.hotel.member.model.enums.MemberLevel;
import com.hotel.member.model.enums.MemberStatus;
import com.hotel.member.model.enums.PointsBusinessType;
import com.hotel.member.repository.MemberLevelMapper;
import com.hotel.member.repository.MemberMapper;
import com.hotel.member.repository.PointsLogMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class MemberService {

    private final MemberMapper memberMapper;
    private final MemberLevelMapper levelMapper;
    private final PointsLogMapper pointsLogMapper;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${member.points.checkin-reward:100}")
    private int checkinReward;

    @Value("${member.points.consume-rate:1}")
    private int consumeRate;

    @Value("${member.points.expire-months:12}")
    private int expireMonths;

    /**
     * 注册会员。
     */
    @Transactional
    public MemberResponse register(RegisterMemberRequest request) {
        Member existing = memberMapper.selectByPhone(request.getPhone());
        if (existing != null) {
            throw new BusinessException("该手机号已注册");
        }

        Member member = new Member();
        member.setMemberId("MEM" + UUID.randomUUID().toString().replace("-", "").substring(0, 24));
        member.setMemberNo("MB" + System.currentTimeMillis());
        member.setPhone(request.getPhone());
        member.setMemberName(request.getMemberName());
        member.setGender(request.getGender());
        member.setEmail(request.getEmail());
        member.setOpenid(request.getOpenid());
        member.setUnionid(request.getUnionid());
        member.setNickname(request.getNickname());
        member.setAvatar(request.getAvatar());
        member.setBirthday(request.getBirthday());
        member.setMemberSource(request.getMemberSource() != null ? request.getMemberSource() : 1);

        // 默认基础会员等级
        MemberLevelConfig baseLevel = levelMapper.selectByType(MemberLevel.BASIC.getCode());
        member.setLevelId(baseLevel != null ? baseLevel.getLevelId() : "BASIC");
        member.setLevelName(MemberLevel.BASIC.getDesc());
        member.setTotalPoints(0L);
        member.setAvailablePoints(0L);
        member.setTotalStay(0);
        member.setTotalNights(0);
        member.setTotalConsume(BigDecimal.ZERO);
        member.setBalance(BigDecimal.ZERO);
        member.setStatus(MemberStatus.ACTIVE.getCode());
        member.setRegisterTime(LocalDateTime.now());
        member.setCreatedTime(LocalDateTime.now());
        member.setUpdatedTime(LocalDateTime.now());
        memberMapper.insert(member);

        log.info("会员注册成功: memberId={}, memberNo={}, phone={}", member.getMemberId(), member.getMemberNo(), member.getPhone());
        return toResponse(member);
    }

    /**
     * 入住后奖励积分。由 checkin-events 触发。
     */
    @Transactional
    public PointsResponse earnCheckinPoints(String memberId, String hotelId, String orderId,
                                             String orderNo, String checkinId, BigDecimal amount) {
        Member member = memberMapper.selectById(memberId);
        if (member == null) {
            throw new BusinessException("会员不存在");
        }

        // 计算积分：消费金额 × 积分倍率
        MemberLevelConfig level = levelMapper.selectByType(
                MemberLevel.valueOf(member.getLevelName().replace("会员", "")).getCode());
        BigDecimal rate = level != null ? level.getPointsRate() : BigDecimal.ONE;
        long points = amount.multiply(rate).longValue() + checkinReward;

        // 更新会员积分和入住统计
        memberMapper.updateAfterStay(memberId, points, amount, 1, 1, hotelId);

        // 记录积分日志
        PointsLog logEntry = new PointsLog();
        logEntry.setLogId("PTL" + UUID.randomUUID().toString().replace("-", "").substring(0, 24));
        logEntry.setMemberId(memberId);
        logEntry.setHotelId(hotelId);
        logEntry.setPoints(points);
        logEntry.setBalanceBefore(member.getAvailablePoints());
        logEntry.setBalanceAfter(member.getAvailablePoints() + points);
        logEntry.setBusinessType(PointsBusinessType.CHECKIN.getCode());
        logEntry.setOrderId(orderId);
        logEntry.setOrderNo(orderNo);
        logEntry.setCheckinId(checkinId);
        logEntry.setDescription("入住奖励积分: " + points);
        logEntry.setExpireDate(LocalDate.now().plusMonths(expireMonths));
        logEntry.setCreatedTime(LocalDateTime.now());
        pointsLogMapper.insert(logEntry);

        // 检查会员升级
        checkLevelUpgrade(memberId, member.getTotalPoints() + points);

        log.info("入住积分奖励: memberId={}, points={}, checkinId={}", memberId, points, checkinId);
        return PointsResponse.builder()
                .memberId(memberId)
                .totalPoints(member.getTotalPoints() + points)
                .availablePoints(member.getAvailablePoints() + points)
                .earned(points)
                .balanceAfter(member.getAvailablePoints() + points)
                .description("入住奖励积分")
                .createdTime(LocalDateTime.now())
                .build();
    }

    /**
     * 通用积分获取。
     */
    @Transactional
    public PointsResponse earnPoints(EarnPointsRequest request) {
        Member member = memberMapper.selectById(request.getMemberId());
        if (member == null) {
            throw new BusinessException("会员不存在");
        }

        PointsLog logEntry = new PointsLog();
        logEntry.setLogId("PTL" + UUID.randomUUID().toString().replace("-", "").substring(0, 24));
        logEntry.setMemberId(request.getMemberId());
        logEntry.setHotelId(null);
        logEntry.setPoints(request.getPoints());
        logEntry.setBalanceBefore(member.getAvailablePoints());
        logEntry.setBalanceAfter(member.getAvailablePoints() + request.getPoints());
        logEntry.setBusinessType(request.getBusinessType());
        logEntry.setBusinessId(request.getBusinessId());
        logEntry.setOrderId(request.getOrderId());
        logEntry.setOrderNo(request.getOrderNo());
        logEntry.setCheckinId(request.getCheckinId());
        logEntry.setDescription(request.getDescription());
        logEntry.setExpireDate(LocalDate.now().plusMonths(expireMonths));
        logEntry.setCreatedTime(LocalDateTime.now());
        pointsLogMapper.insert(logEntry);

        memberMapper.updateAfterStay(request.getMemberId(), request.getPoints(), BigDecimal.ZERO, 0, 0, null);

        checkLevelUpgrade(request.getMemberId(), member.getTotalPoints() + request.getPoints());

        log.info("积分获取: memberId={}, points={}, type={}", request.getMemberId(), request.getPoints(), request.getBusinessType());
        return PointsResponse.builder()
                .memberId(request.getMemberId())
                .totalPoints(member.getTotalPoints() + request.getPoints())
                .availablePoints(member.getAvailablePoints() + request.getPoints())
                .earned(request.getPoints())
                .balanceAfter(member.getAvailablePoints() + request.getPoints())
                .description(request.getDescription())
                .createdTime(LocalDateTime.now())
                .build();
    }

    /**
     * 积分扣减（兑换/退款等）。
     */
    @Transactional
    public PointsResponse deductPoints(String memberId, Long points, String description) {
        Member member = memberMapper.selectById(memberId);
        if (member == null) {
            throw new BusinessException("会员不存在");
        }
        if (member.getAvailablePoints() < points) {
            throw new BusinessException("可用积分不足");
        }

        int updated = memberMapper.deductPoints(memberId, points);
        if (updated == 0) {
            throw new BusinessException("积分扣减失败");
        }

        PointsLog logEntry = new PointsLog();
        logEntry.setLogId("PTL" + UUID.randomUUID().toString().replace("-", "").substring(0, 24));
        logEntry.setMemberId(memberId);
        logEntry.setHotelId(null);
        logEntry.setPoints(-points);
        logEntry.setBalanceBefore(member.getAvailablePoints());
        logEntry.setBalanceAfter(member.getAvailablePoints() - points);
        logEntry.setBusinessType(PointsBusinessType.EXCHANGE.getCode());
        logEntry.setDescription(description);
        logEntry.setCreatedTime(LocalDateTime.now());
        pointsLogMapper.insert(logEntry);

        log.info("积分扣减: memberId={}, points={}, description={}", memberId, points, description);
        return PointsResponse.builder()
                .memberId(memberId)
                .totalPoints(member.getTotalPoints())
                .availablePoints(member.getAvailablePoints() - points)
                .earned(-points)
                .balanceAfter(member.getAvailablePoints() - points)
                .description(description)
                .createdTime(LocalDateTime.now())
                .build();
    }

    /**
     * 查询会员信息。
     */
    public MemberResponse queryMember(String memberId) {
        Member member = memberMapper.selectById(memberId);
        if (member == null) {
            throw new BusinessException("会员不存在");
        }
        return toResponse(member);
    }

    /**
     * 按手机号查询。
     */
    public MemberResponse queryByPhone(String phone) {
        Member member = memberMapper.selectByPhone(phone);
        if (member == null) {
            throw new BusinessException("会员不存在");
        }
        return toResponse(member);
    }

    /**
     * 查询积分记录。
     */
    public List<Map<String, Object>> getPointsLogs(String memberId, int limit) {
        return pointsLogMapper.selectByMemberId(memberId, limit).stream().map(l -> {
            Map<String, Object> m = new HashMap<>();
            m.put("logId", l.getLogId());
            m.put("points", l.getPoints());
            m.put("balanceBefore", l.getBalanceBefore());
            m.put("balanceAfter", l.getBalanceAfter());
            m.put("businessType", l.getBusinessType());
            m.put("businessTypeDesc", PointsBusinessType.values()[l.getBusinessType() - 1].getDesc());
            m.put("description", l.getDescription());
            m.put("expireDate", l.getExpireDate());
            m.put("createdTime", l.getCreatedTime());
            return m;
        }).collect(Collectors.toList());
    }

    /**
     * 查询所有会员等级配置。
     */
    public List<Map<String, Object>> getLevelConfigs() {
        return levelMapper.selectList(null).stream().map(l -> {
            Map<String, Object> m = new HashMap<>();
            m.put("levelId", l.getLevelId());
            m.put("levelCode", l.getLevelCode());
            m.put("levelName", l.getLevelName());
            m.put("levelType", l.getLevelType());
            m.put("minPoints", l.getMinPoints());
            m.put("maxPoints", l.getMaxPoints());
            m.put("pointsRate", l.getPointsRate());
            m.put("discountRate", l.getDiscountRate());
            return m;
        }).collect(Collectors.toList());
    }

    /**
     * 处理过期积分。
     */
    @Transactional
    public int processExpiredPoints() {
        int totalExpired = 0;
        // 简单实现：按 memberId 扫描
        var members = memberMapper.selectList(null);
        for (Member member : members) {
            List<PointsLog> expiring = pointsLogMapper.selectExpiringPoints(member.getMemberId());
            long expiredSum = expiring.stream().mapToLong(PointsLog::getPoints).sum();
            if (expiredSum > 0 && member.getAvailablePoints() >= expiredSum) {
                memberMapper.deductPoints(member.getMemberId(), expiredSum);
                totalExpired++;
                log.info("积分过期处理: memberId={}, expiredPoints={}", member.getMemberId(), expiredSum);
            }
        }
        log.info("积分过期处理完成: 处理会员数={}", totalExpired);
        return totalExpired;
    }

    private void checkLevelUpgrade(String memberId, long newTotalPoints) {
        MemberLevelConfig newLevel = levelMapper.selectByPoints(newTotalPoints);
        if (newLevel == null) return;

        Member member = memberMapper.selectById(memberId);
        if (member == null) return;

        if (!newLevel.getLevelId().equals(member.getLevelId())) {
            memberMapper.updateLevel(memberId, newLevel.getLevelId(), newLevel.getLevelName());
            log.info("会员升级: memberId={}, oldLevel={}, newLevel={}",
                    memberId, member.getLevelName(), newLevel.getLevelName());
        }
    }

    private MemberResponse toResponse(Member m) {
        return MemberResponse.builder()
                .memberId(m.getMemberId())
                .memberNo(m.getMemberNo())
                .nickname(m.getNickname())
                .avatar(m.getAvatar())
                .memberName(m.getMemberName())
                .gender(m.getGender())
                .phone(m.getPhone())
                .email(m.getEmail())
                .levelId(m.getLevelId())
                .levelName(m.getLevelName())
                .totalPoints(m.getTotalPoints())
                .availablePoints(m.getAvailablePoints())
                .totalStay(m.getTotalStay())
                .totalNights(m.getTotalNights())
                .totalConsume(m.getTotalConsume())
                .balance(m.getBalance())
                .status(m.getStatus())
                .statusDesc(MemberStatus.of(m.getStatus()).getDesc())
                .registerTime(m.getRegisterTime())
                .createdTime(m.getCreatedTime())
                .build();
    }
}
