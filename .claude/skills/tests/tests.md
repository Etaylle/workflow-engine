# Testing Strategy and Test Specification

| Attribute | Value |
|---|---|
| Version / status | 0.5, draft |
| Identifier prefix | `TST-` (stable; never renumbered or reused; withdrawn rules stay listed as *withdrawn*) |
| Obligation level | The keyword in the statement: **MUST** (= shall), **SHOULD**, **MAY** |
| Verification methods | **T** test (executed, automated or manual) · **I** inspection (review of an artefact) · **A** analysis · **D** demonstration |
| Basis abbreviations | PYR = The Practical Test Pyramid (Vocke) and the test pyramid (Fowler) · GHK = Gherkin reference and Cucumber guidance (syntax, keyword semantics, "Better Gherkin") · WM = WireMock documentation · HTTP = HTTP semantics (RFC 9110) · OAS = OpenAPI Specification · WCAG = WCAG 2.2 success criterion · PP = project practice |
| Defaults and parameters | Values marked **default**, and every table labelled *default*, are non-normative: they apply only until the matching project parameter in section 26 is filled, are overridden by it, and have no power over any rule (section 2.1). |

## 1. Scope and definitions

**Scope.** This document is the single contract for how a product is tested: the test levels and the shape of the suite (the test pyramid), the rules for behaviour scenarios written in Gherkin and run by Cucumber, the frontend integration tests that run against a WireMock server, the end-to-end tests that exercise the whole application without any stub, test data, test identifiers, reporting, the pipeline gates and the agent workflow. It is independent of any product, brand, framework or programming language: the test tools are selected from the main frameworks of the project (section 4). The document is self-contained: it states the rules of neighbouring disciplines that it needs (API contract, interface identifiers, accessibility, localisation) so that it can be used with any framework, language and project structure. Security tests and observability tests are deferred to the final project phase (section 21).

| Term | Definition |
|---|---|
| System under test (SUT) | The part of the product that a test exercises, from the entry point of the test to the boundary where it ends. |
| Test level | A class of tests with the same subject, collaborators and purpose (section 3.1). |
| Unit test | A test of one unit of logic (function, class, module) in one process, without input or output, network, database or clock. |
| Component test | A test of one user-interface component or view, rendered and operated, with its backend adapter replaced. |
| Backend integration test | A test of the backend application over its real interface (HTTP, messages) with its real database and owned infrastructure. External services that the backend calls, if any, are replaced by a stub server. |
| Contract test | A test that verifies that real behaviour conforms to a written interface contract (provider side), or that a consumer's expectations are met (consumer-driven). |
| Frontend integration test | A test of the built frontend in a real browser against a stub server (WireMock) that mocks the backend and every external HTTP service. The real backend is not started. |
| End-to-end (E2E) test | A test of the whole application deployed as in production, driven through a real browser, with no stub of any component of the application. |
| Whole application | Every component that the product deploys: reverse proxy or gateway, frontend, backend, database and any other owned service. |
| Target environment | The environment that an end-to-end run tests: a local stack, a development, test, staging or preview environment, or another non-production environment, however it is deployed (containers, virtual machines, platform services). |
| Environment profile | The file of the end-to-end package that holds the addresses and settings of one target environment (TST-E2E-033). |
| Managed mode | The mode in which the end-to-end package starts the whole application from the deployment definition, runs, and destroys it (TST-E2E-031). |
| External mode | The mode in which the end-to-end package runs against an environment that already runs, and starts and stops nothing (TST-E2E-031). |
| Stub server | A server that answers HTTP requests with configured responses. The default is WireMock. |
| Stub mapping | One configured rule of the stub server: a request pattern and the response (or fault) that it returns. |
| Unmatched request | A request that reached the stub server and matched no stub mapping. |
| Scenario | One example of behaviour in Gherkin: `Given` state, `When` action, `Then` outcome. |
| Feature file | A plain-text file with one feature and its scenarios, in Gherkin. |
| Step | One line of a scenario that begins with `Given`, `When`, `Then`, `And` or `But`. |
| Step definition | The code that executes a step. |
| Glue | The step definitions, hooks, parameter types and helpers that a Gherkin runner loads. |
| Scenario context | The object that holds the state of one running scenario (responses, created identifiers, the browser page). It exists once per scenario. |
| Tag | An annotation that begins with `@` on a feature or scenario, used for selection, reporting and traceability. |
| Happy path | The scenario category in which the intended flow succeeds. |
| Wrong path | The scenario category in which an actor does something that business rules reject. |
| Broken data | The scenario category in which received or stored data is malformed, inconsistent or missing. |
| Edge case | The scenario category in which valid input or state is at or near a boundary, or is unusual: empty, single, minimum, maximum, duplicate, very long, unusual characters, time boundaries. |
| Unexpected event | The scenario category in which the environment, the timing or the system itself fails or misbehaves (dependency failure, timeout, double submission, concurrent change, lost connection, exception). |
| Kind of case | One of four classes of test case that every level covers: good path, bad path, edge cases, and error or unexpected cases (section 3.4). |
| Smoke scope | The small, fast set of critical scenarios that runs on every change. |
| Test identifier | A stable attribute on an interface element, used only to locate it in tests (default `data-testid`). |
| Test double | A replacement of a dependency in a test: stub (fixed answers), fake (a working simplified implementation), spy or mock (records calls). |
| Factory | Code that builds valid test objects with sensible defaults and explicit overrides. |
| Seeder | A tool that fills a system with a defined, realistic dataset through its public interface. |
| Flaky test | A test that passes and fails without a change of code or data. |
| Quarantine | The state of a flaky test that is excluded from the gates, reported, and owned, for a limited time. |
| Pyramid shape check | The check that the suite is not inverted (section 3.2, TST-PYR-008). |
| Project parameter | A project-specific decision recorded in section 26. |

## 2. Constraints and assumptions

| ID | Statement | Ver. | Basis |
|---|---|---|---|
| TST-CON-001 | Tests are product code. They are written, reviewed, versioned and maintained with the same care as production code, and a change is not done without its tests. | I | PP |
| TST-CON-002 | Every acceptance criterion and every documented behaviour of an operation or a user flow MUST be verified by at least one automated test, placed at the lowest level that can verify it with confidence (TST-PYR-003). A requirement that cannot be tested MUST be reported to the user, not silently dropped. | I | PYR |
| TST-CON-003 | Tests MUST be deterministic and independent. They MUST NOT depend on execution order, shared mutable state, the real clock, the real network outside the test environment, unseeded randomness, or the locale and time zone of the machine. Time, randomness, locale and time zone are explicit inputs. | T | PP |
| TST-CON-004 | Behaviour at a boundary (the HTTP interface of the backend, the user interface in a browser, the whole application) MUST be specified and verified with Gherkin scenarios executed by a Cucumber-family runner (section 5). Unit tests and component tests are not written in Gherkin. | I | PP |
| TST-CON-005 | Three Gherkin suites exist: backend integration (section 8), frontend integration (section 10) and end-to-end (section 11). They share the language, the writing rules (section 5), the tag taxonomy and the reports. They do not share step code between the browser-driven suites (TST-PYR-014). | I | PP |
| TST-CON-006 | Frontend integration tests MUST use a stub server (default WireMock) that mocks the backend as their only backend, and MUST make no call to a real backend. The backend suite uses a stub server only where the backend calls external services over HTTP (TST-INT-005). End-to-end tests MUST NOT use WireMock or any other stub server for any component of the application. | T | PP |
| TST-CON-007 | A test double MUST NOT replace the production database engine, message broker or other owned infrastructure in integration or end-to-end tests. They run as the production engine and version in a container or an equivalent isolated instance. In-memory substitutes with different behaviour MUST NOT be used. | T | PP |
| TST-CON-008 | The test tools are selected from the main frameworks of the project (section 4) and recorded (P01 to P05). The rules of this document are stated by contract: a rule that names a tool is a default, and the capability named in section 4 is the rule. | I | PP |
| TST-CON-009 | Security tests (authentication, authorisation, input hardening, security headers, rate limits, abuse) and observability tests (logging, metrics, tracing, alerting, health reporting beyond a readiness check) are deferred to the final project phase (section 21). Agents MUST NOT write, configure, scaffold or modify them earlier. Tests of business rules (validation, state transitions, conflicts, concurrency) are not security tests and are not deferred. | I | PP |
| TST-CON-010 | Project parameters (section 26) MUST be completed before the work that depends on them starts. Until then defaults apply. | I | PP |
| TST-CON-011 | An agent MUST read the business-requirements document and the frameworks document before it selects tools or writes scenarios, and MUST ask the user when they do not answer a question. It MUST NOT invent acceptance criteria, business rules, expected values or error behaviour. | I | PP |
| TST-CON-012 | Tests MUST NOT run against production, and MUST NOT use production data or personal data. Test data is synthetic (TST-DAT-004). | T | PYR |
| TST-CON-013 | A failing test is fixed, in the code or in the test with a recorded reason. It MUST NOT be weakened, skipped, commented out or deleted to make a run pass. | I | PP |
| TST-CON-014 | A test written only to raise a metric (coverage, count per level) without verifying behaviour MUST NOT be committed. | I | PP |
| TST-CON-015 | Test artefacts (reports, screenshots, traces, videos, logs, stub journals) MUST NOT contain secrets or personal data. | T | PP |
| TST-CON-016 | The tests of a change are part of the same change as its code. Scenarios for a new operation or flow SHOULD be written before the implementation and be agreed with the requirement they verify. | I | PP |
| TST-CON-017 | The suite MUST run without a pipeline: every level has a documented command that runs it locally (TST-PYR-012). The pipeline runs the same commands. | T | PP |

### 2.1 Rule classes and precedence

| Class | Definition | Can be overridden by |
|---|---|---|
| Fixed rule | A requirement whose basis is GHK, HTTP, OAS or WCAG. | Nothing. A project MAY only tighten it. |
| Rule | Any other requirement (basis PYR, WM, PP). | A project parameter that tightens it, or a recorded and human-approved deviation that relaxes it. |
| Parameter | A project value in section 26. | Replaces defaults. Cannot relax a fixed rule; cannot relax a rule without a deviation. |
| Default | A value labelled *default* and every table labelled *default*. | Overridden by any parameter. A default has no power over any rule. |

| ID | Statement | Ver. | Basis |
|---|---|---|---|
| TST-PRE-001 | The order of precedence MUST be: fixed rules, then rules, then parameters, then defaults. | I | PP |
| TST-PRE-002 | A default MUST NOT be used to justify a violation of a rule. Where a default conflicts with a rule, the rule prevails and the default is void. | I | PP |
| TST-PRE-003 | A value marked *default* in a requirement is overridable. An unmarked value is part of the rule. | I | PP |
| TST-PRE-004 | Verification MUST run against the effective values after overrides, never against the defaults. | T | PP |
| TST-PRE-005 | If two rules in this document conflict, the more specific rule wins. If equally specific, the stricter rule wins. The conflict MUST be recorded. | I | PP |
| TST-PRE-006 | A project's own API, frontend-architecture and design-system documents govern the interface under test, as long as they do not contradict a fixed rule of this document. Where they require a test level or a check that this document makes optional, their requirement prevails. A contradiction is recorded (TST-PRE-005). | I | PP |
| TST-PRE-007 | A statement that names a tool describes the default tool. A project MAY replace it by a tool with the capabilities listed for it in section 4, and records the replacement (P03 to P05). Replacing a tool never relaxes a rule. | I | PP |
| TST-PRE-008 | Examples and illustrative snippets are non-normative. | I | PP |

## 3. Test levels and the shape of the suite

### 3.1 The levels

The levels are named as in this table. The names are used in every document, report, tag and command of the project.

| Level | Name | Subject | Collaborators | Written in | Share of the suite |
|---|---|---|---|---|---|
| L1 | Unit | One unit of logic: business rules, state machines, validators, mappers, formatters, utilities | Replaced or pure; no input or output, network, database or clock | The unit test framework of the language | The largest count |
| L2 | Component | One interface component or view: rendering, states, interaction | The backend adapter replaced; a real browser engine where behaviour needs one | The component test runner | Many |
| L3 | Backend integration | The backend application over HTTP with its real database and owned infrastructure | Where the backend calls external HTTP services, the stub server replaces them; otherwise none | Gherkin | Some |
| L4 | API contract | Real responses against the written contract (provider side) | The backend application as in L3 | The contract test tool | One test set per operation |
| L5 | Frontend integration | The built frontend in a real browser | The stub server for every backend and external call; no real backend | Gherkin | Some |
| L6 | End-to-end | The whole application through a real browser | None replaced | Gherkin | Very few |
| L7 | Non-functional | Load, endurance and other measured qualities | A production-like environment | The tool of the quality | By requirement |

Automated accessibility checks and visual checks run inside L2, L5 and L6 (section 14) and are not separate levels. Security and observability tests are deferred (section 21).

```text
                 /\
                /L6\            very few   whole application, no stubs
               /----\
              / L5   \          some       browser + stub server
             /--------\
            /  L3  L4  \        some       backend + real database, contract
           /------------\
          /   L2         \      many       components
         /----------------\
        /       L1         \    most       units
       /--------------------\
```

### 3.2 Rules for the levels and the shape

| ID | Statement | Ver. | Basis |
|---|---|---|---|
| TST-PYR-001 | The levels and names of 3.1 MUST be used consistently. A tool's own names (for example "service test", "API test", "UI test") are mapped to a level in the project's level inventory (26.2) and are not used as level names. | I | PYR |
| TST-PYR-002 | The default shape of the suite is the pyramid: many fast low-level tests, fewer integration and contract tests, very few end-to-end tests. A project MAY choose another shape (diamond, trophy, honeycomb) with a recorded reason (P02, 3.3). The rules about placement and duplication below apply to every shape. | I | PYR |
| TST-PYR-003 | Each test belongs to exactly one level and MUST sit at the lowest level that can verify the behaviour with confidence. A test is pushed down when a lower level can verify it. | I | PYR |
| TST-PYR-004 | A behaviour already verified at a lower level MUST NOT be verified again at a higher level. The exceptions are the dual coverage cases of section 17, where the levels verify different things (the contract of an operation, the behaviour of the interface, the journey of a user). | I | PYR |
| TST-PYR-005 | When a test of a higher level fails and no test of a lower level fails, a test at the lowest suitable level that reproduces the failure MUST be added with the fix. | I | PYR |
| TST-PYR-006 | The higher levels verify what the lower levels cannot: wiring and configuration, serialisation, routing, HTTP behaviour, browser behaviour, and the deployed whole. They MUST NOT be used to test every rule or every input combination. | I | PYR |
| TST-PYR-007 | The pipeline MUST report, for every run and every level: number of tests or scenarios, duration, passed, failed, skipped, flaky (section 16). | T | PP |
| TST-PYR-008 | The pyramid shape check MUST fail the pipeline when the suite is inverted. The default check compares counts: units plus components MUST outnumber backend integration plus frontend integration scenarios, and these MUST outnumber end-to-end scenarios (P02). The ratios between levels are not targets and MUST NOT be pursued for their own sake. The check only detects an inverted suite. | T | PYR |
| TST-PYR-009 | Pipeline stages MUST be ordered by speed and scope, not by test type: fast, narrow tests first, slow, broad tests last (section 18). | I | PYR |
| TST-PYR-010 | Exploratory manual testing complements the automated suite (P28). Each defect it finds MUST be turned into an automated test at the lowest suitable level, and each finding is treated as feedback on the suite. | I | PYR |
| TST-PYR-011 | The same behaviour is called by the same name in requirements, scenarios, tests, reports and the interface (the glossary of the business-requirements document). | I | PP |
| TST-PYR-012 | Every level has one documented command that runs it locally, a known location in the repository, and, for Gherkin suites, a selection by tag (section 5.3). The commands are recorded (26.2). | T | PP |
| TST-PYR-013 | A level MUST NOT depend on the infrastructure of a higher level. Unit and component tests start no server and no database. Frontend integration tests start no backend. Backend integration tests start no browser. | T | PP |
| TST-PYR-014 | The frontend integration suite (L5) and the end-to-end suite (L6) are separate suites, each with its own configuration, step definitions, scenario context, hooks and run command. They MAY share wording conventions and parameter-type definitions in a small library. They MUST NOT share step definitions, so that stub-specific steps never run against the real system. L6 is an independent npm package outside the frontend and backend directories (TST-E2E-029). | I | PP |
| TST-PYR-015 | The same scenario text MAY appear in L5 and L6 only for a primary journey; the purposes differ (the interface behaves correctly with known answers; the whole application works). | I | PP |
| TST-PYR-016 | A higher-level test that covers the whole of what a lower-level test covers, and nothing else, MUST be deleted. Tests that no longer add confidence are removed. | I | PYR |

### 3.3 Choosing the shape (informative)

| Project profile | Shape | Remark |
|---|---|---|
| Small team of developers, little manual testing | Pyramid | The default of this document |
| Larger product, developers only, little manual testing | Trophy or honeycomb | More weight on integration tests; static analysis at the base |
| Many small services whose complexity is in their interaction | Honeycomb | Integration tests dominate; unit tests only for non-trivial logic |
| Brittle unit tests that block refactoring | Diamond | Fewer unit tests, strong integration tests, few end-to-end tests |
| Heavy manual testing, little automation (an anti-pattern to leave) | Ice-cream cone, crab | The inverted shape is slow and costly; the suite is moved down step by step |

The shape serves the goal of shipping working software, not a coverage number. Tests that resemble real use give the most confidence, and automation complements manual testing without replacing it.

### 3.4 The four kinds of case at every level

Every level covers four kinds of case for the behaviour it verifies. The kinds map to the scenario categories of 5.4. A kind that is verified at a higher level concerns what that level adds (the interface, the whole), not the rules of the lower levels (TST-PYR-004, TST-PYR-019).

| Kind | Scenario categories | Meaning |
|---|---|---|
| Good path | `@happy-path` | Valid input and a working environment give the intended result |
| Bad path | `@wrong-path`, `@broken-data` | Invalid, forbidden or malformed input, or an action that business rules reject, is refused in the documented way |
| Edge cases | `@edge-case` | Valid input or state at or near a boundary, or unusual: empty, single, minimum, maximum, duplicate, very long, unusual characters, time boundaries |
| Error or unexpected | `@unexpected-event` | The environment, the timing or the system itself fails or misbehaves: a dependency fails or is slow, a timeout, a repeated or concurrent action, a lost connection, an exception |

| Level | Good path | Bad path | Edge cases | Error or unexpected |
|---|---|---|---|---|
| L1 Unit | Valid input gives the expected result; valid transitions succeed | Invalid input and forbidden transitions are rejected with the expected error or code | Boundary values (the limit, just inside, just outside); empty, single, duplicate; unusual text; rounding; time boundaries (month end, leap day, daylight-saving change) | A replaced collaborator fails, times out or returns nothing; an exception is raised and mapped; overflow or impossible input |
| L2 Component | The component renders valid data and the interaction completes | Invalid input shows its messages; forbidden actions stay disabled | Empty, single, very many items; very long and unusual text; missing optional values; narrow width; right-to-left text where supported | The adapter fails, is slow or rejects; repeated activation; unmounting during a request; cleanup |
| L3 Backend integration | Each operation succeeds with valid input and the state is as documented | Validation errors, unknown resources, state conflicts, failed preconditions, malformed or inconsistent data | Empty and single results; minimum and maximum lengths and values; page-size limits, first and last page; duplicates; unusual valid text; large numbers; date and time boundaries | A dependency fails or is slow (stub faults); a constraint is violated; a concurrent change; a repeated request; no partial state remains |
| L4 API contract | Documented successes and examples validate | Documented error responses validate | Limits, formats, optional and nullable properties at their boundaries | No undocumented status or route; server errors on edge input found by contract fuzzing |
| L5 Frontend integration | The flow works with known backend answers | Validation messages next to the fields with the input kept; business rejections with message and next action | Empty and single-item collections; maximum size; very long and unusual text; missing optional values; first and last page; narrow viewport | Faults, delays, server errors, malformed bodies, reset connections; double submission; leaving the page during a request; recovery after the fault |
| L6 End-to-end | The primary journey works through the whole application | One rejection from the real backend that the user sees | The largest or smallest realistic data and unusual characters through the whole chain | Reload in the middle, back navigation, repeated submission, abandoning and returning |
| L7 Non-functional | The expected workload meets the thresholds | Invalid and rejected requests at volume do not degrade valid traffic | Peak, spike, largest payloads, cold start | A failing or slow dependency under load, and recovery afterwards |

| ID | Statement | Ver. | Basis |
|---|---|---|---|
| TST-PYR-017 | Every level MUST cover all four kinds of 3.4 for each unit, component, operation, flow or journey that it verifies. A kind that does not apply MUST be justified in the test group or in the review. A level that covers only the good path MUST be rejected (TST-REV-026). | T | PP |
| TST-PYR-018 | Tests of levels without scenario tags (L1, L2, L4) MUST be grouped or named by kind (for example the groups `good path`, `bad path`, `edge cases`, `errors and unexpected`), so that a reviewer sees the four kinds at once. Gherkin suites use the category tags (5.3). | I | PP |
| TST-PYR-019 | A kind at a higher level MUST add something that the lower levels cannot show (TST-PYR-006). It MUST NOT repeat the rules that are tested below (TST-PYR-004). | I | PYR |

## 4. Selecting the tools

The tools follow from the main frameworks of the project. The agent reads the frameworks document, fills the selection record (26.3) and then writes tests. Sections 4.2 to 4.5 are informative starting points; the rules of 4.1 are binding. Tools change: the status and versions in the tables were checked when this document was written and MUST be checked again at selection time.

### 4.1 Selection rules

