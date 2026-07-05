package com.docsynth.infrastructure.persistence;

import com.docsynth.domain.proxy.TryItAllowlistEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface TryItAllowlistJpaRepository extends JpaRepository<TryItAllowlistEntry, UUID> {

    @Query("SELECT e FROM TryItAllowlistEntry e WHERE e.projectId = :projectId AND e.revokedAt IS NULL")
    List<TryItAllowlistEntry> findActiveByProjectId(UUID projectId);

    @Modifying
    @Query("UPDATE TryItAllowlistEntry e SET e.revokedAt = CURRENT_TIMESTAMP WHERE e.id = :id")
    void revoke(@Param("id") UUID id);
}
