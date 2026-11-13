// Models of the domain "documents" as the API returns them (camelCase, ISO-8601 times, money as decimal string).
// Placeholder until the API client is generated from api/openapi.yaml (ADR-007); keep names equal.

export type RequirementType = 'functional' | 'quality' | 'constraint';
export type Priority = 'must' | 'should' | 'could' | 'wont';
export type DocumentKind = 'requirements' | 'project_plan';
export type DocumentFormat = 'docx' | 'md' | 'pdf';

/** An epic = one domain of the client software; E-0 is the template setup. (documents.epics) */
export interface Epic {
  id: string;
  projectId: string;
  key: string;
  name: string;
  domainName: string;
  seq: number;
  createdAt: string;
  updatedAt: string;
}

/** One requirement with ID, priority and source. (documents.requirements) */
export interface Requirement {
  id: string;
  projectId: string;
  epicId?: string | null;
  key: string;
  type: RequirementType;
  text: string;
  priority: Priority;
  source?: string | null;
  createdAt: string;
  updatedAt: string;
}

/** Rendered requirements and project plan (insert-only). (documents.generated_documents) */
export interface GeneratedDocument {
  id: string;
  projectId: string;
  kind: DocumentKind;
  docVersion: number;
  fileFormat: DocumentFormat;
  createdAt: string;
}
