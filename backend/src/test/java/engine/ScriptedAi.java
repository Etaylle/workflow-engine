package engine;

import engine.projects.Brief;
import engine.projects.BriefWriter;
import engine.projects.StubBriefWriter;
import engine.questions.QuestionDraft;
import engine.questions.QuestionWriter;
import engine.questions.StubQuestionWriter;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

/** Replaces every Claude call by writers that answer from a queue, else with the canteen stub. */
@TestConfiguration
public class ScriptedAi {

    /** Queue of results (a value or a RuntimeException) and the corrections the service sent. */
    public static class Script<T> {
        public final Deque<Object> queue = new ArrayDeque<>();
        public final List<String> corrections = new ArrayList<>();

        @SuppressWarnings("unchecked")
        public T next(String correction, T fallback) {
            corrections.add(correction);
            Object next = queue.poll();
            if (next instanceof RuntimeException e) {
                throw e;
            }
            return next == null ? fallback : (T) next;
        }

        public void reset() {
            queue.clear();
            corrections.clear();
        }
    }

    /** The brief writer of the tests. */
    public static class ScriptedBriefWriter extends Script<Brief> implements BriefWriter {
        @Override
        public Brief write(String apiKey, String wish, String correction) {
            return next(correction, new StubBriefWriter().write(apiKey, wish, correction));
        }
    }

    /** The question writer of the tests. */
    public static class ScriptedQuestionWriter extends Script<QuestionDraft> implements QuestionWriter {
        @Override
        public QuestionDraft write(String apiKey, String wish, String brief, String correction) {
            return next(correction, new StubQuestionWriter().write(apiKey, wish, brief, correction));
        }
    }

    @Bean
    @Primary
    ScriptedQuestionWriter scriptedQuestionWriter() {
        return new ScriptedQuestionWriter();
    }

    @Bean
    @Primary
    ScriptedBriefWriter scriptedBriefWriter() {
        return new ScriptedBriefWriter();
    }
}
