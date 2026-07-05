package com.docsynth.infrastructure.notification;

import com.docsynth.domain.drift.DriftReport;
import com.docsynth.domain.notification.Notifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * EmailNotifier — sends drift summary via SMTP. v1 stub: logs dispatch.
 * Real implementation uses Spring's JavaMailSender; secrets (SMTP
 * password) are resolved from Key Vault at send time.
 */
@Component
public class EmailNotifier implements Notifier {

    private static final Logger log = LoggerFactory.getLogger(EmailNotifier.class);

    @Override
    public String kind() { return "email"; }

    @Override
    public void send(DriftReport report, Map<String, Object> channel) {
        log.info("[email-stub] would email drift report {} ({} added, {} removed, {} changed)",
            report.getId(), report.summary().added(), report.summary().removed(), report.summary().changed());
    }
}
