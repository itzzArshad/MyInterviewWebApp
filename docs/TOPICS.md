# All topics

Every topic in the curriculum, in learning order. Generate a lesson for any of them with the master prompt
([LESSON_PROMPT.md](LESSON_PROMPT.md)) — or, easier, open the topic in the app and press **Copy prompt**,
which fills in the topic, its chapter and the right section template for you.

App URL for a topic: `https://learn.arshadarif.in/learn/<slug>` · editor: `…/learn/<slug>/edit`

**155 topics** — 4 written so far.


## Core Java

The language and the runtime underneath it — how Java really works.

### Fundamentals & the JVM

| # | Topic | Slug | Template | Status |
|---|---|---|---|---|
| 1 | JDK, JRE and JVM | `jdk-jre-jvm` | Concept | ✅ written |
| 2 | JVM architecture | `jvm-architecture` | Concept | planned |
| 3 | Data types and variables | `data-types-variables` | Concept | planned |
| 4 | Operators and expressions | `operators` | Concept | planned |

### Object-Oriented Programming

| # | Topic | Slug | Template | Status |
|---|---|---|---|---|
| 5 | OOP principles | `oop-principles` | Concept | planned |
| 6 | Classes, objects and constructors | `classes-objects-constructors` | Concept | planned |
| 7 | Inheritance | `inheritance` | Concept | planned |
| 8 | Polymorphism | `polymorphism` | Concept | planned |
| 9 | Abstraction and encapsulation | `abstraction-encapsulation` | Concept | planned |
| 10 | Interfaces vs abstract classes | `interfaces-vs-abstract-classes` | Concept | planned |

### Strings

| # | Topic | Slug | Template | Status |
|---|---|---|---|---|
| 11 | Strings and immutability | `string-immutability` | Concept | ✅ written |
| 12 | StringBuilder and StringBuffer | `stringbuilder` | Concept | planned |

### Exception Handling

| # | Topic | Slug | Template | Status |
|---|---|---|---|---|
| 13 | Exception hierarchy | `exception-hierarchy` | Concept | planned |
| 14 | Checked vs unchecked exceptions | `checked-vs-unchecked` | Concept | planned |
| 15 | try-with-resources | `try-with-resources` | Concept | planned |

### Collections Framework

| # | Topic | Slug | Template | Status |
|---|---|---|---|---|
| 16 | Collections Framework overview | `collections-overview` | Concept | planned |
| 17 | List, ArrayList and LinkedList | `list-arraylist-linkedlist` | Concept | planned |
| 18 | Set implementations | `set-implementations` | Concept | planned |
| 19 | Map implementations | `map-overview` | Concept | planned |
| 20 | equals() and hashCode() | `equals-hashcode` | Concept | ✅ written |
| 21 | HashMap internal implementation | `hashmap-internals` | Concept | ✅ written |
| 22 | Queue and Deque | `queue-deque` | Concept | planned |
| 23 | Comparable and Comparator | `comparable-comparator` | Concept | planned |
| 24 | Fail-fast vs fail-safe iterators | `fail-fast-fail-safe` | Concept | planned |

### Generics

| # | Topic | Slug | Template | Status |
|---|---|---|---|---|
| 25 | Generics basics | `generics-basics` | Concept | planned |
| 26 | Type erasure | `type-erasure` | Concept | planned |
| 27 | Wildcards and PECS | `wildcards-pecs` | Concept | planned |

### Java 8 and Functional Programming

| # | Topic | Slug | Template | Status |
|---|---|---|---|---|
| 28 | Lambda expressions | `lambda-expressions` | Concept | planned |
| 29 | Functional interfaces | `functional-interfaces` | Concept | planned |
| 30 | Method references | `method-references` | Concept | planned |
| 31 | Stream API | `stream-api` | Concept | planned |
| 32 | Stream internals and laziness | `stream-internals` | Concept | planned |
| 33 | Optional | `optional` | Concept | planned |

