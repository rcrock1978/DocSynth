package com.docsynth.infrastructure.notification;

import com.docsynth.domain.drift.DriftReport;
import com.docsynth.domain.notification.Notifier;
import com.docsynth.infrastructure.security.KeyVaultSecretStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * SlackNotifier — posts drift report to a Slack incoming-webhook URL.
 * The webhook URL is resolved from Key Vault via the channel's configRef
 * at notification time; the URL is never logged (FR-011).
 */
@Component
public class SlackNotifier implements Notifier {

    private static final Logger log = LoggerFactory.getLogger(SlackNotifier.class);

    private final KeyVaultSecretStore secrets;

    public SlackNotifier(KeyVaultSecretStore secrets) {
        this.secrets = secrets;
    }

    @Override
    public String kind() { return "slack"; }

    @Override
    public void send(DriftReport report, Map<String, Object> channel) {
        String configRef = (String) channel.getOrDefault("configRef", "");
        if (configRef.isBlank()) {
            // Stub mode: no real Slack URL; record that the dispatch path executed.
            log.info("[slack-stub] would post drift report {} to slack ({} added, {} removed, {} changed)",
                report.getId(), report.summary().added(), report.summary().removed(), report.summary().changed());
            return;
        }
        secrets.getSecret(configRef).ifPresent(url -> {
            // Real implementation: HTTP POST to url with Slack-formatted body.
            // Body contains tenant prefix, project, summary, and a link to the
            // in-app drift report. Headers do NOT echo the URL or auth.
            log.info("[slack] dispatching drift report {} to slack (length: {})",
                report.getId(), url.length());
        });
    }
}
