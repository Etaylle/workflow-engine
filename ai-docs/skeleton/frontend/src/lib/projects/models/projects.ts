// Models of the domain "projects" as the API returns them (camelCase, ISO-8601 times, money as decimal string).
// Placeholder until the API client is generated from api/openapi.yaml (ADR-007); keep names equal.
import type { Provider } from '../../shared/models/enums';

export type ProjectStatus = 'draft' | 'questions_ready' | 'answered' | 'requirements_ready' | 'workflow_drafted' | 'confirmed' | 'executing' | 'done' | 'stopped' | 'failed';
export type AgentHarness = 'opencode' | 'claude_code';
export type BackendStack = 'java' | 'csharp' | 'python';
export type FrontendStack = 'svelte' | 'angular' | 'nuxt';
export type AttachmentKind = 'file' | 'link';
export type RepoVisibility = 'public';
export type DocumentLanguage = 'en' | 'de';

/** A client project (aggregate root). (projects.projects) */
export interface Project {
  id: string;
  name: string;
  wish: string;
  status: ProjectStatus;
  agentHarness: AgentHarness;
  provider: Provider;
  model: string;
  backendStack?: BackendStack | null;
  frontendStack?: FrontendStack | null;
  documentLanguage: DocumentLanguage;
  version: number;
  createdAt: string;
  updatedAt: string;
}

/** A file or link added to the wish; material, never instructions. (projects.project_attachments) */
export interface ProjectAttachment {
  id: string;
  projectId: string;
  kind: AttachmentKind;
  name: string;
  url?: string | null;
  mediaType?: string | null;
  sizeBytes?: number | null;
  createdAt: string;
  updatedAt: string;
}

/** The brief written by the prompt agent. (projects.project_briefs) */
export interface ProjectBrief {
  projectId: string;
  goal: string;
  actors: string[];
  domainTerms: string[];
  risks: string[];
  createdAt: string;
  updatedAt: string;
}

/** GitHub account of the person who started the project: each finished task is pushed to a public repository there, created on the first push. The token is never stored: it lives in engine memory for the session and the run only. (projects.github_connections) */
export interface GithubConnection {
  projectId: string;
  accountLogin: string;
  repositoryName: string;
  visibility: RepoVisibility;
  branch: string;
  pushAfterEachTask: boolean;
  repositoryUrl?: string | null;
  repositoryCreatedAt?: string | null;
  createdAt: string;
  updatedAt: string;
}