| ID | Statement | Ver. | Basis |
|---|---|---|---|
| TST-SEL-001 | Tools MUST be selected per level, after the frameworks document is read, from the main frameworks of the backend, the frontend and the build system. The selection is recorded (26.3) with reason and alternatives considered before the first test is written. | I | PP |
| TST-SEL-002 | The backend unit tests, the backend integration tests and the Gherkin runner of the backend suite MUST use the language and platform of the backend, because steps call backend code, factories and containers directly. A Cucumber implementation for the same platform as the production code is preferred. | I | PP |
| TST-SEL-003 | The Gherkin runner of the frontend integration suite (L5) is chosen by the language of the test project. The Gherkin runner of the end-to-end suite (L6) is a Node.js runner, because L6 is an npm package (TST-E2E-029). The default for both is Cucumber-JS with Playwright. A runner that generates tests for the browser tool's own runner (for example playwright-bdd) conforms equally. The choice is recorded (P03). | I | PP |
| TST-SEL-004 | A Cucumber implementation MUST be official or semi-official (it uses the Cucumber Gherkin parser, tag expressions and messages) and actively maintained, unless the project records a risk acceptance. The maintenance status is checked and recorded at selection time. | I | PP |
| TST-SEL-005 | A Gherkin runner MUST provide: tag expressions for selection; parameterised steps (Cucumber Expressions); `Scenario Outline`; hooks selected by tag; a scenario context created once per scenario; parallel execution with control over exclusive resources; strict mode (undefined, pending and ambiguous steps fail the run); a dry run; JSON, message or HTML reports; rerun of failed scenarios; configurable timeouts. | I | GHK |
| TST-SEL-006 | A browser automation tool MUST provide: real browser engines (at least Chromium); automatic waiting for actionability; interception of network requests; an isolated context per scenario; traces, screenshots and video on failure; viewport and device emulation; control of the browser clock; headless operation in a container; parallel execution. | I | PP |
| TST-SEL-007 | A stub server MUST provide: request matching on method, path, query, headers and body; stateful behaviour (a scenario state machine); response delay and fault injection; a request journal with verification; reporting of unmatched requests; an administration interface to configure it at run time; file-based mappings; response templating; operation as a separate process or container; several instances in parallel. | I | WM |
| TST-SEL-008 | Integration and end-to-end tests MUST obtain their database and other owned infrastructure from a container tool or a compose definition that starts the production engine in the production major version (default Testcontainers, or the compose definition of the project). | T | PP |
| TST-SEL-009 | One tool serves one purpose at one level. Two runners for the same level are allowed only during a recorded migration. | I | PP |
| TST-SEL-010 | Feature files MUST stay plain, portable Gherkin. Runner-specific syntax (other than tags) MUST NOT appear in them, so that a runner can be replaced without rewriting scenarios. | I | GHK |
| TST-SEL-011 | Versions of test tools are pinned in the lock file or build file. An upgrade is a change that needs a green run. The dependency governance of the project applies to test tools. | I | PP |
| TST-SEL-012 | Test tools MUST run from a clean checkout, headless, in the pipeline container, without paid accounts and without services outside the test environment. Hosted browser clouds MAY be added as an extension. | T | PP |
| TST-SEL-013 | Where the main framework has an official test tooling (a test context, a test host, a test builder), that tooling is preferred for L1 to L3 over custom wiring. | I | PP |
| TST-SEL-014 | Every Gherkin suite names six parts in the selection record: the runner; the entry point under the platform's unit-test runner (so that IDEs and the pipeline see one result); the dependency injection of the scenario context; the driver (HTTP client or browser tool); the source of real infrastructure or the stub server; the report adapter. | I | PP |
| TST-SEL-015 | The frontend unit and component runner follows the build tool of the frontend (a Vite-based project uses Vitest by default; a project whose framework requires another runner uses it). A real-browser mode is used where behaviour needs a real engine. | I | PP |
| TST-SEL-016 | Tools for the deferred levels (security, observability) MUST NOT be selected, installed or configured before the final phase (TST-CON-009). | I | PP |
| TST-SEL-017 | When the frameworks document names no test tool, the agent proposes the default of 4.3 and 4.4 for the stack, records it as a decision, and asks the user to confirm it before installing it. | I | PP |

### 4.2 Cucumber implementations by platform (informative)

A Cucumber implementation for the same platform or language as the production code is preferred. The statuses are those of the Cucumber installation page when this document was written.

| Platform | Implementation | Status | Notes |
|---|---|---|---|
| Java, Kotlin, Scala | Cucumber-JVM (Cucumber-Scala for Scala) | official | JUnit Platform engine with `@Suite` and `@IncludeEngines("cucumber")`; dependency injection per scenario (Spring, PicoContainer and others); parallel execution with named exclusive resources; rerun plugin |
| JavaScript, TypeScript | Cucumber-JS | official | `--parallel N` workers; `setParallelCanAssign` to control concurrency; formatters for html, json and message |
| JavaScript, TypeScript | playwright-bdd | community | Generates Playwright tests from feature files; the Playwright runner keeps control (fixtures, traces, sharding); needs a generation step before every run |
| Ruby | Cucumber-Ruby | official | The reference implementation |
| .NET | Reqnroll | semi-official | Successor of SpecFlow, which reached end of life on 31 December 2024; packages for xUnit, NUnit and MSTest; hooks selected by tag |
| Python | pytest-bdd, Behave | semi-official | pytest-bdd runs inside pytest and uses its fixtures; Behave has its own runner |
| PHP | Behat | semi-official | |
| Go | gocuke (semi-official), Godog (official, listed as unmaintained), GoBDD (unofficial) | check at selection | Check the maintenance status before use (TST-SEL-004) |
| Other languages | Community implementations | unofficial | Feature files remain portable (TST-SEL-010) |

### 4.3 Backend stack to tools (informative defaults)

| Main backend framework | L1 unit | L3 Gherkin runner and driver | Real infrastructure | Stub server (only if the backend calls external HTTP services) | Coverage |
|---|---|---|---|---|---|
| Java or Kotlin with Spring Boot | JUnit 5, Mockito or MockK, AssertJ | Cucumber-JVM on the JUnit Platform with the Spring integration; MockMvc, WebTestClient or REST Assured | Testcontainers | WireMock with its JUnit or Spring support | JaCoCo |
| Java with Quarkus or Micronaut | JUnit 5, AssertJ | Cucumber-JVM with the framework's test support | Testcontainers or the framework's dev services | WireMock | JaCoCo |
| Node.js or TypeScript (NestJS, Express, Fastify) | Vitest or Jest | Cucumber-JS; the started application called over HTTP | Testcontainers for Node.js | WireMock as a container (request-mocking libraries only in L1) | c8 or Istanbul |
| Python (FastAPI, Django, Flask) | pytest | pytest-bdd or Behave; the framework's test client or an HTTP client | testcontainers-python | WireMock as a container | coverage.py |
| .NET (ASP.NET Core) | xUnit or NUnit | Reqnroll with xUnit or NUnit; `WebApplicationFactory` | Testcontainers for .NET | WireMock.Net | Coverlet |
| Go | The standard `testing` package with testify | gocuke or Godog; `httptest` or the started binary | testcontainers-go | WireMock as a container | `go test -cover` |
| PHP (Laravel, Symfony) | PHPUnit or Pest | Behat; the framework's test client | Compose services | WireMock as a container | PHPUnit coverage |
| Ruby (Rails) | RSpec or Minitest | Cucumber-Ruby with Rack::Test or Capybara | Compose services | WebMock or VCR in L1, WireMock as a container in L3 | SimpleCov |

### 4.4 Frontend stack to tools (informative defaults)

| Main frontend framework | L1 unit | L2 component | L5 and L6 browser driver |
|---|---|---|---|
| Angular | The framework's unit-test builder with Vitest (Karma and Jasmine in older projects) | TestBed | Playwright |
| React, Next.js | Vitest or Jest | Testing Library for React | Playwright |
| Vue, Nuxt | Vitest | Vue Test Utils or Testing Library for Vue | Playwright |
| Svelte, SvelteKit | Vitest | Svelte Testing Library | Playwright |
| Web Components (Lit, native) | Vitest | Vitest browser mode or Web Test Runner, in a real browser | Playwright |
| Other or none | The runner of the build tool | The Testing Library of the framework | Playwright |

Alternatives to Playwright: Cypress (own runner, Gherkin through a preprocessor), Selenium or WebdriverIO (when legacy browsers or mobile automation are required). The same capability list (TST-SEL-006) applies.

### 4.5 Pairings that work well with Cucumber (informative)

| Combination | Why it works |
|---|---|
| Cucumber-JVM, the JUnit Platform engine and Suite, Spring test context, Testcontainers, REST Assured or MockMvc, AssertJ, and WireMock where the backend calls external services | One Gradle or Maven entry, one Spring context shared across scenarios, real database in a container, tags map to the platform's filters |
| Cucumber-JS, TypeScript, Playwright, WireMock | Gherkin keeps the scenarios readable; Playwright provides auto-waiting, isolated contexts and traces; WireMock mocks the backend and isolates the frontend |
| playwright-bdd and Playwright | The Playwright runner provides fixtures, sharding, retries, trace viewer and its HTML report; feature files are compiled to tests |
| Reqnroll, xUnit or NUnit, `WebApplicationFactory`, Testcontainers for .NET | The platform runner provides discovery and parallelism; scenario hooks reset state per scenario |
| pytest-bdd, pytest fixtures, Playwright for Python, testcontainers-python | Fixtures supply the scenario context; the pytest ecosystem supplies plugins and reports |
| Any Cucumber runner and Allure | The JSON or message output is turned into one combined report across suites (section 16) |

## 5. Behaviour scenarios in Gherkin

Gherkin scenarios are the form of acceptance criteria at the boundaries of the product. They are written for people first (the user, the product owner, the reviewer) and executed second. The same language, the same writing rules and the same tags apply to all three Gherkin suites.

### 5.1 Feature files

| ID | Statement | Ver. | Basis |
|---|---|---|---|
| TST-GHK-001 | Acceptance criteria of operations (L3), of user-visible behaviour against stubs (L5) and of whole-application journeys (L6) MUST be written as Gherkin scenarios in feature files, stored in version control. Feature files are the single source of these criteria. | I | PP |
| TST-GHK-002 | `Given` MUST put the system in a known state, `When` MUST describe one action of the actor, and `Then` MUST state an observable outcome. `And` and `But` continue the previous keyword. A `When` MUST NOT follow a `Then` in the same scenario. | I | GHK |
| TST-GHK-003 | A feature file contains one feature: one capability, user story or group of related operations. The file name is kebab-case and names the capability (`cart-checkout.feature`). | I | PP |
| TST-GHK-004 | A scenario title MUST state the behaviour and its outcome (`An expired voucher is rejected`), not a technique (`Test voucher`) and not an implementation (`POST /vouchers returns 422`). | I | GHK |
| TST-GHK-005 | Scenarios MUST be declarative: they describe what the system does for the actor in domain language, not how it is done. The wording MUST survive a change of implementation. | I | GHK |
| TST-GHK-006 | Scenarios of the browser-driven suites (L5, L6) MUST NOT name interface mechanics (click, type, select, scroll, selector, URL path of a page, element identifier). The step definitions hold the mechanics. Scenarios of the backend suite (L3) MAY name the operation (by business name or operation identifier), status codes and error codes, because these are the observable contract. | I | GHK |
| TST-GHK-007 | A scenario MUST verify one behaviour. A scenario with several unrelated `When` actions is split. | I | GHK |
| TST-GHK-008 | A scenario has at most 10 steps (**default**, P08). A longer scenario is split, or its detail moves into step definitions or helper steps. | T | PP |
| TST-GHK-009 | Scenarios MUST be independent. A scenario MUST NOT depend on the outcome, the data or the order of another scenario and MUST pass when run alone, in any order and in parallel with others. | T | PP |
| TST-GHK-010 | `Background` is used only for `Given` steps that every scenario of the feature needs, at most 5 steps. It MUST NOT hold data that only some scenarios need or steps that act on the system under test. | I | GHK |
| TST-GHK-011 | `Scenario Outline` with `Examples` MUST be used for the same behaviour with different data (for example validation limits), and MUST NOT be used to combine different behaviours. Each example row is independent. An outline has at most 20 rows (**default**, P08). | I | GHK |
| TST-GHK-012 | Values that matter for the behaviour appear in the scenario. Incidental values come from factories or step defaults. A scenario contains no magic identifiers and no generated identifier that is typed in by hand. | I | PP |
| TST-GHK-013 | Steps use the vocabulary of the business-requirements document and of the interface (the glossary). The same concept has the same word in every feature file. | I | PP |
| TST-GHK-014 | Feature files are written in one natural language (P09, **default** English) with the Gherkin keywords of that language. | I | PP |
| TST-GHK-015 | Data tables and doc strings carry structured data. A payload appears in a doc string only when the payload is the subject of the scenario. | I | PP |
| TST-GHK-016 | A scenario that is not yet implemented is tagged `@wip`. The tag MUST NOT exist on the main branch (**default**). An undefined or pending step MUST fail the run (strict mode), never count as passed or skipped. | T | PP |
| TST-GHK-017 | Every scenario carries the tag of each requirement it verifies (5.3). Scenarios without a requirement tag are allowed only for technical scenarios (the seeder, deployment checks), which are tagged accordingly. | T | PP |
| TST-GHK-018 | A failing step MUST report an actionable message: the expectation, the actual value and, for HTTP, the request and the response (method, path, status, body, correlation identifier) and, for the browser, the page state (a screenshot and the identifier of the element). | T | PP |
| TST-GHK-019 | Feature files that define new behaviour are reviewed and agreed with the user or product owner before the implementation is written (TST-CON-016). Changing an agreed scenario is a change to the requirement and is recorded. | I | PP |
| TST-GHK-020 | Feature files MUST contain no code, no SQL, no selector, no credentials, no personal data and no environment-specific address. | T | PP |
| TST-GHK-021 | A scenario uses one consistent person and tense across the project: `Given` describes a state that already holds, `When` an action in the present, `Then` an outcome. | I | GHK |
| TST-GHK-022 | The feature files are readable without tools: the reports (section 16) render them with their tags, and the product owner can browse them in the repository or the report. | D | PP |

Example (non-normative):

```gherkin
@phase-2 @cart @p0 @smoke
Feature: Voucher at checkout
  A customer can redeem a valid voucher once.

  @req-CART-014 @happy-path
  Scenario: A valid voucher reduces the total
    Given a cart worth 100.00 EUR
    And an active voucher worth 10 percent
    When the customer redeems the voucher
    Then the cart total is 90.00 EUR

  @req-CART-015 @wrong-path
  Scenario: An expired voucher is rejected
    Given a cart worth 100.00 EUR
    And an expired voucher
    When the customer redeems the voucher
    Then the voucher is rejected as expired
    And the cart total is 100.00 EUR
```

### 5.2 Step definitions

| ID | Statement | Ver. | Basis |
|---|---|---|---|
| TST-STP-001 | Steps MUST be parameterised with Cucumber Expressions (or regular expressions where an expression cannot express the pattern). A step text MUST match exactly one step definition. An ambiguous or duplicate match MUST fail the run. | T | GHK |
| TST-STP-002 | Step definitions are organised by domain and by actor, not by feature file. Shared steps live in a common layer. Mechanics (HTTP calls, selectors, page structure) live in helper layers (API clients, screen drivers), and the steps stay thin. | I | PP |
| TST-STP-003 | A step definition MUST NOT call another step. Shared behaviour is shared through helper functions. | I | PP |
| TST-STP-004 | A step definition MUST NOT contain conditional logic or loops that change what is verified. A step either verifies its statement or fails. | I | PP |
| TST-STP-005 | Every `Then` step MUST assert. A `Then` step that asserts nothing, asserts a constant, or only logs MUST NOT exist. Assertions use an assertion library with messages. The language's built-in `assert` statement MUST NOT be used, because it can be disabled. | T | PP |
| TST-STP-006 | A `Given` step prepares state through the public interface of the layer under test, through a documented factory or through a seeder. It MUST NOT change the internals of the system under test in ways the product cannot reach. | I | PP |
| TST-STP-007 | A `When` step performs exactly one action of the actor and stores the result (response, resulting page state, error) in the scenario context. | I | PP |
| TST-STP-008 | The scenario context MUST be created once per scenario and hold all scenario state: responses, created resources with their real identifiers, the browser context and page. Static or global mutable state MUST NOT be used. Dependency injection MUST be scoped to the scenario. | T | PP |
| TST-STP-009 | Identifiers of resources that the system creates MUST be taken from the system's response and stored in the scenario context. Steps MUST NOT invent identifiers for resources that exist. | T | PP |
| TST-STP-010 | Hooks are selected by tag. `Before` and `After` hooks do per-scenario set-up and tear-down. `BeforeAll` and `AfterAll` hooks start expensive shared resources (containers, the browser, the stub server) that scenarios use read-only. In parallel runs these hooks run once per worker and MUST be idempotent per worker. | I | PP |
| TST-STP-011 | The tear-down in an `After` hook MUST run when the scenario fails, MUST NOT mask the failure, and MUST collect the evidence of the failure (screenshot, trace, response, stub journal) before it cleans. | T | PP |
| TST-STP-012 | Step timeouts are explicit (**default** 30 s, P08). Waits are conditional (a condition with a timeout), never fixed sleeps. | T | PP |
| TST-STP-013 | Strict mode is on. A dry run of all feature files (syntax and binding of every step) MUST run in the pipeline before the suites (section 18). | T | GHK |
| TST-STP-014 | Domain concepts used in steps (money, dates, durations, identifiers, roles) are expressed as custom parameter types, so that scenarios read in the language of the domain and conversion is in one place. | I | PP |
| TST-STP-015 | Test configuration comes from a `test` profile and from environment variables. Base addresses, ports and credentials of test infrastructure are never written into feature files or step definitions. | T | PP |
| TST-STP-016 | Step definitions and helpers are compiled and linted with the tooling of the project and reviewed like production code. | T | PP |
| TST-STP-017 | A step that covers an unexpected event (a delay, a fault, a double submission) is named for the event in domain terms, and its parameters are explicit (`the pricing service does not answer within 5 seconds`). | I | PP |

### 5.3 Tags

All Gherkin suites use one tag taxonomy. Tags are lowercase and kebab-case. A tag on a feature is inherited by its scenarios.

| Group | Tags | Meaning | Cardinality |
|---|---|---|---|
| Phase | `@phase-<n>` (the phase naming of P08) | The delivery phase that introduced the scenario | Exactly one |
| Priority | `@p0`, `@p1`, `@p2` | `@p0` blocks a release and belongs to smoke; `@p1` is important; `@p2` is useful (meanings in P08) | Exactly one |
| Category | `@happy-path`, `@wrong-path`, `@edge-case`, `@broken-data`, `@unexpected-event` | The scenario category (section 1, 3.4, 5.4) | Exactly one |
| Domain | `@<domain>` (the domain names of the architecture, lowerCamelCase turned to kebab-case) | The bounded area the scenario verifies | One or more |
| Scope | `@smoke`, `@regression`, `@full` | `@smoke` runs on every change; `@regression` runs when a change is merged and nightly; `@full` marks slow scenarios that run nightly and before a release | Exactly one |
| Requirement | `@req-<id>` (the identifier pattern of P08) | Traceability to the requirement | One or more |
| Visual | `@visual` | Adds screenshot evidence or comparison (section 14) | Optional |
| Serial | `@serial` | The scenario needs exclusive use of a shared resource and MUST NOT run in parallel with others | Optional |
| Work in progress | `@wip` | Not yet implemented; excluded from every run | Optional, never on the main branch |
| Quarantine | `@quarantine` | Known flaky; excluded from the gates, run and reported, with an owner and an expiry (section 19) | Optional |
| Seeder | `@seeder` | A data-seeder feature (section 12); excluded from test runs | Optional |
| Authorisation (reserved) | `@auth-<role>` | Reserved for the final phase (section 21). It MUST NOT be used earlier | Reserved |

Selection by tag expression (**default**): a change runs `@smoke`; a merge to the main branch runs `@smoke or @regression`; the nightly run and the release run everything except `@wip` and `@quarantine`.

| ID | Statement | Ver. | Basis |
|---|---|---|---|
| TST-TAG-001 | The taxonomy of 5.3 MUST be identical in the backend, frontend integration and end-to-end suites. A project MAY add groups, recorded in 26.4. | I | PP |
| TST-TAG-002 | Every scenario MUST carry exactly one phase tag, one priority tag, one category tag, one scope tag, at least one domain tag and at least one requirement tag (or the technical exception of TST-GHK-017). A tag lint check in the pipeline MUST fail a scenario that does not, a scenario with two tags of a group that allows one, and a scenario that carries a tag that is not in the catalogue (5.3, 26.4). | T | PP |
| TST-TAG-003 | Runs are selected only with tag expressions. A suite MUST NOT be selected by file name lists, by test names or by directories. | I | PP |
| TST-TAG-004 | `@smoke` scenarios MUST be fast, independent, and cover the critical behaviour of every domain. The smoke scope has a time budget (**default** 5 minutes per suite, P20). | T | PP |
| TST-TAG-005 | `@wip` is excluded from every run. `@quarantine` is excluded from the gates but MUST run in the nightly run and appear in the reports (TST-STB-006). | T | PP |
| TST-TAG-006 | `@serial` scenarios are mapped to an exclusive resource in the runner configuration, so that they never run in parallel with the scenarios that use the same resource. Their number is kept small. | T | PP |
| TST-TAG-007 | The `@auth-*` tags are reserved. Scenarios about authentication, authorisation and role isolation are written in the final phase (TST-DEF-001). | I | PP |
| TST-TAG-008 | The tag set is part of the report: scenarios can be filtered by every group in the reports (section 16). | D | PP |

### 5.4 Scenario categories and minimum sets

| Category | What it covers | Examples |
|---|---|---|
| Happy path | The intended flow succeeds | A valid order is placed; a valid form is saved |
| Wrong path | The actor does something that business rules reject | Missing required value; a value outside its limits; an unknown identifier; an action in the wrong state; a precondition that fails |
| Edge case | Valid input or state at or near a boundary, or unusual | Empty and single results; minimum and maximum lengths and values; page-size limits; duplicates; very long text; unusual characters; dates at boundaries |
| Broken data | Received or stored data is malformed, inconsistent or missing | Malformed body; wrong types; unknown properties; inconsistent or incomplete stored records; legacy records that lack newer fields |
| Unexpected event | The environment, the timing or the system itself fails or misbehaves | A dependency fails or is slow; a request times out; the same action is submitted twice; two actors change the same resource; the connection is lost; the user reloads or navigates away mid-flow; an exception is raised |