### Multithreading and Concurrency

| # | Topic | Slug | Template | Status |
|---|---|---|---|---|
| 34 | Threads and the thread lifecycle | `threads-basics` | Concept | planned |
| 35 | synchronized and intrinsic locks | `synchronization` | Concept | planned |
| 36 | volatile and the Java Memory Model | `volatile-jmm` | Concept | planned |
| 37 | Lock, ReentrantLock and ReadWriteLock | `locks` | Concept | planned |
| 38 | ExecutorService and thread pools | `executor-service` | Concept | planned |
| 39 | Concurrent collections | `concurrent-collections` | Concept | planned |
| 40 | CompletableFuture | `completable-future` | Concept | planned |
| 41 | Virtual threads | `virtual-threads` | Concept | planned |

### JVM Internals

| # | Topic | Slug | Template | Status |
|---|---|---|---|---|
| 42 | JVM memory areas | `jvm-memory` | Concept | planned |
| 43 | Garbage collection | `garbage-collection` | Concept | planned |
| 44 | Class loading | `class-loading` | Concept | planned |
| 45 | Reflection | `reflection` | Concept | planned |
| 46 | Annotations | `annotations` | Concept | planned |

### Modern Java

| # | Topic | Slug | Template | Status |
|---|---|---|---|---|
| 47 | Records | `records` | Concept | planned |
| 48 | Sealed classes | `sealed-classes` | Concept | planned |
| 49 | Pattern matching (instanceof and switch) | `pattern-matching` | Concept | planned |
| 50 | var and text blocks | `text-blocks-var` | Concept | planned |

## Spring & Spring Boot

Dependency injection to production-grade REST APIs, data access and security.

### Spring Framework Core

| # | Topic | Slug | Template | Status |
|---|---|---|---|---|
| 51 | Why Spring was introduced | `why-spring` | Concept | planned |
| 52 | Inversion of Control and Dependency Injection | `ioc-di` | Concept | planned |
| 53 | The Spring container and ApplicationContext | `spring-container` | Concept | planned |
| 54 | Bean lifecycle | `bean-lifecycle` | Concept | planned |
| 55 | Bean scopes | `bean-scopes` | Concept | planned |
| 56 | Component scanning and configuration | `component-scanning-config` | Concept | planned |
| 57 | Autowiring | `autowiring` | Concept | planned |
| 58 | Spring AOP | `spring-aop` | Concept | planned |
| 59 | Proxies (JDK dynamic vs CGLIB) | `spring-proxies` | Concept | planned |
| 60 | Spring MVC and the DispatcherServlet | `spring-mvc` | Concept | planned |

### Spring Boot

| # | Topic | Slug | Template | Status |
|---|---|---|---|---|
| 61 | Why Spring Boot exists | `why-spring-boot` | Concept | planned |
| 62 | Auto-configuration internals | `auto-configuration` | Concept | planned |
| 63 | Starter dependencies | `starters` | Concept | planned |
| 64 | Spring Boot application lifecycle | `boot-lifecycle` | Concept | planned |
| 65 | Properties, YAML and profiles | `properties-yaml-profiles` | Concept | planned |
| 66 | REST API development | `rest-controllers` | Concept | planned |
| 67 | Controllers, services and repositories | `layered-architecture` | Concept | planned |
| 68 | DTOs and entity mapping | `dtos-mapping` | Concept | planned |
| 69 | Validation | `validation` | Concept | planned |
| 70 | Global exception handling | `global-exception-handling` | Concept | planned |
| 71 | Logging | `logging` | Concept | planned |
| 72 | Actuator | `actuator` | Concept | planned |
| 73 | Testing Spring Boot applications | `testing-spring-boot` | Concept | planned |

### Spring Data JPA and Hibernate

