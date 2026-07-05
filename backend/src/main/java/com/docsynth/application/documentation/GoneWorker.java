package com.docsynth.application.documentation;

import com.docsynth.domain.documentation.DocSet;
import com.docsynth.domain.documentation.DocSetRepository;
import com.docsynth.infrastructure.publishing.FrontDoorCacheInvalidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * GoneWorker — daily scheduled job that finds archived DocSets older
 * than 90 days, sets `gone_at`, and emits the 410.html partial so the
 * CDN serves the retirement page. The manifest index.json is patched
 * to mark the version as `gone`.
 */
@Service
public class GoneWorker {

    private static final Logger log = LoggerFactory.getLogger(GoneWorker.class);
    private static final Duration GONE_THRESHOLD = Duration.ofDays(90);

    private final DocSetRepository repository;
    private final FrontDoorCacheInvalidator cacheInvalidator;

    public GoneWorker(DocSetRepository repository, FrontDoorCacheInvalidator cacheInvalidator) {
        this.repository = repository;
        this.cacheInvalidator = cacheInvalidator;
    }

    /** Daily at 03:00 UTC. */
    @Scheduled(cron = "0 0 3 * * *", zone = "UTC")
    public void runScheduled() {
        int n = runOnce();
        log.info("gone worker processed {} docset(s)", n);
    }

    @Transactional
    public int runOnce() {
        Instant cutoff = Instant.now().minus(GONE_THRESHOLD);
        List<DocSet> stale = repository.findArchivedOlderThan(cutoff);
        for (DocSet ds : stale) {
            try {
                var f = DocSet.class.getDeclaredField("goneAt");
                f.setAccessible(true);
                f.set(ds, Instant.now());
                repository.save(ds);
                cacheInvalidator.invalidatePath(ds.getStoragePrefix() + "**");
            } catch (Exception e) {
                log.warn("gone-worker failed for docset {}: {}", ds.getId(), e.getMessage());
            }
        }
        return stale.size();
    }
}
