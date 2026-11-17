# Master prompt for lessons

> **Easiest:** open any topic in the app and press **Copy prompt** — it is this prompt with the topic, chapter,
> neighbouring topics and the correct section template already filled in.

## How to use it

1. Copy the prompt below and replace the `<…>` placeholders (topic titles are in [TOPICS.md](TOPICS.md)).
2. Send it to ChatGPT or Claude (a reasoning-capable model gives the most accurate internals).
3. Copy the **whole** answer with the chat's copy button (it copies raw Markdown — selecting the rendered text loses formatting).
4. In the app open the topic → **Add lesson** (or **Edit**) → paste → check the live preview → **Save lesson**.
   The app removes the `~~~quiz` block from the lesson body and turns it into the lesson's quiz.
5. The lesson is stored in PostgreSQL and committed to your private `academy-content` GitHub repo.

If the editor reports a quiz problem (it names the question and line), ask the chat: *"Fix only the quiz YAML: <error>"* and paste again.

**LLD and HLD problems** (chapters *LLD Practice Problems* / *HLD Practice Problems*) use different section lists —
the in-app **Copy prompt** switches automatically. The templates are in
`backend/src/main/resources/prompts/` (`sections-concept.md`, `sections-lld.md`, `sections-hld.md`).

## The prompt (concept lessons)

````text
You are a senior Java backend engineer and an exceptional teacher. Write ONE complete lesson for my personal interview-preparation academy.

TOPIC: <TOPIC TITLE>
Where it sits: <SUBJECT> → <CHAPTER>
Lesson type: Concept lesson
Neighbouring topics in my curriculum (for context and cross-references only — do not teach them here): <other topics in the same chapter>

GOAL
Teach this topic from first principles so I understand how it really works internally and can explain it confidently in a technical interview and use it in real backend work. Progress from beginner → intermediate → advanced. No superficial definitions, no filler. Be technically precise; when behaviour depends on a Java / Spring / library version, name the version. Prefer what official documentation says (OpenJDK, Oracle Java docs, Spring docs, PostgreSQL docs, etc.). If you are unsure about a detail, say so instead of guessing.

OUTPUT FORMAT — follow it exactly, my app parses your answer automatically
1. Output raw Markdown only. Do NOT wrap the whole answer in a code block. Write nothing before the front matter and nothing after the quiz block.
2. Start with this YAML front matter (fill in the values):
---
title: <TOPIC TITLE>
difficulty: BEGINNER | INTERMEDIATE | ADVANCED   ← pick exactly one
minutes: <estimated reading time as an integer>
summary: <one sentence, max 25 words, that makes me want to read the lesson>
---
3. Then use exactly these level-2 headings, in this order, numbered exactly as shown (the text after "→" is guidance for you — do not copy it into the heading):
## 1. What is this concept?  → plain-language definition, then a precise one; a small example
## 2. Why was it introduced?  → the history / motivation
## 3. What problem does it solve?  → concrete pain points it removes
## 4. How did developers solve this before?  → the older approach and its drawbacks
## 5. Understand it from first principles  → build the idea up from basics, step by step
## 6. Internal working and implementation details  → what happens under the hood (JVM / library / framework internals), with a diagram
## 7. Syntax and practical Java code examples  → runnable, realistic examples from simple to advanced
## 8. Real-world backend development use cases  → where it appears in Spring Boot services, APIs, databases, production
## 9. Common mistakes and misconceptions  → bugs and wrong mental models, with fixes
## 10. Performance considerations and best practices  → costs, complexity, tuning, rules of thumb
## 11. Interview questions and answers  → 6–10 questions from basic to senior level, each with key points and follow-ups (format below)
## 12. Related concepts and dependencies  → what to learn before and after, and how they connect
## 13. Summary and revision notes  → crisp bullet-point cheat sheet for quick revision
   You may use ### sub-headings inside any section.
4. Formatting rules:
   - Every code sample goes in a fenced block with a language tag: ```java, ```sql, ```yaml, ```bash, ```json, ```xml, ```text.
   - Java code must compile on Java 21 (unless it deliberately shows a bug — then say so). Include imports when they are not obvious.
   - Use tables for comparisons, > blockquotes for key insights and one-line interview answers, **bold** for key terms.
   - Diagrams: use ```mermaid blocks (flowchart, sequenceDiagram, classDiagram, erDiagram) or ASCII art inside ```text.
   - Interview section — my app turns it into spoken-answer practice, so use exactly this shape for every question:

     **Q1. The question, as an interviewer would ask it?**

     The strong answer in normal paragraphs, structured as: what it is → why it exists → how it works inside → a concrete example → trade-offs or pitfalls.

     - Key points: first point the answer must cover | second point | third point | fourth point
     - Follow-up: The deeper question the interviewer asks next? → Its short, precise answer

     Give 4–6 key points (short phrases, separated by " | ") and 1–2 follow-ups per question. Number questions Q1, Q2, … and keep the "**Q1. …**" line on its own.
5. Finish with the heading "## 14. Quiz" followed by exactly ONE quiz block. Fence it with TILDES (~~~quiz … ~~~), not backticks. Inside, write YAML in exactly this schema:

~~~quiz
questions:
  - key: q1
    type: MCQ
    difficulty: BEGINNER
    prompt: |
      The question (Markdown allowed).
    code: |
      // optional code shown with the question — omit the whole field if not needed
    options:
      - "First option"
      - "Second option"
      - "Third option"
      - "Fourth option"
    answer: B
    hints:
      - "A small nudge that does not give the answer away"
    explanation: |
      Why the correct answer is right AND why the tempting wrong options are wrong.
~~~

QUIZ RULES
- 10 to 12 questions ordered from easiest to hardest. Keys q1, q2, q3 … (unique).
- Mix: at least 4 MCQ, 1 TRUE_FALSE, 2 PREDICT_OUTPUT (use 2 SCENARIO instead if the topic has no code), 1 FIND_BUG, 1 SCENARIO, 1 EXPLAIN, 1 INTERVIEW. Optionally 1 CODE.
- `type` is one of: MCQ, TRUE_FALSE, PREDICT_OUTPUT, FIND_BUG, SCENARIO, CODE, EXPLAIN, INTERVIEW.
- `difficulty` is one of: BEGINNER, INTERMEDIATE, ADVANCED.
- MCQ, FIND_BUG, SCENARIO: 3–5 options; `answer` is the LETTER of the single correct option (A, B, C, …).
- TRUE_FALSE: no options; `answer: true` or `answer: false`.
- PREDICT_OUTPUT: either options + a letter answer, or no options and `answer: |` containing the exact console output.
- EXPLAIN, INTERVIEW, CODE: no options and no answer. Put a complete model answer / reference solution in `explanation` — I grade myself against it.
  Also add `keyPoints:` — a list of 4–6 short quoted phrases the answer must cover (my app shows them as a self-scoring checklist).
- Every question has 1–2 hints and an explanation.
- Test understanding, not memorisation: internals, trade-offs, edge cases, debugging, and realistic backend scenarios.
- YAML safety: use `|` block scalars for prompt, code, multi-line answers and explanation. Wrap every option and hint in double quotes (escape inner double quotes as \"). Indent with 2 spaces, never tabs. Inside the quiz block you may use ``` code fences in explanations, but never start a line with ~~~.

Now write the complete lesson for: <TOPIC TITLE>
````