| ID | Statement | Ver. | Basis |
|---|---|---|---|
| TST-SCN-001 | Every operation of the backend (L3) MUST have at least: one happy-path scenario; one scenario for each documented error response (status and error code); and, where applicable, scenarios for a validation error, an unknown resource, a conflict or state error, and a failed precondition (`412` or `428`). The minimum set also contains the scenarios of TST-SCN-006, TST-SCN-011 and TST-SCN-014. A missing applicable error type MUST be justified in the review. | T | PP |
| TST-SCN-002 | Scenarios for unauthenticated (`401`) and unauthorised (`403`) callers, and for role isolation, belong to the final phase (TST-DEF-001). They MUST NOT be written earlier. The minimum of TST-SCN-001 is then extended by them. | I | PP |
| TST-SCN-003 | Every error code of the API's error catalogue MUST be produced by at least one backend scenario (L3). Every error code that the interface handles specifically MUST also be covered by a frontend integration scenario (L5) that checks the displayed message and the offered action. | T | PP |
| TST-SCN-004 | Every user-visible flow (L5) MUST have a happy-path scenario and scenarios for each state the interface defines: loading, empty, error, validation, partial data, and unavailable. | T | PP |
| TST-SCN-005 | Every flow MUST have unexpected-event scenarios (L5): the backend answers with a server error, with a timeout, with a malformed body, with a slow response, and with a conflict; the user submits twice or navigates away during a request. | T | PP |
| TST-SCN-006 | Every operation that reads stored data or accepts a structured body MUST have broken-data scenarios (L3): malformed body, wrong types, missing required properties, unknown properties, and inconsistent or incomplete stored records. | T | PP |
| TST-SCN-007 | Every primary user journey (L6) MUST have a happy-path scenario and one representative scenario of each other kind, chosen for what only the whole application shows: a bad path (a rejection from the real backend that the user sees), an edge case (the largest or smallest realistic data, and unusual characters through the whole chain), and an unexpected case (reload in the middle, back navigation, repeated submission, abandoning and returning). Every journey scenario MUST check that the browser console shows no error. | T | PP |
| TST-SCN-008 | A scenario has exactly one category tag. A scenario that mixes categories is split. | T | PP |
| TST-SCN-009 | The detailed error paths, edge cases and unexpected events are verified in L1, L2, L3 and L5. L6 contains only the representative scenarios of TST-SCN-007 and the cases that only the whole application can show (a misrouted request, a missing static asset, an unhandled failure between components). | I | PYR |
| TST-SCN-010 | Every level (L1 to L7) MUST contain, for each unit, component, operation, flow or journey that it verifies, tests of all four kinds of 3.4: good path, bad path, edge cases, and error or unexpected cases. A kind that does not apply MUST be justified in the test group or in the review. | T | PP |
| TST-SCN-011 | Every operation that accepts input or returns a collection MUST have edge-case scenarios (L3): empty and single-item results; minimum and maximum lengths and values; page-size limits and the first and last page; duplicate values; unusual valid text (non-Latin scripts, emoji, right-to-left text, leading and trailing spaces); large numbers and exact decimal values; dates and times at boundaries (month end, leap day, daylight-saving change, time-zone offsets). | T | PP |
| TST-SCN-012 | Every user-visible flow MUST have edge-case scenarios (L5): empty and single-item collections, the maximum documented collection size, very long and unusual text, missing optional values, the first and last page, and the narrow viewport. | T | PP |
| TST-SCN-013 | Every flow that submits input MUST have bad-path scenarios (L5): invalid input shows its validation messages next to the fields and keeps what the user entered; a business rejection (conflict, unknown resource, failed precondition) shows its message and offers the next action. | T | PP |
| TST-SCN-014 | Every operation that depends on an external service, the database, a message broker or optimistic concurrency MUST have unexpected-event scenarios (L3): the dependency fails, is slow or times out (stub faults, TST-INT-005); a constraint is violated; a concurrent change occurs; the same request is repeated. The response and the resulting state are as documented, and no partial state remains. | T | PP |

## 6. Unit tests (L1)

| ID | Statement | Ver. | Basis |
|---|---|---|---|
| TST-UNT-001 | A unit test MUST verify one condition of one unit through its public interface. Its name states the behaviour and the condition (`rejects_an_expired_voucher`). | I | PYR |
| TST-UNT-002 | A unit test has the structure arrange, act, assert (or given, when, then), with the three parts visibly separated. | I | PYR |
| TST-UNT-003 | A unit test MUST NOT use a database, the network, the file system, the real clock, unseeded randomness, environment variables, or the locale and time zone of the machine. Time comes from an injected clock, identifiers and random values from injected generators. | T | PYR |
| TST-UNT-004 | A unit test verifies observable behaviour (results, state, emitted events), not the order of internal calls, unless the call is the behaviour. A private method is not tested directly; where it seems to need a test, the unit is split. A refactoring that keeps behaviour MUST NOT break a test. | I | PYR |
| TST-UNT-005 | Collaborators are real when they are cheap and deterministic. Collaborators that are slow, non-deterministic or have side effects are replaced at the boundaries (ports). A type that the project does not own is wrapped in an adapter, and the adapter is replaced, not the third-party type. | I | PYR |
| TST-UNT-006 | Business rules MUST be covered completely: every non-trivial path, the boundary values (the limit, just inside, just outside), every valid and every invalid state transition, every validation message or code, rounding and money arithmetic, and empty and absent values. Role-dependent permission rules belong to the final phase (TST-DEF-001). | T | PP |
| TST-UNT-007 | The same logic with different inputs MUST be tested with a parameterised (data-driven) test, not by copying tests. | I | PP |
| TST-UNT-008 | A test verifies one logical outcome. It uses an assertion library with failure messages. The built-in `assert` statement of a language MUST NOT be used. | T | PP |
| TST-UNT-009 | Branch coverage of the business logic MUST reach 80 % (**default**) and of the domain core 90 % (**default**), measured by the coverage tool of the platform and enforced as a gate (P07). The excluded code (generated code, trivial accessors, start-up wiring) is listed and justified. Coverage is a floor that detects untested code. It is not evidence of quality and not a target. | T | PP |
| TST-UNT-010 | Generated code is not tested for itself. The code that adapts or uses it is tested. | I | PP |
| TST-UNT-011 | Frontend logic (stores, backend adapters, mappers, formatters, utilities) is unit-tested in the unit runner, without a browser, unless it needs a browser platform interface. | I | PP |
| TST-UNT-012 | A unit test runs in milliseconds. The unit suite stays within its time budget (**default** 2 minutes, P20). A slow unit test is moved to a higher level or made fast. | T | PYR |
| TST-UNT-013 | A defect fix MUST include a test at the lowest suitable level that fails without the fix. | T | PYR |
| TST-UNT-014 | Property-based tests MAY verify invariants (round trips, idempotence, ordering, parsing) (P27). A failing run prints the seed so that it can be reproduced. | I | PP |
| TST-UNT-015 | Mutation testing MAY be applied to the domain core to judge the strength of the tests (P26). Surviving mutants are reviewed. It is not a gate by default. | I | PP |
| TST-UNT-016 | Test doubles are named for what they are (stub, fake, spy, mock) and are created per test. A double that holds state MUST NOT be shared between tests. | I | PYR |
| TST-UNT-017 | Backend unit tests follow the convention of the platform and are consistently discoverable (section 20). Frontend unit tests of logic that is not a component sit next to the file that they test (TST-CMP-014). | I | PP |
| TST-UNT-018 | For each unit with business rules the tests MUST cover the four kinds of 3.4: the good path; the bad path (invalid input and forbidden transitions are rejected with the expected error); the edge cases (boundary values, empty, single, duplicate, unusual text, rounding, time boundaries); and the error or unexpected cases (a replaced collaborator fails, times out or returns nothing; an exception is raised and mapped). A pure unit without collaborators covers the last kind with impossible or unexpected input and overflow. Tests are grouped by kind (TST-PYR-018). | T | PP |

## 7. Component tests (L2) and the location of frontend unit tests

| ID | Statement | Ver. | Basis |
|---|---|---|---|
| TST-CMP-001 | Every shared interface component, and every view or component that owns behaviour, MUST have component tests. | T | PP |
| TST-CMP-002 | A component test verifies public behaviour: rendering from inputs; the states loading, content, empty, error, unavailable, disabled and validation; interaction and the outputs or events it emits; focus and keyboard behaviour; cleanup of listeners, timers and pending work; remounting. | T | PP |
| TST-CMP-003 | The boundary to the backend (the adapter of the domain) MUST be replaced by a fake. A component test MUST NOT call a real backend and MUST NOT use the stub server of section 10. | T | PP |
| TST-CMP-004 | Where a component updates optimistically, a test MUST show the optimistic result and the reversal after a failure. | T | PP |
| TST-CMP-005 | Where a submit control depends on the validity of the input, a test MUST show that it is disabled (or the submission refused with a message) until the required input is valid, and enabled afterwards. | T | PP |
| TST-CMP-006 | Behaviour that depends on a real engine (focus, layout, shadow tree, form association, native dialogs) MUST be tested in a real browser engine. A simulated DOM is acceptable for logic-only components. | T | PP |
| TST-CMP-007 | Elements are located by role, label or accessible name, or by test identifier (section 13), with the locale fixed. They MUST NOT be located by style class, position or generated names. | I | PP |
| TST-CMP-008 | Automated accessibility checks MUST run in component tests for every state of every shared component, against the standard that the interface contract names (**default** WCAG 2.2 AA, P16). Automated checks find a part of the problems only (section 14). | T | WCAG |
| TST-CMP-009 | A test asserts on the text that the catalogue provides for the fixed locale, or on the message key through a test localisation layer. Copy typed into a test is not the source of truth. | I | PP |
| TST-CMP-010 | Time is controlled with a fake clock. Animations and transitions are disabled. Waiting uses conditions, never sleeps. | T | PP |
| TST-CMP-011 | Component tests do not replace backend validation tests. A rule that the backend enforces is tested in the backend. | I | PP |
| TST-CMP-012 | Each test creates its own instance and cleans the document and the listeners afterwards. A test MUST NOT depend on global registries being clean or on another test having run. | T | PP |
| TST-CMP-013 | Snapshots of large trees are not primary assertions. A snapshot update is reviewed like code. | I | PP |
| TST-CMP-014 | Frontend unit tests (L1) and component tests (L2) MUST be located in the folder of the component, view or unit that they test, beside the file that they test. Every component, view and shared interface component has its own folder (as in Angular), which holds its markup, logic and styles in the way the framework arranges them, its tests, and the fakes and fixtures that only it uses. Tests MUST NOT be placed in a separate test tree or in the folder of another component. The rule holds for every frontend framework. | T | PP |
| TST-CMP-015 | A test file carries the name of the file that it tests plus the test suffix of the framework (`article-card.component.spec.ts`, `ArticleCard.test.tsx`), so that it is found beside its subject. | T | PP |
| TST-CMP-016 | Fakes, factories and fixtures that only one component uses live in its folder. Those that several components of one domain use live in a domain-local testing folder inside that domain. Those without domain vocabulary live in a testing folder of the shared layer. A global tree of test helpers MUST NOT exist. | I | PP |
| TST-CMP-017 | The build MUST exclude test files, fakes and fixtures that sit beside the code from the production output, and the pipeline MUST verify that no test file is in the output (TST-QLT-006). Coverage tools exclude test files from their figures. | T | PP |
| TST-CMP-018 | A test in the folder of a component tests that component through its public interface only. It MUST NOT import the internals of another component (TST-QLT-005). A test of a view together with its child components sits in the folder of the view. | I | PP |
| TST-CMP-019 | Moving or renaming a component moves or renames its folder together with its tests in the same change. | I | PP |
| TST-CMP-020 | For each component the tests MUST cover the four kinds of 3.4: the good path (valid data renders, the interaction completes); the bad path (invalid input shows its messages, forbidden actions stay disabled); the edge cases (empty, single and very many items, very long and unusual text, missing optional values, narrow width, right-to-left text where supported); and the error or unexpected cases (the adapter fails, is slow or rejects; repeated activation; unmounting during a request; cleanup). Tests are grouped by kind (TST-PYR-018). | T | PP |

## 8. Backend integration tests (L3)

| ID | Statement | Ver. | Basis |
|---|---|---|---|
| TST-INT-001 | Backend integration scenarios MUST run against the real backend application, started in the test process or as the built image, and call it over its real interface, so that routing, serialisation, validation and error mapping are exercised. | T | PP |
| TST-INT-002 | The database is the production engine in the production major version, started in a container. Its schema MUST be created by the project's real migrations, never by automatic schema generation or by a different script. Applying all migrations to an empty database at the start of the suite verifies them on every run. | T | PP |
| TST-INT-003 | Other owned infrastructure (message broker, cache, object store, search index) MUST also be real, in containers. A vendor emulator is allowed only when the project records it. | T | PP |
| TST-INT-004 | Containers start once per suite run and are shared by the scenarios, with random ports and readiness awaited by a health condition, never by a fixed sleep. They are removed at the end of the run. | T | PP |
| TST-INT-005 | A stub server (default WireMock) in the backend suite is optional. It is used only where the backend calls external HTTP services outside the product (third-party interfaces, identity-provider admin interfaces, object storage without an emulator, mail and payment gateways). Where it is used, it starts for the suite, the test profile points the application at it, stubs are installed per scenario and reset after each scenario. A backend that calls no external service has no stub server (TST-INT-025). | T | WM |
| TST-INT-006 | Stubs of external services MUST agree with the provider's published contract or with a recorded real response that carries its date and source (TST-CTR-005). | T | PP |
| TST-INT-007 | Every scenario creates the data it needs, with unique values, and MUST NOT read data created by another scenario. The database is left as the scenario found it, or the data cannot collide (P30). | T | PP |
| TST-INT-008 | Steps create data through the public operations and keep the real identifiers from the responses (TST-STP-009). Factories or direct inserts are used only for states the interface cannot reach (for example historical records) and are documented where used. | I | PP |
| TST-INT-009 | A scenario asserts the response (status, relevant headers, body properties) and, for a state change, the resulting state through a follow-up read operation. Direct database queries are allowed only for properties not observable through the interface, and are documented. | I | PP |
| TST-INT-010 | Each documented error mapping MUST have a scenario that shows the documented status and problem body. A generic handler MUST NOT shadow a specific one: a scenario exists for each specific mapping so that a reordering of handlers fails a test. | T | PP |
| TST-INT-011 | Where the API defines optimistic concurrency or idempotency, scenarios MUST cover the successful write with the current validator, the stale validator (`412`), the missing validator (`428`), and the repetition of an idempotent request. | T | HTTP |
| TST-INT-012 | Time-dependent behaviour is tested with a controllable clock that exists only in the test configuration. A route, header or switch that changes the clock MUST NOT exist in production configuration. | T | PP |
| TST-INT-013 | Collection operations MUST have scenarios for the first page, the last page, an empty result, the limit boundaries, sorting and filtering, and an invalid or expired cursor. | T | PP |
| TST-INT-014 | Database-specific behaviour (document or JSON columns, constraints, collations, indexes, transactions, isolation) MUST be tested against the real engine. A scenario MUST NOT be weakened because a substitute engine behaves differently. | T | PP |
| TST-INT-015 | A violation of a unique, foreign-key or check constraint MUST be mapped to the documented conflict or validation problem, and a scenario shows it. | T | PP |
| TST-INT-016 | Limits that the contract documents (body size, string length, collection size) MUST be tested at the limit and just beyond it. | T | OAS |
| TST-INT-017 | Where the backend publishes messages or events, scenarios verify that the right message is published, with the right content, once, using the real broker and a recording consumer. | T | PP |
| TST-INT-018 | File upload and download MUST be tested with small fixture files, including the size and type boundaries, against the real or recorded object store. | T | PP |
| TST-INT-019 | The test profile differs from production only in addresses of external services, the controllable clock, and the absence of the deferred concerns. Until the final phase the application has no authentication and the tests do not simulate it (TST-DEF-001). | I | PP |
| TST-INT-020 | The application context is started once per suite run and shared by scenarios. Scenarios MUST be able to run in parallel (isolated data) or are tagged `@serial`. | T | PP |
| TST-INT-021 | A start-up scenario verifies that the application starts with the test profile and that its readiness check reports ready only when its dependencies work. | T | PP |
| TST-INT-022 | Backend integration scenarios MUST NOT render or drive an interface. | I | PP |
| TST-INT-023 | A scenario that depends on a long process (a scheduled job, a retry with back-off, an expiry) MUST control the process (a shortened interval in the test profile, a triggered job) and MUST NOT wait for real time. | T | PP |
| TST-INT-024 | A backend integration scenario that fails MUST attach the request, the response and the application log of the scenario to the report (TST-STP-011). | T | PP |
| TST-INT-025 | A backend that calls no external service MUST NOT have a stub server, stub mappings or stub tooling in its integration suite. The first external call that the backend gains adds the stub server, its mappings and the contract validation of those stubs (TST-CTR-005) in the same change. | I | PP |

## 9. API contract tests (L4)

| ID | Statement | Ver. | Basis |
|---|---|---|---|
| TST-CTR-001 | A provider contract test MUST verify for every operation: it exists with its documented method and path; each documented status is produced by at least one test; real responses, including error responses, validate against the documented schemas; no route exists that the contract does not describe; and no undocumented status code is returned. | T | OAS |
| TST-CTR-002 | The HTTP client of the backend suite MUST validate every response against the contract (status, headers, body schema). A scenario passes only when its responses conform. The check is on by default. | T | OAS |
| TST-CTR-003 | The client MUST validate every request against the contract, except in scenarios of the categories `@wrong-path` and `@broken-data`, where the invalid request is the point. The exemption is explicit in the scenario's tags. | T | OAS |
| TST-CTR-004 | Every example in the contract MUST validate against its schema and be accepted by the provider. | T | OAS |
| TST-CTR-005 | Every stub of the stub server (section 10 and TST-INT-005) MUST be validated against the contract of the service that it simulates: its request pattern matches a documented operation, and its response validates against the documented response schema for that status. A stub that fails validation fails the pipeline. For services without a contract, stubs are derived from recorded real responses that carry their date and source, and are re-checked at every release (**default**, P18). | T | PP |
| TST-CTR-006 | A contract change, its implementation, its stubs, its examples and its tests MUST be in the same change set. A change of observable behaviour without a contract change MUST fail the contract test. | T | PP |
| TST-CTR-007 | The comparison of the contract with the last released contract (breaking changes) is a gate of the API contract. The test stages MUST run after it. | I | PP |
| TST-CTR-008 | Property-based tests derived from the contract (**default** Schemathesis) MAY run against a test environment to find server errors on edge inputs and responses that violate the schema (P18). They run in the nightly run, not on every change. A defect they find becomes a scenario. | T | PP |
| TST-CTR-009 | Consumer-driven contract tests (**default** Pact) MAY be added when several independent consumers exist. They complement the provider tests and do not replace them. | T | PYR |
| TST-CTR-010 | Collections for manual API testing (for example Postman, run with Newman) MAY exist per module (P32). They are exploration and regression aids. They are not a substitute for L3, use environment variables for addresses, and run in the pipeline only while they are kept in step with the contract. | I | PP |
| TST-CTR-011 | The contract test stage runs before the browser-driven suites. A failing contract test blocks the change. | T | PP |
| TST-CTR-012 | The contract tests MUST cover the four kinds of 3.4 for every operation: documented successes (good path); documented error responses (bad path); limits, formats and optional and nullable properties at their boundaries (edge cases); and undocumented outcomes (error or unexpected: no undocumented status or route, and server errors on edge input found by contract fuzzing). Tests are grouped by kind (TST-PYR-018). | T | OAS |

## 10. Frontend integration tests with a stub server (L5)

Frontend integration tests verify the interface, not the backend. The built frontend runs in a real browser, and a WireMock server (the default stub server) answers every call that the frontend makes. No real backend runs.

```text
 Browser (real engine, one isolated context per scenario)
        |
        v
 Application origin: the production build, served as in production
        |  API path prefix: the same routing rule as in production
        v
 Stub server (default WireMock, port 8089 or one per worker)
        ^
        |  administration interface: install stubs, verify requests, read unmatched requests, reset
 Test harness (Gherkin steps)
```

### 10.1 Purpose and structure

| ID | Statement | Ver. | Basis |
|---|---|---|---|
| TST-FIN-001 | Frontend integration tests MUST verify the behaviour of the interface against known backend answers: rendering, navigation, forms and validation, every defined state, optimistic updates and their reversal, retries, client caching and revalidation, error handling, localisation, accessibility, and that the frontend sends the correct requests. | T | PP |
| TST-FIN-002 | The frontend MUST run as built for production: the production bundle served as in production, or, where the framework needs a server process, the production build started in test mode. A development server MUST NOT be used for a gate run. | T | PP |
| TST-FIN-003 | The stub server is the only backend. The real backend MUST NOT be started, and no request leaves the test environment (TST-FIN-004). | T | PP |
| TST-FIN-004 | Backend traffic MUST reach the stub server through the same routing as in production: a rule that forwards the API path prefix to the stub server, so that the frontend's relative addresses are unchanged. Production sources MUST NOT contain a test-only switch for the API address. | T | PP |
| TST-FIN-005 | L5 verifies how the interface presents and reacts to backend answers. It MUST NOT verify rules that the backend enforces (these belong to L3). A stub MUST NOT re-implement backend logic beyond fixed answers and simple state sequences (TST-FIN-018). | I | PP |
| TST-FIN-006 | L5 is a separate suite (TST-PYR-014): its own configuration, step definitions, scenario context, hooks and run command. | I | PP |
| TST-FIN-007 | Scenario text does not mention the stub server (`Given the catalogue has two articles`, not `Given WireMock returns two articles`). Step definitions install the stubs. A step for an unexpected event names the event in domain terms (TST-STP-017). | I | GHK |
| TST-FIN-008 | L5 runs on every change, in parallel with the backend suite. It depends on neither the backend nor the end-to-end environment. | T | PP |
| TST-FIN-009 | Where the product delivers translations at runtime, the translation resource is stubbed like any other operation: the seed catalogue as a body file, a conditional request answered with `304`, a changed file after revalidation, and a fault that makes the client fall back to the seed text. | T | PP |

