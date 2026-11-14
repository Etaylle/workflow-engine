package engine.documents;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Chooses the real requirement writer, or the stub when ENGINE_AI_STUB=true. */
@Configuration
class DocumentConfig {

    @Bean
    @ConditionalOnMissingBean
    RequirementWriter requirementWriter(@Value("${engine.ai.stub:false}") boolean stub) {
        return stub ? new StubRequirementWriter() : new AnthropicRequirementWriter();
    }
}
