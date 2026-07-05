package com.docsynth.infrastructure.persistence;

import com.docsynth.domain.drift.NotificationChannel;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface NotificationChannelJpaRepository extends JpaRepository<NotificationChannel, UUID> {
    List<NotificationChannel> findByProjectId(UUID projectId);
}
