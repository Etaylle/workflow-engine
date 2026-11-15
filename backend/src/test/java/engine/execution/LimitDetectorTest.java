package engine.execution;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class LimitDetectorTest {

    @Test
    void recognisesTheRateLimitInTheWordsOfTheApiAndOfClaudeCode() {
        assertThat(LimitDetector.classify("API Error: 429 {\"type\":\"rate_limit_error\"}"))
                .isEqualTo("rate_limit");
        assertThat(LimitDetector.classify("Claude usage limit reached|1763200000")).isEqualTo("rate_limit");
        assertThat(LimitDetector.classify("You've hit your limit")).isEqualTo("rate_limit");
        assertThat(LimitDetector.classify("API Error: 529 Overloaded")).isEqualTo("rate_limit");
    }

    @Test
    void recognisesAnExhaustedCreditBeforeTheRateLimit() {
        assertThat(LimitDetector.classify("Credit balance is too low to access the API"))
                .isEqualTo("credit");
        assertThat(LimitDetector.classify("429: credit balance too low")).isEqualTo("credit");
    }

    @Test
    void anOrdinaryFailureIsNotALimit() {
        assertThat(LimitDetector.classify("")).isNull();
        assertThat(LimitDetector.classify("error_max_turns: ran out of turns")).isNull();
        assertThat(LimitDetector.classify("tests failed: 4290 assertions")).isNull();
    }

    @Test
    void onlyAFailedOutcomeOfALimitSubtypeIsLimited() {
        assertThat(new StepOutcome(false, BigDecimal.ZERO, 0, 1, "rate_limit", "x").limited()).isTrue();
        assertThat(new StepOutcome(false, BigDecimal.ZERO, 0, 1, "credit", "x").limited()).isTrue();
        assertThat(new StepOutcome(false, BigDecimal.ZERO, 0, 1, "error_max_turns", "x").limited())
                .isFalse();
        assertThat(new StepOutcome(true, BigDecimal.ZERO, 0, 1, "rate_limit", "x").limited()).isFalse();
    }
}