| # | Topic | Slug | Template | Status |
|---|---|---|---|---|
| 74 | JPA and Spring Data JPA | `jpa-basics` | Concept | planned |
| 75 | Hibernate internals and the persistence context | `hibernate-internals` | Concept | planned |
| 76 | Transactions and @Transactional | `transactions-spring` | Concept | planned |
| 77 | Lazy and eager loading | `lazy-eager-loading` | Concept | planned |
| 78 | The N+1 query problem | `n-plus-one` | Concept | planned |
| 79 | Pagination and sorting | `pagination-sorting` | Concept | planned |
| 80 | Connection pooling with HikariCP | `connection-pooling-hikari` | Concept | planned |

### Spring Security

| # | Topic | Slug | Template | Status |
|---|---|---|---|---|
| 81 | Authentication and authorization | `authn-vs-authz` | Concept | planned |
| 82 | The security filter chain | `security-filter-chain` | Concept | planned |
| 83 | SecurityContext | `security-context` | Concept | planned |
| 84 | Password encoding | `password-encoding` | Concept | planned |
| 85 | JWT authentication | `jwt-auth` | Concept | planned |
| 86 | OAuth 2.0 | `oauth2` | Concept | planned |
| 87 | Role-based and method-level security | `rbac-method-security` | Concept | planned |
| 88 | CORS and CSRF | `cors-csrf` | Concept | planned |

## Backend Engineering

Networking, APIs, databases, messaging and distributed-systems building blocks.

### Web and Networking Fundamentals

| # | Topic | Slug | Template | Status |
|---|---|---|---|---|
| 89 | HTTP and HTTPS | `http-https` | Concept | planned |
| 90 | HTTP methods and status codes | `http-methods-status` | Concept | planned |
| 91 | DNS and TCP/IP | `dns-tcp-ip` | Concept | planned |
| 92 | Cookies, sessions and tokens | `cookies-sessions-tokens` | Concept | planned |
| 93 | Reverse proxies and load balancers | `reverse-proxy-load-balancer` | Concept | planned |

### API Design

| # | Topic | Slug | Template | Status |
|---|---|---|---|---|
| 94 | REST API design | `rest-api-design` | Concept | planned |
| 95 | API versioning | `api-versioning` | Concept | planned |
| 96 | Idempotency | `idempotency` | Concept | planned |
| 97 | Rate limiting | `rate-limiting` | Concept | planned |

### Databases

| # | Topic | Slug | Template | Status |
|---|---|---|---|---|
| 98 | Relational database concepts | `relational-concepts` | Concept | planned |
| 99 | SQL essentials | `sql-essentials` | Concept | planned |
| 100 | Database indexing | `indexing` | Concept | planned |
| 101 | Query optimization | `query-optimization` | Concept | planned |
| 102 | ACID properties | `acid` | Concept | planned |
| 103 | Isolation levels | `isolation-levels` | Concept | planned |
| 104 | Normalization and denormalization | `normalization` | Concept | planned |

### Caching

| # | Topic | Slug | Template | Status |
|---|---|---|---|---|
| 105 | Caching strategies | `caching-strategies` | Concept | planned |
| 106 | Redis fundamentals | `redis` | Concept | planned |

### Messaging

| # | Topic | Slug | Template | Status |
|---|---|---|---|---|
| 107 | Synchronous vs asynchronous communication | `sync-vs-async` | Concept | planned |
| 108 | Message queues | `message-queues` | Concept | planned |
| 109 | Kafka fundamentals | `kafka` | Concept | planned |
| 110 | RabbitMQ fundamentals | `rabbitmq` | Concept | planned |

### Microservices and Resilience

| # | Topic | Slug | Template | Status |
|---|---|---|---|---|
| 111 | Microservices architecture | `microservices-architecture` | Concept | planned |
| 112 | Service discovery | `service-discovery` | Concept | planned |
| 113 | API gateways | `api-gateway` | Concept | planned |
| 114 | Distributed transactions and sagas | `distributed-transactions` | Concept | planned |
| 115 | Event-driven architecture | `event-driven-architecture` | Concept | planned |
| 116 | Retries and circuit breakers | `retries-circuit-breakers` | Concept | planned |
| 117 | Observability: logs, metrics and traces | `observability` | Concept | planned |

