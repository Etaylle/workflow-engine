package engine.execution;

import engine.shared.SessionKey;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Settings from the environment, and the real builder or the stub when ENGINE_AI_STUB=true. */
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
            @Value("${engine.execution.check-timeout-minutes:10}") long checkMinutes) {
        Path root = workspaceDir.isBlank()
                ? Path.of(System.getProperty("user.home"), ".workflow-engine", "workspaces")
                : Path.of(workspaceDir);
        return new ExecutionSettings(root.toAbsolutePath(), Path.of(templateDir).toAbsolutePath(),
                claudeBin, model, useSignIn, createDatabase, costLimit, Duration.ofMinutes(stepMinutes),
                Duration.ofMinutes(checkMinutes));
    }

    @Bean
    @ConditionalOnMissingBean
    Builder builder(@Value("${engine.ai.stub:false}") boolean stub, SessionKey sessionKey,
            ExecutionSettings settings) {
        return stub ? new StubBuilder() : new ClaudeCodeBuilder(sessionKey, settings);
    }
}