### 10.2 Network isolation

| ID | Statement | Ver. | Basis |
|---|---|---|---|
| TST-FIN-010 | A network guard MUST be active in every browser context. It blocks and records any request to an origin outside the allow-list (the application origin, the stub server, `data:` and `blob:` addresses, and the origins in P05). A scenario with a recorded blocked request MUST fail. | T | PP |
| TST-FIN-011 | After every scenario, and before the reset, the harness MUST read the unmatched requests from the stub server (default `GET /__admin/requests/unmatched`) and fail the scenario when any exist. The failure names each request and the closest stubs (near misses). | T | WM |
| TST-FIN-012 | The stub server MUST answer an unmatched request with an error status (WireMock: `404` with a diagnostic body) and MUST NOT return a default success. | T | WM |
| TST-FIN-013 | Where the frontend renders on a server (server-side rendering, server functions, static generation at build time), the server process calls the API itself, and the browser guard does not see those calls. The process MUST be started with its API address set by configuration to the stub server, and the unmatched-request check (TST-FIN-011) covers these calls equally. A build that fetches data at build time MUST run against the stub server with the same stubs. | T | PP |
| TST-FIN-014 | The application origin and the stub server SHOULD be the same origin for the browser (through the routing rule of TST-FIN-004), so that no cross-origin policy is needed. Where a cross-origin setup is unavoidable, the stub server's cross-origin option is enabled in the test environment only. | I | HTTP |
| TST-FIN-015 | The browser console MUST show no error and no unhandled rejection during a scenario, except those that the scenario declares as expected. An undeclared one fails the scenario. | T | PP |

### 10.3 Stub management

| ID | Statement | Ver. | Basis |
|---|---|---|---|
| TST-FIN-016 | Stub mappings MUST be files in version control, in the stub server's mapping format, grouped by domain and named by operation and outcome (`listArticles-200-empty.json`). Large payloads are body files. Files are reviewed like code and contain no secrets and no personal data. | I | WM |
| TST-FIN-017 | Each scenario installs exactly the stubs it needs through the administration interface (default `POST /__admin/mappings`), in `Given` steps or tag-selected hooks, from the mapping files. A small baseline (configuration, the translation catalogue, static lookups) MAY be installed for every scenario and is recorded in 26.5. | T | WM |
| TST-FIN-018 | Sequences that depend on earlier calls (create, then list shows the new item; the first call fails and the retry succeeds; a poll that completes after several calls) MUST use the stub server's scenario states, with a scenario name that is unique per test scenario. | T | WM |
| TST-FIN-019 | After every scenario the harness MUST reset the stubs, the request journal and the scenario states (default `POST /__admin/reset` and `POST /__admin/scenarios/reset`). The checks of TST-FIN-011 and the evidence collection run before the reset. Stub state MUST NOT be shared between scenarios. | T | WM |
| TST-FIN-020 | Stub responses MUST use contract examples or factories that are validated against the contract (TST-CTR-005). A shape that the contract does not allow MUST NOT be stubbed. A stub for an operation that the contract does not contain MUST NOT exist. | T | OAS |
| TST-FIN-021 | Stubs match the method and the path explicitly, and match query parameters, headers and body only where they belong to the behaviour under test. Over-specified stubs are brittle, and under-specified stubs hide defects. Overlapping stubs have explicit priorities: specific stubs rank above generic ones. | I | WM |
| TST-FIN-022 | Responses carry the headers that the contract documents (`Content-Type`, `ETag`, `Cache-Control`, `Location`, `Retry-After`, the correlation header). Error responses use the problem format and the error codes of the catalogue, so that the interface's handling of each code is exercised (TST-SCN-003). | T | HTTP |
| TST-FIN-023 | Dynamic values in responses (echoed request fields, generated identifiers, dates relative to the request) MAY use the stub server's response templating, enabled as the tool documents. Templates stay simple; logic does not move into stubs. | I | WM |
| TST-FIN-024 | Collections are stubbed with their pages: query matching selects the page, and the cursor values are opaque strings that the stub supplies. Sorting and filtering that the backend performs are stubbed as fixed answers per query. | I | PP |
| TST-FIN-025 | Uploads are accepted by a stub and verified through the journal. Downloads use body files with the right content type and content disposition. | I | WM |
| TST-FIN-026 | Streaming channels (server-sent events, WebSocket) use the stub server's messaging support where it exists. Otherwise a small project-owned test server serves that channel, recorded as a deviation (TST-PRE-005) with its owner. | I | WM |

### 10.4 Unexpected events and verification

| ID | Statement | Ver. | Basis |
|---|---|---|---|
| TST-FIN-027 | Unexpected events MUST be produced with the stub server's fault and delay facilities: a response delay longer than the client timeout; an empty response; a malformed response chunk; a reset connection; random data then close; error statuses with problem bodies; a malformed or truncated JSON body; a wrong content type; a slow answer after a fast one. | T | WM |
| TST-FIN-028 | Where a delay would make the test slow, the browser clock is advanced instead of waiting (the clock facility of the browser tool). | I | PP |
| TST-FIN-029 | Scenarios MUST verify what the frontend sent, through the request journal (default `POST /__admin/requests/count` with a request pattern, or the find operation): method, path, query, the relevant headers (`If-Match`, `If-None-Match`, `Idempotency-Key`, `Accept-Language`), and body. Verification covers the count (exactly one request after a double submission, none after a blocked action) and, where it matters, the order. | T | WM |
| TST-FIN-030 | Conditional requests MUST be tested: a stub that answers a request with `If-None-Match` with `304`, so that the client's revalidation and the reuse of its stored entry are verified. | T | HTTP |
| TST-FIN-031 | The unexpected-event scenarios of TST-SCN-005 MUST be implemented with the facilities of TST-FIN-027. A scenario MUST also show that the interface recovers: after the fault is removed, a retry succeeds and the interface returns to its normal state. | T | PP |
| TST-FIN-032 | Accessibility checks (section 14) run at the defined states of each page. Visual evidence (section 14) is attached for `@visual` scenarios. | T | WCAG |

### 10.5 Execution

| ID | Statement | Ver. | Basis |
|---|---|---|---|
| TST-FIN-033 | The harness starts the stub server (standalone JAR, container, package, or embedded) before the run, waits until the administration interface answers, and stops it after the run. The port comes from configuration (**default** 8089, P05). | T | WM |
| TST-FIN-034 | With parallel workers, each worker MUST have its own stub server instance (port = base port plus worker index) and its own routing to it, so that the stubs of parallel scenarios never mix. A different isolation (a per-scenario namespace) MUST be recorded (P24). | T | PP |
| TST-FIN-035 | The browser context of a scenario has an explicit locale, time zone, viewport and clock (TST-CON-003). L5 MUST run at the viewports of P11 (**default** one desktop and one mobile viewport), and in the engines of P11 for the smoke scope (**default** Chromium). | T | PP |
| TST-FIN-036 | On failure the harness attaches to the report: the installed mappings, the request journal, the unmatched requests with near misses, the console log, a screenshot and a trace. It does so before the reset (TST-STP-011). | T | PP |
| TST-FIN-037 | A stub server other than WireMock conforms when it satisfies TST-SEL-007. The replacement is recorded (P05). | I | PP |
| TST-FIN-038 | Stubs are the executable form of the API contract as the frontend understands it. A change of the contract MUST change the affected stubs in the same change set (TST-CTR-006). | T | PP |

### 10.6 Example (non-normative)

A stub mapping:

```json
{
  "name": "listArticles-200",
  "priority": 5,
  "request": {
    "method": "GET",
    "urlPath": "/api/v1/articles",
    "queryParameters": { "limit": { "equalTo": "20" } }
  },
  "response": {
    "status": 200,
    "headers": { "Content-Type": "application/json", "ETag": "\"v1\"" },
    "jsonBody": { "items": [ { "id": "a-1", "title": "First article" } ], "page": { "limit": 20 } }
  }
}
```

A fault and a delay:

```json
{
  "mappings": [
    { "name": "listArticles-reset", "request": { "method": "GET", "urlPath": "/api/v1/articles" },
      "response": { "fault": "CONNECTION_RESET_BY_PEER" } },
    { "name": "listArticles-slow", "request": { "method": "GET", "urlPath": "/api/v1/articles" },
      "response": { "status": 200, "fixedDelayMilliseconds": 8000, "jsonBody": { "items": [], "page": { "limit": 20 } } } }
  ]
}
```

A state sequence (the first call fails, the retry succeeds):

```json
{
  "mappings": [
    { "name": "retry-1", "scenarioName": "articles-retry-s1", "requiredScenarioState": "Started", "newScenarioState": "second-call",
      "request": { "method": "GET", "urlPath": "/api/v1/articles" },
      "response": { "status": 503, "headers": { "Content-Type": "application/problem+json", "Retry-After": "1" },
                    "jsonBody": { "type": "about:blank", "title": "Service Unavailable", "status": 503, "code": "SRV_001" } } },
    { "name": "retry-2", "scenarioName": "articles-retry-s1", "requiredScenarioState": "second-call",
      "request": { "method": "GET", "urlPath": "/api/v1/articles" },
      "response": { "status": 200, "jsonBody": { "items": [ { "id": "a-1", "title": "First article" } ], "page": { "limit": 20 } } } }
  ]
}
```

A request count (the body of the verification call):

```json
{ "method": "POST", "urlPath": "/api/v1/items", "headers": { "Idempotency-Key": { "matches": ".+" } } }
```

The answer is `{ "count": 1 }` after one submission.

A scenario:

```gherkin
@phase-1 @articles @p1
Feature: Article list

  @req-ART-003 @happy-path @smoke
  Scenario: The list shows the available articles
    Given the catalogue has 2 articles
    When the visitor opens the article list
    Then both articles are shown

  @req-ART-004 @unexpected-event @regression
  Scenario: The list recovers after a temporary failure
    Given the catalogue service fails once and then answers
    And the visitor has opened the article list and sees the unavailable message
    When the visitor retries
    Then both articles are shown
    And the browser console shows no unexpected error
```

## 11. End-to-end tests (L6)

End-to-end tests verify that the whole application works as deployed. They run through a real browser against every component of the product, with no stub, no mock and no WireMock. They are an independent npm package in `<project>/tests/e2e`, outside the frontend and backend folders, that tests any target environment (local, development, test or another non-production environment) through its public entry point, however that environment is deployed.

```text
 Browser (real engine)
        |
        v
 Public entry point (reverse proxy or gateway)
        |
        +--> Frontend (as deployed)
        +--> Backend (as deployed) --> Database (production engine)
                                   --> other owned services
 Test harness (Gherkin steps): drives the browser and, where needed, the public HTTP interface
 Target: the environment profile selects the address (local, dev, test, other)
```

### 11.1 Purpose and rules

| ID | Statement | Ver. | Basis |
|---|---|---|---|
| TST-E2E-001 | End-to-end tests MUST verify that the whole application works as deployed: routing, static assets, frontend, backend, database and owned services together, through primary user journeys driven in a real browser. | T | PYR |
| TST-E2E-002 | End-to-end tests MUST NOT use WireMock or any other stub server, test double, mocked route or intercepted-and-rewritten response for any component of the application. The browser tool MAY observe traffic and MAY apply the network guard of TST-FIN-010; it MUST NOT change a request or a response. | T | PP |
| TST-E2E-003 | An external service outside the product boundary is used through the provider's sandbox when one exists. Where none exists, the integration is covered in L3 with stubs and the journeys avoid it. A project-owned, production-like double deployed inside the environment is allowed only as a recorded deviation (TST-PRE-005) with an owner and a reason, listed in 26.5 and visible in the report. | I | PP |
| TST-E2E-004 | The environment under test MUST run the images or build artefacts that will ship, tagged by commit. What is tested is what ships: the artefacts are built once and used for the tests and for the deployment. In managed mode the package starts them from the project's deployment definition (11.2). | T | PP |
| TST-E2E-005 | In managed mode each run uses a fresh, isolated environment with an empty database and the minimal seed data of 12.2 (TST-DAT-013). One command starts the environment, waits until every component reports ready, and destroys it after the run, even when the run fails. A keep-alive switch (P12) leaves it running for debugging. In external mode the environment is shared and is not reset by the suite (TST-E2E-037). | T | PP |
| TST-E2E-006 | L6 is a separate suite (TST-PYR-014) and an independent package (TST-E2E-029), with its own dependencies, configuration, step definitions, scenario context, hooks and run command. It MUST NOT reuse stub-specific steps of L5. | I | PP |
| TST-E2E-007 | L6 covers only primary user journeys: those on which the value of the product depends, listed in the requirements and in P12. Each journey has the four kinds of case of TST-SCN-007, each as one representative scenario. L6 MUST NOT test every operation, every state or every validation rule. The count stays small (TST-PYR-008). | I | PYR |
| TST-E2E-008 | A scenario creates the data it needs through what a user or consumer can do (the interface, or the public HTTP interface), and keeps the real identifiers. It MUST NOT write to the database. Only the start-up seed of 12.2 is loaded by the environment. Unique data per scenario prevents collisions in a shared environment, and the scenario removes what it created through the same public interface where the interface allows it (TST-E2E-037). | T | PP |
| TST-E2E-009 | `@smoke` scenarios of L6 run on every change after the images are built. The full set runs when a change is merged and nightly (5.3). | T | PP |
| TST-E2E-010 | A journey that is also covered at L3 and L5 (dual coverage, section 17) verifies what only the whole application shows: the interplay of the components. It MUST NOT repeat the checks of the lower levels. | I | PYR |
| TST-E2E-011 | Every journey MUST assert that the browser console shows no error and that no request failed unexpectedly (an unexpected `4xx` or `5xx` observed on the network). | T | PP |
| TST-E2E-012 | L6 runs in Chromium and in the engines and viewports of P11 (**default** Chromium at one desktop and one mobile viewport). It runs headless in the pipeline and MAY run headed locally. | T | PP |
| TST-E2E-013 | On failure the harness collects: a screenshot, a trace or video, the console log and the network log; in managed mode also, before the environment is destroyed, the logs of every component. In external mode it links the logs that the project makes available for the environment. | T | PP |
| TST-E2E-014 | Waiting uses conditions. Before the first scenario the harness waits for the readiness of every component and performs a warm-up request. | T | PP |
| TST-E2E-015 | A scenario is retried at most once (**default**, P13). A scenario that passes only on a retry is reported as flaky and enters the process of section 19. | T | PP |
| TST-E2E-016 | Step, scenario and run timeouts are explicit. A run that exceeds its timeout fails and collects the evidence of TST-E2E-013. | T | PP |
| TST-E2E-017 | Scenarios MAY run in parallel in one shared environment when their data is unique (TST-E2E-008). Scenarios that need exclusive use of a resource are tagged `@serial`. | T | PP |
| TST-E2E-018 | L6 uses the real clock. A behaviour that depends on a long time span is not tested in L6, or uses a shortened duration in the environment's documented configuration. A route, header or switch that changes the clock MUST NOT exist. | T | PP |
| TST-E2E-019 | Where the product supports several locales, L6 runs its journeys in the source locale and one journey per additional active locale, including the language switch. | T | PP |
| TST-E2E-020 | Automated accessibility checks (section 14) run at the key states of each journey. | T | WCAG |
| TST-E2E-021 | A smoke scenario MUST verify what only the whole can show: the application is reachable through the public entry point; the frontend loads with all of its assets; the API prefix is routed to the backend; the readiness check passes; and a permanent redirect or a missing page behaves as documented. | T | PP |
| TST-E2E-022 | Authentication and test accounts are not part of L6 before the final phase (TST-DEF-001). | I | PP |
| TST-E2E-023 | L6 MUST run against every non-production environment that has a profile (TST-E2E-033), with the same suite or the subset that the profile allows. It MUST NOT run against production (TST-CON-012, TST-E2E-035). | T | PYR |
| TST-E2E-024 | Steps drive the interface through screen drivers (page objects) that locate elements by test identifier (section 13). Selectors live in the drivers, not in the steps. | I | PP |
| TST-E2E-025 | A failure is classified in the report notes as a product defect, a test defect or an environment defect. Environment faults (a component that did not start, a port clash, a resource shortage) are reported separately from failed scenarios. | I | PP |
| TST-E2E-026 | The resources of the environment (memory, processors, disk) are documented and fit the pipeline runner. A suite that does not fit is reduced, not run partially. | I | PP |
| TST-E2E-027 | A product without a user interface MUST have its L6 through the public HTTP interface of the deployed whole, with the same environment rules. | I | PP |
| TST-E2E-028 | Where it adds confidence, a journey MAY verify consistency across components (data created through the interface is visible through the API, and the reverse). | I | PP |

### 11.2 The package and its target environments

| ID | Statement | Ver. | Basis |
|---|---|---|---|
| TST-E2E-029 | L6 MUST be an independent npm package in `<project>/tests/e2e` (**default** location, P23): its own `package.json`, lock file, installed dependencies, language and runner configuration, and scripts. It lies outside the frontend and backend directories, is not built, installed or imported by them, and imports no code from them. It is installed and run from its own directory (`npm ci`, then its scripts), with a pinned Node.js version. | T | PP |
| TST-E2E-030 | In external mode the package needs only HTTP or HTTPS access to the public entry point of the target. It MUST NOT need access to the container runtime, the database, an internal network, the product's source code or the deployment tooling. | T | PP |
| TST-E2E-031 | The package has two modes that run the same feature files and step definitions. In managed mode its scripts start the whole application from the deployment definition (for example a compose definition), wait for readiness, run, collect evidence and destroy the environment. In external mode it runs against an environment that already runs (a local stack, development, test, staging, preview or another non-production environment), however it is deployed, and starts and stops nothing. The commands of both modes are recorded (26.2). | T | PP |
| TST-E2E-032 | Whether an environment is deployed in containers MUST make no difference to the suite. Container-specific details (service names, internal ports, compose networks, log commands) appear only in the managed-mode scripts, in one place, and never in feature files, steps, screen drivers or profiles. | I | PP |
| TST-E2E-033 | The target is selected by an environment profile: one file per environment name in the package (`config/<name>`, for example `local`, `dev`, `test`, as listed in P31), chosen by one command-line option or variable (**default** `local`). Every value of a profile MAY be overridden by a variable. A profile holds the address of each public entry point, the API path prefix, timeouts, browser and viewport settings, the data prefix, and the run conditions of TST-E2E-039. A profile contains no secrets. | T | PP |
| TST-E2E-034 | Scenarios, steps and screen drivers MUST use only paths relative to the entry point and the API prefix of the profile, so that an environment with another host, port or base path works without a change. Where an environment has several public entry points (a separate host for the frontend and for the API), the profile names each, and steps use the name. | T | PP |
| TST-E2E-035 | The profiles are an allow-list. Before any scenario runs, the package MUST verify that the host of the target is on the allow-list and not on the deny-list (production hosts), and MUST fail the run before any request that changes data otherwise. An override variable cannot target a host that is not on the allow-list. | T | PP |
| TST-E2E-036 | Before the first scenario the package MUST run a preflight: the entry point is reachable, the readiness check passes, the minimal start-up data is present (TST-E2E-038) and, where the target exposes it, the deployed version is read. The result is written to the report. A failed preflight is an environment error (TST-E2E-025), not a failed scenario. A profile MAY require a version match, and the preflight then fails when it does not match. | T | PP |
| TST-E2E-037 | In external mode the environment is shared and is not empty. Scenarios use unique data (the data prefix of the profile, the run identifier and the scenario), touch only data that they created, remove it through the public interface where the interface allows it (in an `After` hook that does not mask the failure), and depend neither on data created by other users or runs nor on the absence of data. Data that cannot be removed carries the prefix and is accepted as residue, and an owner of the environment decides on its periodic cleanup. | T | PP |
| TST-E2E-038 | Journeys need only the minimal start-up data of 12.2. The environment provides it, and the preflight verifies it through a read operation. | T | PP |
| TST-E2E-039 | A profile states who may run the suite against the environment and when (after a deployment, on request, on a schedule), which scope (**default** `@smoke`, or all), the maximum number of parallel workers, and the load the suite may produce, so that a shared environment is not disturbed. | I | PP |
| TST-E2E-040 | Certificate verification is on by default. A profile for a non-production environment MAY trust a named private certificate authority or switch verification off. The exception is explicit in that profile and listed in 26.5. | I | HTTP |
| TST-E2E-041 | After each deployment to a development or test environment (P31), the pipeline MAY run the `@smoke` scope of the package against it (post-deployment smoke), as a stage that depends on the deployment. It uses the same package and the same commands (TST-PIP-017). | T | PP |
| TST-E2E-042 | The report records the mode, the profile name, the entry points, the deployed version where exposed, and the versions of the tools. | I | PP |
| TST-E2E-043 | The package MUST NOT change the clock of the target and MUST NOT compare the clock of the runner with the clock of the target. Scenarios check relative time only. | T | PP |
| TST-E2E-044 | The package has its own README (TST-QLT-008) with the Node.js version, the installation of the browser, both modes, the profile format, the run conditions and the diagnosis of an environment error. | I | PP |

## 12. Test data, fixtures and seeders

### 12.1 Rules

