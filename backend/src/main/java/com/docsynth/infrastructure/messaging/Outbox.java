package com.docsynth.infrastructure.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.time.Instant;
import java.util.UUID;

/**
 * Transactional outbox.
 *
 * Producers write events to the outbox table within the same JDBC transaction
 * as the business write. A separate relay worker polls the table, publishes
 * to Service Bus topics, and marks rows as published. This is the canonical
 * "transactional outbox" pattern: avoids the dual-write problem (commit
 * succeeds but message send fails) without distributed transactions.
 */
@Component
public class Outbox {

    private final DataSource dataSource;
    private final ObjectMapper objectMapper;

    public Outbox(DataSource dataSource, ObjectMapper objectMapper) {
        this.dataSource = dataSource;
        this.objectMapper = objectMapper;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void append(String topic, String aggregateType, UUID aggregateId, Object payload) {
        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(
                 "INSERT INTO outbox (id, topic, aggregate_type, aggregate_id, payload, created_at, published) "
                 + "VALUES (?, ?, ?, ?, ?::jsonb, ?, FALSE)")) {
            ps.setObject(1, UUID.randomUUID());
            ps.setString(2, topic);
            ps.setString(3, aggregateType);
            ps.setObject(4, aggregateId);
            ps.setString(5, objectMapper.writeValueAsString(payload));
            ps.setObject(6, Instant.now());
            ps.executeUpdate();
        } catch (Exception e) {
            throw new OutboxException("Failed to append outbox event", e);
        }
    }
}
