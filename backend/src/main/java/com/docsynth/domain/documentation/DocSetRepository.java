package com.docsynth.domain.documentation;

import com.docsynth.domain.tenant.TenantId;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Repository port for DocSet aggregate. */
public interface DocSetRepository {
    DocSet save(DocSet docSet);
    Optional<DocSet> findById(DocSetId id);
    Optional<DocSet> findByProjectIdAndDisplayVersion(UUID projectId, String displayVersion);
    List<DocSet> findByProjectId(UUID projectId, int limit, int offset);
    Optional<DocSet> findLatestActiveForProject(UUID projectId);
    void supersedePreviousActive(UUID projectId, DocSetId replacementId);
    List<DocSet> findArchivedOlderThan(Instant cutoff);
}
