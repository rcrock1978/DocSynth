package com.docsynth.domain.documentation;

import com.docsynth.domain.tenant.TenantId;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Repository extension for the GoneWorker. */
public interface DocSetRepositoryExtension {
    List<DocSet> findArchivedOlderThan(Instant cutoff);
}