| ID | Statement | Ver. | Basis |
|---|---|---|---|
| TST-DAT-001 | Test objects MUST be built by factories (or builders), one set per domain, that create a valid object with sensible defaults and explicit overrides. A test states only the values that matter to it (TST-GHK-012). | I | PP |
| TST-DAT-002 | Factories are domain-local. Shared factories contain no domain vocabulary. A factory MUST NOT hide a rule that the test is about. | I | PP |
| TST-DAT-003 | Every test and scenario creates its own data. It does not rely on data left by another test, a previous run, or a shared dataset that it modifies. | T | PP |
| TST-DAT-004 | Test data is synthetic. It MUST NOT be copied from production, and MUST NOT contain real personal data, real credentials, real addresses or real payment data. Realistic formats are generated or invented. | T | PP |
| TST-DAT-005 | Random values come from a seeded generator whose seed is printed in the report, so that a failure can be reproduced. A test MUST NOT depend on an unseeded random value. | T | PP |
| TST-DAT-006 | Unique values (names, addresses, references) carry a unique suffix per scenario, so that parallel scenarios do not collide and a failed run leaves no data that breaks the next. | T | PP |
| TST-DAT-007 | Time-dependent data uses fixed dates and the controlled clock of the level (TST-UNT-003, TST-FIN-028, TST-INT-012). | T | PP |
| TST-DAT-008 | Fixture files (payloads, documents, images, stub bodies) are small, named for their purpose, kept beside the suite, validated against the contract or schema where one exists, and reviewed like code. | I | PP |
| TST-DAT-009 | Data that exercises boundaries and unusual input (maximum and minimum lengths, empty values, unusual characters, right-to-left text, long words, emoji, very large numbers, dates at boundaries) is part of the standard fixture set. | T | PP |
| TST-DAT-010 | Cleanup is stated per level (P30): L1 and L2 create objects in memory; L3 leaves the database as it found it or uses non-colliding data; L5 resets the stub server; L6 in managed mode uses a fresh environment per run, and in external mode unique data per scenario that the scenario removes itself (TST-E2E-037). A test MUST NOT depend on cleanup done by another test. | T | PP |
| TST-DAT-011 | Data volumes for performance tests are generated by a documented generator and are never committed as large files. | I | PP |

### 12.2 Seed data

| ID | Statement | Ver. | Basis |
|---|---|---|---|
| TST-DAT-012 | Seed data is separated by purpose: the minimal start-up data that the application needs to run in a test environment (reference data, lookups), the realistic demo or development data, and test-specific data created by scenarios. The three MUST NOT be mixed. | I | PP |
| TST-DAT-013 | Minimal start-up data is loaded by the environment (a seed migration or a seeder), is documented, is small, and is the same in every run. | T | PP |
| TST-DAT-014 | A seeder that builds a realistic demo or development dataset through the public interface MAY be written as a feature file tagged `@seeder`, with a documented command. Seeder features are excluded from test runs and from the shape check, and are verified in the pipeline by a run against a fresh environment (a check that the seeder still works). | T | PP |
| TST-DAT-015 | The seed data of each delivery phase is extended in the same change as the phase's features and is verified by a check that the seeded system starts and shows the data. | T | PP |
| TST-DAT-016 | The demo dataset covers every state a user can see (empty, typical, long, error-prone) so that reviewers and the product owner can inspect all interface states. | I | PP |

## 13. Test identifiers and element selection

| ID | Statement | Ver. | Basis |
|---|---|---|---|
| TST-IDN-001 | Interactive elements, state containers and key regions MUST expose a stable test identifier (**default** attribute `data-testid`, P10). The identifier names the purpose of the element, not its position, style or implementation. | I | PP |
| TST-IDN-002 | The default form is `{feature}-{component}-{element}` in kebab-case (`article-card-open`, `checkout-form-submit`). A repeated entity adds its stable entity identifier (`article-card-open-a-1`). An array index MUST NOT be used. | I | PP |
| TST-IDN-003 | A test identifier MUST NOT contain personal, sensitive, secret or volatile data. Its value is stable across visual refactoring and changes only when the public behaviour changes. | T | PP |
| TST-IDN-004 | Test identifiers are testing hooks only. They MUST NOT be used for production logic, styling, analytics or communication between components. | I | PP |
| TST-IDN-005 | Browser-driven tests (L5, L6) MUST locate elements by test identifier. They MAY additionally use role and accessible name for accessibility assertions, with the locale fixed. They MUST NOT locate elements by style class, position in the document, structural path or visible text. | T | PP |
| TST-IDN-006 | Component tests (L2) MAY locate elements by role, label, accessible name or test identifier, with the locale fixed (TST-CMP-007). | I | PP |
| TST-IDN-007 | A test that needs an element without an identifier adds the identifier in the same change. A test MUST NOT fall back to a fragile locator. | I | PP |
| TST-IDN-008 | A scenario fails with a clear message when an identifier is missing, duplicated where it should be unique, or hidden (TST-GHK-018). | T | PP |
| TST-IDN-009 | The identifiers of one component are listed in its documentation or are discoverable in its test, so that the screen drivers and the component tests agree. | I | PP |
| TST-IDN-010 | Where the build removes test identifiers from production output, the build step is documented and tested. The default is to keep them, because they hold no sensitive data. | I | PP |

## 14. Visual and accessibility tests

Automated accessibility checks and visual checks run inside L2, L5 and L6. They are not separate levels.

| ID | Statement | Ver. | Basis |
|---|---|---|---|
| TST-VIS-001 | Rule-based accessibility checks (**default** axe-core) MUST run in L2 for every state of every shared component, in L5 for every page and every defined state, and in L6 at the key states of each journey. The standard is the one the interface contract names (**default** WCAG 2.2 AA, P16). A violation fails the run. | T | WCAG |
| TST-VIS-002 | A known violation MAY be excepted only with an owner, a reason and an expiry date recorded in the report configuration. An expired exception fails the run. | T | PP |
| TST-VIS-003 | Automated checks find only a part of the accessibility problems. The review of a new interaction pattern MUST add the manual checks that automation cannot do (keyboard operation, screen reader announcement, zoom and reflow) and record the result in the change. | I | WCAG |
| TST-VIS-004 | L5 MUST contain scenarios that complete the primary flows with the keyboard only: the tab order follows the reading order, the focus is always visible, no focus trap exists, and every action is reachable. | T | WCAG |
| TST-VIS-005 | Visual evidence: scenarios tagged `@visual` attach a screenshot to the report at their last `Then` step (**default** in L5). The screenshot is evidence for the reviewer and is not an assertion. | T | PP |
| TST-VIS-006 | Visual regression (comparison against a stored baseline) is optional (P15). Where it is used, rendering MUST be deterministic: fixed viewport and device scale, fixed locale and time zone, bundled fonts, disabled animations and transitions, hidden caret, masked dynamic regions, a controlled clock, and the same container image and browser build as the baselines. | T | PP |
| TST-VIS-007 | Baselines are kept in version control or in an artefact store that is versioned with the code. They are created in the pipeline's container image, not on a developer's machine. | I | PP |
| TST-VIS-008 | The difference threshold is explicit and recorded (P15). A difference above it fails the run. A baseline update is a reviewed change that shows the old and the new image, and the pipeline MUST NOT accept a baseline automatically. | T | PP |
| TST-VIS-009 | Baselines are kept per engine and viewport that are in scope for the check. Differences between engines are never merged into one baseline. | I | PP |
| TST-VIS-010 | Where the product is localised, one L5 scenario per page type runs with the pseudo-locale and with long text and checks that no content is clipped or overlapping and that the page does not scroll horizontally at the viewports of P11. | T | PP |
| TST-VIS-011 | Where the interface contract requires it, L5 MUST emulate reduced motion, forced colours, increased text size, and zoom to 400 % (320 CSS pixels wide) and verify that the content and functions remain available. | T | WCAG |
| TST-VIS-012 | Where themes exist (light, dark, high contrast), the visual and accessibility checks run for each theme. | T | PP |
| TST-VIS-013 | Screenshots, traces and videos contain only synthetic data (TST-DAT-004, TST-CON-015). | T | PP |

## 15. Non-functional tests (L7)

| ID | Statement | Ver. | Basis |
|---|---|---|---|
| TST-NFT-001 | Performance and load tests MUST derive from the non-functional requirements (response-time percentiles, throughput, concurrency). The thresholds are pass or fail criteria. Without a stated requirement a load test is an exploration and is not a gate. | T | PP |
| TST-NFT-002 | The load-test tool is chosen in P17 (**default** k6). Scripts are in version control, parameterised by environment, and use the workload model of TST-NFT-003. | I | PP |
| TST-NFT-003 | The workload model is realistic: it follows the primary journeys, with a ramp-up, a steady state, think times, and data from a documented generator (TST-DAT-011). Runs are reproducible: the same data volume and the same environment size. | I | PP |
| TST-NFT-004 | Load tests run against a production-like environment, never against production. They run outside the change path (nightly and before a release). A very short load smoke MAY run when a change is merged. | T | PYR |
| TST-NFT-005 | Load tests MUST NOT send traffic to third-party services. These are replaced by the stub server in the load environment. | T | PP |
| TST-NFT-006 | Frontend performance budgets (bundle size, loading metrics) that the frontend or interface contract defines are verified in the build or in L5 and L6. A violation fails the stage. | T | PP |
| TST-NFT-007 | Resilience is tested at the boundaries through faults: at L5 with TST-FIN-027, at L3 with faults of the stub server for external services (TST-INT-005). | T | WM |
| TST-NFT-008 | The result of each load run is compared with the previous run. A regression beyond the tolerance (**default** 10 %, P17) is reported and, where the requirement is a threshold, fails the run. | T | PP |
| TST-NFT-009 | Endurance (soak) tests are optional (P17). Where used, they run for a stated duration and check for resource growth. | I | PP |
| TST-NFT-010 | Compatibility across browsers and devices is verified by the engine and viewport matrix of L5 and L6 (P11). A hosted device cloud is an optional extension. | T | PP |
| TST-NFT-011 | Load tests MUST cover the four kinds of 3.4: the expected workload meets the thresholds; invalid and rejected requests at volume do not degrade valid traffic; peak, spike, largest payloads and cold start are handled; a failing or slow dependency under load is survived and the recovery afterwards is shown. | T | PP |

## 16. Reporting and living documentation

| ID | Statement | Ver. | Basis |
|---|---|---|---|
| TST-RPT-001 | Every suite MUST write machine-readable results (Cucumber JSON or message output; the standard result format of the unit runner) and a human-readable HTML report, and MUST publish them as pipeline artefacts, also when the run fails. | T | PP |
| TST-RPT-002 | One combined report across L3, L5 and L6 (and the summaries of L1 and L2) MUST be built from the machine-readable results with the report tool of P14 (**default** Allure) and published on an access-controlled static site. The product owner and the reviewers have read access. | D | PP |
| TST-RPT-003 | The report shows the feature and scenario text with tags, the steps with status and duration, the attachments (screenshots, traces, requests and responses, stub journals), the failure messages, the history and trend, the counts per level, the flaky list and the quarantine list. | I | PP |
| TST-RPT-004 | The feature files MUST be browsable by non-developers in the report, grouped by domain and phase. The text shown is the scenario text, not code (living documentation). | D | GHK |
| TST-RPT-005 | The shape report (counts, duration, pass rate per level; TST-PYR-007) is part of every run. | T | PP |
| TST-RPT-006 | Code coverage reports are published per level where coverage is measured. The thresholds of TST-UNT-009 are gates. | T | PP |
| TST-RPT-007 | The requirement-coverage report lists requirements against scenario tags, operations against scenarios, and error codes against scenarios (section 17). | T | PP |
| TST-RPT-008 | The flaky report lists scenarios that passed only after a retry or that alternate between pass and fail, with their owners (section 19). | T | PP |
| TST-RPT-009 | Reports and attachments contain no secrets and no personal data (TST-CON-015). | T | PP |
| TST-RPT-010 | A failing scenario shows first the expectation, the actual value and, for HTTP, the request and response (TST-GHK-018). | T | PP |
| TST-RPT-011 | Each report links to the commit, the run, the environment and the versions of the tools used. | I | PP |
| TST-RPT-012 | Reports are retained for a stated period (**default** 30 days). The latest report of the main branch stays published (P14). | I | PP |
| TST-RPT-013 | A run writes the list of failed scenarios in the rerun format of the runner, so that they can be rerun quickly. | T | PP |
| TST-RPT-014 | Reports of a private product are not public. Publication is access-controlled, and the access list is recorded (P14). | I | PP |
| TST-RPT-015 | The report shows, for each Gherkin suite, the number of scenarios per category and, per operation, flow and journey, which of the four kinds are covered (TST-QGT-017). | T | PP |

## 17. Coverage per change and traceability

### 17.1 The minimum tests for a change

| Change | L1 unit | L2 component | L3 backend integration | L4 contract | L5 frontend integration | L6 end-to-end |
|---|---|---|---|---|---|---|
| New operation | Its rules | | Happy path and the error scenarios of TST-SCN-001 | Contract test | If the interface uses it: happy path, states, unexpected events | Only if on a primary journey |
| New view or component | Its logic | All states | | | Its flow with all states | Only if on a primary journey |
| New business rule | Every path and boundary | | One scenario per observable outcome | | The visible consequence, if any | |
| New error code | Where it is produced | | A scenario that produces it | The documented response | If the interface handles it specifically | |
| Changed contract | | | Updated scenarios | Updated contract test | Updated stubs | |
| Defect fix | A test that fails without the fix, at the lowest suitable level | | | | | |
| New primary journey | | | Covered below | | Covered below | Happy path and unexpected behaviour |
| Refactoring without behaviour change | No test change expected | | | | | |

| ID | Statement | Ver. | Basis |
|---|---|---|---|
| TST-COV-001 | The table of 17.1 is the minimum set of tests for a change. Every set of tests that it names contains the four kinds of 3.4. The review checks it. | I | PP |
| TST-COV-002 | Dual coverage rule: every new operation that a user can reach MUST be verified by backend scenarios (L3: contract, business rules, fast) and by frontend integration scenarios (L5: how the interface shows and uses it), and, if it is on a primary journey, by a journey in L6. The happy path is verified at each of these levels for its own purpose. Error paths are verified mainly in L3 and L5. | T | PYR |
| TST-COV-003 | A refactoring without a change of behaviour MUST NOT need a change of tests. A test that breaks in such a refactoring is coupled to the implementation and is repaired, not adapted to the new structure. | I | PYR |
| TST-COV-004 | A traceability matrix MUST be generated from the tags and the results: requirements to scenarios and results, operations to scenarios, error codes to scenarios. An item without a passing scenario is shown as uncovered. | T | PP |
| TST-COV-005 | The backend HTTP client records the operation identifier and the error code of every call. The report lists the operations of the contract that no scenario called and the error codes of the catalogue that no scenario produced. | T | PP |
| TST-COV-006 | The pipeline fails when a requirement, operation or error code that the change adds is uncovered. Uncovered legacy items are reported and tracked. The strictness is a parameter (P19). | T | PP |
| TST-COV-007 | Each delivery phase is done only when its requirements have scenarios that pass and carry the phase tag. | I | PP |
| TST-COV-008 | Removing or changing a requirement removes or changes its scenarios in the same change. A retired scenario is deleted, not left skipped. | I | PP |
| TST-COV-009 | Before a test is accepted, it MUST have been seen to fail for the right reason: written before the implementation, or run once with the change temporarily reverted. The change description records it. A test that cannot fail does not verify anything. | I | PP |
| TST-COV-010 | Coverage figures are information. A threshold exists only where this document or a parameter states one (TST-UNT-009). | I | PP |
| TST-COV-011 | Every defined state of every interface view MUST be covered at L2 or L5 (TST-SCN-004), and the smoke scope contains at least one scenario per domain (TST-TAG-004). | T | PP |

## 18. Pipeline, environments and execution

Stages are ordered by speed and scope. Where no pipeline exists yet, the same stages run locally with the documented commands (TST-PIP-014).

| Stage | Content | Runs on | Depends on | Time budget (**default**, P20) |
|---|---|---|---|---|
| 1 Static checks | Lint, format, type check; contract lint and comparison; dry run of all feature files; tag lint; stub validation against the contract | Every change | | 3 min |
| 2 Unit and component | L1 and L2 with coverage gates and accessibility checks of components | Every change | 1 | 5 min |
| 3a Backend integration and contract | L3 and L4, `@smoke` on a change, `@smoke or @regression` on a merge | Every change | 1 | 10 min |
| 3b Frontend integration | L5, `@smoke` on a change, `@smoke or @regression` on a merge | Every change | 1 | 10 min |
| 4 Build | Images or artefacts, tagged by commit | Every change | 2, 3a, 3b | 10 min |
| 5 End-to-end smoke | L6 `@smoke` in managed mode against the built artefacts | Every change | 4 | 10 min |
| 5b Post-deployment smoke | L6 `@smoke` in external mode against a development or test environment after its deployment | After each deployment to that environment | The deployment | 10 min |
| 6 Regression | L6 `@regression`; every Gherkin suite at `@regression`; flake check of changed scenarios | Merge to the main branch | 5 | 30 min |
| 7 Nightly and release | All scenarios except `@wip` and `@quarantine`, random order; the quarantine run; contract fuzzing; load tests (L7); visual regression where enabled | Nightly, release | 4 | 60 min |
| 8 Report | Combined report, coverage and traceability, shape check | Always, also after a failure | | 5 min |

| ID | Statement | Ver. | Basis |
|---|---|---|---|
| TST-PIP-001 | Stages MUST be ordered by speed and scope (TST-PYR-009). A failing stage stops the stages that depend on it. The report stage runs always. | T | PYR |
| TST-PIP-002 | Every change MUST run stages 1 to 5. A merge to the main branch MUST also run stage 6. The nightly run and the release run stage 7. | T | PP |
| TST-PIP-003 | Stages 3a and 3b run in parallel and are independent of each other. Stage 5 starts only after the images are built and stages 2, 3a and 3b have passed, because it is the most expensive. | T | PP |
| TST-PIP-004 | The pipeline calls the documented commands of 26.2. It MUST NOT contain test logic that cannot be run locally (TST-CON-017). | T | PP |
| TST-PIP-005 | The deployment definition of the project (compose or equivalent) is the single source of truth for the managed-mode end-to-end environment, locally and in the pipeline. | I | PP |
| TST-PIP-006 | Artefacts are built once per commit and tagged by the commit. Stage 5 and the deployment use the same artefacts (TST-E2E-004). | T | PP |
| TST-PIP-007 | Dependency and image caches are allowed. Test results MUST NOT be cached. Every run starts from a clean state: new containers, new browser contexts, empty stub server, and in managed mode a fresh environment. | T | PP |
| TST-PIP-008 | A stage that exceeds its time budget by 50 % raises a warning and a task to repair it. A stage that exceeds its hard timeout fails. Budgets are not raised to make a run pass. | T | PP |
| TST-PIP-009 | A merge needs every required stage to be green. A red gate is overridden only by a recorded deviation that the user or owner approved (TST-PRE-005). | I | PP |
| TST-PIP-010 | The scheduled nightly run is owned. A failure opens a task and is triaged on the working day (TST-STB-008). | I | PP |
| TST-PIP-011 | Browsers and drivers are installed from pinned versions in the pipeline image. | T | PP |
| TST-PIP-012 | A suite MAY be sharded across runners. Sharding MUST keep scenario independence (TST-GHK-009), and the reports of the shards are merged into one. | T | PP |
| TST-PIP-013 | The nightly run MUST run the scenarios in random order (the order option of the runner) and report the seed, to expose order dependence. | T | PP |
| TST-PIP-014 | Until a pipeline exists, the gates MUST be runnable locally with documented commands, and their results MUST be recorded with the change. | I | PP |
| TST-PIP-015 | The tests and the pipeline need no real secrets and no real credentials. Test configuration holds only addresses of test infrastructure. | T | PP |
| TST-PIP-016 | The run publishes the pyramid shape check (TST-PYR-008), the tag lint, the contract validation of stubs, and the traceability report as gates or reports as this document states. | T | PP |
| TST-PIP-017 | The pipeline MAY run the `@smoke` scope of the end-to-end package in external mode after each deployment to a development or test environment, using the profile of that environment (TST-E2E-041). A failure marks the deployment as unverified and opens a task. | T | PP |

## 19. Stability, flaky tests and isolation

| ID | Statement | Ver. | Basis |
|---|---|---|---|
| TST-STB-001 | Tests MUST NOT use fixed sleeps. They wait for a condition with a timeout (the automatic waiting of the browser tool, retrying assertions, readiness checks, awaited events). | T | PP |
| TST-STB-002 | Each scenario has its own data, its own browser context and its own stub state, and depends on no order (TST-GHK-009, TST-DAT-003). | T | PP |
| TST-STB-003 | Retries: L1, L2, L3 and L5 use none. L6 uses at most one (**default**, P13). A scenario that passes only on a retry is reported as flaky. | T | PP |
| TST-STB-004 | A flaky test MUST be repaired within the period of P13 (**default** 14 days) or quarantined with an owner, a reason and an expiry date. An expired quarantine ends in a repair or in the deletion of the test with a replacement at a lower level. | I | PP |
| TST-STB-005 | The cause of a flaky test is recorded by category: waiting and timing, shared state, order dependence, data collision, environment, time or locale, network, defect of the test, or race condition in the product. A race condition in the product is a product defect and is fixed as one. | I | PP |
| TST-STB-006 | `@quarantine` scenarios are excluded from the gates, run in the nightly run, and appear in the reports. The share of quarantined scenarios MUST NOT exceed 2 % (**default**, P13); a higher share fails the check. | T | PP |
| TST-STB-007 | A flaky test MUST NOT be repaired by raising a timeout blindly, adding retries beyond the policy, or weakening an assertion (TST-CON-013). | I | PP |
| TST-STB-008 | A red run is triaged within the working day and classified as a product defect, a test defect or an environment defect (TST-E2E-025). | I | PP |
| TST-STB-009 | New and changed scenarios MUST be run repeatedly (**default** 5 times, P13) before a merge, to reveal flakiness. A scenario that fails once in the repetitions is not merged. | T | PP |
| TST-STB-010 | A suite run with several workers and with one worker MUST give the same results. A difference is a defect of isolation. | T | PP |
| TST-STB-011 | Containers, browsers, stub servers, temporary files and processes MUST be closed after the run, also after a failure. A leaked process or container fails the check. | T | PP |
| TST-STB-012 | Locale, time zone, clock and random seed are explicit in every test (TST-CON-003). | T | PP |
| TST-STB-013 | No gate run depends on a real external service. External dependencies of the backend are replaced by the stub server (where the backend has any), the frontend's backend is mocked by the stub server, and L6 uses a sandbox (TST-E2E-003). | T | PP |
| TST-STB-014 | A test that fails only on one machine or runner is treated as flaky until the cause is found. | I | PP |
| TST-STB-015 | A defect that is found by an unstable test, and that cannot be reproduced, is recorded with the seed, the logs and the evidence, so that it can be analysed later. | I | PP |

