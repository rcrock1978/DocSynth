package com.docsynth.interfaces.api;

import com.docsynth.application.drift.DetectDriftCommand;
import com.docsynth.application.drift.DetectDriftUseCase;
import com.docsynth.domain.ingestion.ApiSpecId;
import com.docsynth.domain.project.ProjectId;
import com.docsynth.domain.tenant.TenantId;
import com.docsynth.infrastructure.security.TenantContextResolver;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import java.util.Map;
import java.util.UUID;

/**
 * WebhookController — receives GitHub push events. Verifies the X-Hub-Signature-256
 * HMAC, resolves the project from the repository URL, and enqueues a
 * drift detection job. The webhook secret is stored in Key Vault; the
 * signature header is the only authentication.
 */
@RestController
@RequestMapping("/api/v1/webhooks/github")
public class WebhookController {

    private final DetectDriftUseCase useCase;
    private final TenantContextResolver tenantContext;
    private final String webhookSecret;

    public WebhookController(
        DetectDriftUseCase useCase,
        TenantContextResolver tenantContext,
        @Value("${docsynth.webhooks.github-secret-env:GITHUB_WEBHOOK_SECRET}") String webhookSecretEnvName
    ) {
        this.useCase = useCase;
        this.tenantContext = tenantContext;
        this.webhookSecret = System.getenv(webhookSecretEnvName);
    }

    @PostMapping
    public ResponseEntity<Void> receive(
        @RequestHeader(value = "X-Hub-Signature-256", required = false) String signature,
        @RequestHeader(value = "X-GitHub-Event") String event,
        @RequestBody Map<String, Object> payload
    ) {
        if (!"push".equals(event)) {
            return ResponseEntity.accepted().build();
        }
        if (signature == null || !verifySignature(signature, payload.toString())) {
            return ResponseEntity.status(401).build();
        }

        // Resolve project from repository full_name (e.g., "owner/repo").
        Map<String, Object> repository = (Map<String, Object>) payload.get("repository");
        String fullName = (String) repository.get("full_name");
        // Real implementation: project lookup by GitHub repo binding.
        UUID projectId = UUID.nameUUIDFromBytes(fullName.getBytes(StandardCharsets.UTF_8));
        UUID specId = UUID.nameUUIDFromBytes((fullName + ":head").getBytes(StandardCharsets.UTF_8));

        useCase.execute(new DetectDriftCommand(
            new ProjectId(projectId),
            new TenantId(UUID.nameUUIDFromBytes(("tenant:" + fullName).getBytes(StandardCharsets.UTF_8))),
            new ApiSpecId(specId),
            "webhook",
            null
        ));

        return ResponseEntity.accepted().build();
    }

    private boolean verifySignature(String header, String body) {
        if (webhookSecret == null || webhookSecret.isBlank()) return false;
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(webhookSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] sig = mac.doFinal(body.getBytes(StandardCharsets.UTF_8));
            String expected = "sha256=" + HexFormat.of().formatHex(sig);
            return constantTimeEquals(expected, header);
        } catch (Exception e) {
            return false;
        }
    }

    private boolean constantTimeEquals(String a, String b) {
        if (a.length() != b.length()) return false;
        int r = 0;
        for (int i = 0; i < a.length(); i++) r |= a.charAt(i) ^ b.charAt(i);
        return r == 0;
    }
}
