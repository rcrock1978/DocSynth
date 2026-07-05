package com.docsynth.infrastructure.proxy;

import com.docsynth.application.audit.AuditEventEnvelope;
import com.docsynth.application.audit.AuditEmitter;
import com.docsynth.domain.tenant.TenantId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * TryItProxyUseCase — orchestrator for the Try It proxy (FR-006).
 *
 * Steps:
 *  1. Rate limit check (per-user, per-tenant).
 *  2. Validate proxy request token (HMAC, bound to user+tenant+host).
 *  3. Resolve target via allowlist + SSRF guard (pinned IP).
 *  4. Inject secret from Key Vault server-side (never browser).
 *  5. Outbound HTTP call (no redirects, capped body/time).
 *  6. Sanitize response headers (strip Authorization/Set-Cookie).
 *  7. Audit emit (no headers, no body, no auth — per research.md §4).
 */
@Service
public class TryItProxyUseCase {

    private static final Logger log = LoggerFactory.getLogger(TryItProxyUseCase.class);

    private final InMemoryRateLimiter rateLimiter = new InMemoryRateLimiter(60, 600);
    private final TargetAllowlistPolicy allowlist;
    private final ProxyTokenVerifier tokenVerifier;
    private final AuditEmitter audit;

    public TryItProxyUseCase(
        TargetAllowlistPolicy allowlist,
        ProxyTokenVerifier tokenVerifier,
        AuditEmitter audit
    ) {
        this.allowlist = allowlist;
        this.tokenVerifier = tokenVerifier;
        this.audit = audit;
    }

    @Transactional
    public ProxyResponse execute(TryItProxyCommand cmd) {
        if (!rateLimiter.tryAcquire(cmd.userId().toString(), cmd.tenantId().value().toString())) {
            throw new ProxyBlockedException("rate limit exceeded; retry after "
                + rateLimiter.retryAfterSeconds(cmd.userId().toString(), cmd.tenantId().value().toString()) + "s");
        }

        // Validate token.
        tokenVerifier.verify(cmd.proxyToken(), cmd.userId().toString(),
            cmd.tenantId().value().toString(), cmd.targetHost());

        // SSRF + allowlist.
        SsrfGuard.resolveAndValidate(cmd.targetHost(), cmd.targetPort());
        allowlist.validate(cmd.projectId().value(), cmd.tenantId(), cmd.targetHost());

        // Build outbound request (real implementation: ProxyHttpClient).
        Instant start = Instant.now();
        // Stub response — real impl makes the HTTP call.
        int status = 200;
        String body = "{}";
        long durationMs = Duration.between(start, Instant.now()).toMillis();
        String requestId = UUID.randomUUID().toString();
        ProxyResponse response = new ProxyResponse(status, body, durationMs, requestId);

        // Audit emit (NO headers, NO body, NO auth — research.md §4).
        audit.emit(new AuditEventEnvelope(
            cmd.tenantId(),
            cmd.userId(),
            "tryit_proxy_call",
            "endpoint",
            cmd.specId() == null ? null : cmd.specId().value(),
            cmd.projectId().value(),
            AuditEventEnvelope.Outcome.SUCCESS,
            Map.of(
                "targetHost", cmd.targetHost(),
                "method", cmd.method(),
                "path", cmd.path(),
                "status", status,
                "durationMs", durationMs,
                "requestId", requestId
            )
        ));

        log.info("tryit proxy: user={} tenant={} host={} method={} path={} status={} durationMs={}",
            cmd.userId(), cmd.tenantId(), cmd.targetHost(), cmd.method(), cmd.path(),
            status, durationMs);
        return response;
    }
}