## 20. Test code quality and repository layout

### 20.1 Quality of test code

| ID | Statement | Ver. | Basis |
|---|---|---|---|
| TST-QLT-001 | Test code MUST be reviewed, linted and formatted like production code. Skipped, focused (`only`) or commented-out tests MUST NOT be committed without an owner, a reason and an expiry date. | T | PP |
| TST-QLT-002 | Tests favour readability over avoiding duplication. Some duplication is acceptable when it makes a test clearer. Shared code is extracted after the third use. | I | PYR |
| TST-QLT-003 | Test bodies contain no conditionals and no loops. Logic is in helpers and is kept simple. | I | PP |
| TST-QLT-004 | Tests MUST NOT change global state that another test can observe, and MUST restore anything they change. | T | PP |
| TST-QLT-005 | Tests of L3, L5 and L6 treat the application as a black box over its interface. They MUST NOT import production internals. Unit tests MAY import the unit under test only. | I | PP |
| TST-QLT-006 | Production code MUST NOT depend on test code, and production artefacts MUST NOT contain test code, test hooks, test routes or test switches. Test identifiers on elements are allowed (TST-IDN-001). | T | PP |
| TST-QLT-007 | Test helpers, drivers and factories have their own tests where they contain non-trivial logic. | I | PP |
| TST-QLT-008 | Each suite has a README that states how to run it, its prerequisites, its tags, its configuration and how to diagnose failures. The commands match 26.2. | I | PP |
| TST-QLT-009 | Test code uses the language, typing and quality level of the production code of its platform. | I | PP |
| TST-QLT-010 | A test that no longer adds confidence is deleted (TST-PYR-016). | I | PYR |
| TST-QLT-011 | Test names and descriptions state behaviour (TST-UNT-001, TST-GHK-004). | I | PP |

### 20.2 Repository layout (default, P23)

Backend L1 tests follow the convention of the platform. Frontend L1 and L2 tests always sit in the folder of the component or unit that they test, and every component has its own folder (TST-CMP-014). The folder names below follow the project's frontend architecture.

```text
project/
  backend/
    src/                          production code (L1 tests follow the platform convention)
    tests/
      integration/                L3 and L4
        features/                 Gherkin feature files
        steps/                    step definitions
        support/                  scenario context, hooks, drivers, factories, runner entry point
        stubs/                    stub mappings and body files for external services (only if the backend calls any)
  frontend/
    <domain>/
      components/
        <component>/              one folder per component
          <component>.<ext>       the component (markup, logic, styles as the framework arranges them)
          <component>.spec.<ext>  its L1 and L2 tests, beside it
          fixtures/               fixtures and fakes that only this component uses (optional)
      views/
        <view>/                   one folder per view, with its tests
      testing/                    fakes and factories shared inside this domain only
    shared/
      ui-components/
        <component>/              one folder per shared component, with its tests
      testing/                    helpers without domain vocabulary
    tests/
      integration/                L5 (never unit or component tests)
        features/
        steps/
        support/                  scenario context, hooks, screen drivers, stub client
        stubs/                    mappings and body files, grouped by domain
  tests/
    e2e/                          L6, an independent npm package, outside frontend and backend
      package.json                own dependencies, lock file and scripts
      features/                   Gherkin feature files
      steps/                      step definitions
      support/                    scenario context, hooks, screen drivers
      config/                     environment profiles (local, dev, test, others)
      scripts/                    managed-mode scripts: start, wait for readiness, collect logs, stop
      README.md
    load/                         L7 scripts
    collections/                  optional collections for manual API testing
  ai-docs/
```

| ID | Statement | Ver. | Basis |
|---|---|---|---|
| TST-QLT-012 | Each level has one known location. The locations and commands are recorded (26.2). Frontend L1 and L2 tests live in the folders of their components (TST-CMP-014), L5 lives in the frontend's `tests/integration`, and L6 lives in its own package `<project>/tests/e2e`, outside the frontend and backend directories (TST-PYR-014, TST-E2E-029). | I | PP |
| TST-QLT-013 | Stub mappings live beside the suite that uses them (TST-FIN-016, TST-INT-005). L6 has none. | I | PP |

## 21. Deferred items and optional levels

| ID | Statement | Ver. | Basis |
|---|---|---|---|
| TST-DEF-001 | The following tests are deferred to the final project phase, and agents MUST NOT write, configure, scaffold or modify them earlier: authentication and authorisation (scenarios for `401` and `403`, role isolation, the `@auth-*` tags, test accounts, login journeys), input hardening and injection tests, security headers, rate limits and abuse, session and token handling, dependency and secret scanning, and penetration tests. | I | PP |
| TST-DEF-002 | The following tests are deferred to the final project phase on the same terms: assertions on log content and format, metrics, tracing, alerting, audit trails and health reporting beyond a readiness check. | I | PP |
| TST-DEF-003 | Not deferred, and required now: tests of business rules, validation, state transitions, conflicts, concurrency, error mapping, the contract, and the readiness check. | I | PP |
| TST-DEF-004 | When the final phase starts, the agent writes the test plan for the deferred areas from the project's requirements, and the user approves it before implementation. The minimum sets of TST-SCN-001 and TST-SCN-002 are then extended, the `@auth-*` tags become active, and L6 gains its login journeys. | I | PP |
| TST-DEF-005 | Nothing is scaffolded for the deferred areas now. The design only keeps the way open: the tags are reserved, the test profile can be extended, and the HTTP client and the browser context can carry credentials later. | I | PP |
| TST-DEF-006 | Optional techniques are not required until a parameter enables them: mutation testing (P26), property-based testing (P27), consumer-driven contract tests (TST-CTR-009), visual regression (P15), endurance tests (TST-NFT-009), hosted device clouds, and chaos experiments. The architecture and tooling MUST NOT prevent them from being added. | I | PP |
| TST-DEF-007 | Test-only code in production (hooks, switches, routes) is prohibited for deferred areas as for all others (TST-QLT-006). | I | PP |

## 22. Edge cases and error conditions

### 22.1 Scenarios and steps

| ID | Condition | Required behaviour | Ver. | Basis |
|---|---|---|---|---|
| TST-EDG-001 | A step text matches two step definitions | The run fails with the ambiguous match. The steps are made distinct. | T | GHK |
| TST-EDG-002 | A step has no definition | The run fails (strict mode). It is neither skipped nor passed. | T | GHK |
| TST-EDG-003 | A step is pending | The run fails, as for a missing definition. | T | GHK |
| TST-EDG-004 | A scenario needs the result of another scenario | Not allowed. The scenario creates its data in its own `Given` steps. | T | PP |
| TST-EDG-005 | Background steps are needed by only some scenarios | They move into those scenarios, or the feature is split. | I | GHK |
| TST-EDG-006 | A scenario outline has rows that test different behaviours | It is split into scenarios. | I | GHK |
| TST-EDG-007 | A scenario needs more than ten steps | It is split, or detail moves into step definitions. A justified exception is recorded. | I | PP |
| TST-EDG-008 | A `Then` step cannot be asserted at this level | The behaviour belongs to another level, and the scenario moves there. | I | PYR |
| TST-EDG-009 | A step needs a value that exists only after creation | The step reads it from the scenario context. | T | PP |
| TST-EDG-010 | A scenario needs a precondition that the interface cannot reach | A documented factory or seeder creates it, and the step name says so in domain terms. | I | PP |
| TST-EDG-011 | Two feature files use different words for the same concept | The glossary term is used in both. | I | PP |
| TST-EDG-012 | A step definition needs a conditional to cope with two behaviours | Two steps or two scenarios are written. | I | PP |
| TST-EDG-013 | A feature file contains a URL path, a selector or a query | It is removed and moved into a step definition. | I | PP |
| TST-EDG-014 | Every `Then` step of a scenario is empty | The scenario is rejected in review. Every `Then` asserts. | T | PP |
| TST-EDG-015 | A tag is misspelled or unknown | The tag lint fails. | T | PP |
| TST-EDG-016 | A scenario carries two scope tags | The tag lint fails. | T | PP |

### 22.2 Stub server and frontend integration

| ID | Condition | Required behaviour | Ver. | Basis |
|---|---|---|---|---|
| TST-EDG-017 | The frontend calls an endpoint for which no stub exists | The stub server answers with an error. The unmatched-request check fails the scenario and names the request and the closest stubs. | T | WM |
| TST-EDG-018 | A stub matches a request that the contract does not allow | The stub validation fails the pipeline. | T | OAS |
| TST-EDG-019 | Two stubs match the same request | The stub with the higher priority answers. Overlapping stubs with equal priority are rejected in review. | I | WM |
| TST-EDG-020 | A scenario leaves stubs behind | The reset after each scenario removes them. A check at the start of a scenario fails when a stub, a journal entry or a scenario state remains. | T | WM |
| TST-EDG-021 | The stub server is not ready when the first scenario starts | The harness waits for the administration interface. After the timeout the run fails with an environment error. | T | WM |
| TST-EDG-022 | The port of the stub server is taken | The run fails with a clear environment error. With several workers each worker derives its own port. | T | PP |
| TST-EDG-023 | Parallel workers share one stub server | Not allowed. Each worker has its own instance. | T | PP |
| TST-EDG-024 | The browser requests a font, a script or an analytics resource from an external origin | The network guard blocks and records it, and the scenario fails. The origin is removed from the application, or added to the allow-list with a recorded reason (P05). | T | PP |
| TST-EDG-025 | A server-rendered page calls the API from the server | The server's API address points to the stub server. The unmatched-request check covers the call; the browser guard does not see it. | T | PP |
| TST-EDG-026 | The frontend retries a failed request automatically | The stub sequence uses scenario states. The journal verifies the number of attempts and their spacing. | T | WM |
| TST-EDG-027 | The user submits the same action twice quickly | The journal shows exactly one request, or the documented pair that carries the same idempotency key. | T | WM |
| TST-EDG-028 | A response arrives after the user has left the page | No console error occurs and no stale data appears on the new page. | T | PP |
| TST-EDG-029 | The stub returns a malformed JSON body | The interface shows its error state, does not crash, and logs only the declared error. | T | WM |
| TST-EDG-030 | The stub returns an unexpected content type | The interface treats the answer as an error and shows its error state. | T | HTTP |
| TST-EDG-031 | The stub returns `429` or `503` with `Retry-After` | The interface honours the delay, shown with the controlled clock, and offers the action again afterwards. | T | HTTP |
| TST-EDG-032 | The stub returns an error code that the interface does not know | The interface shows the generic error and keeps the correlation identifier for support. | T | PP |
| TST-EDG-033 | The stub answers a conditional request with `304` | The interface reuses its stored entry without a visible reload. | T | HTTP |
| TST-EDG-034 | The stub answers a write with `412` | The interface shows the conflict message and offers to reload and retry. | T | HTTP |
| TST-EDG-035 | A response is slower than the client timeout | The interface shows the timeout state and offers a retry. A late response is ignored. | T | WM |
| TST-EDG-036 | The connection is reset or the response is empty | The interface shows the network-error state and offers a retry. | T | WM |
| TST-EDG-037 | A stub contains dates relative to the current time | Not allowed. Stubs use fixed dates and the browser clock is controlled. | T | PP |
| TST-EDG-038 | A scenario needs a large stub body | The body is a file, and the scenario uses the smallest data that shows the behaviour. Large volumes belong to L7. | I | PP |
| TST-EDG-039 | A scenario needs data from several operations | One stub per operation is installed by the step that sets the state. | I | PP |
| TST-EDG-040 | The contract changes and the stubs are not updated | The stub validation fails the pipeline. Contract and stubs change together. | T | OAS |
| TST-EDG-041 | The frontend uses a channel that the stub server cannot simulate | A project-owned test server serves that channel, recorded as a deviation. | I | WM |
| TST-EDG-042 | A scenario passes in L5 and the same journey fails in L6 | The stub differs from the real backend. The stub is corrected from the contract or from L3 behaviour and validated again. | I | PYR |
| TST-EDG-043 | The stub returns properties that the interface does not know | The interface ignores them, and a scenario shows it. | T | PP |
| TST-EDG-044 | The stub omits an optional property | The interface shows its absent state. | T | OAS |
| TST-EDG-045 | The stub returns an empty collection | The interface shows its empty state and no error. | T | PP |
| TST-EDG-046 | The stub returns the maximum documented collection size | The interface handles it, with paging or scrolling, without breaking the layout. | T | OAS |

### 22.3 End-to-end environment

| ID | Condition | Required behaviour | Ver. | Basis |
|---|---|---|---|---|
| TST-EDG-047 | A component of the environment does not start | The harness reports an environment error with the log of that component. The scenarios are not reported as failed. | T | PP |
| TST-EDG-048 | The environment starts more slowly than the start timeout | The run fails with an environment error and the logs are collected. | T | PP |
| TST-EDG-049 | The database is not empty at the start of a managed run | The run fails its start check. Managed environments are fresh. | T | PP |
| TST-EDG-050 | A previous run left its environment behind | The start step removes it or refuses to start. Each run uses a unique project name. | T | PP |
| TST-EDG-051 | Two runs execute on the same runner | Names and ports are unique per run, so they do not clash. | T | PP |
| TST-EDG-052 | A scenario creates data and fails midway | The data is unique, the environment is destroyed after the run, and no later scenario depends on it. | T | PP |
| TST-EDG-053 | A journey depends on an external service that has no sandbox | It is not part of L6. It is covered in L3 with stubs, or recorded as a deviation (TST-E2E-003). | I | PP |
| TST-EDG-054 | A journey depends on elapsed time | A shortened duration in the environment's configuration is used, or the behaviour is tested at a lower level. | I | PP |
| TST-EDG-055 | The console shows an error from a third-party script | The scenario fails. The script is removed, or the exception is recorded with an owner and an expiry. | T | PP |
| TST-EDG-056 | A page needs several seconds before it is interactive | The step waits for a condition (an element or a state), never for time. | T | PP |
| TST-EDG-057 | A scenario passes locally and fails in the pipeline | The difference (resources, fonts, timing) is investigated. The scenario is treated as flaky until the cause is found. | I | PP |
| TST-EDG-058 | The suite runs against a shared non-production environment | Scenarios use unique data and touch only their own data. They never delete data they did not create. | T | PP |
| TST-EDG-059 | The logs of the environment contain personal data | The evidence is filtered before it is attached. | T | PP |
| TST-EDG-060 | Production serves the frontend through a content delivery network, and the test environment through the proxy | The environment definition mirrors the routing rules. The differences are listed in the environment inventory. Behaviour specific to the network is checked by a smoke run on the real environment at release. | I | PP |
| TST-EDG-061 | The product has no user interface | L6 runs through the public HTTP interface of the deployed whole (TST-E2E-027). | I | PP |
| TST-EDG-062 | An L6 scenario fails because of a defect that a lower level could catch | A test at the lowest suitable level is added with the fix (TST-PYR-005). | I | PYR |

### 22.4 Backend integration and data

| ID | Condition | Required behaviour | Ver. | Basis |
|---|---|---|---|---|
| TST-EDG-063 | A scenario needs a state that only an old record has | The record is created by a documented factory. The scenario carries the category `@broken-data` when it stands for legacy data. | I | PP |
| TST-EDG-064 | A scenario depends on the order of rows without a specified order | The scenario asserts order only where the contract specifies it, and sorts first otherwise. | T | HTTP |
| TST-EDG-065 | Two scenarios insert the same unique value | Unique suffixes per scenario prevent it (TST-DAT-006). | T | PP |
| TST-EDG-066 | The database container starts slowly | It is shared across the run and awaited by a health condition. The suite budget accounts for it. | T | PP |
| TST-EDG-067 | The container runtime is not available | The run fails with a clear environment message. It MUST NOT fall back to an in-memory database. | T | PP |
| TST-EDG-068 | A migration fails on the empty database | The suite fails at the start with the migration error, and no scenario runs. | T | PP |
| TST-EDG-069 | A migration works on an empty database but not on existing data | The release run applies the migrations to a database restored from the previous release's schema and sample data. | T | PP |
| TST-EDG-070 | A scenario needs to change the clock | The controllable clock of the test configuration is used (TST-INT-012). | T | PP |
| TST-EDG-071 | The application publishes an event asynchronously | The scenario waits for the recorded message with a condition and a timeout. | T | PP |
| TST-EDG-072 | A scenario depends on a scheduled job | The job is triggered by a documented trigger of the test profile, or its interval is shortened. | T | PP |
| TST-EDG-073 | An operation must leave no partial state when a dependency fails midway | A stub fault of the external call makes the dependency fail, and the scenario verifies that no partial state remains. | T | WM |
| TST-EDG-074 | A scenario needs a very large payload | The payload is generated at the boundary size in the step. No large fixture file is committed. | I | PP |
| TST-EDG-075 | A response contains generated values (identifiers, timestamps) | The scenario asserts their format, not their value. | T | PP |
| TST-EDG-076 | The contract does not specify the order of a collection | Assertions ignore order or sort first. | T | HTTP |
| TST-EDG-077 | A scenario fails only when it runs with another | It is an isolation defect (shared data or state) and is repaired (TST-STB-010). | T | PP |
| TST-EDG-078 | The production database engine changes its major version | The test container version changes in the same change set. | T | PP |
| TST-EDG-079 | Money or decimal arithmetic gives different results | Exact types and rounding rules are tested at L1, and one scenario at L3 shows the result over the interface. | T | PP |
| TST-EDG-080 | A product consists of several owned services | Owned services run for real in the L3 environment. Only services outside the product are stubbed (TST-INT-003, TST-INT-005). | T | PP |

### 22.5 Contract and drift

| ID | Condition | Required behaviour | Ver. | Basis |
|---|---|---|---|---|
| TST-EDG-081 | An example in the contract does not validate against its schema | The contract gate fails (TST-CTR-004). | T | OAS |
| TST-EDG-082 | The provider returns a status that the contract does not document | The contract-validating client fails the scenario. | T | OAS |
| TST-EDG-083 | A response contains a property that the schema does not list | The scenario fails when the schema forbids additional properties. Otherwise it follows the contract's rule for additional properties. | T | OAS |
| TST-EDG-084 | A route exists in the code and not in the contract | The provider contract test fails (TST-CTR-001). | T | OAS |
| TST-EDG-085 | A stub of a third-party service is older than its re-check interval | The pipeline warns. At a release it fails until the stub is recorded again or confirmed. | T | PP |
| TST-EDG-086 | A recorded real response contains personal data | It is anonymised before it is committed. The date and source are recorded. | T | PP |
| TST-EDG-087 | A wrong-path scenario sends a deliberately invalid request | The category tag exempts the request from validation. The response is still validated (TST-CTR-003). | T | OAS |
| TST-EDG-088 | A consumer-driven contract fails on the provider | The provider change is not merged. Consumer and provider change in a coordinated change. | T | PYR |

### 22.6 Browser behaviour

| ID | Condition | Required behaviour | Ver. | Basis |
|---|---|---|---|---|
| TST-EDG-089 | The page uses an animation | Animations are disabled in tests, or the step waits for the end state. | T | PP |
| TST-EDG-090 | An element appears twice with the same test identifier | The scenario fails. Identifiers are unique per element in a view unless an entity identifier is appended. | T | PP |
| TST-EDG-091 | An element is covered by an overlay | The action fails its actionability check. It is not forced, and the cause is repaired. | T | PP |
| TST-EDG-092 | A scenario needs a file chooser or a native dialog | The browser tool's own file and dialog handling is used. No automation of the operating system is used. | I | PP |
| TST-EDG-093 | A scenario opens a new tab or window | It is handled through the events of the browser context and asserted. | I | PP |
| TST-EDG-094 | Cookies or storage from a previous scenario are present | Not possible: each scenario has a new browser context. | T | PP |
| TST-EDG-095 | The viewport is a phone | The scenarios that behave differently run at the mobile viewport (P11). | T | PP |
| TST-EDG-096 | A native control behaves differently per engine | The test uses the public behaviour of the component. Engine-specific differences are recorded. | I | PP |
| TST-EDG-097 | Fonts differ between machines | Fonts are bundled, and visual checks run in the container image. | T | PP |
| TST-EDG-098 | A scenario depends on the browser language | The locale is set explicitly in the browser context. | T | PP |
| TST-EDG-099 | A scenario needs clipboard, geolocation or notification permission | The permission is granted explicitly in the context of that scenario and denied otherwise. | T | PP |

### 22.7 Stability and execution

| ID | Condition | Required behaviour | Ver. | Basis |
|---|---|---|---|---|
| TST-EDG-100 | A scenario passes on retry | It is reported as flaky (TST-STB-003). | T | PP |
| TST-EDG-101 | A quarantine period expires | The test is repaired, or deleted with a replacement at a lower level. | I | PP |
| TST-EDG-102 | A new scenario fails once in five repetitions | It is not merged (TST-STB-009). | T | PP |
| TST-EDG-103 | A run is cancelled midway | Containers, browsers and stub servers are removed by the teardown or by the start step of the next run. No resource leaks. | T | PP |
| TST-EDG-104 | Parallel execution is slower than serial execution | The number of workers is reduced to what the runner supports (P24). | I | PP |
| TST-EDG-105 | The random order of the nightly run exposes a failure | It is an order dependence and a defect of the scenario. It is repaired with the printed seed. | T | PP |
| TST-EDG-106 | A time zone or a daylight-saving change causes a failure | The test fixes the time zone, and a boundary case is added at L1. | T | PP |
| TST-EDG-107 | A test passes only when a network is available | It is repaired. The network is not used outside the test environment. | T | PP |
| TST-EDG-108 | A run exceeds its time budget | A warning is raised (TST-PIP-008). The slowest tests are listed and moved down a level or made faster. | T | PP |
| TST-EDG-109 | Lower-level tests pass while a higher-level test fails | A test at the lowest suitable level is added (TST-PYR-005). | I | PYR |
| TST-EDG-110 | A secret appears in a log or a report | The run is invalid. The artefact is removed and the cause is repaired. | T | PP |

