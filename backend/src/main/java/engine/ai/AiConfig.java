package engine.ai;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Chooses the real connection, or the stub when ENGINE_AI_STUB=true. */
@Configuration
class AiConfig {

    @Bean
    AiConnection aiConnection(@Value("${engine.ai.stub:false}") boolean stub) {
        return stub ? new StubAiConnection() : new RoutingAiConnection();
    }
}
