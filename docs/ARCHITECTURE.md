# Backend Developer Academy — Technical Architecture

> Single-user, private learning platform: structured Java/Spring/backend curriculum, quizzes,
> Notion-like notes, read-only GitHub sync, spaced-repetition revision.
> **No AI assistant** (deliberately excluded to keep everything free).
> Everything here uses free / open-source tooling.

---

## 1. High-level architecture

```
            ┌──────────────── iPhone (PWA) / Windows browser ────────────────┐
            │                                                                │
            ▼                                                                │
  ┌───────────────────────────┐   same-origin /api/* (rewrite proxy)        │
  │  Next.js 15 (Vercel)      │ ─────────────────────────────────┐          │
  │  - App Router, TS, Tailwind│                                  │          │
  │  - Renders lessons/notes   │                                  ▼          │
  │  - Never holds secrets     │                    ┌──────────────────────────┐
  └───────────────────────────┘                    │ Spring Boot 3.5 (Java 21)│
                                                    │  - Spring Security (JWT  │
     GitHub ── webhook (HMAC-signed) ─────────────▶ │    in HttpOnly cookie)   │
     GitHub ◀── REST API (encrypted token) ──────── │  - Spring Data JPA       │
                                                    │  - Flyway migrations     │
                                                    └────────────┬─────────────┘
                                                                 │ JDBC (TLS)
                                                    ┌────────────▼─────────────┐
                                                    │ PostgreSQL (Neon free)   │
                                                    └──────────────────────────┘
                     GitHub Actions (cron) ── pg_dump ──▶ encrypted backup artifact
```

### Key decisions

| Decision | Choice | Why |
|---|---|---|
| Frontend ↔ backend | Next.js `rewrites` proxy `/api/*` to the backend | Cookies become **first-party** → works on iOS Safari (which blocks third-party cookies); no CORS needed. |
| Auth | Single user, BCrypt password, **stateless JWT in HttpOnly + Secure + SameSite=Strict cookie** | Free hosts sleep/restart — stateless tokens survive restarts. HttpOnly prevents XSS token theft. |
| CSRF | SameSite=Strict cookie + JSON-only mutating endpoints | Cross-site forms cannot send the cookie nor `application/json` without CORS preflight (which we never allow). |
| User provisioning | Owner account seeded at startup from env vars; no public sign-up endpoint | Exactly one authorised user. |
| Login brute force | In-memory per-IP rate limit (5 failures / 15 min) | Good enough for a single instance. |
| DB migrations | Flyway, plain SQL compatible with PostgreSQL and H2 (PostgreSQL mode) | Local dev needs no Docker; prod is real Postgres. |
| Curriculum content | Markdown + YAML front-matter files in the repo (`backend/src/main/resources/curriculum/`), loaded into DB on startup by a versioned importer | Content is reviewable in git, diffable, easy to write; DB holds progress & quiz state. |
| Markdown rendering | `react-markdown` + `remark-gfm` + `rehype` + **Shiki** for syntax highlighting | Same renderer for lessons, notes and GitHub files → consistent look. |
| Notes editor (Phase 3) | **Tiptap** (MIT core) with Markdown paste handling; stored as Markdown + JSON | Notion-like blocks, paste-from-ChatGPT → headings/code/lists/tables preserved. |
| Code execution | **Not** on the backend. Phase 2 quizzes use "predict the output" with stored answers; optional later: client-side sandbox only | Requirement: never execute imported code in production. |

---

## 2. Database schema (all phases — created incrementally via Flyway)

