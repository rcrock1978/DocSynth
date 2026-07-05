package com.docsynth.infrastructure.publishing;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * FrontDoorCacheInvalidator — issues a path-prefix purge against Azure
 * Front Door when a DocSet state changes. Real implementation calls
 * the Azure REST API; v1 logs the invalidation.
 */
@Component
public class FrontDoorCacheInvalidator {

    private static final Logger log = LoggerFactory.getLogger(FrontDoorCacheInvalidator.class);

    public void invalidatePath(String path) {
        log.info("front door cache invalidation requested for path: {}", path);
    }
}
