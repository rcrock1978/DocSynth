package com.docsynth.infrastructure.proxy;

import org.slf4j.MDC;

/**
 * TraceparentCarrier — extracts the W3C traceparent header from MDC so
 * the proxy can forward it as a custom HTTP header. A3 / FR-013
 * conformance: the correlation id flows end-to-end.
 */
public record TraceparentCarrier(String value) {

    public String toHeader() {
        if (value == null || value.isBlank()) {
            String mdc = MDC.get("traceId");
            if (mdc == null || mdc.isBlank()) return "";
            return "00-" + mdc + "-0000000000000000-01";
        }
        return value;
    }
}
