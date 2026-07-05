package com.docsynth.infrastructure.persistence;

import com.docsynth.domain.documentation.DocSet;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DocSetJpaRepository extends JpaRepository<DocSet, UUID> {

    @Query("SELECT d FROM DocSet d WHERE d.projectId = :projectId AND d.displayVersion = :version")
    Optional<DocSet> findByProjectIdAndDisplayVersion(UUID projectId, String version);

    @Query("SELECT d FROM DocSet d WHERE d.projectId = :projectId AND d.state = 'active' ORDER BY d.generatedAt DESC")
    List<DocSet> findActiveByProjectId(UUID projectId);

    @Modifying
    @Query("UPDATE DocSet d SET d.state = 'deprecated', d.deprecatedAt = CURRENT_TIMESTAMP, d.replacementDocSetId = :replacementId " +
           "WHERE d.projectId = :projectId AND d.state = 'active'")
    void supersedePreviousActive(@Param("projectId") UUID projectId,
                                  @Param("replacementId") UUID replacementId);

    @Query("SELECT d FROM DocSet d WHERE d.state = 'archived' AND d.archivedAt < :cutoff AND d.goneAt IS NULL")
    List<DocSet> findArchivedOlderThan(@Param("cutoff") Instant cutoff);
}
