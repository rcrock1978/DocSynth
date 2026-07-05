package com.docsynth.infrastructure.notification;

import com.docsynth.domain.drift.DriftReport;
import com.docsynth.domain.notification.Notifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * CiCheckNotifier — returns a failing check status with the drift
 * report URL and severity counts. v1 stub logs the dispatch.
 */
@Component
public class CiCheckNotifier implements Notifier {

    private static final Logger log = LoggerFactory.getLogger(CiCheckNotifier.class);

    @Override
    public String kind() { return "ci_check"; }

    @Override
    public void send(DriftReport report, Map<String, Object> channel) {
        boolean isBreaking = report.summary().breaking() > 0;
        log.info("[ci-check-stub] would post check result: {} ({} breaking)",
            isBreaking ? "failure" : "success", report.summary().breaking());
    }
}
