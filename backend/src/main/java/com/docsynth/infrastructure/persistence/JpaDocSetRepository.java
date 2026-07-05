package com.docsynth.infrastructure.persistence;

import com.docsynth.domain.documentation.DocSet;
import com.docsynth.domain.documentation.DocSetId;
import com.docsynth.domain.documentation.DocSetRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class JpaDocSetRepository implements DocSetRepository {

    private final DocSetJpaRepository jpa;

    public JpaDocSetRepository(DocSetJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public DocSet save(DocSet docSet) {
        return jpa.save(docSet);
    }

    @Override
    public Optional<DocSet> findById(DocSetId id) {
        return jpa.findById(id.value());
    }

    @Override
    public Optional<DocSet> findByProjectIdAndDisplayVersion(UUID projectId, String displayVersion) {
        return jpa.findByProjectIdAndDisplayVersion(projectId, displayVersion);
    }

    @Override
    public List<DocSet> findByProjectId(UUID projectId, int limit, int offset) {
        return jpa.findActiveByProjectId(projectId, PageRequest.of(0, limit)).stream()
            .filter(d -> true)
            .toList();
    }

    @Override
    public Optional<DocSet> findLatestActiveForProject(UUID projectId) {
        return jpa.findActiveByProjectId(projectId).stream().findFirst();
    }

    @Override
    @Transactional
    public void supersedePreviousActive(UUID projectId, DocSetId replacementId) {
        jpa.supersedePreviousActive(projectId, replacementId.value());
    }

    @Override
    public List<DocSet> findArchivedOlderThan(Instant cutoff) {
        return jpa.findArchivedOlderThan(cutoff);
    }
}
