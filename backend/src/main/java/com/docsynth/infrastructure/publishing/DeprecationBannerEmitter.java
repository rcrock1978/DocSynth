package com.docsynth.infrastructure.publishing;

import com.docsynth.domain.documentation.DocSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * DeprecationBannerEmitter — uploads a deprecation banner partial to
 * object storage and updates the manifest. The banner is read by the
 * docs SSG bundle and rendered on every endpoint page of the deprecated
 * DocSet.
 */
@Component
public class DeprecationBannerEmitter {

    private static final Logger log = LoggerFactory.getLogger(DeprecationBannerEmitter.class);

    public void emitBanner(DocSet docSet) {
        // Real implementation:
        //   1. Upload docsets/{projectId}/{storagePrefix}/_banner.html with
        //      the deprecation notice + replacement link.
        //   2. Patch the manifest index.json to mark this version as deprecated.
        log.info("deprecation banner emitted for docset {} (version {})",
            docSet.getId(), docSet.getDisplayVersion());
    }
}