### Delivery and Operations

| # | Topic | Slug | Template | Status |
|---|---|---|---|---|
| 118 | Git and GitHub | `git-github` | Concept | planned |
| 119 | Docker | `docker` | Concept | planned |
| 120 | CI/CD | `ci-cd` | Concept | planned |
| 121 | Cloud fundamentals | `cloud-fundamentals` | Concept | planned |
| 122 | Deployment and monitoring | `deployment-monitoring` | Concept | planned |

## System Design

Low-level object design and high-level distributed architecture.

### Low-Level Design Principles

| # | Topic | Slug | Template | Status |
|---|---|---|---|---|
| 123 | SOLID principles | `solid` | Concept | planned |
| 124 | Creational design patterns | `design-patterns-creational` | Concept | planned |
| 125 | Structural design patterns | `design-patterns-structural` | Concept | planned |
| 126 | Behavioral design patterns | `design-patterns-behavioral` | Concept | planned |
| 127 | UML and class diagrams | `uml-class-diagrams` | Concept | planned |
| 128 | Composition vs inheritance | `composition-vs-inheritance` | Concept | planned |
| 129 | Clean code and maintainable architecture | `clean-code` | Concept | planned |

### LLD Practice Problems

| # | Topic | Slug | Template | Status |
|---|---|---|---|---|
| 130 | Parking Lot | `lld-parking-lot` | LLD problem | planned |
| 131 | Library Management System | `lld-library-management` | LLD problem | planned |
| 132 | ATM | `lld-atm` | LLD problem | planned |
| 133 | Elevator System | `lld-elevator` | LLD problem | planned |
| 134 | Food Ordering System | `lld-food-ordering` | LLD problem | planned |
| 135 | Notification Service | `lld-notification-service` | LLD problem | planned |
| 136 | Payment Processing System | `lld-payment-processing` | LLD problem | planned |

### High-Level Design Fundamentals

| # | Topic | Slug | Template | Status |
|---|---|---|---|---|
| 137 | Functional and non-functional requirements | `requirements-nfr` | Concept | planned |
| 138 | Scalability: vertical and horizontal | `scalability` | Concept | planned |
| 139 | Availability and reliability | `availability-reliability` | Concept | planned |
| 140 | Latency and throughput | `latency-throughput` | Concept | planned |
| 141 | Load balancing | `load-balancing` | Concept | planned |
| 142 | Caching at scale | `caching-at-scale` | Concept | planned |
| 143 | SQL vs NoSQL | `sql-vs-nosql` | Concept | planned |
| 144 | Replication and sharding | `replication-sharding` | Concept | planned |
| 145 | CAP theorem | `cap-theorem` | Concept | planned |
| 146 | Consistency models | `consistency-models` | Concept | planned |
| 147 | Fault tolerance | `fault-tolerance` | Concept | planned |

### HLD Practice Problems

| # | Topic | Slug | Template | Status |
|---|---|---|---|---|
| 148 | URL Shortener | `hld-url-shortener` | HLD problem | planned |
| 149 | Chat application (WhatsApp) | `hld-whatsapp` | HLD problem | planned |
| 150 | Photo sharing (Instagram) | `hld-instagram` | HLD problem | planned |
| 151 | Video streaming (YouTube) | `hld-youtube` | HLD problem | planned |
| 152 | Notification System | `hld-notification-system` | HLD problem | planned |
| 153 | E-commerce backend | `hld-ecommerce` | HLD problem | planned |
| 154 | Ride booking system | `hld-ride-booking` | HLD problem | planned |
| 155 | Distributed rate limiter | `hld-rate-limiter` | HLD problem | planned |
