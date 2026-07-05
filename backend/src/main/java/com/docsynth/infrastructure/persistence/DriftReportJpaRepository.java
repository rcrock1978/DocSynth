package com.docsynth.infrastructure.persistence;

import com.docsynth.domain.drift.DriftReport;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DriftReportJpaRepository extends JpaRepository<DriftReport, UUID> {

    @Query("SELECT d FROM DriftReport d WHERE d.projectId = :projectId ORDER BY d.generatedAt DESC")
    List<DriftReport> findByProjectIdOrderByGeneratedAtDesc(UUID projectId, Pageable pageable);

    @Query("SELECT d FROM DriftReport d WHERE d.leftSpecId = :left AND d.rightSpecId = :right")
    Optional<DriftReport> findByLeftSpecIdAndRightSpecId(UUID left, UUID right);
}
