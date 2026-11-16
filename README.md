# Backend Developer Academy

Private, single-user platform for becoming an interview-ready Java backend developer.
Architecture, schema, API, wireframes, free-tier notes and roadmap: **[docs/ARCHITECTURE.md](docs/ARCHITECTURE.md)**.

| Part | Stack |
|---|---|
| `backend/` | Java 21 · Spring Boot 3.5 · Spring Security (JWT in HttpOnly cookie) · JPA · Flyway · PostgreSQL 17 |
| `frontend/` | Next.js 15 · TypeScript · Tailwind CSS 4 · next-themes · lucide icons |

## Run locally

**0. Local PostgreSQL** (one-time) — PostgreSQL 17 runs as the Windows service `postgresql-x64-17` on port 5432.
Dev and tests use a dedicated `academy` role with two databases: `academy` (dev) and `academy_test` (tests, wiped on every run).
Credentials live in **`backend/.env.local`** (git-ignored; loaded automatically by the `dev` and `test` profiles):

```properties
POSTGRES_SUPERUSER=postgres
POSTGRES_SUPERUSER_PASSWORD=...
ACADEMY_DB_USERNAME=academy
ACADEMY_DB_PASSWORD=...
```

On a new machine: install PostgreSQL 17, create that file, then as `postgres` run
`CREATE ROLE academy LOGIN PASSWORD '...'; CREATE DATABASE academy OWNER academy; CREATE DATABASE academy_test OWNER academy;`

**1. Backend** — needs JDK 21+ (`JAVA_HOME` pointing at it):

```bash
cd backend && ./mvnw spring-boot:run
```

Starts on http://localhost:8080 with the `dev` profile (database `academy`; Flyway applies migrations on startup)
and a dev login of **`me` / `changeme-dev`** (override with `ACADEMY_OWNER_USERNAME` / `ACADEMY_OWNER_PASSWORD`).

**2. Frontend** — needs Node 20+:

```bash
cd frontend && npm install && npm run dev
```

Open http://localhost:3000. `/api/*` is proxied to the backend (`BACKEND_URL`, default `http://localhost:8080`).

## Tests

Backend integration tests run against the real `academy_test` PostgreSQL database (schema dropped and re-migrated per test context):

```bash
cd backend && ./mvnw test
```

```bash
cd frontend && npm run lint && npm run build
```

## Writing lessons

You write lessons **in the app** — no files, no restarts:

1. Open any topic (all 155 are listed in **[docs/TOPICS.md](docs/TOPICS.md)**) → **Copy prompt**.
   The master prompt ([docs/LESSON_PROMPT.md](docs/LESSON_PROMPT.md)) is filled in for that topic and uses the
   right section template (concept / LLD problem / HLD problem).
2. Send it to ChatGPT or Claude and copy the **whole** answer with the chat's copy button.
3. **Add lesson** / **Edit** → paste → the editor checks it live (sections found, quiz questions by type, any
   problems with the exact question and line) → **Save lesson**.

The answer's `~~~quiz` YAML block (at the end) becomes the lesson's quiz; the rest is rendered as the lesson
with highlighted code, tables, callouts and Mermaid diagrams. Each save is stored in PostgreSQL **and** committed
to the private GitHub repo `academy-content` (`<subject>/<chapter>/<slug>.md`). Delete keeps your progress.
**Settings** has *Back up all lessons* and *Restore from GitHub*.

Structure lives in `backend/src/main/resources/curriculum/curriculum.yml` (subject → chapter → topic). Slugs are
permanent IDs for your progress — rename titles freely, never slugs. Bundled starter lessons sit next to it as
`<subject>/<slug>.md` in the same format; once you edit one in the app, the app's version wins.

Prompt templates: `backend/src/main/resources/prompts/` (`lesson-prompt.md` + `sections-*.md`).

## Deploying

See **[docs/DEPLOYMENT.md](docs/DEPLOYMENT.md)** — Vercel (`learn.arshadarif.in`) + Render (API) + Neon (PostgreSQL), all free.

## Status

- [x] **Phase 1 – Foundation**: auth, database + migrations, dashboard, responsive navigation, light/dark theme
- [x] **Phase 2 – Learning Academy**: 155-topic curriculum map, 4 full Java lessons, quiz engine (8 question types, hints, retry incorrect), progress, mastery, bookmarks
- [x] **Lesson authoring & deployment prep**: master prompt + 155-topic list, paste-to-lesson editor (CRUD, live validation,
  quiz from the pasted answer), GitHub mirror + restore, Mermaid diagrams, installable PWA, PostgreSQL everywhere, CI,
  Render/Vercel/Neon deployment config
- [ ] Phase 3 – Personal Notes
- [ ] Phase 4 – GitHub Integration
- [ ] Phase 5 – Advanced Learning & Revision
- [ ] ~~Phase 6 – AI Assistant~~ (skipped — free only)
- [ ] Phase 7 – Stability & Deployment
