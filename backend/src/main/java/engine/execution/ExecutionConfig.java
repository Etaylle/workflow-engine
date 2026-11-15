package engine.execution;

import engine.shared.SessionKey;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Settings from the environment, and the real builder or the stub when ENGINE_AI_STUB=true.
 * ENGINE_EXECUTION_REAL_BUILDER=true keeps the real builder with stubbed planning (live proof).
 */
@Configuration
class ExecutionConfig {

    @Bean
    ExecutionSettings executionSettings(
            @Value("${engine.execution.workspace-dir:}") String workspaceDir,
            @Value("${engine.execution.template-dir:templates/java-svelte-postgres}") String templateDir,
            @Value("${engine.execution.claude-bin:claude}") String claudeBin,
            @Value("${engine.execution.model:sonnet}") String model,
            @Value("${engine.execution.use-sign-in:false}") boolean useSignIn,
            @Value("${engine.execution.create-database:true}") boolean createDatabase,
            @Value("${engine.execution.cost-limit-usd:10}") BigDecimal costLimit,
            @Value("${engine.execution.step-timeout-minutes:20}") long stepMinutes,
            @Value("${engine.execution.check-timeout-minutes:10}") long checkMinutes,
            @Value("${engine.execution.fix-attempts:2}") int fixAttempts,
            @Value("${engine.execution.max-resumes:3}") int maxResumes,
            @Value("${engine.execution.limit-wait-base-ms:60000}") long limitBaseMs,
            @Value("${engine.execution.limit-wait-max-ms:900000}") long limitMaxMs,
            @Value("${engine.execution.limit-max-retries:6}") int limitMaxRetries) {
        Path root = workspaceDir.isBlank()
                ? Path.of(System.getProperty("user.home"), ".workflow-engine", "workspaces")
                : Path.of(workspaceDir);
        return new ExecutionSettings(root.toAbsolutePath(), Path.of(templateDir).toAbsolutePath(),
                claudeBin, model, useSignIn, createDatabase, costLimit, Duration.ofMinutes(stepMinutes),
                Duration.ofMinutes(checkMinutes), fixAttempts, maxResumes,
                Duration.ofMillis(limitBaseMs), Duration.ofMillis(limitMaxMs), limitMaxRetries);
    }

    @Bean
    @ConditionalOnMissingBean
    Builder builder(@Value("${engine.ai.stub:false}") boolean stub,
            @Value("${engine.execution.real-builder:false}") boolean realBuilder, SessionKey sessionKey,
            ExecutionSettings settings) {
        return stub && !realBuilder ? new StubBuilder() : new ClaudeCodeBuilder(sessionKey, settings);
    }
}
