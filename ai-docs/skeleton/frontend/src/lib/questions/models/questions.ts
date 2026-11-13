// Models of the domain "questions" as the API returns them (camelCase, ISO-8601 times, money as decimal string).
// Placeholder until the API client is generated from api/openapi.yaml (ADR-007); keep names equal.

export type QuestionCategory = 'requirement' | 'feature';
export type AnswerType = 'single_choice' | 'multiple_choice' | 'text';
export type DecidedBy = 'client' | 'ai';

/** One question of the questions agent. (questions.questions) */
export interface Question {
  id: string;
  projectId: string;
  seq: number;
  category: QuestionCategory;
  answerType: AnswerType;
  text: string;
  options: string[];
  aiDefault?: string | null;
  explanation?: string | null;
  createdAt: string;
  updatedAt: string;
}

/** The answer of the client, or the AI default when the client lets the AI decide. (questions.answers) */
export interface Answer {
  id: string;
  questionId: string;
  value: string[];
  decidedBy: DecidedBy;
  createdAt: string;
  updatedAt: string;
}

/** Sources the questions agent used (insert-only). (questions.research_sources) */
export interface ResearchSource {
  id: string;
  projectId: string;
  url: string;
  title?: string | null;
  summary?: string | null;
  createdAt: string;
}
