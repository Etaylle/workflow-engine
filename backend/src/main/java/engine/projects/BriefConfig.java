package engine.projects;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Chooses the real brief writer, or the stub when ENGINE_AI_STUB=true. */
@Configuration
class BriefConfig {

    @Bean
    @ConditionalOnMissingBean
    BriefWriter briefWriter(@Value("${engine.ai.stub:false}") boolean stub) {
        return stub ? new StubBriefWriter() : new AiBriefWriter();
    }
}
