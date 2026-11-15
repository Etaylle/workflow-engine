package engine.workflows;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Chooses the real writer, or the stub when ENGINE_AI_STUB=true. */
@Configuration
class WorkflowConfig {

    @Bean
    @ConditionalOnMissingBean
    WorkflowWriter workflowWriter(@Value("${engine.ai.stub:false}") boolean stub) {
        return stub ? new StubWorkflowWriter() : new AiWorkflowWriter();
    }
}
