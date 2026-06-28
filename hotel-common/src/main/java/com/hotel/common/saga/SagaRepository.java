package com.hotel.common.saga;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class SagaRepository {

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public void save(SagaContext ctx) {
        String sql = """
            INSERT INTO saga_log (saga_id, saga_type, order_id, status, current_step,
                step_records, error_message, retry_count, start_time, update_time)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            ON DUPLICATE KEY UPDATE
                status = VALUES(status),
                current_step = VALUES(current_step),
                step_records = VALUES(step_records),
                error_message = VALUES(error_message),
                retry_count = VALUES(retry_count),
                update_time = VALUES(update_time),
                end_time = IF(status IN ('COMPLETED','COMPENSATED','FAILED'), NOW(), NULL)
            """;
        jdbcTemplate.update(sql,
                ctx.getSagaId(),
                "checkin_saga",
                ctx.getOrderId(),
                ctx.getStatus().name(),
                ctx.getCurrentStep(),
                serializeSteps(ctx.getStepRecords()),
                ctx.getErrorMessage(),
                ctx.getRetryCount(),
                ctx.getStartTime(),
                ctx.getUpdateTime()
        );
    }

    public SagaContext findById(String sagaId) {
        List<SagaContext> list = jdbcTemplate.query(
                "SELECT * FROM saga_log WHERE saga_id = ?",
                this::mapRow, sagaId);
        return list.isEmpty() ? null : list.get(0);
    }

    public List<SagaContext> findStuckSagas(Duration stuckThreshold) {
        LocalDateTime threshold = LocalDateTime.now().minus(stuckThreshold);
        return jdbcTemplate.query(
                "SELECT * FROM saga_log WHERE status IN (?,?) AND update_time < ? ORDER BY update_time",
                this::mapRow,
                SagaStatus.RUNNING.name(), SagaStatus.COMPENSATING.name(), threshold
        );
    }

    public List<SagaContext> findCompensationFailures(int maxRetries) {
        return jdbcTemplate.query(
                "SELECT * FROM saga_log WHERE status = ? AND retry_count < ? ORDER BY update_time",
                this::mapRow,
                SagaStatus.COMPENSATED.name(), maxRetries
        );
    }

    private SagaContext mapRow(ResultSet rs, int rowNum) throws SQLException {
        SagaContext ctx = new SagaContext();
        ctx.setSagaId(rs.getString("saga_id"));
        ctx.setOrderId(rs.getString("order_id"));
        ctx.setStatus(SagaStatus.valueOf(rs.getString("status")));
        ctx.setCurrentStep(rs.getInt("current_step"));
        ctx.setStepRecords(deserializeSteps(rs.getString("step_records")));
        ctx.setErrorMessage(rs.getString("error_message"));
        ctx.setRetryCount(rs.getInt("retry_count"));
        ctx.setStartTime(rs.getObject("start_time", LocalDateTime.class));
        ctx.setUpdateTime(rs.getObject("update_time", LocalDateTime.class));
        ctx.setEndTime(rs.getObject("end_time", LocalDateTime.class));
        return ctx;
    }

    private String serializeSteps(List<SagaStepRecord> records) {
        try {
            return objectMapper.writeValueAsString(records);
        } catch (JsonProcessingException e) {
            return "[]";
        }
    }

    private List<SagaStepRecord> deserializeSteps(String json) {
        try {
            if (json == null || json.isEmpty()) return List.of();
            return Arrays.asList(objectMapper.readValue(json, SagaStepRecord[].class));
        } catch (Exception e) {
            return List.of();
        }
    }
}
