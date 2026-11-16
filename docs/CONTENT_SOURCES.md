# Free content sources (licences verified 2026-11-16)

Goal: reuse existing high-quality explanations instead of writing every lesson by hand.
Licences were read from each repository's licence file / credits page, not assumed.

**Rule:** this project's GitHub repo is **public**, so third-party content is **never committed** here.
It is fetched at runtime into the private database (Phase 4 import), shown with attribution and a link
to the original, and left unmodified where the licence says *NoDerivatives*.

## Importable (open licence)

| Source | What it covers | Licence | Format |
|---|---|---|---|
| [University of Helsinki — Java Programming MOOC](https://java-programming.mooc.fi/) · repo `rage/java-programming` (`data/part-1` … `part-14`) | Core Java from zero: variables → OOP → collections (ArrayList, HashMap) → interfaces → streams, with exercises | CC BY-NC-SA 4.0 (stated in `data/credits.md`) | Markdown |
| `iluwatar/java-design-patterns` | ~200 design patterns, each with intent, real-world analogy, Java code, applicability, trade-offs | MIT | Markdown |
| `donnemartin/system-design-primer` | HLD fundamentals (scalability, caching, DBs, CAP, queues) + worked design problems | CC BY 4.0 | Markdown |
| `spring-guides/gs-*` (e.g. `gs-rest-service`) | Official hands-on Spring Boot guides | Apache-2.0 | AsciiDoc (`README.adoc`) |
| `Snailclimb/JavaGuide` | Very deep Java, JVM, concurrency, MySQL, Redis, distributed systems interview material | Apache-2.0 | Markdown — **Chinese** |
| `ashishps1/awesome-low-level-design` | LLD problems with Java solutions (parking lot, elevator, …) | GPL-3.0 | Markdown + Java |
| `ashishps1/awesome-system-design-resources` | Curated system-design concepts and links | GPL-3.0 | Markdown |

## Personal viewing only — show unmodified, or link

| Source | Covers | Licence |
|---|---|---|
| `karanpratapsingh/system-design` | Complete system design course (fundamentals → case studies) | CC BY-NC-ND 4.0 |
| `ByteByteGoHq/system-design-101` | Visual explanations of backend/system design topics | CC BY-NC-ND 4.0 |
| [roadmap.sh](https://roadmap.sh) (`developer-roadmap`) | Topic summaries + curated links | Personal use only; no republishing → **link only** |

## Link only (no open licence)

| Source | Use for |
|---|---|
| [dev.java](https://dev.java/learn/) (Oracle) | Official Java tutorials incl. JVM, generics, streams, modern Java |
| [Spring reference docs](https://docs.spring.io/) | Spring Framework / Boot / Security / Data internals |
| [PostgreSQL docs](https://www.postgresql.org/docs/), [Redis docs](https://redis.io/docs/), [Kafka docs](https://kafka.apache.org/documentation/) | Databases, caching, messaging |
| Baeldung | Practical Spring/Java articles |

## Coverage gaps

No openly licensed **English** source goes deep on JVM internals, concurrency or Spring internals.
Options: link to dev.java / Spring docs per topic, JavaGuide (Chinese), or hand-written lessons only for those topics.