### 22.8 Tools and selection

| ID | Condition | Required behaviour | Ver. | Basis |
|---|---|---|---|---|
| TST-EDG-111 | The frameworks document names no test tool | TST-SEL-017 applies: the agent proposes the default and asks for confirmation. | I | PP |
| TST-EDG-112 | The chosen Cucumber implementation becomes unmaintained | The risk is recorded, a migration is planned, and the portable feature files (TST-SEL-010) make it possible. | I | PP |
| TST-EDG-113 | Two Gherkin runners are needed for a time | Allowed only during a recorded migration (TST-SEL-009). | I | PP |
| TST-EDG-114 | The browser tool lacks a capability of TST-SEL-006 | The tool is replaced, or the gap is recorded as a deviation with a workaround. | I | PP |
| TST-EDG-115 | A stub server other than WireMock is preferred | It is allowed when it satisfies TST-SEL-007 (TST-FIN-037). | I | PP |
| TST-EDG-116 | The browser tool's own runner executes generated tests | It conforms (TST-SEL-003). The generation step is part of every run command, and stale generated output fails the run. | T | PP |
| TST-EDG-117 | A hosted browser cloud is wanted | It is an optional extension. The suites MUST still run without it (TST-SEL-012). | I | PP |
| TST-EDG-118 | The product is a native mobile application | It is outside this document until the requirements say otherwise. Mobile automation tools are chosen through P01 under the same pyramid rules. | I | PP |

### 22.9 Reporting and coverage

| ID | Condition | Required behaviour | Ver. | Basis |
|---|---|---|---|---|
| TST-EDG-119 | A requirement has no scenario | It is shown as uncovered, and fails the gate when the change adds it (TST-COV-006). | T | PP |
| TST-EDG-120 | A scenario with two requirement tags fails | Both requirements show as failing. | T | PP |
| TST-EDG-121 | Coverage drops after a refactoring | The untested code is covered, or the exclusion is justified. | I | PP |
| TST-EDG-122 | A coverage gate is met by tests that assert nothing | The tests are rejected in review (TST-CON-014, TST-STP-005). | I | PP |
| TST-EDG-123 | A report of an old run is requested | It is available within the retention period. After it, the report is rebuilt from archived results where these were kept. | I | PP |
| TST-EDG-124 | The report site is unavailable | The artefacts of the run stay downloadable, and the run result is unaffected. | I | PP |
| TST-EDG-125 | The report tool fails to build the report | The raw results are kept, the stage fails visibly, and the gates evaluate the results, not the report. | T | PP |
| TST-EDG-126 | The shape check fails because a lower level was reduced | The missing lower-level tests are added. Valuable higher-level tests are not deleted to restore the shape (TST-PYR-016). | I | PYR |

### 22.10 Target environments

| ID | Condition | Required behaviour | Ver. | Basis |
|---|---|---|---|---|
| TST-EDG-127 | The target is unreachable | The preflight fails with an environment error that names the address. No scenario runs. | T | PP |
| TST-EDG-128 | The target runs an older build than expected | The report shows the version. Where the profile requires a version match, the preflight fails. | T | PP |
| TST-EDG-129 | The host of the target is not on the allow-list | The run fails before any request that changes data. | T | PP |
| TST-EDG-130 | An override variable points at a production host | The production guard rejects it (TST-E2E-035). | T | PP |
| TST-EDG-131 | Data of other teams exists in the shared environment | Scenarios touch only data that carries their own prefix. | T | PP |
| TST-EDG-132 | The environment lacks the minimal start-up data | The preflight fails with an environment error. | T | PP |
| TST-EDG-133 | Two runs execute against the same environment at the same time | The run identifier in the data prefix prevents collisions, and the parallelism limit of the profile applies. | T | PP |
| TST-EDG-134 | The environment sits behind another host or base path | Relative paths from the entry points of the profile are used, and no step changes. | T | PP |
| TST-EDG-135 | A development environment uses a self-signed certificate | An explicit exception in that profile, listed in the environment inventory. Profiles of production-like environments keep verification on. | I | HTTP |
| TST-EDG-136 | The environment is slower than the local stack | The profile sets the timeouts. Waits stay conditions, never time. | T | PP |
| TST-EDG-137 | The interface offers no deletion of what a scenario created | The data carries the run prefix and is accepted as residue. The environment owner decides on periodic cleanup. The data is never reused. | I | PP |
| TST-EDG-138 | The data of the environment is reset during a run | The failures are classified as environment defects, and the run is repeated. | I | PP |
| TST-EDG-139 | The runner has no access to the container runtime | External mode works unchanged. Only managed mode needs the runtime (TST-E2E-030). | T | PP |
| TST-EDG-140 | The package is installed with another Node.js version | The pinned version is enforced, and the installation fails on a mismatch. | T | PP |
| TST-EDG-141 | The frontend or backend changes its folder structure | The package is unaffected, because it imports nothing from them (TST-E2E-029). | I | PP |
| TST-EDG-142 | Authentication is added in the final phase | Credentials come from variables of the profile, and login steps are added then (TST-DEF-001). | I | PP |
| TST-EDG-143 | A scenario needs the internal address of a component | Not allowed in external mode. The behaviour is verified at a lower level. | I | PP |
| TST-EDG-144 | The environment serves the frontend and the API from different hosts | The profile names an address for each entry point, and steps use the names (TST-E2E-034). | T | PP |

### 22.11 Location of frontend unit and component tests

| ID | Condition | Required behaviour | Ver. | Basis |
|---|---|---|---|---|
| TST-EDG-145 | A component is used by several domains | It lives in the shared layer, in its own folder with its tests. | I | PP |
| TST-EDG-146 | A fake or factory is used by several components of one domain | It lives in the domain-local testing folder (TST-CMP-016). | I | PP |
| TST-EDG-147 | The framework's generator creates the test file elsewhere | The file is moved into the component folder, and the generator is configured so that it stays there. | I | PP |
| TST-EDG-148 | A component is a single file (a single-file component) | It still has its own folder, with its test beside it. | I | PP |
| TST-EDG-149 | A component has no folder of its own | A folder is created, and the file and its test move into it in the same change (TST-CMP-014). | T | PP |
| TST-EDG-150 | A coverage tool counts test files | They are excluded by pattern (TST-CMP-017). | T | PP |
| TST-EDG-151 | The bundler picks up test files | The build excludes them, and the pipeline check on the output fails when one appears (TST-CMP-017). | T | PP |
| TST-EDG-152 | A test needs a large fixture | The fixture sits in a clearly named subfolder of the component folder and is reviewed. | I | PP |
| TST-EDG-153 | A test covers a view together with its child components | It sits in the folder of the view and uses only the public interfaces of the children. | I | PP |
| TST-EDG-154 | A unit of logic (a store, an adapter, a mapper, a utility) has no component folder | Its test sits next to its source file in the same module folder. | I | PP |
| TST-EDG-155 | A runner writes a snapshot file | The file sits in the component folder, in the place the runner uses, and is reviewed (TST-CMP-013). | I | PP |
| TST-EDG-156 | A test imports the internals of a neighbouring component | Not allowed. The test uses the public interface of that component or a fake of it. | I | PP |
| TST-EDG-157 | The name of a test file does not match its subject | It is renamed (TST-CMP-015). | T | PP |

### 22.12 The four kinds of case

| ID | Condition | Required behaviour | Ver. | Basis |
|---|---|---|---|---|
| TST-EDG-158 | A kind does not apply to a unit (a constant, a toggle without boundaries) | The test group states the reason in one line, and the review accepts or rejects it. | I | PP |
| TST-EDG-159 | A scenario fits two kinds (an invalid value at a boundary) | The main point decides: a valid value at a boundary is an edge case, an invalid value is a bad path. A scenario that makes both points is split (TST-SCN-008). | I | PP |
| TST-EDG-160 | An edge case shows that a limit is not stated in the requirements | The agent asks the user and records the answer. It does not invent the limit (TST-CON-011). | I | PP |
| TST-EDG-161 | A suite meets its coverage threshold with good-path tests only | The review rejects it for the missing kinds (TST-PYR-017). | I | PP |
| TST-EDG-162 | The same check appears in all four kinds at several levels | The level that can verify it best keeps it. The higher levels keep only what they add (TST-PYR-019). | I | PYR |
| TST-EDG-163 | A pure unit has no collaborator that can fail | Its error or unexpected kind uses unexpected input, overflow and impossible values, or is justified. | I | PP |
| TST-EDG-164 | An L6 journey has many possible bad paths | One representative scenario per kind is written. The others live in L3 and L5. | I | PYR |
| TST-EDG-165 | A level has no tags (L1, L2, L4) | The tests are grouped or named by kind, so that the review sees them (TST-PYR-018). | I | PP |
| TST-EDG-166 | A kind is covered at a lower level only | The higher level adds a scenario only when it shows something that the lower level cannot (TST-PYR-019). | I | PYR |
| TST-EDG-167 | A new operation has no external dependency | The unexpected-event kind is covered by a repeated request, a concurrent change and a constraint violation, or is justified. | I | PP |

### 22.13 Optional stub server in the backend suite

| ID | Condition | Required behaviour | Ver. | Basis |
|---|---|---|---|---|
| TST-EDG-168 | The backend calls no external service | The backend suite has no stub server, no stub mappings and no stub tooling (TST-INT-025). | I | PP |
| TST-EDG-169 | The backend gains its first external call | The stub server, its mappings and their contract validation are added in the same change (TST-INT-025, TST-CTR-005). | I | PP |
| TST-EDG-170 | The backend calls an external service for which the vendor offers an emulator | The emulator MAY be used when the project records it (TST-INT-003). A stub server is not required. | I | PP |
| TST-EDG-171 | The backend calls another service that the product owns | That service runs for real in the backend suite and is not stubbed (TST-INT-003). | T | PP |
| TST-EDG-172 | The backend calls an external service only in a start-up step or a scheduled job | The scenarios that reach the call use the stub server, and the job trigger of TST-INT-023 starts it. | T | PP |

## 23. Restrictions

### 23.1 Prohibited patterns

| ID | Prohibited pattern | Basis |
|---|---|---|
| TST-RST-001 | Running tests against production, or using production data or real personal data in tests. | PYR |
| TST-RST-002 | An in-memory or embedded substitute for the production database, broker or other owned infrastructure in integration or end-to-end tests. | PP |
| TST-RST-003 | WireMock or any other stub, mock, or rewritten response in end-to-end tests. | PP |
| TST-RST-004 | A real backend or a real external service in frontend integration tests. | PP |
| TST-RST-005 | Fixed sleeps and arbitrary waits. | PP |
| TST-RST-006 | Scenarios or tests that depend on each other or on a run order, including order encoded in names or numbers. | PP |
| TST-RST-007 | Interface mechanics (clicks, field typing, selectors, page paths) in the text of browser-driven scenarios. | GHK |
| TST-RST-008 | Scenarios that restate the implementation step by step instead of describing behaviour. | GHK |
| TST-RST-009 | A step definition that calls another step. | PP |
| TST-RST-010 | A `Then` step without an assertion, and the language's built-in `assert` statement in tests. | PP |
| TST-RST-011 | Static or global mutable state in glue, and a scenario context shared between scenarios. | PP |
| TST-RST-012 | Invented identifiers (for example random UUIDs) in place of the real identifiers that the system returned. | PP |
| TST-RST-013 | Locating elements in L5 or L6 by style class, position, structural path or visible text. | PP |
| TST-RST-014 | Test identifiers used for production logic, styling, analytics or communication, or containing personal or volatile data. | PP |
| TST-RST-015 | Test-only code, hooks, routes, headers or switches in production, including a way to change the clock. | PP |
| TST-RST-016 | Skipped, focused or commented-out tests without owner, reason and expiry, and `@wip` on the main branch. | PP |
| TST-RST-017 | Weakening a test (an assertion, a threshold, a timeout) to make it pass instead of repairing the cause. | PP |
| TST-RST-018 | The same check at several levels outside the dual coverage cases. | PYR |
| TST-RST-019 | Tests of private methods, of the order of internal calls, or of framework internals. | PYR |
| TST-RST-020 | Snapshots of large trees as primary assertions, and automatic acceptance of snapshots or visual baselines. | PP |
| TST-RST-021 | Dependence on the locale, time zone or clock of the machine, or on unseeded randomness. | PP |
| TST-RST-022 | Stubs that do not validate against the contract, stubs for operations that are not in the contract, and responses that the contract forbids. | OAS |
| TST-RST-023 | Stub state shared between scenarios or between parallel workers. | PP |
| TST-RST-024 | Backend logic re-implemented inside stubs. | PP |
| TST-RST-025 | Over-specified stub matching on irrelevant details (the whole address string, irrelevant headers, whitespace in bodies). | WM |
| TST-RST-026 | Step definitions shared between L5 and L6. | PP |
| TST-RST-027 | An end-to-end suite that tests every operation or every state, or that repeats the error paths of lower levels. | PYR |
| TST-RST-028 | Direct database writes in L6 scenarios, and assertions through database queries where the interface shows the state. | PP |
| TST-RST-029 | Recorded browser sessions or generated tests committed without review and without conversion into Gherkin scenarios and test identifiers. | PP |
| TST-RST-030 | Tests written only to reach a coverage figure. | PP |
| TST-RST-031 | Mocking types that the project does not own, instead of wrapping them in an adapter. | PYR |
| TST-RST-032 | Retries beyond the policy of TST-STB-003, and a blanket retry setting for a whole suite. | PP |
| TST-RST-033 | Security tests, observability tests, their tools or their scaffolding before the final phase. | PP |
| TST-RST-034 | The `@auth-*` tags before the final phase. | PP |
| TST-RST-035 | Secrets or personal data in fixtures, stubs, reports, screenshots, traces or logs. | PP |
| TST-RST-036 | Load tests against production or against third-party services. | PP |
| TST-RST-037 | Runner-specific syntax in feature files. | GHK |
| TST-RST-038 | A copy of the feature files in another tool or repository that becomes a second source of truth. | PP |
| TST-RST-039 | A development server in a gate run of L5. | PP |
| TST-RST-040 | Hard-coded ports, hosts or credentials in feature files or step definitions. | PP |
| TST-RST-041 | A single huge scenario that tests everything. | GHK |
| TST-RST-042 | Changing a scenario that is bound to a requirement without recording the change of the requirement. | PP |
| TST-RST-043 | Tests that need a particular machine, a particular user account or a manual preparation step. | PP |
| TST-RST-044 | Container-specific details (service names, internal ports, compose networks) in feature files, steps, screen drivers or environment profiles of L6. | PP |
| TST-RST-045 | An end-to-end suite inside the frontend or backend directory, built with them, or importing their code. | PP |
| TST-RST-046 | Running L6 against production, or against a host that is not on the allow-list of the profiles. | PP |
| TST-RST-047 | Frontend unit or component tests in a separate test tree, or in the folder of another component. | PP |
| TST-RST-048 | A component, view or shared interface component without its own folder. | PP |
| TST-RST-049 | Test files, fakes or fixtures in the production output. | PP |
| TST-RST-050 | A set of tests that covers only the good path of a unit, component, operation, flow or journey. | PP |
| TST-RST-051 | A stub server in the backend suite for a backend that calls no external service, or stubs for the product's own services. | PP |

### 23.2 Default limits (non-normative, overridable; project parameters P05, P07, P08, P11, P13, P17, P20)

| Limit | Default |
|---|---|
| Steps per scenario | 10 |
| Steps in a `Background` | 5 |
| Rows in an `Examples` table | 20 |
| Step timeout | 30 seconds |
| Scenario timeout in L3 and L5 | 2 minutes |
| Scenario timeout in L6 | 5 minutes |
| Unit suite | 2 minutes |
| Component suite | 3 minutes |
| Smoke scope per Gherkin suite | 5 minutes |
| Backend integration suite (L3) | 10 minutes |
| Frontend integration suite (L5) | 10 minutes |
| End-to-end smoke | 10 minutes |
| End-to-end regression | 30 minutes |
| Nightly run | 60 minutes |
| Retries in L6 | 1 |
| Retries in L1, L2, L3, L5 | 0 |
| Quarantine period | 14 days |
| Share of quarantined scenarios | 2 % |
| Repetitions of a new or changed scenario before a merge | 5 |
| Branch coverage of business logic | 80 % |
| Branch coverage of the domain core | 90 % |
| Load-test regression tolerance | 10 % |
| Stub server port | 8089 |
| Desktop viewport | 1280 by 800 pixels |
| Mobile viewport | 390 by 844 pixels |
| Accessibility standard | WCAG 2.2 AA |
| Report retention | 30 days |

## 24. Quality, verification and acceptance

### 24.1 Review gate

A change MUST be rejected when any of these is true.

| ID | Rejection criterion | Ver. |
|---|---|---|
| TST-REV-001 | A behaviour at a boundary has no Gherkin scenario, or a scenario breaks the writing rules (TST-CON-004, section 5). | I |
| TST-REV-002 | The minimum tests of 17.1 are missing, or the dual coverage rule is not met (TST-COV-001, TST-COV-002). | I |
| TST-REV-003 | A test sits at the wrong level or repeats a check of a lower level (TST-PYR-003, TST-PYR-004). | I |
| TST-REV-004 | L5 uses a real backend or a real external service, or L6 uses WireMock or any stub (TST-CON-006, TST-FIN-003, TST-E2E-002). | T |
| TST-REV-005 | A substitute replaces the production database or other owned infrastructure in L3 or L6 (TST-CON-007, TST-INT-002). | T |
| TST-REV-006 | The network guard, the unmatched-request check or the console-error check is missing in L5 (TST-FIN-010, TST-FIN-011, TST-FIN-015). | T |
| TST-REV-007 | A stub is not validated against the contract, covers an operation that the contract lacks, or contains backend logic (TST-FIN-005, TST-FIN-020, TST-CTR-005). | T |
| TST-REV-008 | Tags are missing, unknown or in conflict (TST-TAG-002, TST-SCN-008). | T |
| TST-REV-009 | A step rule is broken: a `Then` without an assertion, the `assert` statement, a step that calls a step, global state, or an invented identifier (section 5.2). | T |
| TST-REV-010 | A test uses sleeps, depends on order or shared state, uses unseeded randomness, or depends on the machine's locale, time zone or clock (TST-CON-003, section 19). | T |
| TST-REV-011 | Elements are located by style class, position or text, or test identifiers are misused (section 13). | T |
| TST-REV-012 | Test-only code exists in production (TST-QLT-006, TST-INT-012). | I |
| TST-REV-013 | A test is weakened, skipped, focused or commented out without owner, reason and expiry, or `@wip` is on the main branch (TST-CON-013, TST-QLT-001, TST-GHK-016). | I |
| TST-REV-014 | A new test has not been seen to fail (TST-COV-009). | I |
| TST-REV-015 | Real personal data or secrets are in data, fixtures, stubs or reports (TST-CON-012, TST-CON-015, TST-DAT-004). | T |
| TST-REV-016 | A defect fix has no regression test (TST-UNT-013). | I |
| TST-REV-017 | A gate of 24.8 is missing or failing. | T |
| TST-REV-018 | A deferred item was implemented (TST-CON-009, section 21). | I |
| TST-REV-019 | Tools were selected without the record of 26.3, or without reading the frameworks document (TST-SEL-001, TST-SEL-004). | I |
| TST-REV-020 | Accessibility checks are missing for a new state or pattern (TST-VIS-001, TST-VIS-003). | T |
| TST-REV-021 | An L6 rule is broken: a stale or non-empty environment, data written to the database, a journey that is not primary, or retries beyond the policy (section 11). | T |
| TST-REV-022 | Error scenarios are missing: an error code that nothing produces, no broken-data scenario, no unexpected-event scenario (TST-SCN-001 to TST-SCN-007). | T |
| TST-REV-023 | A prohibited pattern from 23.1 is present. | I |
| TST-REV-024 | The L6 package is not independent (it lies inside the frontend or backend, or imports their code), a profile holds a hard-coded address in steps, the production guard or the preflight is missing, or shared-environment data rules are broken (TST-E2E-029 to TST-E2E-044). | T |
| TST-REV-025 | A frontend unit or component test is outside the folder of the unit it tests, a component has no folder of its own, a global tree of test helpers exists, or test files reach the production output (TST-CMP-014 to TST-CMP-019). | T |
| TST-REV-026 | A level lacks one of the four kinds of case (good path, bad path, edge cases, error or unexpected) for a unit, component, operation, flow or journey, without a recorded justification (TST-PYR-017, TST-SCN-010 to TST-SCN-014). | T |

### 24.2 Definition of done

A change is done only when:

- Every acceptance criterion of the change is a scenario at the right level, or a unit or component test, and the minimum tests of 17.1 exist, each with the four kinds of case: good path, bad path, edge cases, and error or unexpected cases.
- Each new test has been seen to fail for the right reason and now passes, and new or changed scenarios have passed their repetitions.
- New operations have backend scenarios (happy path and the error set), a contract test, and, where the interface uses them, frontend integration scenarios with contract-valid stubs.
- New interface states have component tests, in the folder of their component, and frontend integration scenarios, with accessibility checks and test identifiers.
- Primary journeys have end-to-end scenarios without any stub, in the independent package `tests/e2e`, and they pass in managed mode against the shipped artefacts and, where profiles exist, in external mode against those environments.
- Tags, requirement traceability, the shape check and the coverage thresholds pass.
- No test uses sleeps, shared state, the machine's locale or time zone, or unseeded randomness.
- Reports are published and contain no secrets or personal data.
- The commands of 26.2 are current and the suite READMEs are up to date.
- No deferred item was implemented, and no prohibited pattern is present.

