package engine.questions.application.ai;

import engine.questions.domain.AnswerType;
import engine.questions.domain.QuestionCategory;
import java.util.List;

/**
 * Structured output of the QUESTIONS AGENT.
 * Each QuestionDraft becomes one row of questions.questions (seq = position in the list).
 * Validation (FR-AGT-05): 10..25 drafts with category REQUIREMENT, at most 5 with category FEATURE;
 * options required for SINGLE_CHOICE and MULTIPLE_CHOICE.
 */
public record QuestionSetOutput(List<QuestionDraft> questions, List<SourceDraft> sources) {

    /** -> questions.questions */
    public record QuestionDraft(QuestionCategory category, AnswerType answerType, String text,
            List<String> options, String aiDefault, String explanation) {
    }

    /** -> questions.research_sources */
    public record SourceDraft(String url, String title, String summary) {
    }
}
