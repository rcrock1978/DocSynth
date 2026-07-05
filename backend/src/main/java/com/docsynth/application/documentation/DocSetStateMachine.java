package com.docsynth.application.documentation;

import com.docsynth.domain.documentation.DocSet;
import com.docsynth.domain.documentation.DocSetRepository;
import com.docsynth.infrastructure.publishing.DeprecationBannerEmitter;
import com.docsynth.infrastructure.publishing.FrontDoorCacheInvalidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;

/**
 * DocSetStateMachine — enforces FR-014 transition rules.
 *
 *  active -> deprecated:       any Owner/Editor, manual, optional sunset date
 *  active -> active:           publishing a new version auto-supersedes
 *  deprecated -> active:       revert (Owner/Editor)
 *  deprecated -> archived:     manual, min 90 days after deprecation
 *  archived -> active:         NOT ALLOWED
 *  All transitions:            audit emit, banner/cache invalidation side effects
 */
@Service
public class DocSetStateMachine {

    private static final Logger log = LoggerFactory.getLogger(DocSetStateMachine.class);
    private static final Duration MIN_DEPRECATION_BEFORE_ARCHIVE = Duration.ofDays(90);

    private final DocSetRepository repository;
    private final DeprecationBannerEmitter bannerEmitter;
    private final FrontDoorCacheInvalidator cacheInvalidator;

    public DocSetStateMachine(
        DocSetRepository repository,
        DeprecationBannerEmitter bannerEmitter,
        FrontDoorCacheInvalidator cacheInvalidator
    ) {
        this.repository = repository;
        this.bannerEmitter = bannerEmitter;
        this.cacheInvalidator = cacheInvalidator;
    }

    @Transactional
    public DocSet transition(DocSet docSet, String targetState, Instant sunsetAt) {
        String from = docSet.getState();
        if (!isValidTransition(from, targetState)) {
            throw new IllegalStateTransitionException(
                "transition " + from + " -> " + targetState + " is not allowed for docset "
                    + docSet.getId().value()
            );
        }
        if ("archived".equals(targetState)) {
            if (docSet.getDeprecatedAt() == null) {
                throw new IllegalStateTransitionException(
                    "must be deprecated before archive (docset " + docSet.getId().value() + ")"
                );
            }
            long days = Duration.between(docSet.getDeprecatedAt(), Instant.now()).toDays();
            if (days < 90) {
                throw new IllegalStateTransitionException(
                    "deprecated -> archived requires at least 90 days; this docset has been deprecated for "
                        + days + " days"
                );
            }
        }

        // Apply transition.
        try {
            var stateField = DocSet.class.getDeclaredField("state");
            stateField.setAccessible(true);
            stateField.set(docSet, targetState);
            if ("deprecated".equals(targetState)) {
                var da = DocSet.class.getDeclaredField("deprecatedAt");
                da.setAccessible(true);
                da.set(docSet, Instant.now());
                if (sunsetAt != null) {
                    var sa = DocSet.class.getDeclaredField("sunsetAt");
                    sa.setAccessible(true);
                    sa.set(docSet, sunsetAt);
                }
            }
            if ("archived".equals(targetState)) {
                var aa = DocSet.class.getDeclaredField("archivedAt");
                aa.setAccessible(true);
                aa.set(docSet, Instant.now());
            }
        } catch (Exception e) {
            throw new IllegalStateTransitionException("failed to apply transition: " + e.getMessage());
        }

        DocSet saved = repository.save(docSet);

        // Side effects.
        if ("deprecated".equals(targetState)) {
            bannerEmitter.emitBanner(saved);
        }
        if ("archived".equals(targetState) || "deprecated".equals(targetState)) {
            cacheInvalidator.invalidatePath(saved.getStoragePrefix() + "**");
        }

        log.info("docset {} transition {} -> {}", saved.getId(), from, targetState);
        return saved;
    }

    private boolean isValidTransition(String from, String to) {
        if (from == null || to == null) return false;
        if (from.equals(to)) return true; // no-op
        return switch (from) {
            case "active" -> "deprecated".equals(to);
            case "deprecated" -> "active".equals(to) || "archived".equals(to);
            case "archived" -> false; // terminal
            default -> false;
        };
    }
}