### 24.3 Agent workflow for a new operation

1. Read the requirement and the contract. If they do not answer a question, ask the user (TST-CON-011).
2. Write the L3 scenarios first: the happy path, the error set of TST-SCN-001, the broken-data scenarios, the edge cases of TST-SCN-011 and the unexpected events of TST-SCN-014. Tag them per 5.3. Where the behaviour is new, agree the scenarios with the user (TST-GHK-019).
3. Write the L1 tests for the rules, grouped by the four kinds. Implement until the tests pass. Confirm that each test failed first (TST-COV-009).
4. Add or update the contract test and the contract-validating client checks. Update the examples.
5. If the interface uses the operation, add contract-valid stubs, the screen drivers and test identifiers, and the L5 scenarios: happy path, bad path (TST-SCN-013), edge cases (TST-SCN-012), states and unexpected events.
6. If the operation is on a primary journey, add or extend the L6 journey.
7. Run each level with its documented command. Repeat the new scenarios (TST-STB-009).
8. Check the traceability and shape reports, the review gate (24.1) and the definition of done (24.2).

### 24.4 Agent workflow for a new interface feature

1. Read the requirement and the interface contract. List the states of every view.
2. Give the component its own folder. Add test identifiers. Write L2 tests for every state, with accessibility checks, in that folder.
3. Add stubs for every operation that the feature uses, including faults and delays, and the L5 scenarios with the network guard, unmatched check and console check.
4. If the feature is on a primary journey, add the L6 journey.
5. Run the levels, repeat the new scenarios, and check the gates.

### 24.5 Agent workflow for a defect

1. Reproduce the defect with a test at the lowest suitable level, and see it fail.
2. Fix the code. The test passes. If the defect appeared at a higher level only, add the lower-level test as well (TST-PYR-005).
3. If the defect shows that a requirement or scenario was wrong, ask the user before changing it (TST-GHK-019).
4. Record the cause. If the defect was found by exploratory testing, record the charter (TST-PYR-010).

### 24.6 Agent workflow for a new project

1. Read the frameworks document and the business-requirements document. Fill P01 and 26.3 from them, and propose defaults where they are silent (TST-SEL-017).
2. Ask the user to confirm the tools, the Gherkin runners, the stub server and the report tool before installing them.
3. Create the three suites and the unit and component set-up with one scenario each: the application starts (L3), a page renders against the stub server (L5), the application is reachable through the entry point (L6). Create the L6 package in `tests/e2e` with the profile `local` and the managed-mode scripts, and the profiles of the other target environments of P31.
4. Record the commands and locations (26.2), the tag catalogue (26.4) and the environment inventory (26.5), and write the suite READMEs.
5. Run all levels and the report stage once. Only then start writing tests for features.

### 24.7 Acceptance criteria

```gherkin
Feature: Testing strategy

  Scenario: Every level covers the four kinds of case
    Given a new operation that the interface uses
    When the change is reviewed
    Then the unit tests, the backend scenarios, the contract tests and the frontend integration scenarios each contain a good path, a bad path, edge cases and error or unexpected cases
    And the end-to-end journey has one representative scenario of each kind
    And a level that covers only the good path is rejected

  Scenario: A new operation is delivered with complete tests
    Given a new operation that the interface uses and that is on a primary journey
    When the change is reviewed
    Then backend scenarios cover the happy path and the error set
    And a contract test covers the operation
    And frontend integration scenarios with contract-valid stubs cover its states and unexpected events
    And an end-to-end journey covers it without any stub

  Scenario: The frontend integration suite isolates the frontend
    Given the real backend is not running
    When the frontend integration suite runs
    Then every backend call is answered by the stub server
    And the network guard records no request to another origin
    And the unmatched-request check finds no unmatched request

  Scenario: An unmatched request fails the scenario
    Given a scenario whose page calls an operation that has no stub
    When the scenario runs
    Then the stub server answers with an error status
    And the scenario fails naming the request and the closest stubs

  Scenario: The end-to-end suite uses no stub
    Given the whole application started from the shipped artefacts
    When the end-to-end suite runs
    Then no stub server is part of the environment
    And no request or response is rewritten
    And the browser console shows no error

  Scenario: The end-to-end package tests any non-production environment
    Given the package in tests/e2e and the profile "dev" for a deployed development environment
    When the package runs in external mode with the profile "dev"
    Then the preflight passes before the first scenario
    And the scenarios use only paths relative to the entry points of the profile
    And they create and remove their own data and touch no other data
    And the same feature files run in managed mode against the local stack

  Scenario: Production cannot be targeted
    Given an override variable that points at a production host
    When the package starts
    Then the production guard fails the run before any request that changes data

  Scenario: A substitute database is rejected
    Given an integration suite that starts an in-memory database
    When the review gate is applied
    Then the change is rejected for TST-CON-007

  Scenario: A stub that has drifted from the contract fails the pipeline
    Given a stub whose response lacks a property that the contract requires
    When the stub validation runs
    Then the pipeline fails and names the stub and the property

  Scenario: Scenarios are selected by tags
    When a change is submitted
    Then only scenarios tagged smoke run in each Gherkin suite
    And a scenario without a phase, priority, category, scope, domain or requirement tag fails the tag lint

  Scenario: A flaky end-to-end scenario is handled
    Given an end-to-end scenario that passes only on its single retry
    Then the report marks it flaky
    And it is repaired or quarantined with an owner and an expiry within the stated period

  Scenario: An inverted suite is detected
    Given more end-to-end scenarios than backend integration scenarios
    When the shape check runs
    Then the pipeline fails

  Scenario: A defect fix carries a regression test
    Given a defect found in an end-to-end run
    When the fix is submitted
    Then a test at the lowest suitable level fails without the fix and passes with it

  Scenario: Frontend tests sit beside their component
    Given a new interface component
    When the change is reviewed
    Then the component has its own folder
    And its unit and component tests are in that folder, beside the component
    And no test file is in the production output

  Scenario: Elements are located by test identifier
    Given a browser-driven scenario that locates an element by its style class
    When the review gate is applied
    Then the change is rejected for TST-IDN-005

  Scenario: Deferred tests are not written early
    Given an agent working before the final phase
    When it is asked for authentication or security scenarios
    Then it declines to write them and refers to the final phase

  Scenario: Reports are published without secrets
    When a pipeline run ends, also after a failure
    Then the combined report, the machine-readable results and the coverage reports are published
    And they contain no secrets and no personal data
```

### 24.8 Pipeline gates

| ID | Statement | Ver. | Basis |
|---|---|---|---|
| TST-QGT-001 | A dry run of all feature files MUST pass: syntax valid, every step bound exactly once (TST-STP-013). | T | GHK |
| TST-QGT-002 | The tag lint MUST pass: required tags present, no unknown tag, no conflicting tag (TST-TAG-002). | T | PP |
| TST-QGT-003 | The validation of every stub against the contract MUST pass (TST-CTR-005). | T | OAS |
| TST-QGT-004 | The provider contract test and the example validation MUST pass (TST-CTR-001, TST-CTR-004). | T | OAS |
| TST-QGT-005 | L1 and L2 MUST pass, and the coverage thresholds MUST be met (TST-UNT-009). | T | PP |
| TST-QGT-006 | L3 and L5 MUST pass, including the guards of L5: network guard, unmatched requests, console errors (TST-FIN-010, TST-FIN-011, TST-FIN-015). | T | PP |
| TST-QGT-007 | The L6 smoke scope MUST pass against the built artefacts (TST-E2E-009). | T | PP |
| TST-QGT-008 | The pyramid shape check MUST pass (TST-PYR-008). | T | PYR |
| TST-QGT-009 | The traceability check MUST pass: nothing that the change adds is uncovered (TST-COV-006). | T | PP |
| TST-QGT-010 | The accessibility checks of L2, L5 and L6 MUST pass, with only valid exceptions (TST-VIS-001, TST-VIS-002). | T | WCAG |
| TST-QGT-011 | The repetition of new and changed scenarios MUST pass (TST-STB-009). | T | PP |
| TST-QGT-012 | The share of quarantined scenarios MUST be within its limit, and no quarantine may be expired (TST-STB-004, TST-STB-006). | T | PP |
| TST-QGT-013 | No container, browser, stub server or process may be left after a run (TST-STB-011). | T | PP |
| TST-QGT-014 | Where visual regression is enabled, the comparison MUST pass without automatically accepted baselines (TST-VIS-006, TST-VIS-008). | T | PP |
| TST-QGT-015 | The report stage MUST publish the combined report, the machine-readable results and the coverage reports (TST-RPT-001, TST-RPT-002). | D | PP |
| TST-QGT-016 | The layout check MUST pass: every frontend unit and component test sits in the folder of the unit it tests, every component has its own folder, and no test file is in the production output (TST-CMP-014, TST-CMP-017). | T | PP |
| TST-QGT-017 | The kind matrix check MUST pass: for every operation (L3), flow (L5) and journey (L6) that the change adds, scenarios of the required kinds exist (TST-SCN-010 to TST-SCN-014). Missing kinds of legacy items are reported and tracked (P19). | T | PP |

### 24.9 Verification methods

| Method | Meaning | Typical evidence |
|---|---|---|
| T | Test | The run of a suite or a check: dry run, tag lint, stub validation, contract test, shape check, guards of L5, repetition runs |
| I | Inspection | Review of feature files, step definitions, the tool selection record, stubs, deviation records and the review gate |
| A | Analysis | Trends of duration and flakiness, the traceability matrix, the coverage figures, the shape report |
| D | Demonstration | A change that shows its scenarios failing first and passing afterwards; a published combined report that the product owner can browse; an environment started and destroyed by one command |

## 25. References

- [Ham Vocke: The Practical Test Pyramid](https://martinfowler.com/articles/practical-test-pyramid.html)
- [Martin Fowler: Test Pyramid](https://martinfowler.com/bliki/TestPyramid.html)
- [web.dev: Pyramid or Crab? Find a testing strategy that fits](https://web.dev/articles/ta-strategies)
- [Cucumber: Eviscerating the test automation pyramid](https://cucumber.io/blog/bdd/eviscerating-the-test-automation-pyramid)
- [Cucumber: Better Gherkin](https://cucumber.io/docs/bdd/better-gherkin/)
- [Cucumber: Gherkin reference](https://cucumber.io/docs/gherkin/reference/)
- [Cucumber: Installation and implementations](https://cucumber.io/docs/installation)
- [Cucumber-JS: parallel execution](https://github.com/cucumber/cucumber-js/blob/main/docs/parallel.md)
- [Cucumber-JVM: JUnit Platform engine](https://github.com/cucumber/cucumber-jvm/tree/main/cucumber-junit-platform-engine)
- [playwright-bdd](https://github.com/vitalets/playwright-bdd)
- [Playwright](https://playwright.dev)
- [WireMock documentation](https://wiremock.org/docs/)
- [WireMock: stubbing](https://wiremock.org/docs/stubbing/)
- [WireMock: stateful behaviour](https://wiremock.org/docs/stateful-behaviour/)
- [WireMock: verifying requests](https://wiremock.org/docs/verifying/)
- [Testcontainers](https://testcontainers.com/)
- [Pact: comparisons](https://docs.pact.io/getting_started/comparisons)
- [Allure Report: Cucumber-JVM](https://allurereport.org/docs/cucumberjvm/)
- [Reqnroll](https://reqnroll.net/)
- [OpenAPI Specification](https://spec.openapis.org/oas/latest.html)
- [RFC 9110: HTTP Semantics](https://www.rfc-editor.org/rfc/rfc9110.html)
- [RFC 9457: Problem Details for HTTP APIs](https://www.rfc-editor.org/rfc/rfc9457.html)
- [W3C: Web Content Accessibility Guidelines 2.2](https://www.w3.org/TR/WCAG22/)

## 26. Project parameters (to be completed by the agent)

Procedure: (1) read the business-requirements document and the frameworks document, fill every parameter that they answer, set Status to *from requirements* or *from frameworks*, and name the source; (2) ask the user each remaining question, one at a time, using the default as the proposed answer; (3) record the answer, the date and the status; (4) repeat the check when requirements change.

### 26.1 Parameters

| # | Parameter | Question | Source | Default | Answer | Status |
|---|---|---|---|---|---|---|
| P01 | Main frameworks | Which are the main backend, frontend and build frameworks, and the runtime versions? | Frameworks document | None; the tools follow from them (section 4) | | open |
| P02 | Test shape | Which shape does the suite follow, and how does the shape check compare levels? | Team, architecture | Pyramid; units plus components exceed backend plus frontend integration, which exceed end-to-end | | open |
| P03 | Gherkin runners | Which Gherkin runner serves the backend suite, the frontend integration suite and the end-to-end suite? | Frameworks document | Cucumber for the backend platform; Cucumber-JS with Playwright for L5 and L6 | | open |
| P04 | Browser automation tool | Which tool drives the browser, and in which engines? | Frameworks document | Playwright, Chromium | | open |
| P05 | Stub server | Which stub server mocks the backend in L5 (and serves the backend suite where it calls external services), in which form (JAR, container, package, embedded), on which port, and which origins does the network guard allow? | Architecture | WireMock, standalone, port 8089, only the application origin and the stub server | | open |
| P06 | Database and infrastructure containers | Which engines and major versions, and which container tool? | Architecture, production | The production engine and version, Testcontainers or the compose definition | | open |
| P07 | Coverage thresholds | Which branch coverage applies to the business logic and the domain core, measured by which tool, with which exclusions? | Quality policy | 80 % and 90 %, the coverage tool of the platform | | open |
| P08 | Gherkin conventions | Which limits apply to steps, `Background`, `Examples` and timeouts; how are phases named; what do `@p0`, `@p1` and `@p2` mean; what is the pattern of requirement tags? | Process | Section 23.2; `@phase-<n>`; `@p0` blocks a release; `@req-<id>` | | open |
| P09 | Feature file language | In which natural language are feature files written? | Team | English | | open |
| P10 | Test identifier | Which attribute and which naming pattern identify elements for tests? | Interface contract | `data-testid`, `{feature}-{component}-{element}` | | open |
| P11 | Browsers, viewports and locales | Which engines, viewports and locales run in L5 and L6, and which engines in the smoke scope? | Markets, requirements | Chromium; one desktop and one mobile viewport; the source locale | | open |
| P12 | End-to-end package and managed environment | Which deployment definition starts the managed environment, which journeys are primary, and what is the name of the keep-alive switch? | Requirements, infrastructure | The compose definition of the project; the journeys named in the requirements; a switch named `E2E_KEEP_STACK` | | open |
| P13 | Retry and flake policy | How many retries, how long may a flaky test wait for repair, how large may the quarantine be, and how often are new scenarios repeated? | Quality policy | 0 retries (1 in L6), 14 days, 2 %, 5 repetitions | | open |
| P14 | Reporting | Which report tool, where is it published, who may read it, and how long are reports kept? | Team, governance | Allure over the Cucumber JSON results, an access-controlled static site, product owner as reader, 30 days | | open |
| P15 | Visual regression | Is comparison against baselines used, with which threshold and for which scenarios? | Interface contract | No; screenshot evidence for `@visual` scenarios only | | open |
| P16 | Accessibility checks | Which standard and which tool for automated checks? | Interface contract | WCAG 2.2 AA, axe-core | | open |
| P17 | Load and non-functional tests | Which tool, which thresholds, which tolerance, and are endurance tests needed? | Non-functional requirements | k6; thresholds from the requirements; 10 %; no endurance tests | | open |
| P18 | Contract tooling | Which tools validate stubs and contract fuzzing, and how often are recorded stubs of third-party services re-checked? | API contract | Stub validation in the pipeline; Schemathesis nightly; re-check at every release | | open |
| P19 | Traceability strictness | Which uncovered items fail the pipeline? | Quality policy | Items that the change adds | | open |
| P20 | Time budgets | Which time budgets apply to the stages and suites? | Pipeline | Section 23.2 and the table of section 18 | | open |
| P21 | Seed and demo data | What does the minimal start-up data contain, and is a demo seeder needed? | Requirements | Minimal reference data; no seeder | | open |
| P22 | External services and sandboxes | Which external services does the backend call, which have a sandbox, and how is each covered (stub in L3, sandbox in L6)? | Architecture | None; with none, the backend suite has no stub server (TST-INT-025) | | open |
| P23 | Repository layout | Where do the suites live, and what are the names of the architectural folders? | Frameworks, team | The layout of 20.2. The location of frontend unit and component tests is fixed by TST-CMP-014 and is not a parameter | | open |
| P24 | Parallelism and isolation | How many workers run, and how are stub servers and data isolated per worker? | Pipeline | One stub server per worker; unique data per scenario; workers as the runner supports | | open |
| P25 | Test commands | What are the commands that run each level, a tag selection and the whole suite? | Frameworks | One command per level (26.2) | | open |
| P26 | Mutation testing | Is it used on the domain core? | Quality policy | No | | open |
| P27 | Property-based testing | Is it used, and for which units? | Quality policy | No | | open |
| P28 | Exploratory testing | Who performs it, when, and how are findings recorded? | Process | Before each release, by a person other than the author; each finding becomes an automated test | | open |
| P29 | Pipeline system and runners | Which system and which runner resources run the pipeline? | Infrastructure | The system of the project's repository host; the resources the suites need | | open |
| P30 | Data cleanup strategy | How does L3 leave the database clean (truncation, rollback, non-colliding data, database per worker)? | Architecture | Non-colliding data and cleanup of owned tables after each scenario | | open |
| P31 | Target environments | Which environments does the end-to-end package target (local, development, test, staging, preview or others), with which mode, entry points, data prefix, run conditions, allow-list and deny-list, and in which of them does the post-deployment smoke run? | Infrastructure | Local and the pipeline in managed mode; no external profile | | open |
| P32 | Manual API collections | Are collections for manual API testing kept, and are they run in the pipeline? | Team | No | | open |

### 26.2 Level inventory

| Level | Tool | Location | Run command | Tag selection | Gate | Gherkin |
|---|---|---|---|---|---|---|
| L1 Unit | | | | | | no |
| L2 Component | | | | | | no |
| L3 Backend integration | | | | | | yes |
| L4 API contract | | | | | | no |
| L5 Frontend integration | | | | | | yes |
| L6 End-to-end, managed mode | | `tests/e2e` | | | | yes |
| L6 End-to-end, external mode | | `tests/e2e` | | | | yes |
| L7 Non-functional | | | | | | no |
| Report stage | | | | | | |

### 26.3 Tool selection record

| Purpose | Chosen tool and version | Alternatives considered | Reason | Maintenance status checked on | Decided by |
|---|---|---|---|---|---|
| Backend unit tests | | | | | |
| Backend Gherkin runner | | | | | |
| Entry point under the unit runner | | | | | |
| Scenario context and dependency injection | | | | | |
| HTTP driver and contract validation | | | | | |
| Real infrastructure (containers) | | | | | |
| Stub server for external services (only if the backend calls any) | | | | | |
| Frontend unit and component runner | | | | | |
| Browser automation tool | | | | | |
| L5 Gherkin runner | | | | | |
| L5 stub server (mocks the backend) | | | | | |
| L6 Gherkin runner | | | | | |
| Coverage tool | | | | | |
| Report tool | | | | | |
| Load-test tool | | | | | |

### 26.4 Tag catalogue

| Tag | Group | Meaning | Owner |
|---|---|---|---|
| `@phase-1` | Phase | | |
| `@edge-case` | Category | Valid input or state at or near a boundary, or unusual | |
| `@p0` | Priority | | |
| `@smoke` | Scope | | |
| `@<domain>` | Domain | | |

### 26.5 Environments and baselines

Target environments of the end-to-end package:

| Environment | Mode | Entry points | How it is deployed | Purpose | Deviations from production | Data policy and prefix | Run conditions | Owner |
|---|---|---|---|---|---|---|---|---|
| Local | managed | | | | | | | |
| Pipeline | managed | | | | | | | |
| Development | external | | | | | | | |
| Test | external | | | | | | | |

Baseline stubs of the frontend integration suite (TST-FIN-017):

| Stub | Operation | Reason |
|---|---|---|
| | | |

### 26.6 Project-specific rules

| ID | Type (mandatory test · forbidden pattern · rejection criterion · naming rule) | Statement | Ver. | Source |
|---|---|---|---|---|
| TST-PRJ-001 | | | | |
| TST-PRJ-002 | | | | |
| TST-PRJ-003 | | | | |

Rules added here are numbered `TST-PRJ-nnn`, follow the same statement form as sections 3 to 20, and extend the review gate (24.1).

### 26.7 Open questions log

| # | Question | Raised by | Answer | Date | Status |
|---|---|---|---|---|---|
| Q1 | Should L5 and L6 use Cucumber-JS with Playwright (assumed) or a generator such as playwright-bdd? | Agent | | 2026-10-11 | open |
| Q2 | Are authentication, role and `401`/`403` scenarios deferred to the final phase together with security (assumed), or does an earlier phase need role-dependent business behaviour? | Agent | | 2026-10-11 | open |
| Q3 | Are the coverage thresholds 80 % for business logic and 90 % for the domain core (assumed)? | Agent | | 2026-10-11 | open |
| Q4 | Is a combined Allure report on an access-controlled static site the wanted report (assumed)? | Agent | | 2026-10-11 | open |
| Q5 | Is screenshot evidence enough, with visual regression optional (assumed)? | Agent | | 2026-10-11 | open |
| Q6 | Should the shape check fail the pipeline on an inverted suite (assumed), or only report it? | Agent | | 2026-10-11 | open |
| Q7 | Do external services without a sandbox stay out of the end-to-end suite and use stubs in L3 only (assumed)? | Agent | | 2026-10-11 | open |
| Q8 | Are manual API collections and load tests optional (assumed)? | Agent | | 2026-10-11 | open |
| Q9 | Do L5 and L6 locate elements only by test identifier (assumed, stricter than component tests)? | Agent | | 2026-10-11 | open |
| Q10 | Is a read-only smoke run against production wanted? It is not allowed now (TST-CON-012, TST-E2E-035). | Agent | | 2026-10-11 | open |