```
-- Phase 1 ----------------------------------------------------------------
app_user(id PK, username UNIQUE, password_hash, display_name, created_at, last_login_at)
activity_log(id PK, user_id FK, activity_date DATE, minutes INT, items_completed INT,
             UNIQUE(user_id, activity_date))                     -- powers streak/heatmap

-- Phase 2: Learning academy ---------------------------------------------
learning_path(id PK, slug UNIQUE, title, description, sort_order)
subject(id PK, path_id FK, slug UNIQUE, title, description, icon, sort_order)
chapter(id PK, subject_id FK, slug, title, sort_order, UNIQUE(subject_id, slug))
concept(id PK, chapter_id FK, slug UNIQUE, title, difficulty ENUM(BEGINNER|INTERMEDIATE|ADVANCED),
        body_md TEXT, summary_md TEXT, est_minutes, content_hash, sort_order, updated_at)
concept_dependency(concept_id FK, depends_on_id FK, PK(concept_id, depends_on_id))

quiz_question(id PK, concept_id FK, type ENUM(MCQ|TRUE_FALSE|PREDICT_OUTPUT|FIND_BUG|
              CODE|SCENARIO|EXPLAIN|INTERVIEW), difficulty, prompt_md, code_snippet,
              options_json, correct_answer, explanation_md, hints_json, sort_order)
quiz_attempt(id PK, user_id FK, concept_id FK NULL, chapter_id FK NULL, mode ENUM(CONCEPT|CHAPTER|
             INTERVIEW|REVISION), started_at, finished_at, score, total, time_limit_sec)
quiz_answer(id PK, attempt_id FK, question_id FK, given_answer, is_correct, hints_used,
            self_rating, answered_at)

concept_progress(user_id FK, concept_id FK, status ENUM(NOT_STARTED|IN_PROGRESS|COMPLETED),
                 mastery SMALLINT 0-100, last_viewed_at, completed_at, bookmarked BOOL,
                 PK(user_id, concept_id))

-- Phase 3: Notes -----------------------------------------------------------
note(id PK, user_id FK, parent_id FK NULL (self), title, icon, content_md TEXT,
     content_json JSONB/TEXT, is_favorite, is_pinned, sort_order,
     search_vector TSVECTOR (Postgres only, GIN index), created_at, updated_at, deleted_at)
tag(id PK, user_id FK, name, color, UNIQUE(user_id, name))
note_tag(note_id FK, tag_id FK, PK(note_id, tag_id))

-- Phase 4: GitHub ----------------------------------------------------------
github_connection(id PK, user_id FK UNIQUE, github_login, access_token_enc (AES-GCM),
                  token_iv, scopes, connected_at)
github_repo(id PK, connection_id FK, owner, name, branch, webhook_id, webhook_secret_enc,
            last_synced_sha, last_synced_at, sync_status, UNIQUE(owner, name, branch))
github_file(id PK, repo_id FK, path, blob_sha, language, size_bytes, content TEXT,
            last_commit_sha, last_commit_message, last_commit_author, last_commit_at,
            UNIQUE(repo_id, path))                               -- blob_sha ⇒ no duplicate imports
sync_run(id PK, repo_id FK, trigger ENUM(MANUAL|WEBHOOK|SCHEDULED), from_sha, to_sha,
         files_added, files_updated, files_removed, status, error, started_at, finished_at)

-- Phase 5: Revision / interview ------------------------------------------
review_card(id PK, user_id FK, concept_id FK NULL, question_id FK NULL,
            ease_factor NUMERIC(4,2) DEFAULT 2.5, interval_days, repetitions,
            due_at, last_reviewed_at)                            -- SM-2 spaced repetition
design_problem(id PK, kind ENUM(LLD|HLD), slug, title, body_md, sort_order)
```

Indexes: FKs, `concept_progress(user_id, status)`, `review_card(user_id, due_at)`,
`note(user_id, parent_id)`, `github_file(repo_id, path)`, GIN on `note.search_vector`.

---

## 3. API design (REST, JSON, all under `/api`, all authenticated except login & webhook)

| Phase | Method & path | Purpose |
|---|---|---|
| 1 | `POST /api/auth/login` | `{username,password}` → sets `ACADEMY_TOKEN` cookie |
| 1 | `POST /api/auth/logout` | clears cookie |
| 1 | `GET  /api/auth/me` | current user |
| 1 | `GET  /api/dashboard` | progress summary, streak, continue-learning, readiness |
| 1 | `GET  /actuator/health` | liveness (public) |
| 2 | `GET  /api/learn/tree` · `GET /api/learn/concepts/{slug}` | curriculum tree with progress · lesson |
| 2 | `POST /api/learn/concepts/{slug}/view` · `PUT /api/learn/concepts/{slug}/progress` | mark viewed · completed / bookmark |
| 2 | `GET  /api/quiz/concepts/{slug}/questions` | question previews (answers stripped) |
| 2 | `POST /api/quiz/attempts` · `POST /api/quiz/attempts/{id}/answers` · `POST /api/quiz/attempts/{id}/finish` | quiz engine (server grades, updates mastery) |
| 3 | `GET/POST /api/notes` · `GET/PATCH/DELETE /api/notes/{id}` · `GET /api/notes/tree` · `GET /api/search?q=` | notes CRUD, tree, full-text search (paged) |
| 4 | `GET /api/github/oauth/start` · `GET /api/github/oauth/callback` | GitHub OAuth (state param) |
| 4 | `GET /api/github/repos` · `POST /api/github/tracked` · `POST /api/github/tracked/{id}/sync` | select & sync |
| 4 | `GET /api/github/tracked/{id}/tree` · `GET /api/github/files/{id}` | browse imported content |
| 4 | `POST /api/github/webhook` | push events, verified via `X-Hub-Signature-256` |
| 5 | `GET /api/revision/due` · `POST /api/revision/{cardId}/review` | spaced repetition |
| 5 | `POST /api/interview/sessions` | timed mock interview |

