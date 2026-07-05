package com.docsynth.application.documentation;

import com.docsynth.domain.documentation.DocSet;
import com.docsynth.domain.documentation.DocSetId;
import com.docsynth.domain.documentation.DocSetRepository;
import com.docsynth.infrastructure.publishing.FrontDoorCacheInvalidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * PublishDocSetUseCase — supersede a previous active DocSet when publishing
 * a new one. Triggered by GenerateDocSetUseCase after SSG build success.
 */
@Service
public class PublishDocSetUseCase {

    private static final Logger log = LoggerFactory.getLogger(PublishDocSetUseCase.class);

    private final DocSetRepository repository;
    private final FrontDoorCacheInvalidator cacheInvalidator;

    public PublishDocSetUseCase(DocSetRepository repository, FrontDoorCacheInvalidator cacheInvalidator) {
        this.repository = repository;
        this.cacheInvalidator = cacheInvalidator;
    }

    @Transactional
    public void onPublished(DocSet newDocSet) {
        // The DocSet is already saved by GenerateDocSetUseCase; we supersede
        // the previous active and invalidate the CDN cache for the old prefix.
        repository.findLatestActiveForProject(newDocSet.getProjectId().value())
            .filter(prev -> !prev.getId().value().equals(newDocSet.getId().value()))
            .ifPresent(prev -> {
                repository.supersedePreviousActive(newDocSet.getProjectId().value(), newDocSet.getId());
                cacheInvalidator.invalidatePath(prev.getStoragePrefix() + "**");
                log.info("superseded previous active docset {} in favor of {}",
                    prev.getId(), newDocSet.getId());
            });
    }
}
