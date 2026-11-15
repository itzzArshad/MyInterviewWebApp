# Backend Developer Academy

Private, single-user platform for becoming an interview-ready Java backend developer.
Architecture, schema, API, wireframes, free-tier notes and roadmap: **[docs/ARCHITECTURE.md](docs/ARCHITECTURE.md)**.

| Part | Stack |
|---|---|
| `backend/` | Java 21 · Spring Boot 3.5 · Spring Security (JWT in HttpOnly cookie) · JPA · Flyway · PostgreSQL (H2 locally) |
| `frontend/` | Next.js 15 · TypeScript · Tailwind CSS 4 · next-themes · lucide icons |

## Run locally (no Docker needed)

**1. Backend** — needs JDK 21+ (`JAVA_HOME` pointing at it):

```bash
cd backend && ./mvnw spring-boot:run
```

Starts on http://localhost:8080 with the `dev` profile: file-based H2 database in `backend/data/`,
and a dev login of **`me` / `changeme-dev`** (override with `ACADEMY_OWNER_USERNAME` / `ACADEMY_OWNER_PASSWORD`).

**2. Frontend** — needs Node 20+:

```bash
cd frontend && npm install && npm run dev
```

Open http://localhost:3000. `/api/*` is proxied to the backend (`BACKEND_URL`, default `http://localhost:8080`).

## Tests

```bash
cd backend && ./mvnw test
```

```bash
cd frontend && npm run lint && npm run build
```

## Writing lessons

Content lives in `backend/src/main/resources/curriculum/` and is imported at startup
(changed files are detected by hash; your progress is never deleted).

1. **Map** — `curriculum.yml` lists every subject → chapter → concept. A concept without a lesson file shows as *Planned*.
   Slugs are permanent progress IDs: rename titles freely, never slugs.
2. **Lesson** — `<subject>/<concept-slug>.md`:
   ```markdown
   ---
   title: HashMap internal implementation
   difficulty: ADVANCED          # BEGINNER | INTERMEDIATE | ADVANCED
   minutes: 35
   summary: One-sentence hook shown under the title.
   related: [equals-hashcode, map-overview]
   ---
   ## 1. What is this concept?
   …  sections 1–13 (a test enforces all 13 headings; the app adds "14. Quiz")
   ```
3. **Quiz** — `<subject>/<concept-slug>.quiz.yml` (at least 5 questions):
   ```yaml
   questions:
     - key: q1                   # stable id, never reuse
       type: MCQ                 # MCQ | TRUE_FALSE | PREDICT_OUTPUT | FIND_BUG | SCENARIO | CODE | EXPLAIN | INTERVIEW
       difficulty: BEGINNER
       prompt: Markdown question
       code: |                   # optional snippet (highlighted)
         System.out.println(1);
       options: [A, B, C]        # optional; then `answer` is the 0-based index
       answer: 1                 # true/false for TRUE_FALSE; exact text for PREDICT_OUTPUT without options;
                                 # omit for self-graded questions (EXPLAIN / INTERVIEW / CODE)
       hints: [first nudge, bigger nudge]
       explanation: |            # shown after answering (model answer for self-graded)
         Why the answer is right.
   ```

Restart the backend to load changes. Malformed content fails startup with a message naming the file.

## Status

- [x] **Phase 1 – Foundation**: auth, database + migrations, dashboard, responsive navigation, light/dark theme
- [x] **Phase 2 – Learning Academy**: 155-topic curriculum map, 4 full Java lessons, quiz engine (8 question types, hints, retry incorrect), progress, mastery, bookmarks
- [ ] Phase 3 – Personal Notes
- [ ] Phase 4 – GitHub Integration
- [ ] Phase 5 – Advanced Learning & Revision
- [ ] ~~Phase 6 – AI Assistant~~ (skipped — free only)
- [ ] Phase 7 – Stability & Deployment
