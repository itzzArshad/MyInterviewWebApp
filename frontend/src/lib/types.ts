export interface Me {
  username: string;
  displayName: string;
}

// ---------------------------------------------------------------- dashboard
export interface DashboardStats {
  overallProgressPct: number;
  streakDays: number;
  conceptsCompleted: number;
  conceptsTotal: number;
  quizAveragePct: number | null;
  quizzesTaken: number;
  dueForRevision: number;
  interviewReadinessPct: number;
}

export interface SubjectProgress {
  slug: string;
  title: string;
  completed: number;
  published: number;
  total: number;
}

export interface ModuleStatus {
  key: string;
  title: string;
  available: boolean;
  phase: number;
}

export interface ConceptRef {
  slug: string;
  title: string;
  mastery: number;
}

export interface ContinueItem {
  slug: string;
  title: string;
  subjectTitle: string;
  chapterTitle: string;
  started: boolean;
}

export interface Dashboard {
  displayName: string;
  stats: DashboardStats;
  continueLearning: ContinueItem | null;
  subjects: SubjectProgress[];
  weakAreas: ConceptRef[];
  bookmarks: ConceptRef[];
  activeDays: string[]; // ISO dates, newest first
  modules: ModuleStatus[];
}

// ---------------------------------------------------------------- learning
export type Difficulty = "BEGINNER" | "INTERMEDIATE" | "ADVANCED";
export type ProgressStatus = "NOT_STARTED" | "IN_PROGRESS" | "COMPLETED";

export interface ConceptNode {
  slug: string;
  title: string;
  difficulty: Difficulty;
  minutes: number;
  published: boolean;
  progress: ProgressStatus;
  mastery: number;
  bookmarked: boolean;
  questionCount: number;
  quizzed: boolean;
}

export interface ChapterNode {
  slug: string;
  title: string;
  concepts: ConceptNode[];
}

export interface SubjectNode {
  slug: string;
  title: string;
  description: string | null;
  completed: number;
  published: number;
  total: number;
  chapters: ChapterNode[];
}

export interface PathNode {
  slug: string;
  title: string;
  description: string | null;
  subjects: SubjectNode[];
}

export interface Ref {
  slug: string;
  title: string;
}

export interface ProgressView {
  status: ProgressStatus;
  mastery: number;
  bookmarked: boolean;
}

export interface ConceptDetail {
  slug: string;
  title: string;
  difficulty: Difficulty;
  minutes: number;
  summary: string | null;
  bodyMd: string | null;
  published: boolean;
  subject: Ref;
  chapter: Ref;
  progress: ProgressView;
  prev: Ref | null;
  next: Ref | null;
  related: (Ref & { published: boolean })[];
  questionCount: number;
  lastQuiz: { score: number; total: number; finishedAt: string } | null;
  sync: SyncView;
}

// ---------------------------------------------------------------- lesson authoring
export type SyncState = "DISABLED" | "NOT_SYNCED" | "SYNCED" | "FAILED";

export interface SyncView {
  state: SyncState;
  path: string | null;
  url: string | null;
  syncedAt: string | null;
  error: string | null;
}

export interface LessonSource {
  slug: string;
  title: string;
  kind: "CONCEPT" | "LLD_PROBLEM" | "HLD_PROBLEM";
  published: boolean;
  contentSource: "SEED" | "USER";
  markdown: string;
  sync: SyncView;
}

export interface LessonPreview {
  valid: boolean;
  difficulty: Difficulty | null;
  minutes: number | null;
  summary: string | null;
  sections: number;
  expectedSections: number;
  questions: number;
  questionsByType: Record<string, number>;
  body: string;
  errors: string[];
  warnings: string[];
}

export interface SaveResult {
  slug: string;
  questions: number;
  warnings: string[];
  sync: SyncView;
}

export interface GitHubStatus {
  enabled: boolean;
  repo: string | null;
  branch: string;
  url: string | null;
}

export interface BulkResult {
  succeeded: number;
  failed: number;
  messages: string[];
}

// ---------------------------------------------------------------- quiz
export type QuestionType =
  | "MCQ"
  | "TRUE_FALSE"
  | "PREDICT_OUTPUT"
  | "FIND_BUG"
  | "CODE"
  | "SCENARIO"
  | "EXPLAIN"
  | "INTERVIEW";

export interface QuestionView {
  id: number;
  type: QuestionType;
  difficulty: Difficulty;
  prompt: string;
  code: string | null;
  options: string[] | null;
  hints: string[] | null;
  selfGraded: boolean;
  modelAnswer: string | null;
}

export interface AttemptView {
  attemptId: number;
  conceptSlug: string;
  questions: QuestionView[];
}

export interface AnswerResult {
  correct: boolean;
  correctAnswer: string | null;
  explanation: string;
}

export interface FinishResult {
  score: number;
  total: number;
  pct: number;
  mastery: number;
  incorrectQuestionIds: number[];
}
