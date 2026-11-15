package engine.questions;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Chooses the real question writer, or the stub when ENGINE_AI_STUB=true. */
@Configuration
class QuestionConfig {

    @Bean
    @ConditionalOnMissingBean
    QuestionWriter questionWriter(@Value("${engine.ai.stub:false}") boolean stub) {
        return stub ? new StubQuestionWriter() : new AiQuestionWriter();
    }
}