Errors use RFC 9457 `application/problem+json`. Lists are paginated (`?page=&size=`).

---

## 4. UI wireframes

**Desktop**
```
┌──────────────┬───────────────────────────────────────────────┬──────────────┐
│ ◆ Academy    │  🔍 Search…  (Ctrl K)                ☀/☾   ⎋  │              │
│──────────────│───────────────────────────────────────────────│ On this page │
│ ▣ Dashboard  │  Good evening 👋                              │  • What      │
│ ▾ Learn      │  ┌─────────┐┌─────────┐┌─────────┐┌─────────┐  │  • Why       │
│   ▸ Java     │  │Progress ││ Streak  ││ Quiz avg││Readiness│  │  • Internals │
│   ▸ Spring   │  └─────────┘└─────────┘└─────────┘└─────────┘  │  • Code      │
│   ▸ Backend  │  Continue learning ▸ HashMap internals         │  • Interview │
│   ▸ LLD/HLD  │  Subjects  ███████░░░ Java 42%                 │              │
│ ✎ Notes      │            ██░░░░░░░░ Spring 12%               │ (lesson TOC, │
│ ⎇ GitHub     │  Due for revision (5)   Weak areas             │  Phase 2)    │
│ 🎯 Interview │                                                │              │
│ ↻ Revision   │                                                │              │
└──────────────┴───────────────────────────────────────────────┴──────────────┘
```

**Mobile (≤ 768px)**
```
┌─────────────────────────┐
│ ☰  Academy      🔍  ☾   │   ☰ opens sidebar as slide-over drawer
│─────────────────────────│
│ Continue: HashMap ▸     │
│ [Progress] [Streak]     │   cards stack 2-up
│ [Quiz avg] [Readiness]  │
│ Subjects …              │   code blocks scroll horizontally
│─────────────────────────│
│ ▣   📚   ✎   ↻   ⋯      │   bottom tab bar (thumb-reachable)
└─────────────────────────┘
```

**Lesson page (Phase 2)**: breadcrumb → title + difficulty badge → 14 fixed sections
(What / Why / Problem / Before / First principles / Internals / Code / Real-world / Mistakes /
Performance / Interview Q&A / Related / Summary / Quiz) → "Mark complete" → next concept.

---

## 5. Folder structure

```
JavaInterviewWebApp/
├─ docs/ARCHITECTURE.md            ← this file
├─ backend/                        Spring Boot (Maven wrapper)
│  └─ src/main/java/com/academy/
│     ├─ AcademyApplication.java
│     ├─ config/                   SecurityConfig, JwtConfig, properties
│     ├─ auth/                     AuthController, JwtService, LoginRateLimiter, OwnerSeeder
│     ├─ user/                     AppUser entity + repository
│     ├─ dashboard/                DashboardController/Service
│     ├─ common/                   GlobalExceptionHandler, error DTOs
│     ├─ curriculum/  (P2)  quiz/ (P2)  notes/ (P3)  github/ (P4)  revision/ (P5)
│  └─ src/main/resources/
│     ├─ application.yml, application-dev.yml, application-prod.yml
│     └─ db/migration/V1__init.sql …
├─ frontend/                       Next.js 15 + TS + Tailwind 4
│  └─ src/
│     ├─ app/(auth)/login/         login page
│     ├─ app/(app)/                authenticated shell: layout with sidebar
│     │   ├─ dashboard/ learn/ notes/ github/ interview/ revision/
│     ├─ components/               Sidebar, Topbar, MobileNav, ThemeToggle, ui/*
│     ├─ lib/                      api client, types, nav config
│     └─ middleware.ts             redirects to /login when no session cookie
    (curriculum content lives in backend/src/main/resources/curriculum/ — see README "Writing lessons")
```

