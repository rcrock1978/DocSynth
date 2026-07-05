package com.docsynth.infrastructure.persistence;

import com.docsynth.domain.drift.DriftItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface DriftItemJpaRepository extends JpaRepository<DriftItem, UUID> {
    List<DriftItem> findByDriftReportId(UUID driftReportId);
}
