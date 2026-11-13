# Backend Architecture and Implementation Specification

| Attribute | Value |
|---|---|
| Version / status | 0.5, draft |
| Identifier prefix | `BKD-` (stable from version 1.0; while the document is a draft, identifiers are renumbered when rules are removed) |
| Obligation level | The keyword in the statement: **MUST** (= shall), **SHOULD**, **MAY** |
| Verification methods | **T** test (executed, automated or manual) · **I** inspection (review of an artefact) · **A** analysis · **D** demonstration |
| Basis abbreviations | CLA = Clean Architecture (R. C. Martin) · HEX = Hexagonal Architecture, ports and adapters (A. Cockburn) · DDD = Domain-Driven Design (bounded context, aggregate, ubiquitous language) · MON = modular monolith guidance (M. Fowler, "MonolithFirst"; Spring Modulith) · 12F = The Twelve-Factor App · REL = resilience guidance (Release It!, AWS Builders' Library, Google SRE book) · OUT = transactional outbox and idempotent consumer patterns · EVO = evolutionary database design (M. Fowler, parallel change) · HTTP = HTTP semantics (RFC 9110) · PD = Problem Details (RFC 9457) · YAG = YAGNI and evolutionary design (M. Fowler) · DEP = dependency guidance (R. Cox, OpenSSF) · PP = project practice |
| Defaults and parameters | Values marked **default**, and every table labelled *default*, are non-normative: they apply only until the matching project parameter in section 24 is filled, are overridden by it, and have no power over any rule (section 2.1). |

## Contents and changes

1. Scope and definitions
2. Constraints and assumptions
3. Architecture and the monorepo
4. Semantic domains
5. Layers inside a domain
6. Domain layer
7. Application layer
8. Interface layer
9. Persistence and database access
10. Schema migrations
11. Integration with other systems
12. Events and messaging
13. Dependable behaviour
14. Configuration, processes and lifecycle
15. Clean code
16. Build and dependencies
17. Deferred items
18. Evolution and other architecture styles
19. Framework mapping (informative)
20. Edge cases
21. Restrictions
22. Quality and verification
23. References
24. Project parameters and records

Sections 3.1 to 3.4 (layout, domain slices, the shared folder, DRY and KISS inside a domain) decide how the monorepo is organised. Sections 2.2 (MVP profile and extension triggers) and 16.1 (external packages) decide how much is built now and how much is taken from outside.

**Changes in 0.5:** the document is reduced to the backend contract. Removed: the shared kernel, the test folders and the testing rules (test kinds, coverage, acceptance scenarios, test-run gates), the pipeline and container rules, and the frontend rules; the layer called `infrastructure` is now `adapters`, to avoid confusion with the infrastructure document. The repository layout (3.1) is simplified to the folders an MVP needs. Earlier drafts added the MVP profile and extension triggers (2.2), the package policy (16.1), and domain slices, the shared folder and DRY and KISS inside a domain (3.2 to 3.4). Identifiers were renumbered.

## 1. Scope and definitions

**Scope.** This document is the single source of truth for how the backend of a product is structured, written, connected to its database, to its API and to other systems, and made dependable. It covers one backend, built as a **modular monolith in a monorepo whose modules are semantic domains**: business capabilities named in the language of the business, each with its own layers, its own data and a small published interface. A domain is a vertical slice of the monorepo: the same semantic domain name is a folder in the backend, in the frontend and in the API sources, and the domain owns its tables and tests. This document governs the backend area and what the backend needs from the other areas (3.2); the frontend and API documents govern their own areas. DRY and KISS apply inside a domain and not across the whole application (3.4). It is independent of any product, language, framework or database: the rules are stated by capability, and section 19 maps them to Java with Spring, C# with .NET, PHP with Laravel, Node.js with TypeScript, Python, Go and other platforms. Other architecture styles (microservices, event-driven, hexagonal, CQRS and more) are described for information and as a path for future extension (section 18); they are not required now. This document defines the backend only. The other areas and disciplines (frontend, API contract, database, testing, infrastructure, pipelines, security, observability) have their own documents; this document states only what the backend needs from them. Security (authentication, authorisation, hardening) and observability (logging design, metrics, tracing, alerting) are deferred to the final project phase and MUST NOT be implemented or scaffolded earlier (section 17). The document is written for an MVP that is meant to grow: the structure that is costly to change is built from the first day, every other capability follows a trigger (2.2), and external packages are kept to the few that are really needed (16.1).

| Term | Definition |
|---|---|
| Backend | The server-side application of the product: every domain, its data access and its integrations, deployed from one repository. |
| Monorepo | One version-controlled repository that holds the backend, the frontend, the end-to-end tests and the documents of the product. |
| Modular monolith | One deployable application whose code is divided into modules with enforced boundaries, so that each module could become a separate service later. |
| Semantic domain (domain) | A business capability with its own vocabulary, rules and data, named in the ubiquitous language (for example `billing`, `documents`), implemented as one vertical slice of the monorepo (3.2). Synonym: bounded context. |
| Domain slice (slice) | The same-named parts of one domain across the areas of the monorepo: `backend/<domain>`, and the folders of the same name that the other areas define. A slice is not a service; at the MVP all slices are released together. |
| Shared folder | The folder `shared` (also called common) of an area, with the code that more than one domain of that area uses and that has no knowledge of any domain (3.3). |
| Ubiquitous language | The shared vocabulary of the business-requirements glossary, used unchanged in code, database, API, tests and documents. |
| Published interface | The only part of a domain that other domains may use: its use-case interfaces, the data types they exchange, and the domain events it publishes. |
| Platform module | A module with technical capabilities only (clock, identifiers, transaction support, error types, configuration). It has no business vocabulary. It is the technical part of the backend shared folder (`backend/shared/platform`). |
| Composition root | The one place, at start-up, that creates the objects of all domains and connects ports to adapters. |
| Layer | One of the four parts of a domain: domain, application, adapters, interfaces (section 5). |
| Entity | A domain object with an identity that persists over time. |
| Value object | A domain object defined by its values, immutable, without identity. |
| Aggregate | A cluster of entities and value objects with one root, treated as one unit of consistency and of persistence. |
| Domain service | Business logic that belongs to no single entity and is stated in domain terms. |
| Domain event | An immutable fact in the past tense that something happened in a domain. |
| Use case | One operation of the application layer that fulfils one intention of an actor: a command (changes state) or a query (reads state). |
| Port | An interface, owned by the inner layer that needs it, that describes what that layer needs from or offers to the outside. |
| Adapter | Code that implements or calls a port with a specific technology (database, HTTP client, message broker, web framework). |
| Repository | A port that stores and loads aggregates as if they were an in-memory collection. |
| Unit of work | The set of changes that is saved together in one transaction. |
| Anti-corruption layer | An adapter that translates the model of an external system into the model of a domain, so that the external model does not spread. |
| Integration event | A domain event that is published across the domain boundary or the process boundary, with a documented, versioned shape. |
| Outbox | A table in the domain's database in which outgoing messages are saved in the same transaction as the state change, and from which a relay publishes them. |
| Idempotent consumer | A receiver that produces the same result when it receives the same message or request more than once. |
| Migration | A versioned script that changes the database schema or its data. |
| Readiness check | A check that reports whether an instance can serve requests now. |
| Liveness check | A check that reports whether the process is running and not stuck. |
| Process role | The way one artefact is started: `web` (serves requests), `worker` (runs jobs and consumers) or `migrate` (applies migrations and ends). |
| Deviation | A recorded, human-approved departure from a rule, with an owner, a reason and, where it is temporary, an expiry date. |
| Project parameter | A project-specific decision recorded in section 24. |

## 2. Constraints and assumptions

| ID | Statement | Ver. | Basis |
|---|---|---|---|
| BKD-CON-001 | The backend MUST be one deployable application built as a modular monolith. Its code MUST be divided into semantic domains with enforced boundaries (section 4). A different style requires a recorded decision (section 18). | I | MON |
| BKD-CON-002 | The backend MUST live in the `backend` area of the monorepo, organised as in 3.1. It MUST NOT import source code from other areas or from the API sources (generated output of the contract is a build product, BKD-BLD-006). | I | PP |
| BKD-CON-003 | The dependency rule MUST hold in every domain: source-code dependencies point inward, from interfaces and adapters to application, from application to domain, and the domain layer depends on nothing but the language and framework-free base types (section 5). | T | CLA |
| BKD-CON-004 | Business rules live in the domain layer. They MUST NOT live in controllers, in persistence code, in database triggers or procedures, in the frontend, or in framework configuration. | I | CLA |
| BKD-CON-005 | The API contract is the single source of truth for everything the backend exposes over HTTP. The backend implements the contract and MUST NOT expose behaviour that the contract does not describe. | I | PP |
| BKD-CON-006 | The backend returns stable error codes and machine-readable problems. It MUST NOT produce text for display to users; the frontend maps codes to messages. | T | PP |
| BKD-CON-007 | The backend MUST be stateless between requests. State lives in the database or in other backing services, never in the memory or on the disk of one instance (section 14). | T | 12F |
| BKD-CON-008 | Configuration comes from the environment. No address, credential or switch is written into code, and the same artefact runs in every environment (section 14). | T | 12F |
| BKD-CON-009 | Every domain owns its data. No other domain reads or writes its tables (section 9). | T | DDD |
| BKD-CON-010 | The database schema is created and changed only by versioned migrations (section 10). Automatic schema generation by the data-access framework MUST NOT be used in any environment. | T | EVO |
| BKD-CON-011 | Security (authentication, authorisation, input hardening, rate limits, security headers, secret scanning) and observability (logging design, metrics, tracing, alerting, audit logging of calls) are deferred to the final project phase. Agents MUST NOT implement, configure, scaffold or modify them earlier (section 17). | I | PP |
| BKD-CON-012 | An agent MUST read the business-requirements document, the frameworks document and the API contract before it designs a domain, and MUST ask the user when they do not answer a question. It MUST NOT invent business rules, limits, states, error behaviour or domain names. | I | PP |
| BKD-CON-013 | The backend MUST be testable: time and randomness are injected, external services sit behind ports, and the application can run against a real database engine. Test-only code MUST NOT exist in production artefacts. How the backend is tested is defined in the testing document. | T | PP |
| BKD-CON-014 | Project parameters (section 24) MUST be completed before the work that depends on them starts. Until then defaults apply. | I | PP |
| BKD-CON-015 | A framework is a detail. The choice of language and framework follows the frameworks document (P01). The rules of this document are stated by capability, and a rule that names a tool describes the default tool. | I | CLA |
| BKD-CON-016 | The simplest structure that satisfies the rules is the right one. A pattern (CQRS, event sourcing, a message broker, a separate service) is introduced only when a stated requirement needs it and is recorded as a decision (section 18). | I | MON |

### 2.1 Rule classes and precedence

| Class | Definition | Can be overridden by |
|---|---|---|
| Fixed rule | A requirement whose basis is HTTP or PD. | Nothing. A project MAY only tighten it. |
| Rule | Any other requirement (basis CLA, HEX, DDD, MON, 12F, REL, OUT, EVO, YAG, DEP, PP). | A project parameter that tightens it, or a recorded and human-approved deviation that relaxes it. |
| Parameter | A project value in section 24. | Replaces defaults. Cannot relax a fixed rule; cannot relax a rule without a deviation. |
| Default | A value labelled *default* and every table labelled *default*. | Overridden by any parameter. A default has no power over any rule. |

| ID | Statement | Ver. | Basis |
|---|---|---|---|
| BKD-PRE-001 | The order of precedence MUST be: fixed rules, then rules, then parameters, then defaults. | I | PP |
| BKD-PRE-002 | A default MUST NOT be used to justify a violation of a rule. Where a default conflicts with a rule, the rule prevails and the default is void. | I | PP |
| BKD-PRE-003 | A value marked *default* in a requirement is overridable. An unmarked value is part of the rule. | I | PP |
| BKD-PRE-004 | Verification MUST run against the effective values after overrides, never against the defaults. | T | PP |
| BKD-PRE-005 | If two rules conflict, the more specific rule wins. If equally specific, the stricter rule wins. The conflict MUST be recorded. | I | PP |
| BKD-PRE-006 | A deviation MUST be recorded with the reason, the scope, an owner and an expiry or review date. | I | PP |
| BKD-PRE-007 | The API contract, the database document and the documents of the other areas (frontend, testing, infrastructure, pipelines) govern their own subjects. Where they require something of the backend that this document makes optional, their requirement prevails. A contradiction is recorded (BKD-PRE-005). | I | PP |
| BKD-PRE-008 | Examples, illustrative snippets and tables marked informative are non-normative. | I | PP |

### 2.2 MVP profile and extension triggers

The first release (MVP) is the smallest release that delivers the stated scope to real users. The backend is built so that the MVP is small and growth needs no rewrite. The rule is simple: **what is costly to change later is built correctly from the first day; what is cheap to add behind an existing seam is added only when a trigger occurs.** Building a capability before it is needed costs effort now, delays the needed work, adds code to carry and may be wrong when the need arrives; a capability added later behind a clean seam usually costs little more.

| ID | Statement | Ver. | Basis |
|---|---|---|---|
| BKD-MVP-001 | The MVP implements the smallest set of domains and use cases that delivers the MVP scope (P13). Nothing is built for a need that no requirement states. A capability that has a trigger in the table below is not built before its trigger occurs. | I | YAG |
| BKD-MVP-002 | The **fixed core** is built in full from the first commit and is never reduced for the MVP: the four layers and the dependency rule; domain-owned data and published interfaces; the architecture test; the contract-first API with problem responses and stable error codes; versioned migrations; configuration from the environment; stateless instances and the `web`, `worker` and `migrate` roles; one transaction per use case; database constraints; optimistic versions; injected clock and identifiers; timeouts on every outbound call and every wait; the liveness and readiness checks; the correlation identifier; graceful shutdown; the checks of section 22. | I | YAG |
| BKD-MVP-003 | Everything else is **introduced on its trigger** (table below). Until then it exists in its **MVP form**, which is simpler but keeps the **seam**, so that the later component replaces the MVP form without a change in domain code, published interfaces or the API contract. | I | YAG |
| BKD-MVP-004 | A rule whose subject does not exist yet (no external system, no integration event, no job) is not applicable and is not built in advance. The review records "not applicable" for it. The rule applies from the first change that creates its subject. | I | YAG |
| BKD-MVP-005 | The MVP starts with few, coarse domains (**default** at most 8, P13) taken from the glossary. When two business areas are not clearly separate in the glossary, they start as one domain with a clean internal structure. A split later follows a seam of the language (BKD-MOD-012) and is cheap because boundaries are enforced from the start. | I | YAG |
| BKD-MVP-006 | The MVP runs one database engine in one instance, one schema per domain, one migration tool, one deployable artefact and one process role set. No replica, shard, second engine, cache, broker or search engine exists until its trigger. | I | YAG |
| BKD-MVP-007 | The simplest working form is chosen first: a database table polled by the `worker` role before a queue; a database lease before a coordination service; an in-process dispatcher before a broker; a read from the database with the right index before a cache; configuration values before a feature-management system. | I | YAG |
| BKD-MVP-008 | Every shortcut taken for the MVP is recorded in the shortcut register (24.8) with its trigger and an owner. A shortcut MUST NOT relax a fixed core item, a fixed rule or a deferred item (section 17). | I | YAG |
| BKD-MVP-009 | Seams are kept alive by automatic checks, not by good intention: the architecture check (BKD-QGT-002), the contract check (BKD-QGT-003) and the slice check (BKD-QGT-011) fail when a seam is cut. | T | YAG |
| BKD-MVP-010 | An abstraction is added for a reason that exists now: a port for every effect on the outside world (database, external system, clock, storage, notification), a published interface for every domain. An interface with a single implementation and no outside effect, a generic base class, and a plug-in point for an imagined second use MUST NOT be added (BKD-CLN-020). | I | YAG |
| BKD-MVP-011 | Performance work follows measurement against the targets of the non-functional requirements. Until a target is missed, correct queries with the right indexes, bounded results and the limits of 21.2 are the whole performance design. | I | YAG |
| BKD-MVP-012 | The occurrence of a trigger is recorded as a decision (BKD-EVO-006) with the evidence (a measurement, a requirement, an incident), the alternatives, the consequences and the way back. The MVP form is then replaced in one small change per capability. | I | YAG |
| BKD-MVP-013 | The MVP is not an excuse to skip migrations, error handling or the contract. A smaller scope is the only allowed reduction. | I | YAG |
| BKD-MVP-014 | The MVP prepares the split of domains by structure only: one repository, one build, one release and one deployment, but every domain has its own folder, its own API sources, migration history and README. No separate build, release, repository or deployment per domain exists before its trigger (BKD-TRG-019). | I | YAG |

The triggers below are defaults (P13 MAY change them, never below the fixed core).

| ID | Capability | Introduce when (trigger) | MVP form until then | Seam that stays |
|---|---|---|---|---|
| BKD-TRG-001 | Circuit breaker and bulkhead for an external system | A dependency has measured failures or slowness that affect users, or sits on a business-critical path with many calls | Timeouts, a bounded retry for idempotent calls, the documented unavailable behaviour (BKD-INT-007) | The port and its single adapter, which is the only place that calls |
| BKD-TRG-002 | Outbox and relay | The first event or external effect whose loss would leave data inconsistent | In-process dispatch after commit for events whose loss is harmless or recoverable by a rebuild | The dispatcher interface of the platform module (BKD-EVT-016) |
| BKD-TRG-003 | Message broker | A consumer must run in another process, or the outbox relay cannot keep up | The outbox table read by the relay inside the `worker` role | A broker adapter behind the dispatcher interface |
| BKD-TRG-004 | Read models | A screen needs data of several domains that composition of published queries serves too slowly, or a query cannot be made fast | Queries composed in the application layer from published queries, with indexes | Published query interfaces |
| BKD-TRG-005 | Separate read store (CQRS with separate stores) | A read model in the same database cannot meet the targets | A read model as a table of the same database | The repository and query ports |
| BKD-TRG-006 | Cache server or cache | A measured read load or latency target is missed after indexes and query changes | None | A port for the cached read inside the owning domain (BKD-PER-024) |
| BKD-TRG-007 | Search engine | A requirement for relevance ranking, fuzzy or multi-language search that database search cannot meet | Database search with an index | A search port owned by the domain |
| BKD-TRG-008 | Job queue or scheduler framework | Work that exceeds the request deadline, bursts that need smoothing, or schedules that the database table cannot serve | A job table with status, polled in bounded batches by the `worker` role under a database lease | The job entry-point interface and the job status query |
| BKD-TRG-009 | Idempotency record for creating operations | An operation that the API contract declares idempotent by key | Only the operations that the contract names | The decorator of the platform module (BKD-APP-014) |
| BKD-TRG-010 | Custom load shedding | Saturation is measured under the expected load | The limits of the server and the pools by configuration (P10) | The settings of P10 |
| BKD-TRG-011 | Separate deployable for the `worker` role | The worker needs another scaling profile or isolation than the web role | The same artefact started with another role | The role setting (BKD-CFG-008) |
| BKD-TRG-012 | Extraction of a domain into a service | One of the reasons of BKD-EVO-001 holds and the domain is ready (BKD-EVO-002) | The domain in the modular monolith | The published interface, ownership of data, no cross-domain keys |
| BKD-TRG-013 | Event sourcing for one aggregate | A requirement for complete history, replay or temporal queries of that aggregate | State in tables, with a change-history table only if a requirement asks for it | The repository port of the aggregate |
| BKD-TRG-014 | Feature switches | A release must be separated from a deployment, or a gradual rollout is required | None; unfinished work stays on a branch | A settings value read at the use-case entry (BKD-DEP-025) |
| BKD-TRG-015 | Second contract version | A change that cannot be additive | Additive changes only | The versioning rules of the API contract |
| BKD-TRG-016 | Second database engine or a document store | A requirement that the engine of the MVP cannot meet, with evidence | The one engine | The persistence adapters (BKD-PER-022) |
| BKD-TRG-017 | A dependency beyond the foundation set | A requirement that the standard library, the framework and about one hundred lines of own code cannot meet (16.1) | None | The port and the single adapter that wraps the dependency |
| BKD-TRG-018 | Backend for frontend | A client needs a response shape that the contract should not carry | One contract for all clients | The contract and the use cases |
| BKD-TRG-019 | Split of a slice into its own build and release unit, or its own repository | A different release rhythm, team or availability need that the shared release cannot serve (BKD-EVO-001), and the slice is ready (BKD-SLC-008) | One build and one release for all slices; builds MAY be selected by path | The folders of the slice in all areas with their tests, its API sources, its migration history and its published surface |

### 2.3 Minimal start (informative)

For a new or simple project this is enough to begin. Everything else in this document applies when its subject appears.

1. Confirm the framework (P01), a few domains from the glossary (P03), the foundation set of packages and the MVP scope (P12, P13).
2. Create `backend/app` and the first domain with only the folders it needs. Add `backend/shared` when a second domain needs something.
3. Put business rules in `domain/`, use cases in `application/`, database and external-system code in `adapters/`, and controllers, jobs and consumers in `interfaces/`.
4. Take the operations from the API contract and answer errors as problem responses with stable error codes.
5. Create tables only through the domain's migrations, in a schema of its own.
6. Read settings from the environment, and give every wait and every outbound call a timeout.
7. Offer a liveness and a readiness check.
8. Add the architecture check, so that layer and domain boundaries cannot be broken unnoticed.
9. Use no package beyond the foundation set without the user's approval.
10. Build nothing from the trigger table (2.2) before its trigger occurs, and do not touch security or observability before the final phase (section 17).

## 3. Architecture and the monorepo

The product is one monorepo. This document defines its `backend/` area. Inside the backend the code is organised by **semantic domain**: a folder named with the domain's own name (`backend/billing`, `backend/appointments`). A domain is a **vertical slice** of the product: the other areas of the monorepo (frontend, API sources) have a folder of the same name for the same domain, as their own documents define. A slice is not a service. At the MVP everything is built, released and deployed together; the structure only keeps a later split possible.

Inside each backend domain the code is layered by the dependency rule (hexagonal architecture): the business core in the middle, adapters at the edge. Between domains only published surfaces are used: the published interface and events of the backend, and the published operations of the API.

```text
                    Clients (browser, other systems)
                                 |
                  HTTP interface of the backend (the API contract)
                                 |
   +-----------------------------+-----------------------------+
   |  backend/billing            |  backend/appointments       |
   |  interfaces -> application -> domain <- adapters          |
   |  own tables, own migrations                               |
   |       published interface <--- calls / events ---> ...    |
   +-----------------------------+-----------------------------+
                                 |
        backend/shared (clock, ids, transactions, errors, settings)
                                 |
        Database (one engine, one database schema per domain), backing services
```

### 3.1 Repository layout (default, P02)

```text
project/backend/
  app/                      composition root: wires the domains, starts the process roles (web, worker, migrate)
  shared/                   technical code that several domains use (created when first needed)
  <domain-name>/            one folder per semantic domain, for example billing, appointments
    (files at the root)     the published surface: use-case interfaces, exchanged data types, published events; README
    domain/                 entities, value objects, aggregates, domain services, repository ports, domain errors
    application/            use cases, ports to the outside, transaction boundaries
    adapters/               database repositories and queries, clients of external systems, outbox and consumers
    interfaces/             HTTP controllers or handlers, scheduled jobs, message consumers
    migrations/             the migrations of this domain's schema, with its own history
```

Start small. A folder is created when it has content, a folder is split into sub-folders only when it holds more than about ten files, and `shared/` is created when a second domain needs something. A simple domain uses the light profile (BKD-LAY-010): it has `domain/`, `adapters/`, `interfaces/` and `migrations/`, and its use cases live in `domain/`. Build files, the container definition and everything outside `backend/` belong to other documents (BKD-ARC-002).


The other areas of the monorepo (frontend, API, infrastructure, pipelines, tests, documents) have their own documents. This document defines only `project/backend/`; it relies on the domain folders of the other areas carrying the same semantic name (BKD-SLC-001). The folder `domain/` inside a domain is the name of a layer (the business core), not a grouping of domains (BKD-ARC-010).

| ID | Statement | Ver. | Basis |
|---|---|---|---|
| BKD-ARC-001 | The backend folder contains three kinds of module: the domain folders (named by their semantic names), the `shared` folder, and the composition root `app`. No other kind of module MUST exist. A folder that collects code without a purpose name and an owner (`misc`, `utils`, `helpers`, `core`) MUST NOT exist. | I | MON |
| BKD-ARC-002 | The folder names of 3.1 are the default (P02). A project MAY use the idiom of its platform (casing, package separators, one package per layer) and other names for the layers, if the four layers stay recognisable and the choice is recorded in 24.4. Build files and the container definition are placed as the build and infrastructure documents say. | I | PP |
| BKD-ARC-003 | There is one deployable backend artefact. It is built once per commit and started in the process roles `web`, `worker` and `migrate` by configuration. The roles run the same code, so that what is tested is what runs. | T | 12F |
| BKD-ARC-004 | The composition root (`backend/app`) is the only place that knows all backend domains and all adapters. It creates objects, reads configuration and connects ports to adapters. Domains MUST NOT create their own adapters outside it, and MUST NOT use a service locator or a global container. | T | CLA |
| BKD-ARC-005 | The platform module (`backend/shared/platform`) offers technical capabilities that any domain may use: the clock, the identifier generator, transaction support, the base error types, the problem mapping, the configuration loading, the in-process dispatcher. It contains no business term. A capability that only one domain uses belongs to that domain. | I | PP |
| BKD-ARC-006 | The backend MUST be runnable and testable without the frontend and without a message broker. A broker, a cache server or a search engine is added only when a requirement needs it (BKD-CON-016). | T | MON |
| BKD-ARC-007 | The backend is stateless and horizontally scalable: several identical instances can run behind a load balancer without coordination, except for the work that is explicitly protected by a lock (BKD-DEP-019). | T | 12F |
| BKD-ARC-008 | The decision to use this architecture, the list of domains, and every later change of either are recorded as architecture decisions with their reasons. | I | PP |
| BKD-ARC-009 | The backend MAY contain more than one entry-point artefact only when a requirement demands it (for example a separately scaled worker). Then the code is still one codebase, and the artefacts differ in their process role, not in their code (BKD-ARC-003). | I | 12F |
| BKD-ARC-010 | Domains are folders named by their semantic names, directly under the area folder (`backend/billing`, `frontend/billing`). No folder called `domain`, `domains` or `modules` groups them, and the same domain has the same name in every area. | I | DDD |

### 3.2 Domain slices

| ID | Statement | Ver. | Basis |
|---|---|---|---|
| BKD-SLC-001 | A domain is a vertical slice of the monorepo: the same domain name is a folder in each area that has a part of the domain (`backend/<domain>`, and the folders of the same name in the other areas as their documents define). The areas keep their own structure; this document defines only the backend folder. The inventory (24.2) records which areas have a part of the domain. | I | DDD |
| BKD-SLC-002 | A slice is not a service. At the MVP all slices are built, tested, released and deployed together from one repository. The structure prepares a later split (BKD-SLC-008, BKD-TRG-019) and does not perform it. | I | YAG |
| BKD-SLC-003 | The boundary of a domain holds in every area. A part of domain A MUST NOT import, read or call the internals of domain B: its backend internals, its tables, or the internals of its parts in other areas. Domain B is reached only through its published surface: the published interface and events of its backend, its published operations in the API, and the published entry points that other areas define. | T | MON |
| BKD-SLC-004 | The backend connects to the other areas only through the API contract and through the database that it owns. It does not import source code of other areas, and other areas do not import backend source or reach the database. | T | CLA |
| BKD-SLC-005 | One name for one thing: the backend folder, the API path prefix and tag, the database schema and the events of a domain carry the name of the domain from the glossary (for example `billing`, `/billing/...`, schema `billing`), and so do the folders of the other areas. | I | DDD |
| BKD-SLC-006 | Every operation of the API belongs to exactly one domain, and the API sources of a domain are identifiable by its name, in the layout that the API document defines. The sources are bundled by the build into the one public contract, which is what clients see (BKD-EVO-007). | T | PP |
| BKD-SLC-007 | The database part of a domain is its tables and its `migrations` folder in the backend folder of the domain (or its folder in a separate database area, if P02 chooses one). Each domain has its own schema and its own migration history (BKD-MIG-015). The conventions come from the database document. | T | EVO |
| BKD-SLC-008 | A domain is **ready to be split off** when its backend folder, together with `backend/shared`, builds alone; when no import leaves it except into `backend/shared`; and when a check fails on any import into the folder of another domain. The other areas define the same for their folders. A split moves the folders. | T | MON |
| BKD-SLC-009 | The composition root (`backend/app`) contains no domain logic: it wires the domains and the process roles, and a new domain is registered there in one place. The other areas have their own composition points. | I | CLA |
| BKD-SLC-010 | A feature change normally touches one slice. A change that touches several slices (other than the registration at the composition points or a recorded contract change) is reviewed for a missing boundary or a concept that is in the wrong place. | I | MON |
| BKD-SLC-011 | Every slice has one owner, recorded in the inventory (24.2) and in the code-ownership file of the repository, with one entry per area folder of the domain. | I | PP |
| BKD-SLC-012 | The folders of the backend are those of 3.1. A new kind of folder needs a recorded reason. | I | PP |

### 3.3 The shared folder

The backend has a `shared` folder (some projects call it `common`), and the other areas have their own. It is allowed and useful where it removes a real cost. Independence of the domains is worth more than a saved line, so the folder is small, technical and owned. In the rules below, "`shared`" means the `shared` folder of the area concerned.

| ID | Statement | Ver. | Basis |
|---|---|---|---|
| BKD-SHR-001 | A `shared` folder holds only what more than one domain of that area uses and what has no knowledge of any domain. In the backend it holds technical capabilities only (the clock, identifiers, transaction support, base error types, problem mapping, settings loading, the in-process dispatcher). It is created when a second domain needs something, and its parts are named by their purpose. The shared folders of the other areas are defined by their documents. | I | MON |
| BKD-SHR-002 | Domains depend on the `shared` folder of their area. A `shared` folder MUST NOT depend on a domain or on the composition root. The architecture check enforces it. | T | CLA |
| BKD-SHR-003 | A `shared` folder contains no domain vocabulary and no business rule, entity, repository, use case, event, transfer type or table of a domain. A rule that two domains need is owned by one domain and used through that domain's published interface, or each domain keeps its own rule when the meaning is its own. | T | DDD |
| BKD-SHR-004 | Each part of a `shared` folder has a purpose name, an owner, a short README, tests and a small published surface; everything else in it is internal. A part without these is not allowed. | I | MON |
| BKD-SHR-005 | Code moves into a `shared` folder only when all of the following hold: it is the same in **meaning**, not only in shape; it has no domain vocabulary; it is stable; at least two domains need it now; and the copy rule of BKD-DRY-002 is met. The move is recorded in one line with the domains named. | I | PP |
| BKD-SHR-006 | A change to a `shared` folder affects every user. It is backward compatible, or all users are changed in the same change. A breaking change is made as a parallel change: the new form is added beside the old, users move one by one, then the old form is removed. The owner reviews it and every domain's tests run. | T | EVO |
| BKD-SHR-007 | A `shared` folder is not a way around a boundary. A domain that needs shared code in order to read the data or the rules of another domain has a wrong boundary (BKD-SLC-003). | I | MON |
| BKD-SHR-008 | The size of `backend/shared` is watched: above **default** 15 percent of the source lines of the backend (21.2), a review moves code that only one domain uses back into that domain and looks for misplaced domain concepts. | A | PP |
| BKD-SHR-009 | When a domain asks a `shared` folder for a special case (an exception, a flag or a branch for that domain alone), the code moves back into that domain. Shared code does not know its users. | I | PP |
| BKD-SHR-010 | External packages that a `shared` folder uses are entered once in the dependency register (24.7), counted once in the budget, and wrapped as BKD-DPN-012 requires. | I | DEP |

### 3.4 DRY and KISS inside a domain

| ID | Statement | Ver. | Basis |
|---|---|---|---|
| BKD-DRY-001 | The principles DRY (one piece of knowledge, one place) and KISS (the simplest structure that works) apply **inside a domain**, and across the parts of one domain. They do not apply across the whole application. Between domains, independence has priority over reuse. | I | DDD |
| BKD-DRY-002 | **Copy rule.** Code that a second domain also needs is first **copied** into that domain when it is small or when its meaning is the domain's own. It moves to a `shared` folder (BKD-SHR-005) when the same change has to be made in two or more copies (a defect fix, a change of a technical rule) or when a third copy would be made. | I | PP |
| BKD-DRY-003 | A copy belongs to its domain: it has its own tests and MAY diverge. A comment in the copy names where it came from, so that a later fix can be considered in the other copies. | I | PP |
| BKD-DRY-004 | Things that look alike are not the same knowledge. Two structures, validations or screens that have the same shape but represent different concepts are not merged. The test is: would a change for one reason force a change in the other? If not, they stay separate. | I | DDD |
| BKD-DRY-005 | Business rules, transfer types, API types, events, messages and tables are not shared by code between domains. Each domain has its own representation of what it sends and receives, and the contract and the event catalogue document the exchange. | T | DDD |
| BKD-DRY-006 | Each domain chooses the simplest internal structure that satisfies the rules for its size: the strict, the pragmatic or the light layering profile (BKD-LAY-009, BKD-LAY-010), and vertical slices for simple reads. A simple domain does not carry the ceremony of a complex one. The choice is recorded and is revisited when the domain grows (BKD-MOD-012). | I | YAG |
| BKD-DRY-007 | Reviewers do not ask for the removal of duplication across domains unless BKD-SHR-005 holds. Tools that measure duplication run per domain and not across domains. | I | PP |
| BKD-DRY-008 | Inside a domain, duplication is a defect, also when it is spread over layers (the same mapping in a controller and in a repository) or over areas (below). | I | PP |
| BKD-DRY-009 | The backend has one source of truth for what it exchanges over HTTP: the API contract. The backend transfer types are generated from it or checked against it (BKD-BLD-006), so that they cannot drift. A hand-copied definition that differs from the contract is a defect. | T | PP |

## 4. Semantic domains

A domain is chosen for what the business does, not for what the code does. `billing`, `appointments`, `catalogue` are domains. `controllers`, `services`, `repositories`, `models`, `utils` are not.

| ID | Statement | Ver. | Basis |
|---|---|---|---|
| BKD-MOD-001 | Domains MUST be derived from the business-requirements document and its glossary: each domain is a business capability with its own vocabulary, rules and data. The domain list is a project parameter (P03, 24.2) and is agreed with the user before code is written. | I | DDD |
| BKD-MOD-002 | A domain is named with a noun of the ubiquitous language, in the singular or plural form that the glossary uses, in one natural language (**default** English). Names of domains, classes, tables, operations and events use the same words. | I | DDD |
| BKD-MOD-003 | A domain has one sentence of responsibility, one owner, a list of the aggregates it owns, the use cases it publishes, the events it publishes and consumes, and its tables. They are recorded in the domain inventory (24.2). | I | PP |
| BKD-MOD-004 | Things that change for the same reason and at the same time belong in one domain. Things that change independently belong in different domains. A concept with different meanings for different parts of the business is modelled separately in each domain that needs it, with a mapping between them, not as one shared model. | I | DDD |
| BKD-MOD-005 | A domain MUST expose a published surface and nothing else: in the backend the files at the root of its folder (use-case interfaces, exchanged data types, published events), in the API its published operations. Everything in its sub-folders is internal, and the build or a check MUST make a use from another domain a compile-time or check-time failure. | T | MON |
| BKD-MOD-006 | A domain MUST NOT reference the internals of another domain: its entities, repositories, tables, mappers or adapters. It uses that domain's published use cases or consumes its events. | T | MON |
| BKD-MOD-007 | The allowed dependencies between domains form a directed graph without cycles. The graph is recorded (24.3) and verified by a test on every change. A cycle MUST be removed by moving a concept, by introducing an event, or by merging the domains. | T | MON |
| BKD-MOD-008 | Calls between domains are in process through the published interface. They carry published data types, never entities and never persistence types. The caller treats the call as a call to another service: it handles failure and does not depend on the callee's transaction. | I | MON |
| BKD-MOD-009 | A use case of one domain MUST NOT run inside the database transaction of another domain's use case unless a recorded decision says that the two domains form one unit of consistency. The default coupling between domains is eventual, through events (section 12). | I | DDD |
| BKD-MOD-010 | A domain MUST NOT read the tables of another domain, not even for reports. Information from several domains for one view is composed in the application layer from published queries, or from a read model that the consuming domain keeps up to date from events (BKD-PER-016). | T | DDD |
| BKD-MOD-011 | The concepts that are only reference data (lists of countries, categories, states) belong to the domain that owns their rules. Other domains refer to them by a stable code or identifier. | I | DDD |
| BKD-MOD-012 | A domain that grows beyond the limits of 21.2 (use cases, aggregates, lines) is reviewed for a split along a seam of the language. The review is recorded, even when the decision is to keep it. | I | PP |
| BKD-MOD-013 | A new domain is created with its backend folder at once: the published interface, the layers it needs, the migrations and the README, registered in the composition root, with one working use case. An empty skeleton MUST NOT be merged. | I | PP |
| BKD-MOD-014 | A domain MAY be a pure supporting capability (for example `reference-data`, `documents`) that other domains use. It is still a domain: it has a published interface, owns its data and has no knowledge of its users. | I | DDD |
| BKD-MOD-015 | A domain that depends on many other domains for its core rules is a signal of a wrong boundary. The review checks the fan-out and the fan-in of each domain against the limits of 21.2. | A | PP |

## 5. Layers inside a domain

| Layer | Contents | May depend on |
|---|---|---|
| Domain | Entities, value objects, aggregates, domain services, domain events, repository ports, domain errors | Nothing but the language and base types of the `shared` folder that are free of framework code |
| Application | Use cases (commands and queries), ports to the outside world (clients, clock, notifications), transaction boundaries, mapping between published types and domain objects | Domain |
| Adapters | Repositories, mappings to the database, queries, clients of external systems, outbox, consumers' technical parts | Application and domain (it implements their ports) |
| Interfaces | Entry points: HTTP controllers or handlers, scheduled jobs, message consumers; request validation and mapping; error mapping | Application (and published types) |

| ID | Statement | Ver. | Basis |
|---|---|---|---|
| BKD-LAY-001 | The four layers of the table MUST exist in every domain that has business logic. Dependencies MUST follow the table. A dependency against the table is a defect, and a test of the architecture (BKD-QGT-002) MUST fail on it. | T | CLA |
| BKD-LAY-002 | The domain layer MUST NOT import the web framework, the data-access framework, the dependency-injection container, serialisation annotations, logging or any library that performs input or output. Language-level libraries (collections, dates, decimals) are allowed. | T | CLA |
| BKD-LAY-003 | A port is owned by the layer that needs it and is written in that layer's language. Repository ports are owned by the domain layer; ports to clients, clock, notification and similar capabilities are owned by the application layer. The adapter that implements a port lives in infrastructure. | I | HEX |
| BKD-LAY-004 | Data that crosses a layer boundary is a simple data structure shaped for the inner layer: published types, commands, queries, results. Entities, persistence rows, framework requests and responses MUST NOT cross a boundary inward or outward. | T | CLA |
| BKD-LAY-005 | Every adapter converts at the boundary. A persistence row is mapped to a domain object in the repository adapter. A wire request is mapped to a command in the interface layer. A client response of an external system is mapped to a port type in the client adapter. | I | CLA |
| BKD-LAY-006 | The interface layer contains no business decision. It parses, validates the shape, calls one use case and maps the result or the error. A controller method that contains a rule, a loop over data, a database call or a call to another system is rejected. | I | CLA |
| BKD-LAY-007 | The application layer contains no business rule. It loads aggregates, calls domain behaviour, saves, and publishes events. It decides the order of steps and the transaction, and it orchestrates ports. A condition that expresses a business rule is moved into the domain layer. | I | CLA |
| BKD-LAY-008 | The adapters layer contains no business rule. A repository stores and loads. A query adapter answers a query. An external client calls and translates. | I | HEX |
| BKD-LAY-009 | The strict profile (**default**, P05) keeps a separate domain model and persistence model, with a mapper between them. A project whose framework uses active-record models (for example Eloquent, Django ORM, Rails) MAY choose the pragmatic profile: the framework model class serves as persistence model inside the adapters layer, and the domain model stays plain; or, by a recorded deviation, the model class holds domain behaviour without input or output, without framework calls and without queries inside its rule methods. BKD-LAY-001 and BKD-LAY-004 hold in both profiles between domains and towards the interface layer. | I | PP |
| BKD-LAY-010 | A domain whose rules are simple (a reference-data list, plain record keeping, a proxy to an external system) MAY use the light profile: it collapses the domain layer and the application layer into one, but keeps the published interface, the separation of interfaces and adapters, and its data ownership. The profile is chosen per domain (KISS applies inside a domain, BKD-DRY-006), recorded in the domain inventory, and revisited when the rules grow. | I | PP |
| BKD-LAY-011 | Layers are folders (or packages, or projects) inside the domain, not modules of the build, unless the platform enforces the dependency rule only by separate projects (section 19). Either way, the rule is enforced by the build or by a test, not by convention. | T | PP |
| BKD-LAY-012 | The dependency rule is enforced automatically: by a compile-time mechanism where the platform has one, and by an architecture test on every change (BKD-QGT-002). The test checks layers, domain boundaries, cycles and the forbidden imports of the domain layer. | T | MON |

## 6. Domain layer

| ID | Statement | Ver. | Basis |
|---|---|---|---|
| BKD-DMN-001 | The domain layer expresses the business in the ubiquitous language: a reader who knows the business can read the class and method names without knowing the technology. | I | DDD |
| BKD-DMN-002 | Aggregates are the unit of consistency, of loading and of saving. All access to the inside of an aggregate goes through its root. An aggregate protects its invariants in every method, so that an invalid state cannot be constructed or reached. | T | DDD |
| BKD-DMN-003 | Aggregates are small. They refer to other aggregates by identifier, never by object reference. A use case changes one aggregate per transaction (BKD-APP-004). | I | DDD |
| BKD-DMN-004 | Entities have behaviour. Rules that concern one entity are methods of that entity. Business logic MUST NOT sit in classes that only get and set the data of passive entities (the anemic model) when the rule belongs to the entity. | I | DDD |
| BKD-DMN-005 | Value objects are immutable and compared by value. They validate themselves at construction. Primitive types that carry meaning (money, e-mail address, quantity, percentage, reference number) are value objects, not bare strings and numbers. | I | DDD |
| BKD-DMN-006 | State changes of an entity with a life cycle are modelled as a state machine with named states and explicit transitions. An invalid transition is refused with a domain error that names the current state and the attempted transition. The allowed transitions are one table in code and are tested exhaustively. | T | DDD |
| BKD-DMN-007 | Domain services hold rules that involve several aggregates or that belong to no entity. They are stateless, named by the business operation, and receive what they need as arguments. | I | DDD |
| BKD-DMN-008 | A domain error is a typed error with a stable error code, a short neutral name and structured data (the values involved). It carries no text for users. The codes are the codes of the API error catalogue, and each is raised by exactly one kind of cause. | T | PD |
| BKD-DMN-009 | Rules are checked in the domain, in order: the shape of values (value objects), the invariants of the aggregate, then the rules across aggregates (domain services or the application layer). The first failure is returned with its code; a use case that validates a whole input MAY collect all violations of one input into one error. | I | PP |
| BKD-DMN-010 | Time comes from an injected clock, identifiers from an injected generator, and random values from an injected source. The domain layer MUST NOT call the system clock, a random generator or a global identifier function. | T | PP |
| BKD-DMN-011 | Money and quantities that need exact results use an exact decimal type or integer minor units with a currency, and a rounding rule that is stated by the requirements. Binary floating point MUST NOT be used for money. | T | PP |
| BKD-DMN-012 | Dates and times are modelled by what they mean: an instant (UTC), a calendar date without time, a local time with a zone, a duration. The domain never stores a local time without its zone when it means an instant. | T | PP |
| BKD-DMN-013 | An aggregate raises domain events for facts that other parts of the system may care about. Events are named in the past tense (`InvoiceIssued`), are immutable, carry the identifier of the aggregate, the time and the data needed to understand the fact, and are collected by the aggregate and published by the application layer after the state is saved (section 12). | T | DDD |
| BKD-DMN-014 | Reading a value is not a rule. A query that only reads for display MAY bypass aggregates and use a read model (BKD-PER-015). A write MUST NOT bypass the aggregate. | I | DDD |
| BKD-DMN-015 | The domain layer has no knowledge of who calls it, over which protocol, or how it is stored. The actor of an operation (when security exists) is an input value of the use case, not a global (section 17). | I | HEX |
| BKD-DMN-016 | Optional values are explicit (an option or nullable type that the language checks) and collections are never null. A function does not return null to signal an error. | I | PP |

## 7. Application layer

| ID | Statement | Ver. | Basis |
|---|---|---|---|
| BKD-APP-001 | Every operation of the backend is a use case: a class or function with one public entry point, named by the intention (`IssueInvoice`, `FindOpenCases`), taking a command or query object and returning a result object or a domain error. | I | CLA |
| BKD-APP-002 | Commands change state and return at most the identifier and the new version of what they changed. Queries return data and change nothing. A use case is one or the other. | I | CLA |
| BKD-APP-003 | A use case orchestrates: it loads the aggregate through a port, calls domain behaviour, saves through a port, publishes events, and maps the result. It contains no business rule (BKD-LAY-007). | I | CLA |
| BKD-APP-004 | One use case runs in one transaction and changes one aggregate. Consistency across aggregates or across domains is reached by events, and is eventual. A use case that must change two aggregates atomically records the reason (BKD-MOD-009). | T | DDD |
| BKD-APP-005 | The transaction starts and ends in the application layer (a decorator, an interceptor or an explicit unit of work provided by the platform module). Domain code and controllers do not start transactions. A read-only use case runs in a read-only transaction or none. | T | PP |
| BKD-APP-006 | No call to an external system, no message publication to a broker and no long computation runs while a database transaction is open. Outgoing effects are saved as outbox entries in the transaction (BKD-EVT-004) or run after the commit. | T | OUT |
| BKD-APP-007 | A use case is idempotent or declares that it is not. Write operations that the API contract declares idempotent are implemented so (BKD-IFC-008). A use case that is repeated with the same intention produces the same state. | T | HTTP |
| BKD-APP-008 | Optimistic concurrency is the default for updates of aggregates that several actors can change: the command carries the expected version, a mismatch raises a conflict error, and the new version is returned. | T | HTTP |
| BKD-APP-009 | A use case checks that its input is complete and consistent as the use case needs it (existence of referenced aggregates, state preconditions). Format checks are done earlier by the contract validation in the interface layer; business rules are done by the domain. Each check has one home and is not repeated in another layer. | I | PP |
| BKD-APP-010 | A use case receives the context of the request (correlation identifier, and later the actor) as an explicit parameter object. It MUST NOT read it from a global or thread-local variable that the domain also reads. | I | PP |
| BKD-APP-011 | A use case calls another domain only through that domain's published interface (BKD-MOD-006) and handles its failure explicitly. A failure of a called domain is mapped to a domain error of the caller or to a documented unavailable error. | T | MON |
| BKD-APP-012 | Queries that serve lists apply the pagination, the sort and the limits of the API contract in the query itself (BKD-PER-011). A query never loads an unbounded set. | T | PP |
| BKD-APP-013 | Long-running work (reports, imports, exports, batch changes) is started by a command that returns quickly with an identifier, and runs as a background job with its own status that a query can read. The job is idempotent and resumable (BKD-DEP-018). | I | PP |
| BKD-APP-014 | Cross-cutting behaviour of use cases (transaction, retry of a conflict, idempotency record, mapping of unexpected errors) is implemented once, as decorators or middleware in the platform module, not repeated in each use case. | I | PP |
| BKD-APP-015 | A use case has a small number of collaborators (**default** at most 5 constructor parameters, 21.2). More means that it does too much or that a collaborator hides a missing concept. | I | PP |

## 8. Interface layer

| ID | Statement | Ver. | Basis |
|---|---|---|---|
| BKD-IFC-001 | The HTTP interface is generated from or checked against the API contract (contract-first). Controllers implement the generated interfaces or are verified against the contract on every change. A route, parameter, status or property that is not in the contract MUST NOT exist. | T | PP |
| BKD-IFC-002 | A controller does five things only: reads the request, validates its shape against the contract, builds a command or query, calls one use case, maps the result or error to the response (BKD-LAY-006). | I | CLA |
| BKD-IFC-003 | Request and response types are their own types in the interface layer. They are not entities, not persistence rows and not domain objects, and they are mapped by explicit mappers. Internal names and identifiers never appear on the wire. | T | PP |
| BKD-IFC-004 | Every error response, including the ones that the framework produces for unknown paths, unsupported methods, unsupported media types, malformed bodies and unhandled exceptions, MUST be a problem in the format of RFC 9457 with `application/problem+json`, a stable error code and the correlation identifier. | T | PD |
| BKD-IFC-005 | One central error mapper in the interface layer converts domain errors, application errors and unexpected exceptions into problems. The mapping from error code to status is one table, covered by tests. A domain error never decides an HTTP status itself. | T | PD |
| BKD-IFC-006 | An unexpected exception becomes a `500` problem with a generic code and the correlation identifier. The message, the stack trace, class names, SQL and file paths MUST NOT reach the client. The exception is kept for the operators in the plain log of the platform. | T | PD |
| BKD-IFC-007 | Status codes, headers and bodies follow HTTP semantics and the contract: `201` with `Location` for creation, `204` without body, `304` for conditional reads, `412` and `428` for failed preconditions, `409` for state conflicts, `422` or `400` for validation as the contract states, `503` with `Retry-After` for shedding. | T | HTTP |
| BKD-IFC-008 | The interface layer implements the contract's concurrency and idempotency features: `ETag` from the aggregate version, `If-Match` and `If-None-Match`, and the `Idempotency-Key` header where the contract declares it. A repeated request with the same key returns the original status and body, the same key with another request returns the documented error, and the key record is saved in the same transaction as the change. | T | HTTP |
| BKD-IFC-009 | Collections are paginated in the way the contract states (default: cursor pagination with an opaque cursor, a documented maximum page size, and a stable total order with a unique tie-breaker). The cursor is signed or encoded so that a client cannot depend on its content, and an invalid cursor returns a problem. | T | PP |
| BKD-IFC-010 | Limits are enforced at the edge of the application, before the use case runs: the maximum size of a request body, of a header, of a page, of an upload, and the time a request may take (BKD-DEP-004). An exceeded limit returns the documented problem. | T | HTTP |
| BKD-IFC-011 | The backend accepts the trace context header and returns a correlation identifier in a response header and in every problem, as the API contract states. The identifier is created when the request has none, and it is passed to every use case, outbound call and event. Nothing else of observability is built now (section 17). | T | PP |
| BKD-IFC-012 | Operational endpoints (liveness, readiness) are outside the versioned business contract, are documented separately, are not reachable from the public entry point, and expose no internal detail (BKD-DEP-010). | I | PP |
| BKD-IFC-013 | Strings that the user reads are not produced by the backend. Responses carry codes, values and data. Dates and amounts are in their neutral wire form (RFC 3339 instants and dates, decimal strings or integer minor units with a currency code), and formatting for a locale is the frontend's work. Where the product stores user-facing texts as data (for example content managed by editors), they are data with a language code, not messages. | T | PP |
| BKD-IFC-014 | The request language, where the product has several, is read from the header the contract names and passed to the use case as a value. It never changes the business result or the error code. | I | PP |
| BKD-IFC-015 | Message consumers and scheduled jobs are interfaces too: they live in the interfaces layer, translate the incoming message or trigger into a command, call one use case, and acknowledge only after success (section 12). | I | HEX |
| BKD-IFC-016 | Input is parsed strictly: unknown properties, duplicate keys, wrong types and values outside the declared ranges are refused as the contract says, and nothing is silently coerced or dropped. | T | PP |
| BKD-IFC-017 | File uploads and downloads stream through the interface layer without loading the whole content into memory. The file content is stored through a storage port; its metadata is stored by the domain that owns it. | T | PP |
| BKD-IFC-018 | The same behaviour is available regardless of the client: the backend makes no decision based on the identity of the frontend, the user-agent string or other client details, except for explicitly documented versioning. | I | PP |

## 9. Persistence and database access

The database document governs the schema design in detail. This section fixes how the backend uses the database.

| ID | Statement | Ver. | Basis |
|---|---|---|---|
| BKD-PER-001 | Each domain owns its tables, in a schema of its own (or a name prefix where the engine has no schemas). Only that domain's persistence adapters read or write them. Table and column names use the ubiquitous language, in one naming style, one language. | T | DDD |
| BKD-PER-002 | There is one repository port per aggregate, owned by the domain layer, with methods named in business terms (`findOpenByCustomer`, not `selectWhereStatus`). A repository loads and saves whole aggregates. A generic repository that exposes arbitrary queries to the application layer MUST NOT exist. | I | DDD |
| BKD-PER-003 | Persistence types (ORM entities, rows, query results) stay inside the persistence adapter. Lazy loading, proxies and open sessions MUST NOT escape the adapter. A domain object is fully loaded when it leaves the repository. | T | CLA |
| BKD-PER-004 | A domain MUST NOT join, query or reference the tables of another domain, and a database foreign key MUST NOT cross a domain boundary (**default**, so that a domain can be extracted). References to another domain's data are identifiers, checked through the published interface when written and tolerated when the referenced data is gone. | T | DDD |
| BKD-PER-005 | Constraints are the last line of defence and are always defined: `NOT NULL`, unique, check and foreign keys inside a domain. Application checks give good errors; constraints guarantee integrity under concurrency. A violated constraint is mapped to the documented conflict or validation error, never to a `500`. | T | PP |
| BKD-PER-006 | The default transaction isolation is the engine's default (read committed for most engines). A stronger level is chosen per use case by a recorded reason. Under repeatable read or serializable, a use case MUST retry the whole transaction on a serialization failure or deadlock, within a bounded number of attempts, and only when the use case is idempotent (BKD-APP-007). | T | REL |
| BKD-PER-007 | Aggregates have a version column. An update is conditional on the expected version, increments it, and a mismatch raises the conflict error (BKD-APP-008). The version is the source of the `ETag`. | T | HTTP |
| BKD-PER-008 | Pessimistic locks (row locks, advisory locks) are used only when optimistic concurrency cannot express the rule, are held for the shortest time, are taken in a fixed order, and are recorded with their reason. | I | REL |
| BKD-PER-009 | Identifiers of aggregates are created by the application through the injected generator (**default** time-ordered UUID, P06), are opaque to clients, and are never reused. Human-readable business numbers come from a sequence or a counter owned by the domain, are formatted by a rule, and are not the primary key. | T | PP |
| BKD-PER-010 | Instants are stored in UTC with a zone-aware type; calendar dates in a date type; money as exact decimals with a currency or as integer minor units; text in UTF-8 with an explicit collation. `created` and `updated` times are set from the injected clock by the application, not by database defaults, so that tests control them. | T | PP |
| BKD-PER-011 | Queries are bounded and paginated. Lists use keyset (cursor) pagination over a stable total order with a unique tie-breaker, backed by an index. Offset pagination is allowed only for small, bounded sets. The maximum page size is enforced in the query (P08). | T | PP |
| BKD-PER-012 | The N+1 query pattern MUST NOT occur. Loading a list loads what it needs with a bounded number of statements, selected explicitly. The integration tests of list use cases assert the number of statements for a list of several items. | T | PP |
| BKD-PER-013 | Every query that a use case runs is supported by an index that is created in a migration, together with the query. Foreign-key columns and sort keys are indexed. Indexes that nothing uses are reviewed and removed in a later migration. | I | PP |
| BKD-PER-014 | Document or JSON columns are used only for data whose shape is truly variable (for example a form definition stored as data), with a schema that is validated before saving. They MUST NOT replace modelling of data that the business rules read or constrain. | I | PP |
| BKD-PER-015 | A query that serves a screen MAY use a read model: a table or a view owned by one domain, shaped for reading, filled in the same transaction or from events. A read model is rebuildable from the source of truth, carries the version of the data it reflects, and is never written by a use case that changes an aggregate except by the mechanism that maintains it. | I | DDD |
| BKD-PER-016 | Information from several domains for one use case is composed in the application layer from the published queries of those domains, or from a read model that the consuming domain maintains from events. A reporting need MAY use a separate reporting store that is filled from events; it is not a shortcut around BKD-PER-004. | I | DDD |
| BKD-PER-017 | Soft deletion is used only when a requirement needs it (retention, restore, audit). Then one mechanism is used in every domain: a deletion marker with the time, excluded by default in every repository query, handled in unique constraints, and ended by a defined retention rule. Otherwise rows are deleted. | I | PP |
| BKD-PER-018 | Values reach the database only as bound parameters of a statement. A statement is never built by joining input into text. Raw statements are allowed in persistence adapters when the data-access framework cannot express the query. | T | PP |
| BKD-PER-019 | The connection pool is small and bounded: its size starts from the formula in P07 and is confirmed by a load test; the wait for a connection has a timeout; a separate pool serves long-running or batch work. The number of connections of all instances MUST stay below the database limit. | T | REL |
| BKD-PER-020 | Statement time and lock wait have limits set on the connection (P07). A statement that exceeds its limit fails with a documented error instead of holding a connection. | T | REL |
| BKD-PER-021 | Bulk changes and backfills run in batches of bounded size, each in its own transaction, are idempotent and resumable, and never run inside a request. | I | REL |
| BKD-PER-022 | All environments run the production database engine in the production major version. Engine-specific features are used deliberately and only in the adapters layer, and are recorded. No logic depends on a substitute engine. | T | PP |
| BKD-PER-023 | Every table has the columns the database document names for audit and concurrency (version, created and updated times). Columns for the acting user exist and stay empty or hold a system value until the security phase (section 17). | I | PP |
| BKD-PER-024 | A cache is not used by default. Where measurement shows the need, a cache is used with the cache-aside pattern inside one domain, with a bounded size, a time to live and invalidation by events, and it is never the source of truth. A cache keyed per user or per tenant includes that key. | I | PP |
| BKD-PER-025 | Files and large objects are stored through a storage port (object store or file service). The database holds metadata and the key of the object, not the content, unless a requirement says otherwise. | I | PP |

## 10. Schema migrations

| ID | Statement | Ver. | Basis |
|---|---|---|---|
| BKD-MIG-001 | The schema changes only through migrations: versioned, ordered, stored in the `migrations` folder of the domain that owns the tables (BKD-SLC-007), and applied by automation. A migration of a domain changes only that domain's tables. | T | EVO |
| BKD-MIG-002 | A migration that has been merged to the main branch MUST NOT be edited. A mistake is repaired by a new migration. | T | EVO |
| BKD-MIG-003 | A migration is small, has one purpose, and is named by that purpose (`add_status_to_invoice`). Schema changes and large data changes are separate migrations. | I | EVO |
| BKD-MIG-004 | Migrations are forward-only. A rollback of a release is a deployment of the previous version, which MUST run on the new schema (BKD-MIG-005). Down scripts are not relied on. | I | EVO |
| BKD-MIG-005 | Every migration is compatible with the previous release of the application (N-1 compatibility): the old code still works on the new schema. A change that breaks the old code is split by the expand-contract (parallel change) pattern over several releases. | T | EVO |
| BKD-MIG-006 | Expand-contract steps: (1) add the new structure, nullable or with a default, without removing the old one; (2) write to both, and backfill existing data in batches; (3) switch reads to the new structure; (4) stop writing the old one; (5) remove the old structure in a later release. Each step is a separate release. | I | EVO |
| BKD-MIG-007 | Migrations avoid long locks and table rewrites: new columns are nullable or have a cheap default; indexes are created without blocking writes where the engine supports it; constraints are added unvalidated and validated afterwards; a lock timeout is set. A migration that needs an exclusive lock on a large table is planned and recorded. | I | REL |
| BKD-MIG-008 | Destructive changes (dropping a table or column, narrowing a type, deleting data) are made only in the contract step, after the code no longer uses the structure for at least one release, with the approval of the user and a backup noted in the change. | I | EVO |
| BKD-MIG-009 | In shared and production environments migrations run once, as the `migrate` process role, before the new version of the application starts, under a lock that prevents concurrent runs. Application instances MUST NOT apply migrations on start in those environments. On a developer machine and in tests, they MAY apply them at start. | T | 12F |
| BKD-MIG-010 | A failed migration stops the deployment. The application refuses to start when the schema version differs from the one it expects (validation mode, no schema generation). | T | EVO |
| BKD-MIG-011 | Data migrations are idempotent, tested on realistic data, and verified by counts or checksums before and after. They report what they changed. | T | EVO |
| BKD-MIG-012 | Reference data that the application needs to run (lookup values, state tables) is loaded by versioned or repeatable migrations. Demonstration and development data is loaded by a separate seeder and never by migrations. | I | PP |
| BKD-MIG-013 | A migration linter runs on every change: version order and naming, no edit of a merged migration, no change of another domain's tables, no destructive statement without an approved marker, no statement that requires an exclusive lock without a recorded reason. | T | PP |
| BKD-MIG-014 | The current schema of every domain is documented in the database document, and the document changes in the same change as the migration. | I | PP |
| BKD-MIG-015 | Each domain keeps its own migration history (a history table in its own schema), so that the history moves with the domain. The `migrate` role runs the histories of all domains. No migration depends on the state of another domain's schema, and the order between domains does not matter. | T | EVO |

## 11. Integration with other systems

An external system is any system outside the backend's own process and database: a third-party service, another product's API, a mail or payment gateway, an identity or storage service. A system that the product itself owns and runs as a separate deployable is external for this section.

| ID | Statement | Ver. | Basis |
|---|---|---|---|
| BKD-INT-001 | Every external system is used through a port owned by the application layer, named in domain terms (`PaymentGateway`, not `StripeClient`), and implemented by one adapter in `adapters`. Business code never imports a vendor library. | T | HEX |
| BKD-INT-002 | The adapter is an anti-corruption layer. It translates between the external model and the domain's model in both directions. External types, status codes and error texts do not leave the adapter. It reads responses tolerantly (unknown fields are ignored) and validates what it relies on. | I | DDD |
| BKD-INT-003 | Every outbound call has a timeout for connecting and a timeout for the whole call, set per dependency (P09). A call without a timeout MUST NOT exist, including calls to other processes on the same machine. | T | REL |
| BKD-INT-004 | Retries are made only for transient failures (timeouts, connection errors, `502`, `503`, `504`, `429` with `Retry-After`), only for idempotent operations or with an idempotency key, with exponential backoff and jitter, with a small maximum number of attempts (**default** 3, P09), and at one layer of the call stack only. Client errors (`4xx`) are not retried. | T | REL |
| BKD-INT-005 | From its trigger (BKD-TRG-001), each dependency has a circuit breaker that opens when the failure rate or the slow-call rate passes a threshold, answers fast while open, and probes with a few calls before it closes. The thresholds are parameters (P09). Until the trigger, BKD-INT-003, BKD-INT-004 and BKD-INT-007 are the protection. | T | REL |
| BKD-INT-006 | From its trigger (BKD-TRG-001), each dependency has its own limits (concurrent calls, thread or connection pool) so that a slow dependency cannot exhaust the resources of the whole application (bulkhead). Until then the bounded pools and deadlines of section 13 apply. | T | REL |
| BKD-INT-007 | For each dependency, the behaviour when it is unavailable is decided and documented in the domain inventory: fail fast with a documented error (`503` problem with a code), use stored or cached data and mark it as such, defer the work through the outbox, or skip an optional enrichment. The choice is a business decision confirmed by the user. | I | REL |
| BKD-INT-008 | *Withdrawn.* Merged into BKD-APP-006 (no outbound call inside an open database transaction). | – | – |
| BKD-INT-009 | Calls that change state in the external system carry an idempotency key derived from a stable business identifier, so that a retry or a repeated job does not duplicate the effect. Where the external system offers no idempotency, the adapter checks the state before repeating, and the risk is recorded. | T | HTTP |
| BKD-INT-010 | The responses of external systems are never trusted for the domain: values are validated, sizes are limited, and unexpected shapes are mapped to an error of the adapter that the use case handles. | T | PP |
| BKD-INT-011 | The contract of each external system (its API description, or recorded examples with date and source) is stored in the repository. The adapter is checked against it, and a change of the provider's contract is detected by that check. | T | PP |
| BKD-INT-012 | Addresses and credentials of external systems come from configuration, differ per environment, and point to sandboxes outside production. No test run reaches a real recipient, account or payment. | T | 12F |
| BKD-INT-013 | The rate limits of an external system are respected on the client side (a limiter in the adapter), and a `429` response is handled as a transient failure with the delay given. | T | REL |
| BKD-INT-014 | Inbound callbacks from external systems (webhooks) are entry points in the interfaces layer. They record the message quickly, answer, and process it as a background job; processing is idempotent by the external event identifier. The authenticity checks of such callbacks are part of the deferred security work (section 17). | I | OUT |
| BKD-INT-015 | Data sent to an external system is limited to what it needs. Non-production environments send synthetic data only. | I | PP |
| BKD-INT-016 | Reference data fetched from an external system and cached is cached with a time to live and a stale-if-error rule, so that a short outage does not stop the application. | I | REL |
| BKD-INT-017 | An external system is used by one domain through that domain's own port. When two domains need the same external system, each declares its own port, and a transport client of the platform module MAY carry the technical part (connection, timeouts, retries). Business mapping is never shared. | I | DDD |
| BKD-INT-018 | Asynchronous results from an external system (a payment confirmation, a document that was processed) are handled as events into the owning domain, with a timeout rule for the case that the result never arrives. | I | REL |
| BKD-INT-019 | A flow that calls several external systems or domains in steps, and that must be undone if a later step fails, is modelled as a process with explicit steps, stored state and compensating actions (BKD-EVT-012), not as one long synchronous call. | I | REL |

## 12. Events and messaging

The default is in-process events inside the one application. A message broker is a later extension (BKD-EVT-010).

| ID | Statement | Ver. | Basis |
|---|---|---|---|
| BKD-EVT-001 | Aggregates raise domain events (BKD-DMN-013). The application layer collects them and dispatches them after the aggregate is saved, in the same use case. Dispatching never changes the result of the use case that raised them. | T | DDD |
| BKD-EVT-002 | Two kinds of event exist. A domain event is internal to a domain. An integration event is published to other domains or systems, belongs to the published interface, has a documented and versioned shape, and is not the same type as the domain event (it is mapped). | I | DDD |
| BKD-EVT-003 | Handlers in other domains run after the commit of the publishing transaction, each as its own use case in its own transaction. A handler failure never rolls back the publisher. The delivery is at least once. | T | OUT |
| BKD-EVT-004 | An integration event that must not be lost is written to an outbox table in the same transaction as the state change. A relay reads the outbox in order and publishes the events, marks them as sent, and retries on failure. This gives delivery at least once, and an event exists if and only if the change was committed. | T | OUT |
| BKD-EVT-005 | Consumers are idempotent. A consumer records the identifiers of processed messages, in the same transaction as its effect, or is idempotent by nature. A message delivered twice has the same effect as one. | T | OUT |
| BKD-EVT-006 | Every integration event has an identifier, a type name, a version, the time it occurred, the identifier of the aggregate, the correlation identifier and a payload. Changes are additive within a version. A breaking change creates a new version, and both versions are published during the transition. | T | OUT |
| BKD-EVT-007 | An event states a fact that happened (`CaseClosed`). It is not a hidden command (`PleaseSendMail`). A command is a direct call to the published interface of the domain that is meant to act. | I | DDD |
| BKD-EVT-008 | Events carry the data consumers need, so that they do not have to call back (event-carried state), but not more than that, and no data that the consumer is not allowed to hold. Otherwise they carry the identifier and the consumer queries the published interface. | I | DDD |
| BKD-EVT-009 | A handler that fails is retried a bounded number of times with backoff and jitter. After the last attempt the message goes to a dead-letter store with its error, and later messages are processed. A documented procedure replays a dead letter after the cause is repaired. A poison message never blocks the queue. | T | REL |
| BKD-EVT-010 | No global ordering of events is assumed. Ordering per aggregate is provided where it is required (by a key and a sequence number), and consumers tolerate duplicates and out-of-order delivery by checking the version of the aggregate. A message broker is introduced only when a requirement needs it (consumers in other processes, external subscribers, durable fan-out). Then the outbox relay publishes to the broker, the published interface does not change, and the broker is hidden behind a port. | I | MON |
| BKD-EVT-011 | Every integration event is listed in the event catalogue of its domain: name, version, producer, known consumers, payload schema and an example. The catalogue is checked by tests. When a broker exists, an asynchronous API description (AsyncAPI) MAY hold it. | I | PP |
| BKD-EVT-012 | No distributed transaction (two-phase commit) is used. A business process across domains or systems is a saga or a process manager: explicit steps, state stored in the database, compensating actions for each step that can be undone, time limits, and a defined end state when a step fails. | I | REL |
| BKD-EVT-013 | Read models that are filled from events are rebuildable by replay, carry the version they reflect, and ignore events they have already applied. | T | OUT |
| BKD-EVT-014 | Time-based triggers (expiry, reminders, timeouts) are stored as data with a due time and are started by a job entry point that finds due items. In-memory timers MUST NOT hold business deadlines, because they are lost on restart. | T | REL |
| BKD-EVT-015 | Event sourcing and CQRS with separate stores are not the default. They are introduced for one domain at a time, only for a stated requirement, and are recorded as a decision (section 18). | I | PP |
| BKD-EVT-016 | The in-process dispatcher is part of the platform module, supports the outbox and the idempotent-consumer record, and has the same interface that a broker adapter will have, so that a later broker changes no domain code. | I | HEX |

## 13. Dependable behaviour

A dependable backend keeps working correctly, or fails in a defined and harmless way, when parts of its environment fail. The attributes are availability (ready when needed), reliability (continuity of correct service), integrity (no improper change of data), and maintainability (ease of repair and change). The rules below apply them.

| ID | Statement | Ver. | Basis |
|---|---|---|---|
| BKD-DEP-001 | Every dependency (database, external system, broker, file store, other domain) can fail. For each use case and each dependency that it uses, the result of a failure is defined: the status and the error code, whether the work is retried, and what state remains. The definitions are recorded in the domain inventory. | T | REL |
| BKD-DEP-002 | The application fails fast on what it can know at start or at the edge: invalid or missing configuration stops the start; invalid input is refused before any work; an unmet precondition is refused before any change. | T | REL |
| BKD-DEP-003 | Every wait is bounded: connections, locks, pool acquisition, queue waits, outbound calls, statements and requests. An unbounded wait MUST NOT exist. | T | REL |
| BKD-DEP-004 | Every request has a deadline (**default** 30 seconds, P10). The remaining time is passed to downstream calls, so that work for an abandoned request is stopped. A request that exceeds its deadline ends with the documented problem (`503` or `504` with a code), and its transaction is rolled back. | T | REL |
| BKD-DEP-005 | Overload is handled by shedding load early. Concurrency and queues are bounded (P10). When a limit is reached, the backend answers at once with `503`, a problem code and `Retry-After`, instead of queueing without limit. The first line of defence is the configured limits of the server and the pools; custom shedding code follows its trigger (BKD-TRG-010). | T | REL |
| BKD-DEP-006 | Resources are bounded in size: request and response bodies, page sizes, in-memory collections, caches, thread and connection pools, upload sizes and background batches. Growth of memory per request or per instance over time is a defect. | T | REL |
| BKD-DEP-007 | Optional capabilities degrade instead of failing the whole operation (for example an enrichment from an external system is skipped, with the response marking it absent). The degraded path is specified in the domain inventory and is tested. | T | REL |
| BKD-DEP-008 | Retries do not multiply: a retry is made at one layer only (BKD-INT-004), the clients of the backend retry only idempotent operations (BKD-IFC-008), and a retry never runs after the request deadline. | T | REL |
| BKD-DEP-009 | Instances hold no state that another instance or a restart would miss: no session in memory, no business data in local files, no in-memory queue of work that is not also stored. Temporary files are small, private to the request, and deleted. Sticky sessions MUST NOT be required. | T | 12F |
| BKD-DEP-010 | The backend offers a liveness check and a readiness check on separate endpoints (BKD-IFC-012). Liveness reports only that the process is running and not stuck, and uses no dependency. Readiness reports that the instance can serve now (the database is reachable, the schema is at the expected version, the required configuration is loaded). Both answer quickly and with a bounded time. Readiness does not include optional dependencies, so that a failure of an optional system does not take every instance out of service. | T | REL |
| BKD-DEP-011 | An instance starts quickly (**default** within 30 seconds, P10) and becomes ready only when it can serve. It does not wait for optional systems. Work that takes long is done after readiness or by the `migrate` or `worker` role. | T | 12F |
| BKD-DEP-012 | On a termination signal the instance stops accepting new requests, finishes the requests in flight within a grace period (P10), stops consumers after their current message, releases locks, closes pools, and exits. Jobs checkpoint their progress. A forced kill after the grace period MUST NOT lose committed work. | T | 12F |
| BKD-DEP-013 | The process may be killed at any moment. Every multi-step piece of work is recoverable from stored state: the half-done step is repeated or completed after the restart, and no step leaves the data in a state that no code path can repair. | T | REL |
| BKD-DEP-014 | Integrity is protected twice: by the aggregates and use cases (good errors) and by the database constraints (guarantee). A use case that must not leave partial changes runs in one transaction. A consistency check job MAY compare derived data with its source and report differences. | T | PP |
| BKD-DEP-015 | Operations are idempotent wherever the semantics allow, and otherwise protected against repetition by a business key or an idempotency record: writes, jobs, message consumers and callbacks. | T | HTTP |
| BKD-DEP-016 | Shared mutable state between requests does not exist. Singletons are immutable or thread-safe. In runtimes with an event loop, a blocking call in the request path is a defect. In runtimes with threads, the number of threads is bounded by configuration. | I | REL |
| BKD-DEP-017 | Consumers of messages and jobs limit their concurrency and the number of messages they pre-fetch, so that a burst is absorbed by the queue and not by memory. | T | REL |
| BKD-DEP-018 | A background job is idempotent and resumable: it processes work in bounded batches, stores its progress, can be started again after a crash, has a timeout, retries a failed batch a bounded number of times, ends in a visible final state (completed, failed with a reason), and can be read by a status query. | T | REL |
| BKD-DEP-019 | A job or schedule that must run once across all instances takes a lock or lease in the database with an expiry, so that the lock of a crashed instance ends by itself. A job is still safe if it runs twice (BKD-DEP-015). | T | REL |
| BKD-DEP-020 | All instances use UTC, and the code does not rely on the clocks of different instances being equal. Where the order of events across instances matters, it comes from the database (a sequence or a version), not from timestamps. | T | PP |
| BKD-DEP-021 | Expected failures are returned as typed errors; unexpected failures are exceptions that are caught only at the boundary. Exceptions keep their cause. Nothing is caught and ignored. Resources are released by scoped constructs of the language (try-with-resources, `using`, `defer`, context managers). | T | PP |
| BKD-DEP-022 | Failures are classified: client error, rule violation, conflict, transient dependency failure, permanent dependency failure, defect. Each class has a status, an error code family and a retry rule, recorded in the error catalogue of the API contract. | I | PD |
| BKD-DEP-023 | The expected load, the response-time targets and the availability target come from the non-functional requirements. The runtime (heap, threads, pools) is configured for the memory and processor limits that the deployment sets. | A | REL |
| BKD-DEP-024 | A release is compatible with the previous one while both run (rolling deployment): the database schema (BKD-MIG-005), the HTTP contract (versioning rules of the API contract) and the integration events (BKD-EVT-006) all work for both versions. | T | EVO |
| BKD-DEP-025 | Feature switches are few, temporary and held in configuration. Each has an owner and a removal date, is read at the entry of a use case and not inside the domain, and both of its positions are tested. | I | PP |
| BKD-DEP-026 | A failure is never reported as success, and a success is never reported as failure. A timeout whose outcome is unknown is reported as such (the client can repeat an idempotent request or read the state). | T | HTTP |

## 14. Configuration, processes and lifecycle

| ID | Statement | Ver. | Basis |
|---|---|---|---|
| BKD-CFG-001 | Per-environment settings (addresses, credentials, limits, switches) come from the environment of the process (environment variables or a configuration service). Files in the repository hold structure and defaults only, never secrets and never the address of a real external system. | T | 12F |
| BKD-CFG-002 | Each domain declares its settings as one typed object. All settings are loaded and validated at start. A missing or invalid required setting stops the start with a message that names the setting and not its value. | T | 12F |
| BKD-CFG-003 | All settings are listed in one table in the repository (name, meaning, type, default, required, example), kept in step with the code. Names are upper-case, prefixed by the product and the domain (`BILLING_PAYMENT_TIMEOUT_SECONDS`) and carry their unit. | I | 12F |
| BKD-CFG-004 | The same artefact runs in every environment. The code contains no branch on the name of the environment. Differences are only values of settings. | T | 12F |
| BKD-CFG-005 | Defaults are safe: a default never points to production or to a real external system, and never switches a protective limit off. | I | 12F |
| BKD-CFG-006 | Settings are read once at start and are immutable. A setting that can change at run time (a switch) is read through a port and documented as reloadable. | I | 12F |
| BKD-CFG-007 | Build, release and run are separate. The artefact is built once, combined with configuration to a release, and run unchanged. Nothing is compiled or installed at run time. | T | 12F |
| BKD-CFG-008 | The process role (`web`, `worker`, `migrate`) is selected by one setting or argument of the one artefact. Administrative tasks (migrations, one-off data repairs, seeders) run as one-off processes of the same artefact with the same configuration. Ad-hoc statements on a shared database are made only by a documented, reviewed procedure. | T | 12F |
| BKD-CFG-009 | The application writes events of its run as a stream to standard output and standard error and manages no log files. The design of logging is deferred (section 17). | I | 12F |
| BKD-CFG-010 | The application binds its own port, given by a setting. Termination of encrypted connections, compression and routing belong to the edge (infrastructure document), and the application behaves correctly behind a reverse proxy (it honours the forwarded-address headers that the infrastructure names). | T | 12F |
| BKD-CFG-011 | Development, test and production are as alike as possible: the same database engine and version, the same migrations, the same artefact, the same process roles. One documented command starts the backend and its dependencies locally. | T | 12F |
| BKD-CFG-012 | The process runs with the time zone UTC and a fixed locale for formatting that is not part of the behaviour. Behaviour that depends on a zone or a language takes it as input. | T | PP |
| BKD-CFG-013 | Secrets (passwords, keys, tokens) are never in the repository, in the image, in logs, in error messages or in URLs. Test and local environments use fake values. The tooling for managing and scanning secrets is part of the deferred security work (section 17). | I | 12F |

## 15. Clean code

| ID | Statement | Ver. | Basis |
|---|---|---|---|
| BKD-CLN-001 | Names reveal intention and come from the ubiquitous language. A name says what a thing is or does, in full words. Abbreviations are used only when the glossary has them. A concept has one name everywhere, and names do not repeat the type (`customerList`, `IUserService`) or a technical detail that the reader does not need. | I | DDD |
| BKD-CLN-002 | A function does one thing, on one level of abstraction, and has one reason to change. The limits of 21.2 (lines, nesting, parameters, cyclomatic complexity) are checked by the static analysis. A function that exceeds them is split, or the exception is recorded. | T | PP |
| BKD-CLN-003 | A class or module has one responsibility and is cohesive. A class named `Manager`, `Helper`, `Processor` or `Util`, or a service with many unrelated methods, MUST NOT exist. A file stays within the size limit of 21.2. | I | PP |
| BKD-CLN-004 | The design follows the SOLID principles: one responsibility per unit; extension by new implementations of a port rather than by editing existing code; implementations that can replace each other without surprise; small ports that a client needs completely; dependence on abstractions that the inner layer owns. | I | CLA |
| BKD-CLN-005 | Composition is preferred to inheritance. Inheritance is used only for a true is-a relation where subtypes can replace the base type, and hierarchies stay shallow (at most two levels). | I | PP |
| BKD-CLN-006 | Data is immutable by default. Mutation is confined to aggregates and to local variables of a function. Shared mutable state does not exist (BKD-DEP-016). | I | PP |
| BKD-CLN-007 | Control flow is flat: guard clauses and early returns replace nesting; a boolean parameter that switches behaviour is replaced by two functions; a long chain of conditions on a type is replaced by polymorphism or a table. | I | PP |
| BKD-CLN-008 | No magic values. Numbers and strings that carry meaning are named constants, value objects or settings, and a name that holds a quantity carries its unit (`timeoutSeconds`). | I | PP |
| BKD-CLN-009 | Comments explain why, never what. Code that needs a comment to be understood is rewritten. Commented-out code MUST NOT be committed. A `TODO` carries the identifier of a task and an owner. The published interface of each domain is documented. | I | PP |
| BKD-CLN-010 | Dead code, unused dependencies, unused settings and unreachable branches are deleted, not kept for later. | T | PP |
| BKD-CLN-011 | Inside a domain, duplication of knowledge (one rule in two places) is removed. Across domains, duplication is allowed and often better (3.4). Duplication of text that expresses different knowledge is allowed. An abstraction is extracted when the same change must be made in several places, and not in advance (rule of three, no speculative generality). | I | PP |
| BKD-CLN-012 | Dependencies are given through the constructor (or the parameters of a function), by the composition root. A service locator, a static mutable singleton, a global registry and hidden global state MUST NOT be used. Time, identifiers and randomness are dependencies. | T | CLA |
| BKD-CLN-013 | Objects tell, they are not asked: behaviour sits with the data it uses. Chains of calls through the internals of other objects (train wrecks) are avoided. | I | DDD |
| BKD-CLN-014 | Formatting, import order and naming style are enforced automatically with one configuration for the whole backend. The pipeline fails on a violation. Reviews do not discuss formatting. | T | PP |
| BKD-CLN-015 | The strictest practical mode of the compiler or type checker is on (strict null checks, no implicit any, no unchecked conversions). Escape hatches of the type system (`any`, `dynamic`, unchecked casts, reflection) are not used in the domain and application layers, and elsewhere only with a comment that gives the reason. | T | PP |
| BKD-CLN-016 | Static analysis runs on every change: complexity, duplication within a domain, unused code, null safety, common defect patterns. A new finding fails the stage unless it is recorded as accepted with an owner and a reason. | T | PP |
| BKD-CLN-017 | Strong types are used for identifiers and quantities so that an order identifier cannot be passed where a customer identifier is expected. | I | DDD |
| BKD-CLN-018 | Business code writes no log statements, creates no metrics and adds no tracing calls. The platform's boundary handler records unexpected errors in the plain log of the framework. The design of logging, metrics and tracing is deferred (section 17). | I | PP |
| BKD-CLN-019 | Everything is internal by default. The visible surface of a module, a class and a package is as small as the use requires. | T | PP |
| BKD-CLN-020 | A new abstraction, layer, library or pattern needs a reason that a requirement or a measured problem gives. "It might be needed" is not a reason. | I | PP |
| BKD-CLN-021 | Refactoring is done in small steps under passing tests and changes no behaviour. Tests of behaviour do not change in a refactoring. A refactoring is a commit of its own or is clearly separated in the change. | I | PP |
| BKD-CLN-022 | Generated code is marked, never edited by hand, and produced by the build from its source (the API contract, for example). | T | PP |
| BKD-CLN-023 | A method that returns a value does not change state, and a method that changes state returns nothing or only an acknowledgement (command and query separation), except where the platform's idiom for atomic operations requires otherwise. | I | PP |

## 16. Build and dependencies

| ID | Statement | Ver. | Basis |
|---|---|---|---|
| BKD-BLD-001 | One command builds, checks and tests the whole backend. Modules are built in the order of the domain dependency graph (BKD-MOD-007). A change builds and tests what it affects when the build tool supports it, and the backend part of one domain can be built and tested alone together with the shared parts it uses. | T | PP |
| BKD-BLD-002 | Dependency versions are pinned. A lock file is committed. One version of each library is used across the backend, from a central catalogue or bill of materials. An upgrade is a change that needs a green run. | T | PP |
| BKD-BLD-003 | The domain layer has no third-party dependency. The application layer has only dependencies that are free of input and output. Frameworks and drivers are dependencies of the adapters layer, interfaces and the composition root. | T | CLA |
| BKD-BLD-004 | Dependencies are kept current by routine maintenance, and a library that is no longer maintained is not introduced into the core path. Vulnerability scanning of dependencies is part of the deferred security work. | I | PP |
| BKD-BLD-005 | The artefact is built once per commit, tagged by the commit, and used in every environment. It is one process, takes its configuration from the environment, and exposes the port and the operational endpoints. Packaging details belong to the infrastructure document. | T | 12F |
| BKD-BLD-006 | Where the platform's contract tool generates server interfaces and models, the build produces them and fails when the code and the contract differ; otherwise a contract test makes the same comparison. The generator is a build-time package (16.1). | T | PP |
| BKD-BLD-007 | Each domain has a short README in its backend folder: responsibility, owner, published surface (interface, operations), events published and consumed, tables, settings, failure behaviour. It changes in the same change as the domain. | I | PP |
| BKD-BLD-008 | Documents that describe the backend (the API contract, the database document, this document) are versioned in the repository and change in the same change as the behaviour. | I | PP |

### 16.1 External packages (dependency policy)

Every external package is a cost: it has to be updated, understood, debugged and trusted, it brings its own dependencies, and it can be abandoned. The backend therefore uses few packages. The standard library and the chosen framework come first, then a few lines of own code, and a package only when those are not enough. A little copying is better than a little dependency, and a hand-written replacement of something hard (cryptography, parsing, time zones, database protocols) is worse than either. The rules apply to runtime, build-time and test-time packages; *package* means any library, plug-in, annotation processor, code generator or container base image that the backend takes from outside the repository.

| ID | Statement | Ver. | Basis |
|---|---|---|---|
| BKD-DPN-001 | The default answer to a new package is **no**. A package is added only for a stated requirement, after the order of preference of BKD-DPN-002 was followed, and with an entry in the dependency register (24.7). | I | DEP |
| BKD-DPN-002 | Order of preference: (1) the standard library of the language; (2) the facilities of the chosen framework; (3) own code of small size (**default** at most 100 lines, P12) with tests; (4) one external package. A package is accepted only when the earlier steps do not meet the requirement. | I | DEP |
| BKD-DPN-003 | A package MUST NOT be added for something that own code of the size in BKD-DPN-002 does in a few functions and that is not hard to get right (trimming, joining, simple mapping, null checks, small collection helpers, simple retry loops that the platform's client already offers, enum helpers). | I | DEP |
| BKD-DPN-004 | Own code MUST NOT replace what is hard to get right and where mistakes are costly: cryptography and password hashing, parsing of standard formats (JSON, XML, CSV, dates and time zones), database drivers and protocols, TLS and HTTP servers, schema migration engines, and the test runner. A reviewed package of the platform is used for them. | I | DEP |
| BKD-DPN-005 | One package per concern. Two packages for the same purpose (two JSON libraries, two HTTP clients, two validation libraries, two mocking libraries, two date libraries) MUST NOT exist, and neither must a package that provides a second way to do what the framework already does. | T | DEP |
| BKD-DPN-006 | The **foundation set** is allowed without further discussion and is recorded once (24.7): the language and framework of P01; the database driver; the migration tool; and the build-time tools for format, static analysis and architecture tests (P04). Every other package needs its own entry and the user's approval (BKD-DPN-021). | I | DEP |
| BKD-DPN-007 | The framework is used with only the modules that the MVP needs. Starter bundles, meta-packages and umbrella packages that pull in unused modules are replaced by the individual modules. Modules for deferred concerns (security, metrics, tracing, API documentation interfaces, administration consoles) are not on the class path before their phase (section 17). | T | DEP |
| BKD-DPN-008 | Utility collections of general purpose (large "commons" or "utils" libraries, functional-programming toolkits, date helpers that duplicate the standard library) MUST NOT be used. The standard library is used, and a missing helper is own code in the module that needs it. | I | DEP |
| BKD-DPN-009 | Libraries that rewrite code at build time or run time (annotation processors that generate accessors, builders or constructors; byte-code weaving; reflection-based object mappers and copiers) MUST NOT be used where the language has the feature (records, data classes, properties, named arguments, copy methods). Mapping between types is written explicitly, because it is short, checked by the compiler and visible in review. | I | DEP |
| BKD-DPN-010 | A data-access technique is chosen as the lightest one that meets the rules of section 9: SQL with a thin helper, a query builder, or an object-relational mapper. An object-relational mapper is optional and is never required. If one is used, it is confined to the persistence adapters, its lazy loading and implicit flush are not used across the adapter (BKD-PER-003), and its schema generation is off (BKD-CON-010). | I | DEP |
| BKD-DPN-011 | The domain layer has no external package (BKD-BLD-003). The application layer has none either, except a package that is free of input and output and that the domain document names as needed (for example an exact decimal type when the language lacks one). All other packages are used in the adapters layer, interfaces and the composition root only. | T | CLA |
| BKD-DPN-012 | A package is wrapped by the adapter or the platform module that uses it. Its types do not appear in ports, in published interfaces, in events, in the API contract or in the domain, and replacing the package changes one adapter (BKD-LAY-005). | T | HEX |
| BKD-DPN-013 | The SDK of an external vendor is used only inside the adapter of that vendor behind a port. Where the adapter needs one or two operations, a plain call with the standard HTTP client is preferred to the SDK, because the SDK brings many dependencies and a release rhythm of its own. | I | DEP |
| BKD-DPN-014 | Before a package is added, it is assessed and the result is written in the register: purpose and the requirement it serves; the alternatives considered (standard library, framework, own code); licence and whether it is allowed (**default** list in P12); maintenance (last release, open defects, number of maintainers); use by others; size; number of transitive packages it adds; known problems in public advisory lists at the time; and the owner who will update it. | I | DEP |
| BKD-DPN-015 | A package is not added if: it has had no release for **default** 12 months (P12) and no stable-by-design reason; its licence is not on the allowed list; it adds more than **default** 25 transitive packages (P12) without a recorded reason; it is a single-maintainer project on the critical path without a recorded plan for its replacement; or it needs install scripts, native builds or network access at build time that the build does not already have. | I | DEP |
| BKD-DPN-016 | Packages come only from the official registry of the platform or from the foundation set's own organisation. A package from a source repository address, a local file, a copy placed in the repository (vendoring) or a personal fork needs a recorded decision with the reason and the plan to return to the registry. | I | DEP |
| BKD-DPN-017 | Code copied from outside (examples, articles, other projects) is treated as a package: its licence is checked, the source is named in a comment at the place, and it follows the rules for own code (tests, limits of 21.2). | I | DEP |
| BKD-DPN-018 | Versions are pinned exactly and locked (BKD-BLD-002). A range, `latest`, a snapshot or a pre-release version MUST NOT be used outside a recorded experiment. Versions are set in one central place for the whole backend. | T | DEP |
| BKD-DPN-019 | The number of direct packages is limited by the **dependency budget** (21.2, P12), counted separately for runtime packages and for build and test packages. Going beyond the budget needs a review, a recorded reason and the removal or merging of another package where possible. | T | DEP |
| BKD-DPN-020 | The dependency register (24.7) and the lock file agree. A direct package that is not in the register, or a register entry without use, fails the gate (BKD-QGT-009). Build-only packages are marked as such and MUST NOT be in the runtime artefact (BKD-BLD-005). | T | DEP |
| BKD-DPN-021 | An agent MUST NOT add a package outside the foundation set without the approval of the user. It proposes the package with a complete register entry and the alternatives, and the user answers. A package that an agent suggests for convenience (a helper, a faster build, a nicer output) is refused unless it meets BKD-DPN-002. | I | DEP |
| BKD-DPN-022 | Automatic discovery and wiring (class-path scanning, auto-configuration, plug-in loading, implicit module registration) is limited to what the composition root names, so that adding or removing a package cannot silently change behaviour, add an endpoint, add a security default or open a port (BKD-FWK-003). | T | DEP |
| BKD-DPN-023 | Own code that replaces a package is subject to the same rules as every other code: the limits of 21.2 and a place in one module with an owner. It is not a loophole for helpers without care. | I | DEP |
| BKD-DPN-024 | Packages are kept current in small steps, as a routine at the end of each phase and when a defect or advisory concerns a package in use. A major upgrade is a separate change with its own green run. A package whose upgrades keep breaking the build is a candidate for replacement. | I | DEP |
| BKD-DPN-025 | At the end of every phase the register is reviewed (the **dependency diet**): unused packages are removed, packages that serve one small use are replaced by own code where BKD-DPN-003 allows, and overlaps are merged. The result is recorded. | I | DEP |
| BKD-DPN-026 | One build tool and one package manager are used for the backend. A second build tool, a second package manager or a second language runtime in the backend needs a recorded reason. Code generators run in the build, are counted as packages, and their output is never edited by hand (BKD-CLN-022). | I | DEP |
| BKD-DPN-027 | When the platform offers both a built-in facility and an add-on package (configuration binding, validation, HTTP client, scheduling, dependency injection, JSON, logging facade, resource pooling, health reporting), the built-in facility is used until a requirement shows its limit. The limit is recorded when the add-on is chosen. | I | DEP |

The table below is informative. It shows, per platform, the usual foundation set of an MVP and what is usually not needed. It names example tools only; it is not a list to install. A tool not listed is judged by the rules above, and the choice is recorded in 24.4 and 24.7.

| Platform | Usually enough for an MVP | Usually not needed (decide against it unless a requirement says otherwise) |
|---|---|---|
| Java or Kotlin with Spring Boot | The web module, the database driver, a JDBC helper or one mapper of choice, one migration tool; records instead of generated accessors | Annotation processors for accessors and mappers, utility libraries, security and metrics modules before their phase, a second JSON or HTTP library, API-documentation interfaces |
| C# with .NET | The platform's web framework, dependency injection, configuration, `HttpClient`, JSON and logging abstractions; one database driver; one migration approach | Libraries for object mapping and in-process mediators, a second validation library, resilience libraries before BKD-TRG-001, authentication packages before their phase |
| PHP with Laravel or Symfony | The framework's own routing, validation, migrations, queue with the database driver, scheduler; one architecture-check tool as a development package | Helper packages for strings, arrays or dates, packages that add admin panels, permission or audit modules before their phase, a second HTTP client |
| Node.js with TypeScript | One web framework, one database driver, one migration tool, one schema-validation approach, the built-in `fetch` and environment access | Utility collections, date libraries that the standard library replaces, a second HTTP client, a mapping library, decorators-based object mappers, a process manager inside the image |
| Python | One web framework, one database driver, one migration tool, the standard `logging`, `datetime`, `json` and `urllib` | Data-analysis packages for business logic, a second HTTP client, task-queue frameworks before BKD-TRG-008, packages that wrap the standard library |
| Go | The standard `net/http`, `database/sql` with one driver, one migration tool | Web frameworks that the standard router replaces, ORMs, utility libraries, dependency-injection containers |
| Other | The platform's standard library, one framework, one database driver, one migration tool | Anything that duplicates the standard library or the framework |

## 17. Deferred items

| ID | Statement | Ver. | Basis |
|---|---|---|---|
| BKD-DEF-001 | The following are deferred to the final project phase, and agents MUST NOT implement, configure, scaffold or modify them earlier: authentication and sessions; authorisation, roles, permissions and tenant isolation; the actor of an operation; input hardening beyond correctness; security response headers and cross-origin policy design; rate limits and abuse detection; secret management tooling and secret scanning; dependency vulnerability scanning; image and database privilege hardening; encryption design for stored data; audit logging of calls. | I | PP |
| BKD-DEF-002 | The following are deferred on the same terms: structured logging design and log shipping, metrics, tracing export, alerting, dashboards, service level objective tooling and usage analytics. | I | PP |
| BKD-DEF-003 | Not deferred and required now: the correctness rules of this document, including bound parameters in statements, errors without internal details, the correlation identifier and the trace context header, the liveness and readiness checks, configuration from the environment, bounded waits, timeouts and retries, and the handling of failures. | I | PP |
| BKD-DEF-004 | The design keeps the way open and scaffolds nothing: use cases receive a request context with a correlation identifier and a slot for an actor that is empty until the security phase (BKD-APP-010); tables have columns for the acting user (BKD-PER-023); operations of the contract carry their security declarations (API document); the platform module has a place for use-case decorators (BKD-APP-014). | I | PP |
| BKD-DEF-005 | Until the security phase the backend has no authentication, and no placeholder for it exists: no permit-all configuration, no fake login, no mock users, no role annotations. Because the operations are open, the backend MUST NOT be deployed to an environment that is reachable from the public internet or that holds real data. | I | PP |
| BKD-DEF-006 | When the final phase starts, the agent writes a plan for each deferred area from the requirements and the domain inventory, and the user approves it before implementation. The plan lists, for each domain, the operations, the roles, the data to protect, the audit events and the telemetry to add. | I | PP |
| BKD-DEF-007 | Optional techniques are not required until a parameter enables them: event sourcing, CQRS with separate stores, a message broker, a cache server, a search engine, multiple deployables. The architecture MUST NOT prevent them from being added (section 18). | I | PP |

## 18. Evolution and other architecture styles

This section is for information. It describes the styles that were considered, why the modular monolith is the default, and how the backend can grow into another style once a requirement calls for it. No rule here requires the styles to be built now. The capabilities that stay optional, and the condition under which each is introduced, are listed in 2.2.

### 18.1 Styles compared (informative)

| Style | Idea | Strengths | Costs and risks | Relation to this document |
|---|---|---|---|---|
| Layered (n-tier) | Presentation, business, data layers with dependencies pointing down | Simple, familiar | Business logic tends to depend on the database; layers grow into large lumps | Replaced inside each domain by the dependency rule with inward dependencies |
| Clean, onion and hexagonal architecture | The business core in the middle; ports and adapters at the edge; dependencies point inward | Testable core, replaceable technology, framework as a detail | More types and mapping; needs discipline | Used inside every domain (section 5) |
| Modular monolith | One deployable, many modules with enforced boundaries and own data | One deployment, simple operation, fast in-process calls, refactoring across modules is cheap, boundaries can be learned before they are fixed | Needs discipline and automated boundary checks; one scaling unit; a shared runtime | The default of this document |
| Vertical slice | Code organised by feature (request to response) rather than by layer | Low coupling between features, little ceremony | Rules may be duplicated; needs conventions for shared logic | MAY be used inside a domain for simple, read-heavy parts; the domain boundary and the dependency rule still hold |
| Microservices | Independently deployable services around business capabilities, own data, communication over the network | Independent deployment and scaling, team autonomy, fault isolation, technology choice | Distributed-system costs: network failure, latency, eventual consistency, versioned contracts, tracing, many deployments; refactoring across services is hard | A future extension of a domain (18.2) |
| Service-based or SOA | A few coarse-grained services, often with a shared database | Fewer moving parts than microservices | Shared data couples the services | Not used; a domain that moves out takes its data |
| Event-driven | Components react to events instead of being called. Four different things carry the name: event notification, event-carried state transfer, event sourcing, CQRS | Loose coupling, extensibility, resilience, audit trail (with sourcing) | The flow is hard to follow and debug; eventual consistency; schema versioning of events | Notification and state transfer are used between domains through the outbox (section 12); sourcing and CQRS only on a recorded need |
| CQRS | Separate models for writes and reads | Reads and writes can be optimised and scaled apart | Two models to keep consistent; often used where simple CRUD is enough | Read models (BKD-PER-015) are the light form; separate stores only per domain on a need |
| Event sourcing | State is stored as a sequence of events | Full history, replay, temporal queries | Schema evolution of events, replay with external effects, a steep learning curve | Not used by default (BKD-EVT-015) |
| Serverless functions | Functions started on demand by events or requests | No server management, scale to zero | Cold starts, limits on duration and state, vendor coupling, harder local testing | Not used; a use case behind the published interface could be hosted as a function without change |
| Backend for frontend | One backend tailored to each kind of client | Client-specific shapes without bloating the core API | More code and deployments | Only if one client needs a different shape than the contract serves; it would sit in front of this backend |
| Strangler fig | New code grows around old code and replaces it piece by piece | Low-risk migration | Needs a routing seam and a transition period | The method for moving to services or from a legacy system (18.3) |

### 18.2 Rules for extending the architecture

| ID | Statement | Ver. | Basis |
|---|---|---|---|
| BKD-EVO-001 | The backend stays a modular monolith until a stated requirement cannot be met by it. The reasons that justify extracting a domain into its own service are: a different scaling profile that a shared runtime cannot serve; a different release rhythm by a separate team; a different availability or isolation requirement; a different technology need; a boundary of regulation. A wish for "modern" architecture is not a reason. | I | MON |
| BKD-EVO-002 | A domain is a candidate for extraction only when it is ready: it has no dependency on another domain's internals in any part (BKD-MOD-006, BKD-SLC-008); no table is shared and no foreign key crosses its border (BKD-PER-004); its published interface uses only simple data types; its calls from other domains are already treated as remote calls (BKD-MOD-008); its consumers are idempotent (BKD-EVT-005); and its events have a documented catalogue (BKD-EVT-011). | A | MON |
| BKD-EVO-003 | The extraction follows the strangler pattern in small steps: (1) put the published interface behind a network contract (HTTP or messages) while the code is still in the monolith; (2) run the domain as a separate process of the same codebase (process role) with its own database user; (3) move its schema to its own database; (4) deploy it independently; (5) remove the old in-process path. Each step is a release that can be reversed. | I | MON |
| BKD-EVO-004 | Before a domain becomes a service, the requirements for its operation are met: its own deployment, its own readiness checks, its contract documents, its tracing and monitoring (final phase), and the handling of partial failure between it and the monolith (BKD-INT-003 to BKD-INT-007). | I | REL |
| BKD-EVO-005 | The synchronous call that was an in-process call becomes a network call. The published interface has been designed for this: coarse operations, no shared transaction, no entities in parameters, timeouts and failure handling at the call site. | I | MON |
| BKD-EVO-006 | Introducing a message broker, an event store, a separate read store, a cache server or a second deployable is a recorded decision with the requirement, the alternatives, the consequences and the way back. | I | PP |
| BKD-EVO-007 | The API contract does not change when a domain moves behind the same entry point. Clients see one API, whatever the deployment. | T | PP |
| BKD-EVO-008 | A slice is first split off inside the monorepo: its own build, test and release pipeline, limited by the path of its folders, with the shared parts it uses consumed as versioned packages or copied. Only after that MAY it move to its own repository. The set of folders of the slice is the unit of the move (BKD-SLC-008). | I | MON |

### 18.3 Extending without a rewrite (informative)

| Need | First step that fits this architecture |
|---|---|
| Read load is much higher than write load in one domain | A read model filled from events, then, if needed, a separate read store for that domain |
| One domain needs to scale or fail independently | Run it as a separate process role of the same artefact; later extract it (BKD-EVO-003) |
| Several consumers need to react to changes | Publish integration events through the outbox; introduce a broker when consumers run in other processes |
| A full history of one aggregate is required | Event sourcing for that aggregate only, behind its repository port |
| A legacy system must be replaced | Strangler fig with an anti-corruption layer per legacy system (BKD-INT-002) |
| A different client needs a different API | A backend for frontend in front of the contract, or a second version of the contract |
| Work is long-running or bursty | Background jobs and a queue with bounded consumers (BKD-DEP-017, BKD-DEP-018) |
| A domain needs its own team and its own release rhythm | Split the slice into its own pipeline inside the monorepo (BKD-TRG-019), then, if needed, into its own repository |

## 19. Framework mapping (informative)

The rules are stated by capability. This table maps them to common platforms. A platform not listed uses the same rules and records its tools in 24.4. The column "Domain module" names the backend part of a domain slice (`backend/<domain>`). The tools named are options, not a set to install: one tool per capability, chosen under the rules of 16.1.

| Platform | Domain module | Layers and published interface | Boundary and architecture tests | Persistence, migrations | Notes |
|---|---|---|---|---|---|
| Java or Kotlin with Spring Boot | A Gradle subproject or Maven module per domain, or one package per domain in one module | Packages `domain`, `application`, `adapters`, `interfaces`; the root package of the domain holds the published types, the rest is package-private or in an `internal` package | Spring Modulith verification, ArchUnit | Spring Data or jOOQ in the adapters layer; Flyway or Liquibase; ORM schema generation set to validate or none | Domain classes carry no Spring or Jakarta annotation; configuration with typed `@ConfigurationProperties`; Testcontainers for tests |
| C# with .NET | A project per domain layer or per domain (`Billing.Domain`, `Billing.Application`, `Billing.Adapters`, `Billing.Http`), one solution; `internal` visibility for non-published types | Project references enforce the direction; the published types sit in a `Billing.Contracts` project | ArchUnitNET, NetArchTest or Roslyn analysers | EF Core or Dapper in the adapters layer; EF migrations or DbUp or FluentMigrator | The composition root is `Program.cs`; options pattern for settings; `WebApplicationFactory` and Testcontainers for tests |
| PHP with Laravel or Symfony | A folder per domain named by the domain (`backend/billing`) or a Composer path package per domain | Namespaces per layer; Eloquent or Doctrine only in the adapters layer (or the pragmatic profile, BKD-LAY-009); controllers in the interface layer | Deptrac | Laravel or Doctrine migrations in the domain folder, registered by the domain's service provider | Domain classes in plain PHP; form requests only validate shape; queue workers are the `worker` role |
| Node.js with TypeScript (NestJS, Fastify, Express) | A workspace package or a NestJS module per domain (pnpm, npm or Nx workspaces) | Folders per layer; the package `index` exports only the published interface | `eslint-plugin-boundaries`, dependency-cruiser, Nx module-boundary rules | Prisma, TypeORM, Drizzle or Kysely in the adapters layer; the tool's migrations | Strict TypeScript; `class-validator` or schemas only in the interface layer; constructor injection by the framework or by hand |
| Python (FastAPI, Django, Flask) | A package per domain in one project (or a `uv` or Poetry workspace); a Django app per domain | Sub-packages per layer; the package `__init__` exports the published interface | import-linter contracts, pytest architecture tests | SQLAlchemy plus Alembic, or Django ORM (pragmatic profile) with Django migrations | Type hints checked in strict mode; Pydantic models in the interface layer; dependency injection by hand or by the framework |
| Go | A package per domain under `internal/<domain>` in one module or a Go workspace | Sub-packages per layer; exported identifiers only in the published package | The compiler (`internal`), plus go-arch-lint or a test | sqlc, pgx or GORM in the adapters layer; goose, Atlas or golang-migrate | Interfaces are declared where they are used; explicit wiring in `main` |
| Ruby on Rails | A Packwerk pack per domain | Folders per layer inside the pack; public API in `app/public` | Packwerk | ActiveRecord (pragmatic profile) with Rails migrations | Keep business logic out of models that mix persistence and rules where practical |
| Rust | A workspace crate per domain | Modules per layer; the crate exports the published interface only | The compiler (`pub` visibility), cargo-deny or tests | sqlx or Diesel in the adapters layer; their migrations | Traits as ports; wiring in the `app` crate |
| Other | A unit of the build or of the namespace that the platform can make private | Four layers as folders | Any tool that fails the build on a forbidden import | A migration tool with versioned scripts | Record tools in 24.4 |

| ID | Statement | Ver. | Basis |
|---|---|---|---|
| BKD-FWK-001 | The framework is selected from the frameworks document (P01). The agent maps the capabilities of this document to it, records the mapping in 24.4 and asks the user to confirm it before generating code. | I | PP |
| BKD-FWK-002 | Framework features that bind business code to the framework (annotations on domain classes, base classes of the framework in the domain, global static helpers) are not used in the domain layer. Where the platform's idiom needs them, the pragmatic profile applies (BKD-LAY-009). | I | CLA |
| BKD-FWK-003 | Auto-configuration, component scanning and convention-based wiring MAY be used in the composition root, limited to what it names (BKD-DPN-022). Their result must stay predictable: a domain is wired explicitly in its own registration unit, so that the list of domains in a running application is visible in one place. | I | PP |
| BKD-FWK-004 | The tool that enforces module boundaries (P04) is chosen before the second domain is created. A project without such a tool writes the boundary rules as tests. | I | MON |

## 20. Edge cases

The rules of sections 3 to 16 apply in the situations below, which are the ones most often handled wrongly. Each row states the required behaviour; each behaviour is verified as the verification method of its rules says. Rows are grouped by theme; the group is named in the first row of the group.

| ID | Situation | Required behaviour | Rules |
|---|---|---|---|
| BKD-EDG-001 | **Boundaries.** Domain A needs a value that domain B stores. | A calls the published query of B, or keeps a read model filled from B's events. A does not read B's table, join to it or hold a foreign key to it. | BKD-MOD-006, BKD-MOD-010, BKD-PER-004 |
| BKD-EDG-002 | Two domains each need a "customer". | Each domain models the customer with the fields it needs, under its own name if the meaning differs, and refers to the other by identifier. No shared customer entity exists. | BKD-MOD-004 |
| BKD-EDG-003 | A change must be atomic across two domains. | The default is eventual consistency with events and a compensating step. If atomicity is truly required, a decision records that the two domains form one unit of consistency, and the domains are reviewed for a merge. | BKD-MOD-009, BKD-APP-004 |
| BKD-EDG-004 | Domain A calls B and B calls A. | The build or the architecture test fails on the cycle. The cycle is removed by an event, by moving the concept, or by merging. A runtime workaround (lazy lookup, late binding) is not allowed. | BKD-MOD-007 |
| BKD-EDG-005 | A developer needs "just a helper" that two domains share. | The copy rule decides (BKD-DRY-002). A small helper whose meaning is each domain's own is copied. A technical helper that is the same in meaning, stable and needed by both moves to the matching part of the `shared` folder with an owner and tests. Business meaning never goes to shared code. | BKD-DRY-002, BKD-SHR-005, BKD-SHR-003 |
| BKD-EDG-006 | A domain has no business logic (a list of reference values). | It may collapse the domain and application layers into one, keeps the published interface and its own data, and records the collapse in the inventory. | BKD-LAY-010 |
| BKD-EDG-007 | A domain grows to several aggregates and many use cases. | The review against the limits of 21.2 runs and is recorded. A split is made along a seam of the language, never by technical layer. | BKD-MOD-012 |
| BKD-EDG-008 | The framework requires its model classes to extend a base class or carry annotations (active record). | The pragmatic profile is chosen and recorded (P05). The domain rules stay free of input, output and framework calls, and the model class does not leave the domain's infrastructure. | BKD-LAY-009 |
| BKD-EDG-009 | **Transactions and concurrency.** Two clients update the same aggregate at the same time. | The second update carries a stale version and receives the conflict problem. No update is lost silently, and the client can read the new state and decide. | BKD-APP-008, BKD-PER-007 |
| BKD-EDG-010 | A client sends an update without the expected version where the contract requires it. | The request is refused with the precondition-required problem before any work is done. | BKD-IFC-008 |
| BKD-EDG-011 | The same create request is sent twice (client retry, double click, network timeout). | With the same idempotency key, the second request returns the original status and body and creates nothing. Without a key where one is required, the request is refused. | BKD-IFC-008, BKD-APP-007 |
| BKD-EDG-012 | The same idempotency key arrives with a different request body. | The documented key-reuse problem is returned. The stored result of the first request is not changed. | BKD-IFC-008 |
| BKD-EDG-013 | Two requests with the same new idempotency key arrive at the same time. | One runs, the other waits for it or receives the in-progress problem. The key record is protected by a unique constraint. | BKD-IFC-008, BKD-PER-005 |
| BKD-EDG-014 | A use case fails after the aggregate was changed and before the commit. | The whole transaction is rolled back. No event, outbox entry or idempotency record is left behind. | BKD-APP-005, BKD-DEP-014 |
| BKD-EDG-015 | The commit succeeds, and the process is killed before the response is sent. | The client sees a failed or unknown request, repeats it with the same key, and receives the stored result. The outbox entry is published after the restart. | BKD-DEP-013, BKD-DEP-026, BKD-EVT-004 |
| BKD-EDG-016 | A use case must call an external system and change a state. | The state change and an outbox entry are committed together. The call is made from the outbox entry or after the commit. A failed call is retried by the relay and does not undo the state. | BKD-APP-006 |
| BKD-EDG-017 | A deadlock or serialisation failure is reported by the database. | The transaction is rolled back and retried a bounded number of times at the application layer. After the last attempt the documented conflict or unavailable problem is returned. | BKD-PER-006, BKD-APP-014 |
| BKD-EDG-018 | A pessimistic lock is held and the lock holder crashes. | The lock is tied to the transaction or to a lease with an expiry, so it ends by itself. Waiters time out with the documented problem. | BKD-PER-008, BKD-DEP-019 |
| BKD-EDG-019 | A use case reads a list and, for every row, loads related rows (N+1). | The defect is found by the statement-count check of the list use case. The query is changed to load what it needs in a bounded number of statements. | BKD-PER-012 |
| BKD-EDG-020 | A list contains new rows while the client pages through it. | Keyset pagination over a stable order with a unique tie-breaker gives no duplicates and no gaps for the rows that existed at the start. | BKD-PER-011, BKD-IFC-009 |
| BKD-EDG-021 | A client sends a page size above the maximum, or a tampered cursor. | A page size above the maximum is limited to the maximum or refused as the contract states. An invalid cursor returns a problem and never reaches the query. | BKD-IFC-009, BKD-IFC-010 |
| BKD-EDG-022 | **Persistence and migrations.** A column is renamed. | It is done by expand and contract over several releases: add, write both, backfill, switch reads, stop writing the old column, drop. A direct rename is refused by the migration linter. | BKD-MIG-005, BKD-MIG-006, BKD-MIG-013 |
| BKD-EDG-023 | A new non-null column is added to a large table. | The column is added nullable or with a cheap default, backfilled in batches, and made non-null afterwards. The migration takes no long lock. | BKD-MIG-007, BKD-PER-021 |
| BKD-EDG-024 | A merged migration contains a mistake. | It is not edited. A new migration repairs it. The checksum check of the migration tool fails the build on any edit. | BKD-MIG-002 |
| BKD-EDG-025 | The new version starts while the schema is not yet migrated. | The application refuses to become ready (schema version differs) and the deployment stops. It does not migrate itself and does not start with a wrong schema. | BKD-MIG-009, BKD-MIG-010 |
| BKD-EDG-026 | A release is rolled back after a migration ran. | The previous version runs on the new schema because every migration is compatible with the previous release. Down scripts are not used. | BKD-MIG-004, BKD-MIG-005 |
| BKD-EDG-027 | Two instances start at the same time and both could run migrations. | Only the `migrate` role runs migrations, under a lock. Application instances never apply them. | BKD-MIG-009 |
| BKD-EDG-028 | A data migration touches millions of rows. | It runs in bounded batches, each in its own transaction, is idempotent and resumable, and reports counts. It does not run inside a request. | BKD-MIG-011, BKD-PER-021 |
| BKD-EDG-029 | A data-access framework offers to create or update the schema automatically. | The option is switched off in all environments, and a test fails if it is on. The schema comes from migrations only. | BKD-CON-010 |
| BKD-EDG-030 | An instant is received without a zone, or a local time is stored as an instant. | The input is refused where an instant is required. The domain never stores a local time without its zone when it means an instant. Stored instants are UTC. | BKD-DMN-012, BKD-PER-010 |
| BKD-EDG-031 | A money amount is divided (instalments, shares). | The rounding rule from the requirements is applied in one place, the parts sum exactly to the whole, and the remainder rule is tested. Floating point is not used. | BKD-DMN-011 |
| BKD-EDG-032 | A query is built from user input (search, sort, filter). | Values are bound parameters. Sort and filter names come from a closed list in the contract and are mapped to columns in the adapter. Text is never joined into a statement. | BKD-PER-018 |
| BKD-EDG-033 | The connection pool is exhausted. | A request waits at most the configured acquisition timeout, then returns the documented unavailable problem. The pool is not enlarged beyond the database limit to hide the problem. | BKD-PER-019, BKD-DEP-003 |
| BKD-EDG-034 | A statement exceeds its time limit. | It is cancelled, the transaction is rolled back, the connection returns to the pool, and the request ends with the documented problem. | BKD-PER-020 |
| BKD-EDG-035 | A deleted record is referenced by other records. | Inside a domain, a foreign key prevents the deletion or the rule decides the effect. Across domains, the owner publishes a deletion event, and other domains handle their own data. | BKD-PER-005, BKD-PER-017 |
| BKD-EDG-036 | **Integration.** An external system does not answer. | The call ends at its timeout, the retry rule applies for idempotent operations, the breaker opens at its threshold, and the use case returns the documented degraded result or problem. | BKD-INT-003 to BKD-INT-007 |
| BKD-EDG-037 | An external system answers slowly but correctly. | The slow call is counted as a failure by the breaker, the bulkhead keeps the rest of the application free, and the request deadline is respected. | BKD-INT-005, BKD-INT-006, BKD-DEP-004 |
| BKD-EDG-038 | An external system returns an unexpected shape, an unknown status or a huge body. | The adapter refuses it with an adapter error, limits the size, and does not pass the external model to the domain. | BKD-INT-002, BKD-INT-010 |
| BKD-EDG-039 | The outcome of a call that changes state in an external system is unknown (timeout after sending). | The request is repeated with the same idempotency key, or the state is read back before acting again. The outcome is never reported as success or failure without knowing. | BKD-INT-009, BKD-DEP-026 |
| BKD-EDG-040 | A callback from an external system arrives twice, late or out of order. | It is recorded once by its identifier, processed idempotently, and an older message does not overwrite newer state. | BKD-INT-014, BKD-EVT-010 |
| BKD-EDG-041 | An external system returns `429` with `Retry-After`. | The adapter waits no longer than the remaining deadline, retries within the attempt limit, and otherwise returns the documented degraded result. | BKD-INT-013, BKD-INT-004 |
| BKD-EDG-042 | A step of a multi-step process fails after earlier steps succeeded. | The process manager runs the compensating actions for the completed steps, or marks the process as failed for manual repair. State is stored, so a restart continues. | BKD-INT-019, BKD-EVT-012 |
| BKD-EDG-043 | A non-production environment would reach a real recipient or account. | The address of the external system comes from configuration and points to a stub or sandbox. A guard fails start-up in a non-production profile if a production address is configured. | BKD-INT-012 |
| BKD-EDG-044 | **Events.** The event consumer receives the same message twice. | The processed-message record (written in the same transaction as the effect) makes the second delivery a no-op. | BKD-EVT-005 |
| BKD-EDG-045 | A consumer fails permanently on one message. | After the bounded retries the message goes to the dead-letter store with its error. Later messages continue. The dead-letter store has an owner and a way to replay. | BKD-EVT-009 |
| BKD-EDG-046 | The relay publishes an event and crashes before marking it as sent. | The event is published again after the restart. Consumers are idempotent, so the effect happens once. | BKD-EVT-004, BKD-EVT-005 |
| BKD-EDG-047 | Events of one aggregate arrive out of order. | Consumers use the per-aggregate sequence number to ignore older events, or are written so that order does not matter. No global order is assumed. | BKD-EVT-010 |
| BKD-EDG-048 | A producer needs to change the shape of an integration event. | Additive changes are made in the same version. A breaking change publishes a new version next to the old one until all consumers moved. The catalogue test passes for both. | BKD-EVT-006, BKD-EVT-011 |
| BKD-EDG-049 | A handler of a domain event fails. | The publisher's transaction is not affected. The handler has its own transaction, retry and dead-letter rule. | BKD-EVT-003 |
| BKD-EDG-050 | A timer for expiry is needed. | The due time is stored as data. A job finds due items in bounded batches and handles them idempotently. An in-memory timer does not hold the deadline. | BKD-EVT-014 |
| BKD-EDG-051 | **Dependability and lifecycle.** A required setting is missing or invalid at start. | The process stops at once with a message that names the setting and not its value. It does not start with a guess. | BKD-CFG-002, BKD-DEP-002 |
| BKD-EDG-052 | A termination signal arrives during a request and a job. | The request finishes within the grace period; the job stops after its current batch and keeps its progress. Committed work is never lost; uncommitted work is repeated. | BKD-DEP-012, BKD-DEP-018 |
| BKD-EDG-053 | The database is down while the process is running. | Liveness stays up, readiness goes down, requests that need the database return the documented unavailable problem fast, and the instance recovers without restart when the database returns. | BKD-DEP-010, BKD-DEP-001 |
| BKD-EDG-054 | An optional external system is down. | Readiness is not affected. The dependent use case degrades as documented, and the response marks what is absent. | BKD-DEP-007, BKD-DEP-010 |
| BKD-EDG-055 | The load exceeds the capacity. | The concurrency and queue limits are reached, and the backend answers `503` with `Retry-After` at once. Latency of accepted requests stays within target. Memory does not grow without bound. | BKD-DEP-005, BKD-DEP-006 |
| BKD-EDG-056 | A client abandons a request. | The remaining deadline reaches the downstream calls so that work is stopped where possible, and no partial change is committed by an abandoned transaction. | BKD-DEP-004 |
| BKD-EDG-057 | A scheduled job is started on several instances at once. | A database lock or lease with an expiry lets one instance run it. If the lock fails, the job is still safe when it runs twice. | BKD-DEP-019, BKD-DEP-015 |
| BKD-EDG-058 | A job crashes in the middle. | It is started again, reads its stored progress, continues with the next batch, and ends in a visible final state. | BKD-DEP-018 |
| BKD-EDG-059 | Instance clocks differ by seconds. | Ordering across instances comes from database sequences or versions. Expiry decisions use the database time or tolerate the skew. | BKD-DEP-020 |
| BKD-EDG-060 | A rolling deployment runs the old and the new version together. | Both versions work against the same schema, accept the same contract, and read and write compatible events. | BKD-DEP-024 |
| BKD-EDG-061 | A feature switch is left on after the feature was released. | The removal date is checked in the review; an expired switch fails the review. Both positions were tested, so removal is safe. | BKD-DEP-025 |
| BKD-EDG-062 | **Interface handling.** A request has an unknown path, an unsupported method, a wrong media type or a malformed body. | The framework default page is not returned. The response is a problem in `application/problem+json` with a stable code and the correlation identifier. | BKD-IFC-004 |
| BKD-EDG-063 | An unexpected exception occurs. | A `500` problem with a generic code and the correlation identifier is returned. No message, stack trace, class name, SQL or path reaches the client. | BKD-IFC-006 |
| BKD-EDG-064 | A request contains unknown properties, duplicate keys or a number out of range. | It is refused as the contract states. Nothing is silently dropped or coerced. | BKD-IFC-016 |
| BKD-EDG-065 | A very large body, header or upload arrives. | The limit is enforced at the edge before the use case runs. The upload is streamed and never held completely in memory. | BKD-IFC-010, BKD-IFC-017 |
| BKD-EDG-066 | A request arrives without a correlation identifier, or with an invalid trace header. | A new correlation identifier is created, the invalid header is ignored, and the identifier is returned and passed to the use case, calls and events. | BKD-IFC-011 |
| BKD-EDG-067 | A client sends a language preference that the backend does not know. | The business result and the error code do not change. The language is a value passed to the use case. No user-facing text is produced. | BKD-IFC-013, BKD-IFC-014 |
| BKD-EDG-068 | A domain error and a framework error would map to the same status. | The central mapping table decides by error code. A domain error never chooses an HTTP status itself. | BKD-IFC-005 |
| BKD-EDG-069 | A conditional read has a matching `If-None-Match`. | `304` without a body is returned and no domain data is loaded beyond the version check where the design allows it. | BKD-IFC-007, BKD-IFC-008 |
| BKD-EDG-070 | **Code and review.** A function grows above the limits of 21.2. | The analysis stage fails. The function is split, or the exception is recorded with owner and reason. | BKD-CLN-002, BKD-CLN-016 |
| BKD-EDG-071 | A reviewer finds a business rule in a controller or a repository. | The change is rejected. The rule moves to the domain layer with a unit test of its own. | BKD-LAY-006, BKD-LAY-008 |
| BKD-EDG-072 | An agent is asked to add a login, a role check, a rate limit or a log format. | It refuses and points to the deferred items, unless the user has started the final phase. No placeholder or permit-all configuration is created. | BKD-DEF-001, BKD-DEF-005 |
| BKD-EDG-073 | A feature seems to need a message broker, a cache or a second service. | The need is stated as a requirement and checked against the simpler option (outbox, read model, process role). The decision is recorded before the component is added. | BKD-CON-016, BKD-EVO-006 |
| BKD-EDG-074 | The API contract and the code disagree. | The build fails. The contract is the source of truth; the code is changed, or the contract is changed first by its own process. | BKD-IFC-001, BKD-BLD-006 |
| BKD-EDG-075 | A requirement is missing or ambiguous while an agent designs a domain. | The agent asks the user and records the answer. It does not guess a business rule. | BKD-CON-012 |
| BKD-EDG-076 | **Packages.** A developer wants a library for a task that own code solves in about twenty lines. | Own code in the module that needs it, with tests. No library and no register entry. If three domains need the same helper, it moves to the platform module (if technical) or is duplicated (if it carries business meaning). | BKD-DPN-002, BKD-DPN-003 |
| BKD-EDG-077 | A requirement needs something hard (JSON parsing, cryptography, time zones, a database protocol). | The reviewed package of the platform is used and wrapped in one adapter or in the platform module. Own code does not replace it. | BKD-DPN-004, BKD-DPN-012 |
| BKD-EDG-078 | A second library for a concern that is already served appears (a second HTTP client, JSON library or validation library). | The gate fails. The first library is used, and the second is removed with its transitive packages. | BKD-DPN-005, BKD-QGT-009 |
| BKD-EDG-079 | A framework starter pulls in a deferred concern (a default login page, default security headers, metrics endpoints, an API documentation page). | The module is removed from the class path. The route inventory test fails while an endpoint outside the contract and the operational document exists. No permit-all setting is written to silence the default. | BKD-DPN-007, BKD-QGT-010, BKD-DEF-005 |
| BKD-EDG-080 | A package becomes unmaintained, changes its licence or gets a public advisory. | The owner in the register decides: upgrade, or replace it behind the same adapter. The replacement changes one adapter and is recorded. Automated scanning stays deferred (section 17); the decision does not. | BKD-DPN-024, BKD-DPN-012 |
| BKD-EDG-081 | Two packages need different versions of the same transitive package. | One version is chosen centrally and both packages are tested with it. If that is impossible, one of the packages is replaced. Two versions of one package are not loaded side by side. | BKD-DPN-018, BKD-DPN-005 |
| BKD-EDG-082 | An agent suggests a package for convenience (a nicer output, a shorter test, a helper). | It is refused unless it meets BKD-DPN-002. If it does, the agent proposes it with a register entry and waits for the user's approval. | BKD-DPN-021 |
| BKD-EDG-083 | The framework offers a built-in facility and a popular add-on package for the same job. | The built-in facility is used. The add-on is chosen only after a requirement shows the limit, and the limit is recorded. | BKD-DPN-027 |
| BKD-EDG-084 | The dependency budget is reached. | A review runs before the next package: unused packages are removed, overlaps are merged, small uses are replaced by own code. The budget is raised only by recorded decision. | BKD-DPN-019, BKD-DPN-025 |
| BKD-EDG-085 | A vendor SDK would save two calls to an external system. | A plain call with the standard HTTP client inside the adapter is used. The SDK stays out unless the adapter would otherwise grow beyond the limits of 21.2. | BKD-DPN-013 |
| BKD-EDG-086 | A package type appears in a port, an event payload or a published interface "because it is convenient". | The adapter maps it to a simple type. The architecture test or the review rejects the change. | BKD-DPN-012, BKD-RST-044 |
| BKD-EDG-087 | An object mapper or an accessor generator would shorten the code. | Mapping and accessors are written explicitly or by the language's native feature (record, data class). No reflection-based mapper is added. | BKD-DPN-009 |
| BKD-EDG-088 | **MVP and extension.** The MVP has one external system and a developer proposes a resilience library with a breaker and bulkhead. | Not before the trigger. The MVP form is timeouts, a bounded retry for idempotent calls, and the documented unavailable behaviour, all inside the one adapter. | BKD-TRG-001, BKD-MVP-003 |
| BKD-EDG-089 | Two domains must react to each other's changes in the MVP. | In-process dispatch after commit is used. If the loss of an event would leave data inconsistent, that event goes through an outbox table from the start. The broker waits for its trigger. | BKD-TRG-002, BKD-TRG-003 |
| BKD-EDG-090 | A screen needs data of three domains and is slow. | Indexes and composed published queries first. A read model is introduced only after a measurement shows the target is missed. | BKD-MVP-011, BKD-TRG-004 |
| BKD-EDG-091 | The team proposes twelve domains at the start. | The MVP starts with fewer, coarser domains from the glossary (default at most eight). Splitting later follows a seam of the language. | BKD-MVP-005, BKD-MOD-012 |
| BKD-EDG-092 | It is unclear whether two business areas are separate domains. | They start as one domain with a clean internal structure. They are split when the glossary or the change pattern shows a seam. | BKD-MVP-005 |
| BKD-EDG-093 | An MVP shortcut becomes load-bearing (other code relies on its form). | The seam tests fail or the register shows that the trigger has passed. The shortcut is replaced in one small change. The register is reviewed at the end of each phase. | BKD-MVP-008, BKD-MVP-009, BKD-MVP-012 |
| BKD-EDG-094 | A rule that was "not applicable" becomes applicable (the first external system, the first event, the first job). | The same change that creates the subject also delivers what the rule asks: port, adapter, timeouts, unavailable behaviour. | BKD-MVP-004 |
| BKD-EDG-095 | Query time grows after months of data growth. | The query is measured, the plan is checked, an index or query change is made. A cache is a later step and follows its trigger. | BKD-MVP-011, BKD-TRG-006 |
| BKD-EDG-096 | Someone proposes a feature-management system. | Not before its trigger. Unfinished work stays on a branch, and a few switches are configuration values. | BKD-TRG-014, BKD-DEP-025 |
| BKD-EDG-097 | Seed data is loaded before the migrations have finished, or an end-to-end job seeds the database without waiting. | Seeders run only after the `migrate` role has completed and the instance is ready. A seed that is repeated gives the same data. | BKD-RST-053, BKD-MIG-012 |
| BKD-EDG-098 | A seeder writes the current time into the data, so tests give different results on each run. | Repeatable seeds use fixed instants or the injected clock. | BKD-RST-053, BKD-DMN-010 |
| BKD-EDG-099 | A list endpoint returns the framework's page object, with internal fields. | A page is a contract type (items, next cursor, flags). The adapter maps the framework's type, which never leaves the persistence adapter. | BKD-RST-049, BKD-IFC-009 |
| BKD-EDG-100 | A response contains an internal value (a hash, a storage key, an internal identifier) because an entity was serialised. | Response types are separate and mapped. The contract check refuses properties that are not in the contract. | BKD-IFC-003, BKD-IFC-001 |
| BKD-EDG-101 | A join table has no version and audit columns and no index on its foreign keys. | The migration is rejected by the linter or the review. The columns and indexes are created with the table. | BKD-PER-023, BKD-PER-013, BKD-MIG-013 |
| BKD-EDG-102 | A table or column exists for a feature that was dropped, or "for later". | The review finds it in the phase-end check. It is removed by a contract migration with the approval of the user. | BKD-RST-054, BKD-MIG-008 |
| BKD-EDG-103 | A shared module holds a base entity with persistence annotations and DTOs for all domains. | Each domain has its own types. The `shared` folder holds technical code only. | BKD-RST-050, BKD-SHR-003 |
| BKD-EDG-104 | The frontend in local development calls the backend from another origin and fails. | The development setup of the frontend proxies the calls. No permissive cross-origin setting is added to the backend before the security phase. | BKD-DEF-001 |
| BKD-EDG-105 | A role name, a permission annotation or a user context appears in the code before the security phase. | It is removed. The operation stays open and the backend is not deployed where it is reachable from the internet. | BKD-DEF-005 |
| BKD-EDG-106 | A database trigger is proposed for derived data or history. | Business rules do not live in triggers. A trigger needs a recorded decision with reason and test, and the history or audit design belongs to the final phase. | BKD-CON-004, BKD-DEF-001 |
| BKD-EDG-107 | A new setting is added in code but not in the settings table or the example values. | The settings gate fails until the table and the example values match. | BKD-CFG-003, BKD-QGT-006 |
| BKD-EDG-108 | A shortcut of the MVP is taken without a record. | The review rejects the change. The shortcut is recorded with trigger and owner, or it is not taken. | BKD-MVP-008 |
| BKD-EDG-109 | **Slices.** A feature needs an operation and a table in one domain. | The use case and the table go into `backend/<domain>`, and the operation goes into the API sources of the same domain, in one change. The API contract is the shared source of the operation. | BKD-SLC-001, BKD-SLC-010 |
| BKD-EDG-110 | Domain A needs data of domain B. | A calls B's published use case, or keeps a read model from B's events. No import of B's internals in any area. | BKD-SLC-003, BKD-MOD-010 |
| BKD-EDG-111 | An operation seems to belong to two domains. | The vocabulary decides the owner. The other domain calls the operation, or has an operation of its own with its own meaning. An operation is never listed in two fragments. | BKD-SLC-006 |
| BKD-EDG-112 | A view needs data of several domains (a dashboard). | A domain that owns the composition keeps its own read model from published queries or events, or the composition is made outside the domains from their published surfaces. No domain imports the internals of the others. | BKD-SLC-003, BKD-PER-016 |
| BKD-EDG-113 | Two domains both start their migration history at version 1. | Each domain has its own history table in its own schema, so that there is no clash and each history moves with its domain. | BKD-MIG-015 |
| BKD-EDG-114 | A migration of domain A needs data that domain B owns. | The migration does not read B's schema. The data arrives through B's published interface or events in application code, or A starts with no copy and fills it from events. | BKD-MOD-010, BKD-MIG-001 |
| BKD-EDG-115 | A new domain is added. | Its backend folder is created (BKD-MOD-013) and registered in the composition root in one place, and its operations are added to the API contract. | BKD-SLC-009 |
| BKD-EDG-116 | A domain is to be split into its own build and release unit, or its own repository. | The folders of the domain in all areas are moved. The shared parts are consumed as versioned packages or copied. The API sources are still bundled into the public contract, and the published surface does not change. | BKD-SLC-008, BKD-EVO-008 |
| BKD-EDG-117 | **Shared and duplication.** A developer wants to move 30 lines used by two domains into `shared/`. | The copy rule decides. If the code is the same in meaning, stable, free of domain words, needed by both now, and a change would have to be made in both copies, it moves and the move is recorded. Otherwise it is copied. | BKD-SHR-005, BKD-DRY-002 |
| BKD-EDG-118 | A defect is found in code that was copied into three domains. | The copy rule has triggered. If the copies are still the same in meaning, the code moves to `shared/` with tests. Otherwise the defect is fixed in each copy and the copies note their source. | BKD-DRY-002, BKD-DRY-003 |
| BKD-EDG-119 | A change in `shared/` breaks the tests of one domain. | The change is made backward compatible, or all users are changed in the same change. A breaking change is a parallel change. | BKD-SHR-006 |
| BKD-EDG-120 | Domain A asks for a special case in shared code (an `if` for A only). | The code moves back into A. Shared code does not know its users. | BKD-SHR-009 |
| BKD-EDG-121 | `backend/shared` grows above the limit of 21.2. | A review runs. Code that only one domain uses moves back. Misplaced domain concepts are named and moved to their domain. | BKD-SHR-008 |
| BKD-EDG-122 | Two domains need the same data shape for a request. | Each domain keeps its own transfer type. The contract documents each. No library of shared transfer types is created. | BKD-DRY-005, BKD-RST-050 |
| BKD-EDG-123 | Two domains have validation rules that look the same today but concern different concepts. | They stay separate. When one of them changes, the other does not need to. | BKD-DRY-004 |
| BKD-EDG-124 | The backend transfer types drift away from the API contract. | The contract is the single source. The backend types are generated from it or checked against it, and the build fails on a difference. | BKD-DRY-009, BKD-BLD-006 |
| BKD-EDG-125 | A reviewer asks to remove duplication between two domains. | The request is declined unless the move conditions of BKD-SHR-005 hold. Duplication inside one domain is still removed. | BKD-DRY-007, BKD-DRY-008 |
| BKD-EDG-126 | A helper in `backend/shared/platform` needs business vocabulary to work. | It is not technical. It moves to the domain that owns the vocabulary, or into each domain that needs it. | BKD-SHR-003 |
| BKD-EDG-127 | Someone proposes a `Customer` type in the `shared` folder. | A `shared` folder holds no entities. Each domain models the customer it needs, and refers to others by identifier. | BKD-SHR-003, BKD-MOD-004 |
| BKD-EDG-128 | A domain is simple record keeping, and the strict layering looks heavy. | The light profile is chosen for that domain and recorded. It is revisited when the rules grow. | BKD-DRY-006, BKD-LAY-010 |
| BKD-EDG-129 | Two domains use one external package for different purposes. | Each domain wraps it in its own adapter. The package is entered once in the register and counted once. | BKD-SHR-010, BKD-DPN-012 |

## 21. Restrictions

### 21.1 Prohibited patterns

Each pattern below MUST NOT appear in the backend. A finding is a defect, found by the architecture test, the static analysis, the migration linter or the review (section 22). An exception needs a recorded deviation (BKD-PRE-006).

| ID | Prohibited pattern | Basis |
|---|---|---|
| BKD-RST-001 | A business rule in a controller, handler, repository, mapper, database trigger, stored procedure or framework configuration. | CLA |
| BKD-RST-002 | An import of the web framework, the data-access framework, the dependency-injection container, serialisation annotations or any input and output library in the domain layer. | CLA |
| BKD-RST-003 | A dependency from an inner layer to an outer layer, or from one domain to the internals of another domain. | MON |
| BKD-RST-004 | A cycle between domains, between layers or between packages. | MON |
| BKD-RST-005 | A table, query, join, view or foreign key that crosses a domain boundary (unless a recorded decision merges the domains). | DDD |
| BKD-RST-006 | A folder, package or module that collects code without a purpose name and an owner (`utils`, `helpers`, `misc`, `core`, or a `shared` or `common` part without owner, README and tests), or that holds domain code placed there to avoid a boundary. | MON |
| BKD-RST-007 | A class named `Manager`, `Helper`, `Processor`, `Util` or `Handler` without a business meaning, or a "god" class that many domains depend on. | PP |
| BKD-RST-008 | An entity, persistence row or framework request returned from a controller or passed across a layer or domain boundary. | CLA |
| BKD-RST-009 | A service locator, a static mutable singleton, a global registry or hidden global state, including thread-local variables that the domain reads. | CLA |
| BKD-RST-010 | A call to the system clock, a random generator or a global identifier function in the domain or application layer. | PP |
| BKD-RST-011 | Automatic schema creation or update by the data-access framework in any environment. | EVO |
| BKD-RST-012 | An edit of a merged migration, a down script that is relied on, a migration that changes another domain's tables, or a destructive statement without approval. | EVO |
| BKD-RST-013 | A statement built by joining input into text. | PP |
| BKD-RST-014 | A query without a bound on rows or time, offset pagination over a large set, the N+1 pattern, or a query without a supporting index. | PP |
| BKD-RST-015 | Lazy loading, proxies or open sessions that escape the persistence adapter. | PP |
| BKD-RST-016 | An outbound call, a broker publication or a long computation inside an open database transaction. | OUT |
| BKD-RST-017 | An outbound call without a timeout, a retry without backoff and jitter, retries at more than one layer, or a retry of a non-idempotent operation without a key. | REL |
| BKD-RST-018 | An unbounded wait, queue, pool, cache, collection, page size or body size. | REL |
| BKD-RST-019 | Business state in the memory or on the local disk of one instance, or a required sticky session. | 12F |
| BKD-RST-020 | A distributed transaction (two-phase commit) across domains or systems. | OUT |
| BKD-RST-021 | An in-memory timer or an in-memory queue that holds business deadlines or work that is not also stored. | REL |
| BKD-RST-022 | An exception that is caught and ignored, an error returned as success, or an exception that crosses the boundary as a raw stack trace. | PP |
| BKD-RST-023 | A response body for an error that is not a problem document, or a message, class name, SQL or path in an error response. | PD |
| BKD-RST-024 | A text for display to users produced by the backend (messages, formatted dates or amounts in a locale). | PP |
| BKD-RST-025 | A route, parameter, status or property that is not in the API contract, or a contract change without the change of the code and the reverse. | PP |
| BKD-RST-026 | Binary floating point for money, or a local time without its zone used as an instant. | PP |
| BKD-RST-027 | A secret, a real address or an environment name in code, in a committed configuration file, in an image or in a log. | 12F |
| BKD-RST-028 | A branch of the code on the name of the environment, or a test hook, test route, test header or clock endpoint in the production artefact. | 12F |
| BKD-RST-029 | A configuration default that points to production or to a real external system, or that switches a protective limit off. | 12F |
| BKD-RST-030 | Log statements, metric calls or tracing calls in business code. | PP |
| BKD-RST-031 | Any implementation, configuration or scaffolding of a deferred item (authentication, roles, rate limits, security headers, secret tooling, logging design, metrics, tracing export, alerting) before the final phase. | PP |
| BKD-RST-032 | A permit-all configuration, a fake login, a mock user or a role annotation as a placeholder. | PP |
| BKD-RST-033 | A message broker, cache server, search engine, second deployable, CQRS with separate stores or event sourcing without a recorded requirement and decision. | PP |
| BKD-RST-034 | A framework chosen or a domain list fixed without the user's confirmation of the matching project parameter. | PP |
| BKD-RST-035 | Commented-out code, dead code, unused dependencies, unused settings, or a `TODO` without a task identifier and owner. | PP |
| BKD-RST-036 | A boolean parameter that switches the behaviour of a function, a function above the limits of 21.2, or a magic value without a name. | PP |
| BKD-RST-037 | Hand edits of generated code, or generated code that is not reproducible from its source by the build. | PP |
| BKD-RST-038 | Test data created in production code, a migration that loads demonstration data, or a seeder that runs in production. | PP |
| BKD-RST-039 | An empty domain skeleton, a domain with one layer, or a new domain without a working use case, tests and a record in the inventory. | PP |
| BKD-RST-040 | A starter bundle, meta-package or umbrella package that pulls in unused modules, or a module of a deferred concern on the class path before its phase. | DEP |
| BKD-RST-041 | A general-purpose utility library, or a second library for a concern that is already served. | DEP |
| BKD-RST-042 | A library that rewrites code (generators of accessors, builders or constructors; byte-code weaving; reflection-based mappers or copiers) where the language has the feature. | DEP |
| BKD-RST-043 | A package that is not in the dependency register, a version range, `latest`, a snapshot or pre-release version, or a package from outside the official registry without a recorded decision. | DEP |
| BKD-RST-044 | A type of an external package in a port, a published interface, an event, the contract or the domain; a vendor SDK outside its adapter. | HEX |
| BKD-RST-045 | Own implementations of cryptography, password hashing, parsing of standard formats, time zones or database protocols. | DEP |
| BKD-RST-046 | Build or development packages in the runtime artefact. | 12F |
| BKD-RST-047 | Automatic discovery that registers routes, filters, handlers, listeners or settings that the composition root does not name. | DEP |
| BKD-RST-048 | A generic base repository, base service, base controller or CRUD template that several domains inherit. | MON |
| BKD-RST-049 | A framework page, request or response type in a port, a use-case result or the contract; a universal response envelope that the contract does not define. | PP |
| BKD-RST-050 | A shared library of DTOs or entities used by several domains, including a base entity class with persistence annotations in a shared module. | MON |
| BKD-RST-051 | An abstraction, option, plug-in point, column or endpoint built for an imagined future need; an interface with one implementation and no effect on the outside world. | YAG |
| BKD-RST-052 | An MVP shortcut without a record in the shortcut register, or a shortcut that relaxes the fixed core, a fixed rule or a deferred item. | YAG |
| BKD-RST-053 | A seeder or start-up script that runs before the migrations have finished, or repeatable seed data that depends on the current time. | PP |
| BKD-RST-054 | A table, column, endpoint, setting or package that nothing uses (dead schema or code kept for later). | YAG |
| BKD-RST-055 | A part of domain A (backend, tables, contract sources, frontend, tests) that imports, reads or calls the internals of a part of domain B. | MON |
| BKD-RST-056 | A `shared` folder that imports a domain or the composition root, or that holds a business rule, entity, repository, use case, event, transfer type or table of a domain. | DDD |
| BKD-RST-057 | A part of a `shared` folder without a purpose name, an owner, a README and tests, or a folder that is shared by exactly two named domains. | MON |
| BKD-RST-058 | Business rules, transfer types, API types, events, messages or tables shared between domains by code. | DDD |
| BKD-RST-059 | Merging the code of two domains because it looks alike when the meaning differs. | DDD |
| BKD-RST-060 | A condition, flag or branch for one domain's sake inside shared code. | PP |
| BKD-RST-061 | A backend that imports source code of another area, or another area that imports backend source or reaches the database. | CLA |
| BKD-RST-062 | A transfer type of the backend that is neither generated from nor checked against the API contract. | PP |
| BKD-RST-063 | An operation, table, event or component that does not carry the name of its domain, or that belongs to two domains. | DDD |
| BKD-RST-064 | Domain logic in a composition point (the backend composition root, the frontend shell, the API root). | CLA |
| BKD-RST-065 | A separate build, release, repository or deployment of one domain before its trigger, a folder that groups domains (`domain`, `domains`, `modules`), or a new kind of folder without a recorded reason. | YAG |
| BKD-RST-066 | A migration history that is shared between domains, or a migration that depends on the schema of another domain. | EVO |

### 21.2 Default limits (default, P11)

The values are defaults. They are overridden by P11 and are checked by the tools named in the last column. A limit that is exceeded needs a recorded exception with an owner and a reason.

| Limit | Default | Applies to | Checked by |
|---|---|---|---|
| Function or method length | 30 lines | All code | Static analysis |
| Cyclomatic complexity | 10 | All code | Static analysis |
| Parameters of a function | 4 | All code (a parameter object beyond that) | Static analysis |
| Nesting depth | 3 | All code | Static analysis |
| File length | 400 lines | All code | Static analysis |
| Constructor parameters (collaborators) | 5 | Use cases, services, adapters | Static analysis, review |
| Depth of inheritance | 2 | All code | Review |
| Use cases per domain before a split review | 40 | Domain | Inventory, review |
| Aggregates per domain before a split review | 8 | Domain | Inventory, review |
| Lines of code per domain before a split review | 20000 | Domain | Review |
| Fan-out (domains used by one domain) | 4 | Domain | Dependency matrix, architecture test |
| Fan-in (domains using one domain), other than a supporting domain | 6 | Domain | Dependency matrix, architecture test |
| Statements per list use case | 5 | Queries | Automatic check |
| Page size: default and maximum | 25 and 100 | Lists | Contract, integration test |
| Request body | 1 MB (upload: 10 MB, streamed) | Edge | Automatic check |
| Payload of an integration event | 64 KB | Events | Automatic check |
| Request deadline | 30 seconds | Requests (P10) | Automatic check |
| Outbound call: connect and total timeout | 2 and 10 seconds | Clients (P09) | Automatic check |
| Retry attempts, backoff base and cap | 3, 100 milliseconds and 2 seconds, with jitter | Clients (P09) | Automatic check |
| Circuit breaker: failure rate over a window, open time | 50 percent over 20 calls, 30 seconds | Clients (P09) | Automatic check |
| Statement timeout and lock wait | 15 and 5 seconds | Database connection (P07) | Automatic check |
| Pool acquisition timeout | 3 seconds | Database connection (P07) | Automatic check |
| Pool size per instance | Cores of the database × 2 + 1, divided by the number of instances, and confirmed by a load test | Database connection (P07) | Measurement |
| Deadlock or serialisation retries | 3 | Application layer | Automatic check |
| Idempotency record retention | 24 hours | Edge | Automatic check |
| Outbox relay batch | 100 entries | Messaging | Automatic check |
| Consumer attempts before the dead-letter store | 5 | Messaging | Automatic check |
| Job batch size | 500 items | Jobs | Automatic check |
| Start-up time and shutdown grace | 30 and 30 seconds | Process (P10) | Automatic check |
| Readiness and liveness answer time | 1 second | Operational endpoints | Automatic check |
| Direct runtime packages of the backend (distinct libraries; the framework and each module of it that is used count one each; the standard library does not count) | 12 | Backend | Dependency register, lock file (P12) |
| Direct build-time packages of the backend (tools) | 12 | Backend | Dependency register, lock file (P12) |
| External packages in the domain layer and the application layer | 0 (the only exception: a package free of input and output that BKD-DPN-011 allows in the application layer) | Domain, application | Architecture test |
| Direct runtime packages used by one domain's adapters and interfaces | 3 | Domain | Dependency register (P12) |
| Own code that replaces a package, at most | 100 lines | Module | Review (P12) |
| Months since the last release of a package at the time it is added | 12 | New package | Register entry (P12) |
| Transitive packages that one new package adds | 25 | New package | Lock file diff (P12) |
| Domains in the MVP | 8 | Backend | Inventory (P13) |
| Allowed licences | Permissive licences such as MIT, BSD, ISC and Apache-2.0; others need a decision of the user | New package | Register entry (P12) |
| Share of `backend/shared` in the source lines of the backend | 15 percent | Backend | Review (A) |
| Copies of a piece of code in different domains before it moves to `shared/` | 2 copies, and the third copy moves it; earlier when one change must be made in several copies | Domains | Review |

## 22. Quality and verification

### 22.1 Review checklist

Every change to the backend is reviewed against the list below by a person or by an agent that has not written the change. Each point is answered with pass, fail or not applicable, and a fail blocks the merge.

| ID | Check | Ver. | Basis |
|---|---|---|---|
| BKD-REV-001 | The change belongs to one domain, or the cross-domain parts go through published interfaces and events only. | I | MON |
| BKD-REV-002 | The architecture test, the dependency matrix (24.3) and the inventory (24.2) agree with the change. | T | MON |
| BKD-REV-003 | Business rules are in the domain layer, free of framework and database code. | I | CLA |
| BKD-REV-004 | Controllers do five things only. Use cases orchestrate and contain no rule. Adapters contain no rule. | I | CLA |
| BKD-REV-005 | Names come from the glossary, and a reader who knows the business understands the code. | I | DDD |
| BKD-REV-006 | The change matches the API contract in both directions, and the problem codes it returns are in the error catalogue. | T | PP |
| BKD-REV-007 | Every use case has a defined transaction, idempotency and concurrency behaviour. | I | HTTP |
| BKD-REV-008 | Every query is bounded, paginated, indexed and bound-parameterised, and list use cases have a bounded statement count. | T | PP |
| BKD-REV-009 | Every migration is forward-only, small, compatible with the previous release, and passes the linter. The database document changed with it. | T | EVO |
| BKD-REV-010 | Every outbound call has a timeout, one retry layer, a decided failure behaviour and, from its trigger, a breaker and a limit. | I | REL |
| BKD-REV-011 | No call to an external system and no publication happens inside a transaction. | T | OUT |
| BKD-REV-012 | New events are in the catalogue with a version and an example. Consumers are idempotent. | T | OUT |
| BKD-REV-013 | Every failure path of the change has a defined result: status, error code and remaining state. | I | REL |
| BKD-REV-014 | New settings are typed, validated at start, documented in the settings table, and have safe defaults. | I | 12F |
| BKD-REV-015 | The change introduces no state in memory or on disk that another instance or a restart would miss. | I | 12F |
| BKD-REV-016 | The change contains no deferred item, no placeholder for one, and no secret. | I | PP |
| BKD-REV-017 | The change adds no pattern, library or layer that no requirement needs (BKD-CLN-020). | I | PP |
| BKD-REV-018 | The limits of 21.2 hold, or every exception is recorded. | T | PP |
| BKD-REV-019 | The domain README, the API contract, the database document and this document's inventory changed in the same change where needed. | I | PP |
| BKD-REV-020 | A refactoring is separate from a change of behaviour, and tests of behaviour did not change in it. | I | PP |
| BKD-REV-021 | The change keeps the previous release working during a rolling deployment (schema, contract, events). | T | EVO |
| BKD-REV-022 | The change raises no new finding of static analysis, dependency checks or the migration linter that is not recorded as accepted. | T | PP |
| BKD-REV-023 | The change produces no text for display to users and no internal detail in an error response. | T | PD |
| BKD-REV-024 | No package was added outside the foundation set without an entry in the register (24.7) and the approval of the user. Each new package passed BKD-DPN-002 and the assessment of BKD-DPN-014. | I | DEP |
| BKD-REV-025 | No package type appears in a port, a published interface, an event, the contract or the domain, and no package is used twice for one concern. | T | DEP |
| BKD-REV-026 | No starter, umbrella package or automatic discovery added a module, an endpoint or a default that the change does not name. The route inventory is unchanged except for the contract. | T | DEP |
| BKD-REV-027 | Mapping between types is explicit. No reflection-based mapper, accessor generator or helper library was added where the language has the feature. | I | DEP |
| BKD-REV-028 | The change delivers what the MVP scope and the requirements need and nothing for an imagined need. No capability of the trigger table (2.2) exists without a recorded trigger. | I | YAG |
| BKD-REV-029 | Every new shortcut is in the shortcut register (24.8) with a trigger and an owner, and none relaxes the fixed core. | I | YAG |
| BKD-REV-030 | Seeds and migrations run in order, are repeatable, and contain no dependence on the current time. | T | PP |
| BKD-REV-031 | The change belongs to one domain, and the backend transfer types agree with the API contract (generated from it or checked against it). | T | DDD |
| BKD-REV-032 | No import crosses a domain boundary in any area, and no `shared` folder imports a domain or a composition point. | T | MON |
| BKD-REV-033 | Everything moved to `shared/` meets BKD-SHR-005 and is recorded. Everything copied between domains names its source. No business rule, transfer type, event or table is shared by code. | I | PP |
| BKD-REV-034 | The domain README, the inventory entry (all parts) and the code-ownership file are current. | I | PP |
| BKD-REV-035 | The layering profile of the domain fits its size, and ceremony was not copied from another domain. | I | YAG |

### 22.2 Definition of done

| ID | Statement | Ver. | Basis |
|---|---|---|---|
| BKD-DOD-001 | A use case is done when it works over HTTP against a real database with real migrations, and its contract operation is implemented and checked. | T | PP |
| BKD-DOD-002 | A domain is done for a phase when its inventory record is complete, all its published use cases of the phase are done, its events are in the catalogue, its failure behaviour is defined, and its README is current. | I | PP |
| BKD-DOD-003 | A phase of the backend is done when every check of 22.4 is green on the main branch, no recorded exception is past its date, the phase-end review (BKD-WRK-010) is recorded, and the user has seen a working demonstration of the new behaviour. | D | PP |
| BKD-DOD-004 | The backend is complete only after the final phase has added the deferred items by the plan approved by the user (BKD-DEF-006). | I | PP |

### 22.3 Workflows for agents

Agents follow the steps in order. A step that needs a decision of the user stops the work until the user answers (BKD-CON-012).

| ID | Workflow | Steps | Ver. | Basis |
|---|---|---|---|---|
| BKD-WRK-001 | Start a backend | (1) Read the business-requirements document, the frameworks document and the API contract. (2) Fill or confirm P01 to P13 with the user, including the MVP scope and the foundation set of packages (16.1). (3) Propose the domain list (P03) with responsibilities and the dependency graph, and get the user's confirmation. (4) Choose the boundary tool (P04) and the build, and record the mapping in 24.4. (5) Create the composition root and the first domain completely (BKD-MOD-013). (6) Make the checks of 22.4 pass on the first commit. | I | PP |
| BKD-WRK-002 | Add a domain | (1) State its responsibility, owner, aggregates, published use cases, events, tables and API path prefix in the inventory. (2) Update the dependency matrix and check for cycles. (3) Create the backend folder of 3.1 with the layers it needs and its migrations. (4) Implement one working use case. (5) Add the migration and the migration history of its schema. (6) Register the domain in the composition root. (7) Run all checks. | I | PP |
| BKD-WRK-003 | Add a use case or an operation | (1) Find the operation in the API contract; if it is missing, stop and ask for the contract change. (2) Write the domain behaviour. (3) Write the use case. (4) Write the adapters and the mapping. (5) Write the controller or handler from the contract. (6) Define the result of each failure path. (7) Run all checks. | I | PP |
| BKD-WRK-004 | Change the schema | (1) Decide whether the change breaks the previous release; if yes, plan the expand-contract steps over releases. (2) Write the migration in the owning domain's folder. (3) Change the repository adapter and the read models. (4) Run the migration against an empty database and against a database with data of the previous release. (5) Change the database document. (6) Run the migration linter and all gates. | I | EVO |
| BKD-WRK-005 | Add an external integration | (1) Record the external system, its contract and sandbox. (2) Define the port in domain terms in the application layer. (3) Write the adapter as an anti-corruption layer with timeouts, retry and limits from P09. (4) Decide and document the unavailable behaviour in the inventory. (5) Put the address and credentials in configuration only. | I | REL |
| BKD-WRK-006 | Publish or consume an event | (1) Write the event in the past tense with its version. (2) Add it to the catalogue with schema and example. (3) Save it through the outbox in the state-changing transaction when its loss would leave data inconsistent (BKD-TRG-002). (4) Write the consumer as an idempotent use case with the processed-message record. (5) Define the behaviour for duplicate, late and out-of-order delivery and for the dead-letter path. | I | OUT |
| BKD-WRK-007 | Change a published interface | (1) Decide whether the change is additive. (2) If it is not, keep the old form beside the new one until all callers moved. (3) Update the callers and the dependency matrix. (4) Record the change in the domain README. | I | MON |
| BKD-WRK-008 | Extract a domain into a service | (1) Check the readiness conditions of BKD-EVO-002. (2) Record the decision with requirement, alternatives, consequences and the way back (BKD-EVO-006). (3) Follow the steps of BKD-EVO-003. (4) Keep the API contract unchanged (BKD-EVO-007). | I | MON |
| BKD-WRK-009 | Add a package | (1) State the requirement. (2) Check the standard library, the framework and own code of at most 100 lines (BKD-DPN-002). (3) If a package remains, assess it and write the register entry (BKD-DPN-014). (4) Show the entry and the alternatives to the user and wait for the approval. (5) Add it with an exact version and the lock file, in the one module that needs it, behind an adapter. (6) Run the gates, including the register check. | I | DEP |
| BKD-WRK-010 | Review at the end of a phase | (1) List the packages, check use, and remove the unused ones. (2) Replace packages that serve one small use where BKD-DPN-003 allows. (3) Review the shortcut register: which triggers occurred, which shortcuts must be replaced. (4) Review the recorded deviations and exceptions to limits. (5) Update the inventory, the matrix and the register. | I | YAG |
| BKD-WRK-011 | Introduce a capability on its trigger | (1) Record the evidence of the trigger (a measurement, a requirement or an incident). (2) Record the decision with the alternatives and the way back (BKD-EVO-006). (3) Replace the MVP form behind the seam in one small change. (4) Remove the shortcut entry. (5) Keep the contract and the domain code unchanged. | I | YAG |
| BKD-WRK-012 | Move code to a `shared` folder | (1) Show that the code is the same in meaning in each place, stable, and free of domain words. (2) Show that at least two domains need it now and that the copy rule is met (BKD-DRY-002). (3) Move it with a README entry to the right part of the `shared` folder of the area, and name the owner. (4) Replace the copies and run all checks. (5) Record the move in one line. | I | PP |
| BKD-WRK-013 | Split a slice off | (1) Check BKD-SLC-008 and BKD-EVO-002. (2) Record the decision (BKD-EVO-006). (3) Give the slice its own build and release unit inside the monorepo, limited to its folders in all areas, with the shared parts consumed as versioned packages or copies (BKD-EVO-008). (4) Run it for a release. (5) Only then, if needed, move the folders to their own repository. | I | MON |

### 22.4 Required checks (gates)

The checks below hold on every change, and a failing check blocks the merge. How they are run (pipelines, order, tooling) is defined in the pipeline document.

| ID | Gate | Ver. | Basis |
|---|---|---|---|
| BKD-QGT-001 | Format, import order, naming style, strict compiler or type-checker mode, and static analysis pass with no new finding (BKD-CLN-014 to BKD-CLN-016). | T | PP |
| BKD-QGT-002 | The architecture test passes: layers follow the dependency rule; domains use only published surfaces; no `shared` folder imports a domain or a composition point; there are no cycles; no import crosses a domain folder in the backend or between areas; the domain layer has no forbidden imports; and the dependency graph equals the matrix of 24.3. | T | MON |
| BKD-QGT-003 | The contract check passes: the generated interfaces are current and the implemented routes, parameters, statuses and problem codes equal the API contract. | T | PP |
| BKD-QGT-004 | The migration linter passes (BKD-MIG-013), and all migrations apply to an empty database of the production engine and to a database with data of the previous release. | T | EVO |
| BKD-QGT-005 | The limits of 21.2 hold, or every exception is recorded and not expired. | T | PP |
| BKD-QGT-006 | The settings table matches the typed settings of the code, and the application starts with the documented example values. | T | 12F |
| BKD-QGT-007 | A scan for forbidden content finds no secret value, no deferred-item scaffold, no test hook and no commented-out code in the change. | T | PP |
| BKD-QGT-008 | The artefact builds once, starts in the `web`, `worker` and `migrate` roles, and answers the liveness and readiness checks. | T | 12F |
| BKD-QGT-009 | The dependency register agrees with the lock file, every direct package is in the register with its scope, the budget of 21.2 holds or the excess is recorded, versions are exact, and no build or test package is in the runtime artefact. | T | DEP |
| BKD-QGT-010 | A route inventory test lists every route and every operational endpoint of the running application and finds nothing that is not in the API contract or the operational document (this includes endpoints and pages added by packages or by framework defaults). | T | DEP |
| BKD-QGT-011 | The slice check passes: every domain has the folders that the inventory lists in each area, a README and an owner entry per area folder; the names of the operations, schema, events and entry point carry the domain's name; and the bundled contract equals the API sources plus the common components. | T | MON |

### 22.5 Traceability

| ID | Statement | Ver. | Basis |
|---|---|---|---|
| BKD-TRC-001 | Every use case is traceable from a requirement through an operation of the API contract to its implementation, by identifier. | I | PP |
| BKD-TRC-002 | Every rule of this document that applies to a change is verified by the gate or review point that the rule's verification method names (T, I, A or D). | I | PP |
| BKD-TRC-003 | A deviation, an exception to a limit and an architecture decision are recorded with an identifier, an owner and a date, and are reviewed at the end of each phase. | I | PP |

### 22.6 Recommendations (informative)

The points below are good practice for an MVP that is meant to grow. They use SHOULD and are not rules; a project that does otherwise records no deviation, but a reviewer MAY ask for the reason.

| ID | Recommendation | Basis |
|---|---|---|
| BKD-REC-001 | Build a **walking skeleton** first: one real use case through all layers, the database, the migration and the contract operation, with the architecture check in place. Then add use cases one at a time as thin vertical slices. | YAG |
| BKD-REC-002 | Start from the glossary. Let the names of domains, use cases, events and tables come from it, and fix a wrong name early, because names spread into the contract and the tests. | DDD |
| BKD-REC-003 | Prefer boring, well-known technology and the defaults of the platform. Choose the option that most team members and agents already know, unless a requirement points elsewhere. | PP |
| BKD-REC-004 | Keep the database simple: one engine, tables with clear names, constraints for every rule that the data can express, indexes created with the queries that need them, and no engine-specific feature without a recorded reason. | PP |
| BKD-REC-005 | Put rules into the database where the database is the better guard (unique, not null, check, foreign key within a domain), and into the domain where the rule needs a good error or a state. Never only in a trigger. | DDD |
| BKD-REC-006 | Let the platform module stay small: the clock, identifiers, the unit of work, the error types, the problem mapping, the settings loader and the in-process dispatcher. Resist adding to it; a technical helper that one domain uses belongs to that domain. | MON |
| BKD-REC-007 | Write ports for effects on the outside world and for nothing else. A port exists because a technology may change or an effect must be replaceable, and a replacement can be written in a few lines. | CLA |
| BKD-REC-008 | Write mappers by hand and keep them next to the adapter that needs them. A mapper that is boring to write is a sign that the boundary is right. | PP |
| BKD-REC-009 | Make commands return the identifier and the version only. Let the client read the state with a query. This keeps use cases small and the contract stable. | CLA |
| BKD-REC-010 | Decide the failure behaviour before writing the handling. For each outbound call, start with the timeout, the retry and the documented degraded result. | REL |
| BKD-REC-011 | Keep the error catalogue short and stable. Add a code when a client needs to act differently, not for each place where an error can arise. | PD |
| BKD-REC-012 | Keep settings few. Each setting is a decision that someone must make in every environment. Prefer a safe default and a name that carries its unit. | 12F |
| BKD-REC-013 | Keep migrations tiny and frequent. A migration that a reviewer can read in one minute is safe to run. Separate structure from data. | EVO |
| BKD-REC-014 | Measure before optimising, and keep a small set of timings in the load test (the slowest list query, the busiest write, the longest job) so that a regression is visible. | YAG |
| BKD-REC-015 | Treat the inventory (24.2), the matrix (24.3), the register (24.7) and the shortcut register (24.8) as living documents. Review them at the end of each phase and delete what no longer holds. | PP |
| BKD-REC-016 | Write short architecture decisions (one page: context, decision, alternatives, consequences, way back) for the choices that are costly to reverse: the domain list, the layering profile, the framework, the data-access technique, each trigger capability. | PP |
| BKD-REC-017 | Spend effort on internal quality continuously. Poor structure starts faster and slows the team within weeks; small refactorings under passing tests keep the speed. | PP |
| BKD-REC-018 | Prefer a few strong types (money, identifier types, status enums with transitions) to many generic ones. Put them in the domain where they carry rules. | DDD |
| BKD-REC-019 | Keep controllers, handlers and jobs so thin that they could be generated. If a reviewer cannot read one in half a minute, logic has leaked in. | CLA |
| BKD-REC-020 | Prepare the way for the final phase by keeping the seams, not by building placeholders: the request context, the acting-user columns, the decorator slot, the declarations in the contract. | DEF |
| BKD-REC-021 | For read-heavy simple screens inside one domain, a direct query that returns a view model is acceptable (BKD-DMN-014). Use the full aggregate path for writes and for rules. | DDD |
| BKD-REC-022 | When two designs are equally good, choose the one with fewer packages, fewer moving parts and fewer settings. | DEP |
| BKD-REC-023 | Review each new package as if it were a new colleague: who maintains it, what does it bring along, what will it cost to leave? Write the answer in the register in three sentences. | DEP |

## 23. References

The rules were derived from the sources below. The table is informative: it shows what each source contributed. A rule stands on its own and does not depend on the source being unchanged.

| Topic | Source | Used for |
|---|---|---|
| Layers and the dependency rule | [The Clean Architecture, R. C. Martin](https://blog.cleancoder.com/uncle-bob/2012/08/13/the-clean-architecture.html) | Inward dependencies, entities and use cases, data across boundaries (sections 5 to 8) |
| Ports and adapters | [Hexagonal Architecture, A. Cockburn](https://alistair.cockburn.us/hexagonal-architecture/) | Ports owned by the inner layer, adapters, testing without the outside world (sections 5, 11) |
| Modular monolith first | [MonolithFirst, M. Fowler](https://martinfowler.com/bliki/MonolithFirst.html) | Starting as one deployable with clear module boundaries (sections 3, 4, 18) |
| Enforced module boundaries | [Spring Modulith, fundamentals](https://docs.spring.io/spring-modulith/reference/fundamentals.html) | Published interfaces, verification of the module structure, events between modules (sections 4, 19) |
| Bounded context and aggregate | [BoundedContext](https://martinfowler.com/bliki/BoundedContext.html) and [DDD Aggregate](https://martinfowler.com/bliki/DDD_Aggregate.html), M. Fowler | Semantic domains, ubiquitous language, small aggregates (sections 4, 6) |
| Web application architecture | [Common web application architectures, Microsoft](https://learn.microsoft.com/en-us/dotnet/architecture/modern-web-apps-azure/common-web-application-architectures) and [Azure architecture patterns](https://learn.microsoft.com/en-us/azure/architecture/patterns/) | Clean-architecture folder structure for a web application, cloud design patterns (sections 3, 11, 13) |
| Application configuration and processes | [The Twelve-Factor App](https://12factor.net/) | Configuration, statelessness, build-release-run, disposability, admin processes (sections 13, 14) |
| Transactional outbox | [Transactional outbox, microservices.io](https://microservices.io/patterns/data/transactional-outbox.html) | Reliable publication of events with the state change (section 12) |
| Evolutionary database design | [Evolutionary Database Design, M. Fowler](https://martinfowler.com/articles/evodb.html) | Versioned migrations, parallel change, continuous integration of the schema (section 10) |
| Timeouts, retries and jitter | [Timeouts, retries and backoff with jitter, AWS Builders' Library](https://aws.amazon.com/builders-library/timeouts-retries-and-backoff-with-jitter/) | Bounded waits, retry at one layer, jitter, idempotency (sections 11, 13) |
| Cascading failures | [Addressing Cascading Failures, Google SRE book](https://sre.google/sre-book/addressing-cascading-failures/) | Load shedding, bounded queues, deadlines, graceful degradation (section 13) |
| Connection pool sizing | [About Pool Sizing, HikariCP](https://github.com/brettwooldridge/HikariCP/wiki/About-Pool-Sizing) | Small pools, the pool formula (section 9, 21.2) |
| Isolation levels | [Transaction isolation, PostgreSQL documentation](https://www.postgresql.org/docs/current/transaction-iso.html) | Default isolation, retries on serialisation failure (section 9) |
| Event-driven styles | [What do you mean by "Event-Driven"?, M. Fowler](https://martinfowler.com/articles/201701-event-driven.html) | The four meanings of event-driven (sections 12, 18) |
| Microservices | [Microservices, M. Fowler and J. Lewis](https://martinfowler.com/articles/microservices.html) | Costs and benefits, characteristics (section 18) |
| CQRS | [CQRS, M. Fowler](https://martinfowler.com/bliki/CQRS.html) | Separate read models (sections 9, 18) |
| Strangler fig | [StranglerFigApplication, M. Fowler](https://martinfowler.com/bliki/StranglerFigApplication.html) | Gradual extraction and replacement (section 18) |
| Error responses | [RFC 9457, Problem Details for HTTP APIs](https://www.rfc-editor.org/rfc/rfc9457) | The format of every error response (section 8) |
| HTTP semantics | [RFC 9110, HTTP Semantics](https://www.rfc-editor.org/rfc/rfc9110) | Status codes, conditional requests, idempotent methods (section 8) |
| Trace context | [W3C Trace Context](https://www.w3.org/TR/trace-context/) | The header accepted and returned for correlation (section 8) |
| Dependability | Release It!, M. Nygard (book); Designing Data-Intensive Applications, M. Kleppmann (book) | Stability patterns (timeout, breaker, bulkhead), consistency and failure models (section 13) |
| Clean code | Clean Code, R. C. Martin (book); Refactoring, M. Fowler (book) | Names, functions, small steps (section 15) |
| Cost of dependencies | [Our Software Dependency Problem, R. Cox](https://research.swtch.com/deps) | Criteria for adopting a package (design, testing, maintenance, use, licence, transitive packages, necessity) (section 16.1) |
| Package health signals | [OpenSSF Scorecard checks](https://github.com/ossf/scorecard/blob/main/docs/checks.md) | Maintenance, licence, pinned dependencies, known vulnerabilities as assessment points (section 16.1) |
| Building only what is needed | [Yagni, M. Fowler](https://martinfowler.com/bliki/Yagni.html) | Costs of building, delay, carry and repair; malleable code instead of speculative flexibility (section 2.2) |
| Internal quality | [Is High Quality Software Worth the Cost?, M. Fowler](https://martinfowler.com/articles/is-quality-worth-cost.html) | Why the structure of the fixed core is built from the first day (section 2.2) |

## 24. Project parameters and records

The agent fills this section with the user before the work that depends on it starts (BKD-CON-014). The *Default* column holds the value that applies until the answer is given. *Status* is *open*, *proposed* or *confirmed*; only *confirmed* answers are final.

### 24.1 Parameters

| # | Parameter | Question | Source | Default | Answer | Status |
|---|---|---|---|---|---|---|
| P01 | Language and framework | Which language, framework and version does the backend use? | Frameworks document | The choice in the frameworks document | | open |
| P02 | Repository layout | Does the project keep the folder layout of 3.1, or use the idiom of its platform? | User | The layout of 3.1 | | open |
| P03 | Domain list | Which semantic domains exist, and what is the one-sentence responsibility of each? | Business requirements, glossary | Proposed by the agent from the glossary and confirmed by the user | | open |
| P04 | Boundary enforcement tool | Which tool or test enforces layers and domain boundaries? | Section 19 | The tool named for the platform in section 19 | | open |
| P05 | Layering profile | Which layering profile does each domain use: strict (separate domain and persistence models), pragmatic (framework model as persistence model) or light (simple domains)? | Section 5, BKD-DRY-006 | Strict for domains with business rules, light for simple record keeping | | open |
| P06 | Identifier generator | Which identifier type and generator do aggregates use? | Database document | Time-ordered UUID | | open |
| P07 | Database limits and pool | What are the statement time, lock wait, pool acquisition time, and the pool size formula, and how many instances run? | Non-functional requirements, 21.2 | The values of 21.2 | | open |
| P08 | Page sizes | What are the default and maximum page sizes of lists? | API contract | 25 and 100 | | open |
| P09 | Outbound limits | What are the timeouts, retry attempts, backoff and breaker thresholds for each external system? | Integration descriptions, 21.2 | The values of 21.2, for each system | | open |
| P10 | Request and process limits | What are the request deadline, concurrency and queue limits, start-up time and shutdown grace? | Non-functional requirements, 21.2 | The values of 21.2 | | open |
| P11 | Code limits | Which limits of 21.2 does the project change? | User | The values of 21.2 | | open |
| P12 | Dependency policy | Which packages form the foundation set, what are the budget, the allowed licences, the age and size limits, and the limit for own replacement code? | Section 16.1, P01 | The limits of 21.2 and the foundation set of BKD-DPN-006 | | open |
| P13 | MVP scope | Which domains and use cases belong to the MVP, which triggers of 2.2 apply (and who decides that one has occurred), and how many domains at most? | User, business requirements | At most 8 domains; the triggers of 2.2 | | open |

### 24.2 Domain inventory

One row per domain (BKD-MOD-003). The agent keeps the inventory current in the same change as the domain.

| Domain | Responsibility (one sentence) | Owner | Aggregates | Published use cases | Events published | Events consumed | Schema and tables | API path prefix and tag | Unavailable behaviour of its dependencies | Layering profile |
|---|---|---|---|---|---|---|---|---|---|---|
| *(to be filled with P03)* | | | | | | | | | | |

### 24.3 Domain dependency matrix

A row calls or consumes a column. Mark *call* for a published use case and *event* for a consumed event. The matrix has no cycle and is equal to the dependency graph found by the architecture test (BKD-QGT-002).

| From \ To | *(domain)* | *(domain)* | *(domain)* |
|---|---|---|---|
| *(domain)* | – | | |
| *(domain)* | | – | |
| *(domain)* | | | – |

### 24.4 Tool selection record

The agent records the tool chosen for each capability after the user confirms the framework (BKD-FWK-001). The table lists the capabilities; the tool column is filled per project.

| Capability | Rule | Tool or mechanism | Decision date |
|---|---|---|---|
| Build and dependency catalogue | BKD-BLD-001, BKD-BLD-002 | | |
| Boundary and layer enforcement | BKD-LAY-012, BKD-MOD-005 | | |
| Format and static analysis | BKD-CLN-014 to BKD-CLN-016 | | |
| Contract-first generation and check | BKD-IFC-001, BKD-BLD-006 | | |
| Data access and mapping | BKD-PER-002, BKD-PER-003 | | |
| Migrations and migration linter | BKD-MIG-001, BKD-MIG-013 | | |
| Transaction support and use-case decorators | BKD-APP-005, BKD-APP-014 | | |
| Outbound client, retry, breaker and bulkhead | BKD-INT-003 to BKD-INT-006 | | |
| In-process event dispatcher and outbox relay | BKD-EVT-004, BKD-EVT-016 | | |
| Job scheduling and locks | BKD-DEP-018, BKD-DEP-019 | | |
| Settings loading and validation | BKD-CFG-002 | | |
| Folder and package idiom (if different from 3.1) | BKD-ARC-002 | | |
| Dependency register and budget check | BKD-DPN-020, BKD-QGT-009 | | |
| Route inventory test | BKD-QGT-010 | | |
| Cross-part boundary check (imports between slices and between parts) | BKD-SLC-003, BKD-SLC-008 | | |
| Contract bundling and client or transfer-type generation | BKD-SLC-006, BKD-DRY-009 | | |
| Code-ownership file | BKD-SLC-011 | | |

### 24.5 Project rules

Rules that the project adds to this document use the prefix `BKD-PRJ-`, the same table form, and a verification method. They may tighten a rule and never relax one (section 2.1).

| ID | Statement | Ver. | Basis |
|---|---|---|---|
| *(none yet)* | | | |

### 24.6 Open questions

The agent asks these before it designs the first domain, unless a document already answers them.

| # | Question | Why it matters |
|---|---|---|
| Q01 | Which actors and which business capabilities does the glossary name, and which of them are separate vocabularies? | It decides the domain list (P03). |
| Q02 | Which external systems exist in the first phase, and does each have a sandbox and a contract description? | It decides the clients and P09. |
| Q03 | What load, response-time and availability targets apply? | It decides the limits of P07 and P10. |
| Q04 | Are there operations that must be atomic across two business areas? | It decides whether two areas form one domain (BKD-MOD-009). |
| Q05 | Which operations are long-running or scheduled? | It decides the jobs, the worker role and the locks. |
| Q06 | Is there a legacy system or database that the backend must read, replace or live beside? | It decides anti-corruption layers and the strangler approach (section 18). |
| Q07 | Does any requirement already call for a broker, a cache, a search engine or separate scaling? | It decides whether BKD-EVO-006 applies now. |

### 24.7 Dependency register

One row per direct package (BKD-DPN-001, BKD-DPN-014). The foundation set is entered once, at the start. Every other package needs the approval of the user before it is added (BKD-DPN-021).

| Package | Scope (runtime, build, test) | Used by (module or adapter) | Purpose and requirement | Alternatives considered (standard library, framework, own code) | Licence | Last release | Transitive packages | Owner | Approved by user (date) | Replacement or removal plan |
|---|---|---|---|---|---|---|---|---|---|---|
| *(language and framework of P01)* | | | | | | | | | | |
| *(database driver)* | | | | | | | | | | |
| *(migration tool)* | | | | | | | | | | |

### 24.8 Shortcut register

One row per MVP shortcut (BKD-MVP-008). A shortcut is replaced when its trigger occurs (BKD-MVP-012).

| # | Shortcut | Trigger that ends it (2.2) | Seam that is kept | Owner | Date taken | Status |
|---|---|---|---|---|---|---|
| *(none yet)* | | | | | | |