---

## 6. Free services & their limitations

> Free-tier terms change often — re-check before deploying (Phase 7).

| Service | Use | Free-tier limits to be aware of |
|---|---|---|
| **Vercel Hobby** | Next.js frontend | Non-commercial use only; function/bandwidth quotas; fine for one user. |
| **Neon (Postgres)** | Production DB | ~0.5 GB storage per project; compute **auto-suspends when idle** (first query after idle has ~1 s cold start); monthly compute-hour cap. Point-in-time restore window is short → we add our own backups. |
| Supabase (alt.) | Postgres | 500 MB; **project pauses after ~7 days of inactivity** (must be resumed manually) — worse fit than Neon for a personal app you might not open for a week. |
| **Render free web service** | Spring Boot (Docker) | 512 MB RAM; **sleeps after ~15 min idle, ~30–60 s cold start**; monthly instance-hour cap. Simplest option. |
| **Oracle Cloud Always Free** | Spring Boot (best perf) | Ampere ARM VM up to 4 OCPU / 24 GB — no sleeping. Needs a card for identity verification; idle instances can be reclaimed; you manage the VM, TLS (Caddy) and updates yourself. |
| Koyeb free (alt.) | Spring Boot | One small instance, scales to zero. |
| **GitHub** | OAuth App, REST API, webhooks, Actions | 5,000 API req/h authenticated; Actions free minutes for private repos (2,000/mo) — enough for nightly `pg_dump` backups. |
| Shiki / Tiptap core / react-markdown | rendering & editor | MIT, free. (Tiptap "Pro" extensions are paid — we don't use them.) |

**Recommended free combo:** Vercel + Render (start) → Oracle Always Free (if cold starts annoy you) + Neon + GitHub Actions backups.

Backup strategy: nightly GitHub Action runs `pg_dump`, encrypts with `age`/GPG, stores as an
Actions artifact (90-day retention) in a **private** repo; Flyway guarantees schema can be recreated;
restore drill documented in Phase 7.

---

## 7. Security checklist (applied from Phase 1)

- Passwords: BCrypt (cost 12). Owner credentials only via env vars, never committed.
- JWT: HS256 with ≥ 256-bit secret from env; 7-day expiry; HttpOnly, Secure (prod), SameSite=Strict.
- No sign-up endpoint; every `/api/**` route requires auth except login, health, GitHub webhook (HMAC-verified).
- GitHub token encrypted at rest (AES-256-GCM, key from env) — Phase 4.
- Bean Validation on all request DTOs; global exception handler never leaks stack traces.
- Rate-limited login; security headers (HSTS, X-Content-Type-Options, frame-deny, CSP on frontend).
- Imported code is **displayed only, never executed**.

---

## 8. Development roadmap

| Phase | Deliverable | "Done" means |
|---|---|---|
| **1 Foundation** ✅ | Backend + frontend skeleton, single-user auth, DB + migrations, dashboard, responsive navigation, light/dark theme | Can log in on laptop & phone viewport, see dashboard, log out; backend tests pass |
| **2 Learning Academy** ✅ | Curriculum model, content importer, lesson page (14 sections), Shiki code blocks, quiz engine, progress tracking, first Java chapters | Learn a concept end-to-end and take its quiz; dashboard reflects progress |
| 3 Personal Notes | Tiptap editor, ChatGPT paste → formatted blocks, nested pages, tags, favorites, search, auto-save | Paste a ChatGPT answer and it renders correctly; search finds it |
| 4 GitHub | OAuth connect, repo/branch selection, import md/java, tree view, webhook sync, manual sync | Push a commit → file updates in app without duplicates |
| 5 Advanced learning | Spring/Boot/Security, backend eng., LLD & HLD content, interview mode, SM-2 revision | Timed mock interview works; due-cards list drives revision |
| 6 ~~AI Assistant~~ | **Skipped** (would require paid APIs) | — |
| 7 Stability & deploy | Security & perf testing, backups, monitoring, deploy, PWA install on iPhone | App installed on iPhone home screen, nightly backup verified |
