package com.docsynth.domain.notification;

import com.docsynth.domain.drift.DriftReport;

public interface Notifier {
    String kind(); // slack | email | ci_check
    void send(com.docsynth.domain.drift.DriftReport report, java.util.Map<String, Object> channel);
    default boolean supports(String kind) { return kind().equals(kind); }
}
