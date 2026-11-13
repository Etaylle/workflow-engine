package engine.execution.domain;

import java.time.Instant;
import java.util.UUID;

/**
 * Evidence: which test proves which criterion.
 * Table: execution.criterion_tests. Plain domain type, no framework annotations (backend.md BKD-DPN-011).
 */
public record CriterionTest(
        UUID id,
        UUID taskRunId,
        String criterionText,
        String testName,
        TestLevel testLevel,
        TestResult result,
        Instant createdAt,
        Instant updatedAt) {
}
