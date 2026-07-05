package com.docsynth.infrastructure.drift;

import com.docsynth.domain.drift.*;
import com.docsynth.domain.ingestion.EndpointDescriptor;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A6 guardrail: structural drift only. Description-only edits classify
 * as `informational`, NEVER as `breaking` or `non_breaking` for the
 * schema or operationId target kind.
 */
class InformationalScopeTest {

    private final OpenApiDiffAdapter adapter = new OpenApiDiffAdapter();

    @Test
    void description_only_edit_is_informational() {
        EndpointDescriptor before = new EndpointDescriptor(
            "GET", "/users", "listUsers", "List users",
            "Original description.",
            List.of(), Map.of(), null, Map.of(), List.of(), false
        );
        EndpointDescriptor after = new EndpointDescriptor(
            "GET", "/users", "listUsers", "List users",
            "Edited description with more context.",
            List.of(), Map.of(), null, Map.of(), List.of(), false
        );
        DriftDiff diff = adapter.diffTwo(before, after);
        assertThat(diff.changed()).isNotEmpty();
        for (DriftItemRecord item : diff.changed()) {
            assertThat(item.compatibility())
                .as("description-only edits must be informational, not breaking/non_breaking")
                .isEqualTo("informational");
        }
    }

    @Test
    void removing_required_field_is_breaking() {
        EndpointDescriptor before = new EndpointDescriptor(
            "POST", "/users", "createUser", "Create user",
            "Creates a user.",
            List.of(), Map.of(),
            Map.of("required", true, "fields", List.of("email", "name")),
            Map.of(), List.of(), false
        );
        EndpointDescriptor after = new EndpointDescriptor(
            "POST", "/users", "createUser", "Create user",
            "Creates a user.",
            List.of(), Map.of(),
            Map.of("required", true, "fields", List.of("email")),
            Map.of(), List.of(), false
        );
        DriftDiff diff = adapter.diffTwo(before, after);
        assertThat(diff.changed())
            .anyMatch(item -> "breaking".equals(item.compatibility()));
    }
}
