package engine.execution;

import java.math.BigDecimal;

/** One development step of a profile: what Claude is told and how the orchestrator checks it. */
record StepDef(String key, String title, boolean gated, BigDecimal capUsd, String prompt) {
}
