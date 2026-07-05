package com.docsynth.domain.drift;

import java.util.List;

public record DriftDiff(
    List<DriftItemRecord> added,
    List<DriftItemRecord> removed,
    List<DriftItemRecord> changed
) {
    public static DriftDiff empty() {
        return new DriftDiff(List.of(), List.of(), List.of());
    }
    public int totalCount() {
        return added.size() + removed.size() + changed.size();
    }
    public int breakingCount() {
        int n = 0;
        for (var r : removed) if ("breaking".equals(r.compatibility())) n++;
        for (var r : changed) if ("breaking".equals(r.compatibility())) n++;
        return n;
    }
    public List<DriftItemRecord> all() {
        var all = new java.util.ArrayList<DriftItemRecord>();
        all.addAll(added);
        all.addAll(removed);
        all.addAll(changed);
        return all;
    }
}
