package com.docsynth.infrastructure.proxy;

import com.docsynth.domain.proxy.TryItAllowlistEntry;
import com.docsynth.domain.tenant.TenantId;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * TargetAllowlistPolicy — resolves and validates a target host against
 * the tenant's allowlist. Wildcard patterns (e.g., "*.example.com") are
 * matched as regex.
 */
@Component
public class TargetAllowlistPolicy {

    private final com.docsynth.domain.proxy.TryItAllowlistRepository repository;

    public TargetAllowlistPolicy(com.docsynth.domain.proxy.TryItAllowlistRepository repository) {
        this.repository = repository;
    }

    public void validate(UUID projectId, TenantId tenantId, String host) {
        if (host == null || host.isBlank()) {
            throw new ProxyBlockedException("target host is required");
        }
        List<TryItAllowlistEntry> entries = repository.findActiveByProjectId(projectId);
        boolean allowed = entries.stream()
            .anyMatch(e -> matches(e.getHostPattern(), host));
        if (!allowed) {
            throw new ProxyBlockedException(
                "target host " + host + " is not in the project's Try It allowlist"
            );
        }
    }

    private boolean matches(String pattern, String host) {
        if (pattern == null) return false;
        if (pattern.equals(host)) return true;
        if (pattern.startsWith("*.")) {
            String regex = "^[^.]+\\." + Pattern.quote(pattern.substring(2));
            return host.matches(regex);
        }
        return false;
    }
}
