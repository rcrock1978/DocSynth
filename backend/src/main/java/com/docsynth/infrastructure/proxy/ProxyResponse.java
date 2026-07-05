package com.docsynth.infrastructure.proxy;

/**
 * ProxyResponse — value object representing a successful (or upstream-error)
 * response from the Try It proxy. Returned to the operator UI.
 */
public record ProxyResponse(int status, String body, long durationMs, String requestId) {}
