# CI/CD contract

| | |
|---|---|
| Document | `.claude/skills/ci-cd/ci-cd.md` |
| Version / status | 1.1, draft for review |
| Purpose | Defines the pipeline that builds, checks, tests, packages, deploys and verifies the product: the steps, the commands, the containers (layered and single-container), the remote deployment, the environments, the self-healing and the evidence. It is the source for creating the pipeline files of any provider. |
| Related | `workflow.md` (stages, loop, deploy targets, goals), `infrastructure.md` (hosts, environments, container definitions), `tests.md` (test kinds and pyramid), `backend.md`, `frontend.md`, `api.md`, `db.md` (the parts that are built), `team.md` and `agents.md` (owners and guardrails). |
| Owner | DevOps Engineer. Changes follow `workflow.md` 5.2 and the canary re-run (WFL-203). |
| Not covered | Security scans and observability (no-touch until all project phases are finished, `workflow.md` WFL-080, WFL-120). Their places in the pipeline are reserved (section 15). The hosts and the container contents are defined in `infrastructure.md`; this document uses them. |

Normative words: MUST, MUST NOT, SHOULD, MAY. Rule IDs are `CIC-nnn`.

## Contents and version

| Section | Content |
|---|---|
| 1 | Scope and definitions |
| 2 | Principles |
| 3 | Position in the workflow, pipeline levels and the MVP profile |
| 4 | Repository layout and the command interface |
| 5 | Triggers and branches |
| 6 | The steps (S01 to S24) |
| 7 | Containers: layered deployment and single-container deployment |
| 8 | Configuration and secrets |
| 9 | Remote deployment |
| 10 | Environments and promotion |
| 11 | Providers: GitHub, GitLab, Gitblit, Jenkins and others |
| 12 | Self-healing and self-learning in the pipeline |
| 13 | Reports, run record and evidence |
| 14 | Speed, caching and cost |
| 15 | Deferred steps (security, observability) |
| 16 | Quality gates and pipeline change |
| 17 | Edge cases |
| 18 | Restrictions |
| 19 | Parameters |
| 20 | Templates |
| 21 | Sources |

Version history: 1.1 MVP review: an explicit MVP profile with a fixed core, simplifications, extension triggers and a shortcut register (3.1 to 3.4); image transfer without a registry; job timeouts, log limits, run isolation and first-deployment rules; 22 new edge cases, 25 new rules, 16 new restrictions, 5 new parameters; 1.0 first version: provider-neutral commands, 24 steps, two container deployments, SSH-compose remote deployment as the baseline, templates for GitHub Actions, GitLab CI and Jenkins (for Gitblit and others).

## 1. Scope and definitions

| Term | Meaning |
|---|---|
| Pipeline | The automated sequence of steps that turns a change into a verified, deployed build. |
| Step | One unit of the sequence with an input, an output, a pass criterion and a command (section 6). |
| Command | A project script that does one step. All commands are called as `ci/run <command>` (section 4). |
| Wrapper | The small file of a provider (a workflow, a `.gitlab-ci.yml`, a `Jenkinsfile`) that only decides when and where commands run. It contains no logic of the project. |
| Provider | The system that runs the wrapper: GitHub Actions, GitLab CI, Jenkins (used with Gitblit and others), Gitea or Forgejo Actions, Azure DevOps, Bitbucket Pipelines, CircleCI, Woodpecker or Drone, or another. |
| Image set | The container images that one build produces. Two sets exist: the **layered set** (one image per layer) and the **single image** (the whole app in one image) (section 7). |
| Layered deployment | The product runs as several containers: database, backend, frontend (and optional helper containers). |
| Single-container deployment | The product runs as one container that holds the whole app: frontend, backend and database. |
| Environment | A place where a build runs: local, ephemeral (per pull request), dev, test, prod (the names and hosts are defined in `infrastructure.md`). |
| Run | One execution of the pipeline for one trigger. |
| Run record | The structured result of a run (13.2). |
| Build id | The commit id of the build. It names the images and is shown on the skeleton page (WFL-183). |
| Promotion | Moving the same build from one environment to the next (WFL-131). |

## 2. Principles

The principles come from current CI/CD practice (trunk-based flow, build once and promote, fast feedback, the test pyramid, pipeline as code, on-demand environments, measuring delivery) and from the uploaded projects (one pipeline for all containers, Docker Compose as the source of truth, images tagged with the commit, rollback by tag).

| ID | Rule |
|---|---|
| CIC-001 | **Build once, promote everywhere.** The pipeline builds the image set once per commit. The same images run locally, on ephemeral environments, on dev, test and prod. Only configuration changes between environments (WFL-131). |
| CIC-002 | **One command, same everywhere.** Every step is a command of the project. A developer, an agent and every provider run the same commands with the same result (WFL-125). |
| CIC-003 | **Thin wrappers.** The provider file decides triggers, order, runners, secrets, approvals and artifacts. It holds no build logic. Changing provider changes only the wrapper (WFL-176). |
| CIC-004 | **Pipeline as code.** The pipeline, its commands, container definitions and environment descriptions live in the repository, are reviewed like code, and change through stories (WFL-065). |
| CIC-005 | **Fail fast, report clearly.** Cheap steps run first. A failing step stops its path and names the step, the cause and the evidence. |
| CIC-006 | **Fast feedback.** A pull request run reaches its verdict within the budget of CP01. Slow work (full pyramid, journeys on test) runs after merge or in parallel. |
| CIC-007 | **Test pyramid.** Many unit tests, fewer integration and API tests, a few end-to-end tests (`tests.md`, WFL-042). |
| CIC-008 | **Immutable artifacts.** An image tagged with a commit id is never changed or re-tagged to other content. |
| CIC-009 | **Idempotent and repeatable.** Every step can run again on the same input with the same result; every deployment can be repeated. |
| CIC-010 | **Every environment from nothing.** An environment can be created from the repository and the secret store alone (WFL-132). |
| CIC-011 | **Trunk-based.** Short-lived branches, small changes, merge to the main branch often. A merged story is deployable (WFL-136). |
| CIC-012 | **Evidence by default.** Every run produces a run record and reports that become the evidence of the story (WFL-066, WFL-057). |
| CIC-013 | **No hidden state.** Nothing is configured by hand on a runner or a host. A manual change is a defect of the environment definition. |
| CIC-014 | **The pipeline serves the story.** Every run names its story and goal; a run without them is rejected (S02). |

## 3. Position in the workflow and pipeline levels

The pipeline is created in goal G-01 (technical setup) and runs from the end of G-01 for every story (`workflow.md` 2.1, 9.1). It grows in goal G-04 without stopping the flow.

| Level | Created in | Steps | Environments |
|---|---|---|---|
| **Level 1: minimal** | G-01 | S01 to S15, S21 to S23: prepare, checks, build, unit tests, images, ephemeral environment, migrate, seed, integration and end-to-end tests, smoke, report, publish, deploy to the first remote server, verify, clean up, notify | Local containers, ephemeral per pull request, one remote server |
| **Level 2: complete** | G-04 | All steps S01 to S24: adds promotion to test and prod, approval, full verification on test, rollback tests, scheduled runs, caching and parallelism tuning | dev, test, prod and others of `infrastructure.md` |

| ID | Rule |
|---|---|
| CIC-020 | The pipeline of Level 1, in the MVP profile of 3.1, is part of the G-01 gate (`workflow.md` WFL-075): green on a clean checkout by someone who did not build it, deployed to local containers and to one remote server, with the skeleton tests green on both. |
| CIC-021 | Level 2 extends Level 1. A step that exists in Level 1 keeps its command name and meaning (WFL-125). |
| CIC-022 | Until Level 2 exists, the steps of Level 2 are reported as "not available" in the run record; they are not skipped silently (WFL-130). |
| CIC-023 | The pipeline files are created by the DevOps Engineer from this document and from `infrastructure.md`, reviewed by the Reviewer, and tested with the skeleton story as the canary (WFL-203). |

### 3.1 The MVP profile

The pipeline of the MVP is the smallest one that lets every story be built, deployed, tested and rolled back the same way. It is Level 1 with the simplifications below. Everything else is a reserved extension with a trigger (3.3). The profile mirrors the MVP profile of `backend.md` 2.2: a fixed core, simplifications, and triggers to grow.

**Fixed core (always present, never simplified away):**

| # | Element | Rule |
|---|---|---|
| 1 | One provider and one wrapper | CIC-240 |
| 2 | The command interface `ci/run` with the commands of Level 1 | CIC-030, CIC-241 |
| 3 | One pull request run with the steps S01 to S12 on an ephemeral environment on the runner | CIC-042 |
| 4 | One merge run that publishes (or transfers) the image set, deploys to one remote server and verifies it | CIC-020 |
| 5 | Build once, immutable tag with the build id | CIC-001, CIC-008 |
| 6 | Health check, smoke check and automatic rollback to the previous build | CIC-124 |
| 7 | Secrets only from the provider's secret store, none in the repository | CIC-111 |
| 8 | A run record with the minimal fields | CIC-247 |
| 9 | Seed twice, migrate from empty on every run | WFL-182 |
| 10 | Approval of the PO before any production deployment, if a prod environment exists | CIC-142 |

**Simplifications at the MVP:**

| Topic | Full pipeline (Level 2) | MVP profile |
|---|---|---|
| Providers | One per project, a second only for migration | Exactly one, hosted runners |
| Commands | One script per command, documented | One script file with one function per command, or a Makefile; the names stay |
| Environments | local, ephemeral, dev, test, prod | local, ephemeral and one remote server (dev); test and prod come with G-04 or when a trigger fires |
| Image transport | A registry | The provider's built-in registry, or, for one host, the image transfer over SSH (CIC-242) |
| Single-container image | Built and tested on every run | Built on merge only; tested with the smoke check and the skeleton checks; the full suite runs on the layered set (CP11) |
| Parallel jobs and caching | Tuned | Sequential jobs; parallel and cache are added when the run is slower than CP01 (3.3) |
| Change filters (skip steps for document-only changes) | Allowed by the project's filter | None: every run is a full run (CIC-248) |
| Coverage | Gate of `tests.md` | Reported; the gate applies as far as `tests.md` sets it |
| Evidence block | Written and checked automatically | The pipeline writes the run record; the Lead checks the evidence block by hand (CIC-249) |
| Failure classification | Automated hints | Manual by the author or the Debug Engineer; the class is written into the run record |
| Flaky tests | Quarantine with automation | A list kept by the Tester with owner and end date (WFL-143) |
| Notifications | Chat or mail integration | The provider's default status and mail for failures on the main branch |
| Scheduled run | Daily | Off until G-04 (S24 reports "not available") |
| Promotion test, prod | S16 to S20 | Reported "not available" (CIC-022) until the environments exist |
| Rollback test | Once per epic on test | Once on the first remote server in G-01 (CIC-143) |
| Image and report retention | Scheduled clean-up | The provider's retention setting; a manual clean-up of the host with `prune` after a deployment (9.2) |
| Pipeline size | Not limited | The own scripts under `ci/` SHOULD stay small (CP14); a growing script is a signal to remove steps, not to add tools |

### 3.2 Tool budget of the pipeline

| ID | Rule |
|---|---|
| CIC-240 | The MVP uses one provider with hosted runners. A self-hosted runner, a second provider or a separate CI server is added only on a trigger of 3.3 (Gitblit needs an external CI server, so there the CI server is the provider). |
| CIC-241 | The pipeline needs only these tools beyond the stack's own build tool: a container engine with the compose plugin, an SSH client, a shell, and the end-to-end test runner of `tests.md`. A further tool needs a story and the Architect's decision (same spirit as `backend.md` 16.1). |
| CIC-242 | With one remote host, the pipeline MAY transfer the image set without a registry: save the images, copy them over SSH, load them on the host, compare the digests with those of the build. This keeps build once (CIC-001). A registry becomes required when a second host or environment is added (3.3). |
| CIC-243 | Provider marketplace actions and plugins are limited to the few that the wrapper needs (checkout, artifact upload, cache). Each is pinned to a version and listed in `ci/README.md`. |
| CIC-244 | No infrastructure-as-code tool, no orchestrator, no GitOps controller, no configuration-management tool at the MVP. The environment is described by compose files and environment files (9.1, 20.5). |
| CIC-245 | The pipeline's own scripts stay small and plain (CP14). Logic that a build tool already provides is called, not rewritten (`backend.md` 16.1 spirit). |
| CIC-246 | The MVP's remote server is one host. High availability, load balancing and multi-host deployment are extensions (3.3). |
| CIC-247 | The minimal run record has these fields: run id, trigger, story and goal, commit and build id, start and end time, result, per step the result and the reason, the images with digests, the deployments with their result, the healing actions. The other fields of 13.2 are added as their steps are added. |
| CIC-248 | At the MVP every run is a full run. A path filter or change detection is added only on the trigger "pull request run longer than CP01 after caching and parallel jobs". |
| CIC-249 | Until the evidence automation exists, the Lead checks the evidence block at stage 15 of `workflow.md`; the pipeline's run record is the proof for the last full run (WFL-066). |

### 3.3 Extension triggers

Each extension is a story with a trigger. The pipeline does not grow ahead of its trigger.

| # | Extension | Trigger |
|---|---|---|
| X1 | Parallel jobs | A pull request run is slower than CP01 |
| X2 | Dependency and layer caching | The same, or a repeated clean build is the main cost |
| X3 | Change filters | The same, after X1 and X2 |
| X4 | A registry (instead of image transfer) | A second host or environment, or an image larger than the transfer allows within the deployment time |
| X5 | Test and prod environments, promotion, approval | Goal G-04, or a real user needs a stable environment |
| X6 | Scheduled remote run | Goal G-04, or a defect was found that a scheduled run would have found |
| X7 | Automatic evidence check | More than one story per day needs it, or an evidence gap was found at an epic gate |
| X8 | Self-hosted runners | Hosted runner limits (time, memory, ports, cost) block a run |
| X9 | Review environments on a remote host | The PO needs to see a story before the merge |
| X10 | Zero-downtime release | A requirement of the PO on availability |
| X11 | An orchestrator or managed container service | One host cannot carry the load or the availability requirement |
| X12 | Dependency update automation | The dependency register has grown beyond a manual review |
| X13 | A second provider | A migration or a requirement of the owner of the repository |
| X14 | Security and monitoring steps | The dormant phase is opened (section 15) |

### 3.4 Shortcut register

A deliberate shortcut of the MVP profile (a simplification of 3.1 that the project keeps) is listed in `ci/README.md` with its reason and the trigger that ends it. The Auditor reads the register at every epic gate (`workflow.md` 10.3, "shortcut clearing").

| ID | Rule |
|---|---|
| CIC-250 | Every simplification the project actually uses is registered with the date, the reason and the trigger. An unregistered shortcut is a finding. |
| CIC-251 | A shortcut never removes an element of the fixed core (3.1). |

## 4. Repository layout and the command interface

The folders are a suggestion; `infrastructure.md` fixes the final names. The principle is fixed: commands in one place, container definitions in one place, provider wrappers where the provider needs them.

```text
project/
  ci/
    run                    entry point: ci/run <command> [arguments]
    commands/              one script per command (build, check, test-unit, ...)
    out/                   results of a run (reports, logs, run record); not committed
  infrastructure/          container definitions, compose files, environment descriptions
    compose.layers.yaml    layered deployment (database, backend, frontend, helpers)
    compose.single.yaml    single-container deployment
    env/                   environment descriptions without secrets (dev, test, prod)
  .github/workflows/       GitHub Actions wrapper
  .gitlab-ci.yml           GitLab CI wrapper
  Jenkinsfile              Jenkins wrapper (used with Gitblit and others)
```

### 4.1 Commands

All commands are `ci/run <command>`. Exit code 0 means the step passed. A command writes its results to `ci/out/<command>/` and prints a short summary. The way a command is implemented (shell, make, a task runner, the build tool of the stack) is a project choice; the names and the meaning are fixed.

| Command | Step | Does | Output |
|---|---|---|---|
| `prepare` | S01 | Checks or installs the pinned toolchains, restores caches, creates the run id | Run id, versions |
| `check-inputs` | S02 | Checks the story id and the goal, the commit message, the branch name | Verdict |
| `build` | S03 | Builds backend, frontend and the bundled API contract from a clean state | Build outputs |
| `check` | S04 | Format, lint, architecture test, contract lint, dependency register and budget, migration rules | Check report |
| `test-unit` | S05 | Unit tests of all areas | JUnit report, coverage |
| `images` | S06 | Builds the layered set and the single image, tags them with the build id | Image list |
| `env-up <mode>` | S07 | Starts an ephemeral environment from the images, `mode` is `layers` or `single` | Environment URL |
| `migrate` | S08 | Runs the migrations from empty or from the previous release | Migration log |
| `seed` | S08 | Loads seed data; a second run changes nothing (WFL-182) | Seed log |
| `test-integration` | S09 | Integration and API contract tests against the running environment | JUnit report |
| `test-e2e` | S10 | End-to-end tests of the journeys | JUnit report, screenshots |
| `smoke <target>` | S11, S15 | Health check and the skeleton check against a target (`local`, an environment name or a URL) | Verdict |
| `report` | S12 | Collects reports, logs and writes the run record | `ci/out/run-record.json` |
| `publish` | S13 | Pushes the image set to the registry with the build id (and the release tag) | Registry references |
| `deploy <env>` | S14, S16, S19 | Deploys the build id to an environment (section 9) | Deployment record |
| `verify <env>` | S15, S17 | Runs the smoke check and the journeys of that environment | Verdict, reports |
| `promote <from> <to>` | S16, S19 | Promotes the build id that is green on `<from>` to `<to>` | Promotion record |
| `rollback <env>` | S20 | Returns an environment to its previous build id and verifies it | Rollback record |
| `env-down` | S21 | Removes the ephemeral environment and its volumes | Verdict |
| `record` | S22 | Writes the evidence for the story (13.3) | Evidence entry |
| `all-local` | S01 to S12, S21 | The whole local run: the "one command" of the workflow (WFL-071) | All of the above |

| ID | Rule |
|---|---|
| CIC-030 | Commands MUST be callable from a clean checkout on a developer machine and on a runner, with the same arguments and the same result. A command that works only in one provider is a defect of the pipeline (WFL-E20). |
| CIC-031 | `ci/run all-local` is the single command of the local pipeline. It runs the steps in order, stops at the first failure, always runs `report` and `env-down`, and exits non-zero on any failure. |
| CIC-032 | A command takes its settings from environment variables and arguments, never from a file that is edited by hand (CIC-013). It prints which settings it uses, without secrets. |
| CIC-033 | A command fails with a clear message: step, cause, where the evidence is. It does not hide errors, truncate failures or return 0 after a failure. |
| CIC-034 | Toolchains and their versions are pinned in the repository (a version file or the build tool's own). `prepare` checks them and fails if they differ. |
| CIC-035 | Commands are small and documented in `ci/README.md` (inputs, outputs, exit codes). A new command is a story (WFL-065). |
| CIC-036 | The mapping from the abstract commands to the stack's tools is recorded in `infrastructure.md` or `ci/README.md`. Examples are in 4.2. |

### 4.2 Examples of the mapping to stacks

| Command | Java with Gradle or Maven | .NET | Node and TypeScript | Python | PHP (Laravel) |
|---|---|---|---|---|---|
| `build` | `./gradlew build -x test` or `./mvnw package -DskipTests` | `dotnet build -c Release` | `npm ci && npm run build` | `pip install -r requirements.txt` or `uv sync` | `composer install --no-dev` |
| `check` | Formatter, linter, architecture test | `dotnet format --verify-no-changes`, analyzers | `npm run lint`, `tsc --noEmit` | `ruff`, `mypy` | `pint --test`, `phpstan` |
| `test-unit` | `./gradlew test` | `dotnet test` | `npm test` | `pytest` | `php artisan test --testsuite=Unit` |
| `migrate` | Flyway or Liquibase one-shot | EF migrations bundle | the project's migration tool | Alembic or Django migrate | `php artisan migrate --force` |
| `images` | `docker build` per layer and for the single image | the same | the same | the same | the same |

The commands are examples: the stack contract (`backend.md` 19, `frontend.md`) decides the real tools.

## 5. Triggers and branches

| Trigger | When | Steps that run | Result |
|---|---|---|---|
| Pull request | A branch is pushed or updated with an open pull request | S01 to S12, S21, S22 (ephemeral environment, no remote deployment) | Verdict on the pull request; the Reviewer sees the evidence |
| Merge to the main branch | A pull request is merged | S01 to S15 (Level 1) or S01 to S17 (Level 2), S21, S22 | The build is published, deployed to dev (and test at Level 2) |
| Release tag | A tag `v<major>.<minor>.<patch>` on the main branch | The merge steps, then S18 to S19 (approval and prod) at Level 2 | A release build in prod after approval |
| Schedule | Daily (CP09) | S01, S15 or S17 against the first remote environment, S22 | The scheduled remote run of `workflow.md` 7.7 |
| Manual | A human starts it (promote, rollback, re-run) | The chosen commands | Recorded with the person who started it |

| ID | Rule |
|---|---|
| CIC-040 | The main branch is protected: a change enters only through a pull request with a green run and the Reviewer's approval (WFL-R04, TEAM-011). |
| CIC-041 | Branches are short-lived and named with the story id (for example `us-02.03-short-title`). A branch without a story id is rejected by S02. |
| CIC-042 | A pull request run MUST NOT deploy to a remote environment or use remote secrets. It uses the ephemeral environment on the runner (S07). |
| CIC-043 | The merge run deploys only the merged commit. Two merges queue; a newer merge may cancel an older run that has not started deploying (CIC-193). |
| CIC-044 | A tag build reuses the image of the commit it tags: no new build (CIC-001). The tag adds the release tag to the existing images. |
| CIC-045 | Release numbers follow semantic versioning. The release notes are generated from the story ids merged since the last tag and checked by the Technical Writer when active. |
| CIC-046 | The scheduled run uses the same commands as the pipeline and adds no monitoring system (WFL-231). |
| CIC-252 | Every job has a timeout (CP13). A hung job fails, is retried once if transient, and is reported. A run without timeouts is not accepted. |
| CIC-253 | Concurrent runs on one runner or host do not clash: the compose project name and the published ports of an ephemeral environment are derived from the run id; a fixed port is not used in a test. |
| CIC-254 | Runs started from a fork or from an untrusted source (an external contributor, an unreviewed branch of another repository) do not receive secrets and run only S01 to S05 (CIC-042, CIC-112). |
| CIC-255 | A run is cancelled when the pull request is closed or a newer commit arrives (CIC-193); the cleanup (S21) runs on cancellation. |
| CIC-047 | A manual run is allowed only for the actions named in the table and is recorded with the person and the reason. |

## 6. The steps

Each step has a number, a command, a pass criterion, a failure reaction and a level (1 = from G-01, 2 = from G-04). The order is the order of execution; steps that are independent run in parallel (CIC-100).

```text
S01 prepare ─► S02 inputs ─► S03 build ─► S04 checks ─► S05 unit tests ─► S06 images
                                                                              │
        ┌─────────────────────────────────────────────────────────────────────┘
        ▼
S07 env up ─► S08 migrate+seed ─► S09 integration ─► S10 e2e ─► S11 smoke ─► S12 report
                                                                              │
              (pull request stops here: S21 clean up, S22 notify)            │
        ┌─────────────────────────────────────────────────────────────────────┘
        ▼   merge to main / tag
S13 publish ─► S14 deploy dev ─► S15 verify dev ─► S16 promote test ─► S17 verify test
                                                                              │
                                                    S18 approval (PO) ◄───────┘
                                                            │
                                         S19 deploy prod ─► S20 verify prod / rollback
                                                            │
                                                    S21 clean up ─► S22 record ─► S23 notify
S24 scheduled run (separate trigger): verify the first remote environment
```

### 6.1 Prepare and validate

| Step | Command | Does | Passes when | On failure | Level |
|---|---|---|---|---|---|
| S01 Prepare | `prepare` | Checks out, pins the toolchain, restores caches, starts the run record with the run id, the commit id and the trigger | Toolchain versions match the pinned ones; caches restored or missing without error | Transient: retry (CIC-120). Version mismatch: stop, fix the pin | 1 |
| S02 Inputs | `check-inputs` | Extracts the story id and the goal from the branch, the commit or the pull request; checks that the story exists, is Ready or in work, and that the contract documents the change touches were updated in the same change | Story id present and valid; task spec referenced | Stop. The run is rejected with the missing item (CIC-014) | 1 |

### 6.2 Build and check

| Step | Command | Does | Passes when | On failure | Level |
|---|---|---|---|---|---|
| S03 Build | `build` | Builds backend, frontend and the bundled API contract from a clean state | No error and no new warning (`backend.md` QGT) | Stop; a build error goes to the owner of the area | 1 |
| S04 Static checks | `check` | Format, lint, the architecture test (layers, domain borders, no cycles), contract lint and consistency (the API description equals the code's published operations), the dependency register and the package budget (`backend.md` 16.1), migration rules (naming, order, backwards compatibility, one owner per table), language key completeness (`i18n.md`) | Every check passes | Stop; list the broken rules by ID | 1 |
| S05 Unit tests | `test-unit` | Runs the unit tests of backend and frontend in parallel | All pass; coverage not below the gate of `tests.md` | Classify (CIC-122); product defect goes to the owner | 1 |

### 6.3 Package

| Step | Command | Does | Passes when | On failure | Level |
|---|---|---|---|---|---|
| S06 Images | `images` | Builds the layered set (database, backend, frontend and the helper containers) and the single image from the same build outputs, tags each with the build id, adds labels (commit, story, date, run id) | All images build; the single image and the layered images contain the same application code (CIC-072) | Stop | 1 |

### 6.4 Ephemeral environment and tests

| Step | Command | Does | Passes when | On failure | Level |
|---|---|---|---|---|---|
| S07 Environment up | `env-up layers`, `env-up single` | Starts the product from the images on the runner: as layers on every run; as the single container on merge runs only in the MVP profile (3.1, CP11) | Health checks green within CP03 | Retry once, then rebuild the environment (CIC-121), then stop | 1 |
| S08 Migrate and seed | `migrate`, `seed` | Runs the migrations from empty (and from the previous release on the merge run), loads the seed data, runs the seed again | Both pass; the second seed changes nothing | Stop; a database defect goes to the Database Engineer | 1 |
| S09 Integration tests | `test-integration` | Integration and API contract tests against the running environment | All pass | Classify (CIC-122) | 1 |
| S10 End-to-end tests | `test-e2e` | The journeys of the story and the standing journeys, including the error path of the skeleton (WFL-181) | All pass | Classify; a flaky test follows CIC-123 | 1 |
| S11 Smoke | `smoke local` | Health, version equals the build id, skeleton page and API answer (WFL-183) | Verdict green | Stop | 1 |
| S12 Report | `report` | Collects reports, logs and screenshots; writes the run record; always runs | Files written | Does not fail the run on its own; the missing report is itself a finding | 1 |

### 6.5 Publish and deploy

| Step | Command | Does | Passes when | On failure | Level |
|---|---|---|---|---|---|
| S13 Publish | `publish` | Pushes the image set to the registry with the build id (and the release tag on a tag run), or, in the MVP profile with one host, saves the images for the transfer (CIC-242); records the digests | Images are in the registry (or ready for transfer), digests recorded | Retry (CIC-120) | 1 |
| S14 Deploy dev | `deploy dev` | Deploys the build id to the first remote environment (section 9) | Health check green after the deploy | Roll back automatically (CIC-124) | 1 |
| S15 Verify dev | `verify dev` | Smoke check and the journeys of the story against dev | Verdict green | Roll back (CIC-124); open a defect story | 1 |
| S16 Promote test | `promote dev test` | Promotes the same build id to test (no rebuild) | Same digests on test | Stop | 2 |
| S17 Verify test | `verify test` | The full pyramid where it applies, the cross-domain journeys, the rollback test (once per epic) | Verdict green | Roll back; open a defect story | 2 |
| S18 Approval | (provider gate) | The PO approves the promotion to prod (WFL-133) | A recorded approval (WFL-260) | Wait; no timeout deploys by itself | 2 |
| S19 Deploy prod | `promote test prod` | Promotes the build id that is green on test; takes a data backup first; applies the migrations as one-shot; starts the new containers | Health check green | Roll back automatically (CIC-124) | 2 |
| S20 Verify prod | `verify prod` | Smoke check and a minimal journey set that changes no real data | Verdict green | Roll back; incident story | 2 |

### 6.6 Finish

| Step | Command | Does | Passes when | On failure | Level |
|---|---|---|---|---|---|
| S21 Clean up | `env-down` | Removes ephemeral environments, containers, networks and volumes of the run | Nothing left behind | Report; a leftover is cleaned by the next run | 1 |
| S22 Record | `record` | Writes the evidence for the story: steps, results, loops, digests (13.3) | Evidence entry created | Report | 1 |
| S23 Notify | (provider) | Tells the Lead and the story owner the result: verdict, failing step, link to the evidence | Message sent | Report | 1 |
| S24 Scheduled run | `verify <first remote env>` | The daily check of the deployed product with the standing journeys (WFL-230) | Verdict green | A defect story with priority (WFL-230) | 2 |

| ID | Rule |
|---|---|
| CIC-100 | Independent steps run in parallel: backend and frontend builds, backend and frontend unit tests, the layered and the single environment checks. A step runs only after the steps it depends on have passed. |
| CIC-101 | A failing step stops the dependent steps. The run still runs `report`, `env-down` and `record`. |
| CIC-102 | Every step writes a start time, an end time, a result (passed, failed, skipped, not available) and a reason for skipped or not available into the run record. |
| CIC-103 | A step may be "skipped" only by a rule of this document (for example S13 on a pull request). A skip by hand is not allowed (WFL-R05). |
| CIC-104 | The step order is fixed. A project may add steps of its own between existing ones (for example a licence check) only through a story and with the same rules; it does not remove or reorder the steps of this document. |
| CIC-105 | A step uses the artifacts of earlier steps of the same run, not a fresh build: S06 uses the outputs of S03, S07 uses the images of S06, S13 publishes the images of S06 (CIC-001). |
| CIC-106 | The pipeline is the same for backend, frontend and the API contract: one run, one build id (WFL-170). |

## 7. Containers

`infrastructure.md` defines the container contents in detail. This section fixes what the pipeline builds, how it names it and what it requires from each deployment mode.

### 7.1 Layered deployment (one container per layer)

| Container | Contains | Port (default) | Health | Volume |
|---|---|---|---|---|
| `database` | The database engine of `db.md` (one engine, one schema per domain) | 5432 or the engine's port, inside the network only | The engine's ready check | Data directory |
| `backend` | The backend runtime and the application; started in the process role `web`; the roles `worker` and `migrate` are started from the same image (`backend.md` 3.1) | 8080 | `GET /health` (liveness and readiness) | none |
| `frontend` | The built frontend and a web server; passes `/api` to the backend | 80 (published) | `GET /` returns the page | none |
| `helpers` (optional) | A one-shot `migrate` container; a seed container; an API documentation or mock container when `api.md` asks for it | none | Exit code | none |

| ID | Rule |
|---|---|
| CIC-060 | The layered set has one image per layer and one compose definition (`infrastructure/compose.layers.yaml`). The compose definition is the source of truth for the layers (uploaded projects, `workflow.md` 9.2). |
| CIC-061 | Containers start in dependency order using health checks, not fixed waits: database, migrate (one-shot), backend, frontend. The frontend serves traffic only when the backend is healthy. |
| CIC-062 | Only the frontend port is published to the host. The database and the backend are reachable inside the network (and, for tests, on the runner's network). |
| CIC-063 | Containers are configured only by environment variables (section 8). Images contain no environment-specific values. |
| CIC-064 | Restart policy `unless-stopped` and health checks give basic self-recovery on remote hosts (`workflow.md` 7.4). |
| CIC-065 | A database volume is named and persistent on remote environments. On ephemeral environments it is removed with the environment (S21). |

### 7.2 Single-container deployment (the whole app in one container)

| Part inside the container | Role |
|---|---|
| Web server | Serves the built frontend and passes `/api` to the backend process |
| Backend process | The application in the role `web`; runs the migrations at start in the role `migrate` before it accepts traffic |
| Database | The same database engine as the layered deployment, started inside the container, with its data in the volume `/data` |
| Process supervisor | A small init or supervisor that starts the parts in order (database, migrate, backend, web server), restarts a failed part, stops them in reverse order, and makes the container exit when a critical part fails |

| ID | Rule |
|---|---|
| CIC-070 | The single image is built from the same build outputs as the layered images (CIC-105). It adds the supervisor and the database; it does not add or change application code (CIC-072). |
| CIC-071 | The single container publishes one port (the web server). The health endpoint reports ready only when the database, the migration and the backend are ready. |
| CIC-072 | The single container and the layered set run the same application code, the same migrations and the same seed. A behaviour that differs between them is a defect of the images (WFL-E20). |
| CIC-073 | The database engine in the single image is the engine of `db.md`. A different embedded engine for convenience is not allowed, because it hides engine-specific defects. |
| CIC-074 | Data lives in the volume `/data`. A single container without a volume is allowed only for ephemeral environments and demos. |
| CIC-075 | The single container is for: a fast remote demo, a test or review environment, a small installation, and the all-in-one smoke test. It is not a replacement for the layered deployment where layers must be scaled or updated separately (`backend.md` 17, 18). |
| CIC-076 | The container stops with a non-zero exit code when a critical part cannot start, so that the restart policy and the health check can react. It does not stay up in a half-started state. |
| CIC-077 | The pipeline tests the single image with the same smoke check and the skeleton checks as the layered set (S07, S11), and with the full end-to-end suite on merge (CP11). |

### 7.3 Choosing the mode per environment

| Environment | Default mode | Why |
|---|---|---|
| Ephemeral (pull request) | Layers for the full tests; single for the smoke check | Layers show the real structure; single is cheap and proves the all-in-one image |
| Local developer run | Layers (single optional) | Closest to production |
| Remote dev | As `infrastructure.md` defines; layers or single | The first remote server may be a single small host |
| Remote test | Layers | Full verification and cross-domain journeys |
| Remote prod | Layers, or single for a small installation | As `infrastructure.md` defines |

| ID | Rule |
|---|---|
| CIC-080 | `infrastructure.md` names the mode per environment. The pipeline supports both modes with the same commands; the mode is a parameter of `env-up` and `deploy`. |
| CIC-081 | An environment keeps its mode for its lifetime. A change of mode is a migration with a data path (a dump and restore) and a story. |

### 7.4 Image rules

| ID | Rule |
|---|---|
| CIC-090 | Images are built in stages: a build stage with the toolchain, a runtime stage with only what runs. The runtime image holds no compiler, no test code and no source. |
| CIC-091 | Base images are pinned to a version, not `latest`. The base images are listed in the dependency register. |
| CIC-092 | Each image has labels: commit id, story id, build date, run id, source repository. |
| CIC-093 | A `.dockerignore` excludes everything not needed (VCS data, local environment files, reports). |
| CIC-094 | Tags: `<build id>` on every image, always and immutable (CIC-008); `<major>.<minor>.<patch>` on a release tag; `dev`, `test`, `prod` are moving pointers kept in the environment description, not as image tags that mean different content over time. |
| CIC-095 | Layer caching is used for speed (CP08) and never changes the result: a clean build must produce an equal image. |
| CIC-096 | The image registry is named in `infrastructure.md`. The pipeline authenticates with a secret from the secret store (section 8). |
| CIC-097 | Image retention: the images of the last releases and of the last builds per environment are kept (CP06); older ones are removed by a scheduled job. An image that an environment runs is never removed. |

## 8. Configuration and secrets

| ID | Rule |
|---|---|
| CIC-110 | All configuration comes from environment variables (twelve-factor style). `infrastructure/env/<env>` holds the non-secret values per environment (names, ports, the mode, the host); secrets come from the provider's secret store or the host's secret mechanism. |
| CIC-111 | No secret, credential or personal data is written to the repository, an image, a log or a report (AGT-043, `backend.md` correctness rules). The pipeline masks secret values in logs. |
| CIC-112 | Secrets are scoped to an environment: the secret of prod is not available to pull request runs or to dev runs (CIC-042). |
| CIC-113 | The remote credentials (SSH key, registry token) are created by a human once and are not visible to agents (WFL-171). They have the least rights needed: the deploy user can run the deployment commands in its directory and nothing else. |
| CIC-114 | The names of the required variables and secrets are listed in `ci/README.md` and in `infrastructure.md`. A missing variable fails S01 with its name. |
| CIC-115 | Seed data has two sets: reference data (needed everywhere, loaded in every environment) and demonstration data (loaded in ephemeral, dev and test only). Production receives only reference data and a human-approved import (`db.md`). |

Standard variables (names are fixed here, values come from the environment):

| Variable | Meaning |
|---|---|
| `APP_ENV` | `local`, `ephemeral`, `dev`, `test`, `prod` |
| `APP_MODE` | `layers` or `single` |
| `BUILD_ID` | Commit id of the build (also shown on the page) |
| `IMAGE_REGISTRY`, `IMAGE_PREFIX` | Registry and name prefix |
| `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER`, `DB_PASSWORD` | Database connection (secret: the password) |
| `API_BASE_URL` | Where the frontend finds the API (usually `/api`) |
| `PUBLIC_URL` | The public address of the environment |
| `SEED_SET` | `reference` or `reference+demo` |

## 9. Remote deployment

The baseline works on any host that can run containers and is reachable over SSH: it is portable across every provider. Provider-native deployment is allowed as an alternative (9.3) if it follows the same algorithm.

### 9.1 Remote host requirements

| Item | Requirement |
|---|---|
| Runtime | A container engine with the compose plugin |
| Access | A deploy user reachable over SSH with a key; no password login for the deploy user |
| Directory | `/opt/<product>/<env>/` holding `compose.yaml`, `env` (non-secret values), `.env.secret` (written at deploy from the secret store, mode 600), `releases.log` (the list of deployed build ids), `backups/` (prod) |
| Network | The published port reachable from users (and from the runner for the smoke check) |
| Registry | The host can pull from the registry with a read-only token |
| Time | Synchronised clock (for logs and expiry) |

### 9.2 The deployment algorithm (`deploy <env>`)

| # | Action | Rule |
|---|---|---|
| 1 | Take the environment lock (one deployment per environment at a time) | CIC-130 |
| 2 | Check the target: reachable, free disk space, the engine running, the registry login works | Fails fast with a clear message |
| 3 | Check that the images exist in the registry for the build id, and that the digests equal those recorded in S13 (or of the source environment for a promotion) | CIC-001 |
| 4 | Record the current build id as `previous` in `releases.log` | Needed for rollback |
| 5 | In prod: take a backup of the database; verify the backup file | CIC-135 |
| 6 | Write the environment files from the description and the secret store | CIC-110 |
| 7 | Pull the images | |
| 8 | Run the migration one-shot (`migrate` role) and wait for it to finish | CIC-132 |
| 9 | Start or recreate the containers with the new build id (layered: database untouched; single: the container is recreated, `/data` kept) | CIC-133 |
| 10 | Wait for the health checks within CP03 | |
| 11 | Run `smoke <env>` | S15 |
| 12 | On success: record the build id as `current`, remove unused images beyond CP07, release the lock. On failure: `rollback` (CIC-124) and release the lock | WFL-141 |

| ID | Rule |
|---|---|
| CIC-130 | One deployment per environment at a time, enforced by the provider's concurrency setting and by a lock on the host. A second deployment waits or is cancelled; it never runs in parallel. |
| CIC-131 | The deployment script is the project's own command (`ci/run deploy`), run on the runner and acting on the host over SSH; it is not copied by hand to a host and edited there. |
| CIC-132 | Database changes follow expand and contract (WFL-135): the new migrations are compatible with the previous build, so that a rollback of the containers is possible without undoing the schema. A migration that is not compatible needs a story, a backup and a decision of the Architect. |
| CIC-133 | The layered database container is not recreated by a normal deployment. It is changed only through a story that names the engine change and its data path. |
| CIC-134 | Deployment is idempotent: running `deploy <env> <build id>` again for the same build id changes nothing. |
| CIC-135 | Before a production deployment with a migration, a database backup is taken and checked. The backup is kept per CP12. This is data safety, not a security feature. |
| CIC-136 | The deployment writes a record: environment, build id, previous build id, digests, time, run id, who approved (for prod). |
| CIC-137 | A deployment that cannot reach the host, the registry or the lock fails the step and reports the cause. It never deploys partly: either the new containers run healthy or the previous build runs. |
| CIC-256 | The first deployment to a host has no previous build. It is a bootstrap: the host is prepared (9.1), the first build id is deployed, the health and smoke checks run, and on failure the new containers are removed and the failure reported; there is nothing to roll back to. A bootstrap is a story and is recorded as such. |
| CIC-257 | Containers have log limits (a maximum size and number of files, set in the compose definition) so that logs cannot fill the disk. This is host hygiene, not observability (section 15). |
| CIC-258 | A volume is never removed by a remote deployment or a clean-up. The flags that remove volumes (`down -v`, `volume prune`) are not used on a remote host. Only ephemeral environments on the runner are removed with their volumes (S21). |
| CIC-259 | The pipeline tests the upgrade path (migrations from the previous release) when the first release exists; before that, from empty on every run (WFL-182). |
| CIC-260 | The host's free disk space is checked before a deployment (9.2 step 2) and the images beyond CP07 are pruned after it, so that the MVP host does not fill up. |
| CIC-138 | Zero-downtime strategies (blue-green, canary) are deferred (section 15). The baseline is a short recreate behind the health check. |

### 9.3 Alternatives to SSH and compose

| Method | When | Condition |
|---|---|---|
| Provider-native deployment (environments, deployment jobs, release gates) | The provider offers it | Calls the same `ci/run deploy` and keeps the algorithm of 9.2 |
| Orchestrator (Kubernetes with Helm or a GitOps controller) | The project outgrows one host | A story and an architecture decision; the build once rule and the same commands hold; this is an extension (`backend.md` 17, 18) |
| Managed container service of a cloud | The hosting is chosen so | The same image, the same configuration variables, the same health check and smoke check |

## 10. Environments and promotion

| Environment | Created | Mode | Image source | Seed | Gate to the next |
|---|---|---|---|---|---|
| Local | By `ci/run all-local` | Layers (single optional) | Built locally | Reference + demo | Green local run |
| Ephemeral | Per pull request run | Layers and single | Built in the run | Reference + demo | Verdict on the pull request |
| Dev | Merge to main | `infrastructure.md` | The published build id | Reference + demo | S15 green |
| Test | Promotion from dev | Layers | The same build id | Reference + demo | S17 green; the Auditor's sign-off at an epic gate |
| Prod | Promotion from test after approval | `infrastructure.md` | The same build id | Reference only | S20 green; rollback ready |

| ID | Rule |
|---|---|
| CIC-140 | Promotion order is dev, test, prod. An environment cannot be skipped unless `infrastructure.md` does not define it; then the run says "not available" (CIC-022). |
| CIC-141 | A build id that failed on an environment is not promoted. It is marked bad in the deployment record; a fix is a new commit and a new build id. |
| CIC-142 | The approval of S18 is recorded by the provider's environment protection or approval feature with the approver's name (WFL-260). The pipeline does not deploy to prod without it. |
| CIC-143 | The rollback is tested on test (S17) once per epic, and on the first remote server in G-01 as far as Level 1 supports it (WFL-134). |
| CIC-144 | Environment definitions are code in `infrastructure/env/` and reviewed; secrets are not part of them. |

## 11. Providers

The pipeline runs on any provider that can run the commands of section 4. The mapping shows what each concept is called; the templates in section 20 give working skeletons.

| Concept | GitHub Actions | GitLab CI | Jenkins (also used with Gitblit) | Others |
|---|---|---|---|---|
| Wrapper file | `.github/workflows/ci.yml` | `.gitlab-ci.yml` | `Jenkinsfile` | Gitea and Forgejo Actions: the same syntax as GitHub Actions in `.gitea/workflows/` or `.forgejo/workflows/`; Azure DevOps: `azure-pipelines.yml`; Bitbucket: `bitbucket-pipelines.yml`; CircleCI: `.circleci/config.yml`; Woodpecker and Drone: `.woodpecker.yml` or `.drone.yml` |
| Trigger | `on:` (pull_request, push, tags, schedule, workflow_dispatch) | `rules:` (merge request, branch, tag, schedule, web) | Multibranch pipeline with webhook, `triggers`, `cron` | Equivalent triggers |
| Runner | `runs-on` | `tags` and runners | `agent` | Agents or runners with a container engine |
| Secrets | Repository and environment secrets | CI/CD variables (masked, protected) | Credentials store | The provider's secret store |
| Environments and approvals | `environment:` with required reviewers | `environment:` and `when: manual` with protected environments | `input` step with submitters | The provider's approval gate |
| Concurrency | `concurrency:` | `resource_group:` | `lock` (Lockable Resources plugin) or `disableConcurrentBuilds` | The provider's equivalent |
| Artifacts and reports | `upload-artifact` | `artifacts:` with `reports:` | `archiveArtifacts`, `junit` | The provider's equivalent |
| Cache | `actions/cache` or the setup action's cache | `cache:` | Workspace or plugin cache | The provider's equivalent |
| Registry | GitHub Container Registry or any | GitLab Container Registry or any | Any registry | Any registry |

### 11.1 Gitblit

Gitblit is a Git server without a built-in CI engine. It is used with an external CI server.

| ID | Rule |
|---|---|
| CIC-150 | With Gitblit, the pipeline runs on an external CI server (Jenkins is the supported default; Woodpecker, Drone or another may be used). The wrapper is the `Jenkinsfile` (or the CI server's file). |
| CIC-151 | Gitblit triggers the CI server on a push through a post-receive hook or a webhook. The Gitblit distribution includes a sample Groovy hook for Jenkins that calls the Jenkins `notifyCommit` URL for the repository; it is enabled through `groovy.postReceiveScripts` or per repository, with `groovy.jenkinsServer` and `groovy.jenkinsGitbaseurl` set. The CI server then fetches the repository and starts the run. |
| CIC-152 | Pull request equivalents depend on the Gitblit workflow (tickets and patchsets). The pipeline treats a pushed ticket branch or patchset ref like a pull request: S01 to S12, S21, S22 without remote deployment (CIC-042). The branch protection and the Reviewer's approval are enforced by the Gitblit permissions and by the Lead (CIC-040). |
| CIC-153 | Status is reported back to the story and the Lead through the CI server's notification (S23), because Gitblit has no check status of its own. |
| CIC-154 | The CI server holds the secrets and the runners; the Gitblit server holds only the code. |

### 11.2 Provider rules

| ID | Rule |
|---|---|
| CIC-160 | One wrapper per provider that the project uses. Two providers at once are allowed only during a migration, with a story and a date to remove one (WFL-E19). |
| CIC-161 | A wrapper contains: triggers, the order of the steps, parallel groups, runners, caching, secrets, environments, approvals, concurrency, artifact upload and notifications. It contains no build, test or deployment logic. |
| CIC-162 | Provider features that cannot be reproduced on another provider (a marketplace action, a plugin) are avoided in the wrapper or are wrapped by a command, so that the move to another provider stays a change of the wrapper only. |
| CIC-163 | Runners need a container engine. The runner image or the runner host provides the pinned toolchains (CIC-034) or `prepare` installs them. |
| CIC-164 | The wrappers are generated from the templates of section 20 and reviewed. A project keeps the wrapper close to the template so that improvements to the template can be applied. |

## 12. Self-healing and self-learning in the pipeline

This section applies `workflow.md` 7.4 and 7.5 to the pipeline.

### 12.1 Self-healing

| ID | Rule |
|---|---|
| CIC-120 | A transient failure (network, registry, a runner problem, a container not yet ready) is retried with waiting time, up to CP02 times (WP08). Each retry is recorded. |
| CIC-121 | A broken environment (a dirty state, a failed migration on an ephemeral or test environment) is removed and rebuilt from nothing, once per run. |
| CIC-122 | A failing test is classified from its evidence: product defect (goes to the owner, with logs and the reproduction), test defect (goes to the Tester), flaky (the same test passes and fails on the same commit), or environment (goes to DevOps). The class is recorded. |
| CIC-123 | A flaky test is rerun at most CP02 times; if it passes it is reported as flaky and quarantined with a story, an owner and an end date (WFL-143). A quarantined test still runs; it does not block the run until its end date. |
| CIC-124 | A deployment that fails its health check or its smoke check is rolled back to the previous build id automatically, and the rollback is verified (WFL-141). This holds for prod, too. |
| CIC-125 | Healing never edits tests, gates, contracts, requirements or settings of the pipeline and never deletes data on a shared or prod environment (WFL-142). |
| CIC-126 | Every automatic reaction is recorded in the run record with the failure, the reaction and the result (WFL-140). |
| CIC-127 | The same failure after a fix counts as a loop (WFL-064). After three full loops the story stops and goes to the Lead and the Architect. |

### 12.2 Self-learning

| ID | Rule |
|---|---|
| CIC-170 | The run record carries the data that the learning loop needs (13.2): failures by step and cause, retries, loops, durations, flaky tests. The Auditor reads them at every epic gate (WFL-250). |
| CIC-171 | A pattern (the same cause twice) is answered first by an automated check: a new `check` rule, a new test, a new smoke assertion or a new step guard. The change is a story, goes through review, and is tested with the canary re-run (WFL-150, WFL-203). |
| CIC-172 | The pipeline's own failures (a step that was slow, flaky or wrong) are findings like any other and enter the same loop. |
| CIC-173 | A learned check is versioned with the pipeline. It is removed only by a story that records why. |

## 13. Reports, run record and evidence

### 13.1 Reports

| Report | Format | Source step |
|---|---|---|
| Unit, integration and end-to-end results | JUnit XML | S05, S09, S10 |
| Coverage | The coverage tool's standard format plus a summary | S05 |
| Static check results | A list of broken rules by ID | S04 |
| Screenshots and traces of end-to-end failures | Images and traces | S10 |
| Container and deployment logs | Text | S07, S14, S19 |
| Migration and seed logs | Text | S08 |
| Smoke results | Text and JSON | S11, S15, S20 |

### 13.2 Run record

One JSON file per run, `ci/out/run-record.json`, stored as a run artifact and attached to the story.

```json
{
  "run_id": "string",
  "trigger": "pull_request | merge | tag | schedule | manual",
  "story": "US-02.03",
  "goal": "G-02",
  "epic": "E-02",
  "commit": "string",
  "build_id": "string",
  "started": "timestamp",
  "finished": "timestamp",
  "result": "passed | failed | cancelled",
  "steps": [
    { "id": "S05", "command": "test-unit", "result": "passed | failed | skipped | not_available",
      "reason": "string", "started": "timestamp", "finished": "timestamp",
      "retries": 0, "failure_class": "product | test | flaky | environment | none" }
  ],
  "images": [ { "name": "string", "tag": "string", "digest": "string", "mode": "layers | single" } ],
  "deployments": [ { "env": "dev", "build_id": "string", "previous": "string", "result": "passed | rolled_back" } ],
  "healing": [ { "step": "S08", "failure": "string", "reaction": "string", "result": "string" } ],
  "approvals": [ { "env": "prod", "approver": "string", "time": "timestamp" } ]
}
```

### 13.3 Evidence for the story

| ID | Rule |
|---|---|
| CIC-180 | The pipeline writes the story's evidence block (`workflow.md` 17.3) from the run record: the criterion to test mapping (from the test names), the last full run, the loops needed, the findings closed, the merged pull request. |
| CIC-181 | The pipeline checks that the evidence block is complete before the story can be closed: team verdicts present, criteria mapped to tests, last full run green on the merged state (WFL-057). A missing item fails the run's final check. |
| CIC-182 | Reports and run records are kept per CP05 and linked from the story. They contain no secrets or personal data (CIC-111). |
| CIC-183 | The run record also feeds the process metrics of `workflow.md` 7.9: loops, first-pass green rate, findings by cause, lead time, quarantined tests. |

## 14. Speed, caching and cost

| ID | Rule |
|---|---|
| CIC-190 | The pull request run aims at CP01 minutes. If it is slower, the cause is a finding and the Lead plans a story to fix it (CIC-172). |
| CIC-191 | Dependencies and build layers are cached with keys that include the lock files and the pinned toolchain versions (CP08). A cache never changes a result (CIC-095). |
| CIC-192 | Tests are split and run in parallel by area and by kind. Slow end-to-end tests run on merge and on schedule; a pull request runs the journeys of its story and the smoke set (CP11). |
| CIC-193 | A run that has been superseded by a newer commit on the same pull request is cancelled (not the deployment of a merge, CIC-043). |
| CIC-194 | Ephemeral environments are always removed (S21), also after a failure or a cancellation, and orphaned ones are removed by a scheduled clean-up. |
| CIC-261 | Notifications go to the people who can act: the author and the Lead for a pull request run; the Lead and DevOps for a failure on the main branch; the PO for a prod approval. A recovery is notified once. There is no notification for every success. |
| CIC-262 | Cost is watched. The run minutes and the storage of artifacts and images are checked at each epic gate (CP15) and the Auditor includes them in the report. |
| CIC-195 | Runner size is chosen to the load. Resource limits are set on containers so that a test cannot starve the runner. |

## 15. Deferred steps (reserved places)

Security and observability are no-touch until all project phases are finished (`workflow.md` WFL-080, WFL-120). The places for later steps are reserved so that the order of the existing steps does not change when they are added.

| Reserved place | Later step | Between |
|---|---|---|
| S04a | Static security analysis, secret scanning, dependency vulnerability scan | S04 and S05 |
| S06a | Image scan, software bill of materials, image signing | S06 and S07 |
| S10a | Dynamic security tests | S10 and S11 |
| S15a, S20a | Post-deploy monitoring checks, alert rules | After S15 and S20 |
| S19a | Zero-downtime release (blue-green, canary), feature flags | Around S19 |

| ID | Rule |
|---|---|
| CIC-200 | No agent implements a reserved step before the phase is opened (WFL-082). The slots are documented here, and the run record lists them as "deferred". |
| CIC-201 | The correctness rules stay active: no secrets in the repository or logs (CIC-111), the dependency register and package budget (S04), pinned base images (CIC-091). They are not security features. |
| CIC-202 | When the phase is opened, each reserved step becomes a story, with the same rules as any other step (commands, run record, canary re-run). |

## 16. Quality gates and pipeline change

| Gate | Where | Rule |
|---|---|---|
| Merge protection | Main branch | A green pull request run and the Reviewer's approval are required (CIC-040) |
| Required checks | Pull request | S02, S03, S04, S05, S08, S09, S10, S11 pass |
| Deployable story | Merge run | S13 to S15 pass at Level 1 (to S17 at Level 2) |
| Production gate | Release | S17 green, the recorded approval of the PO (S18), a ready rollback |
| Evidence gate | Story close | The evidence block is complete (CIC-181) |
| Epic gate | Epic | The Auditor's report; the run records of the epic are consistent with the story evidence |

| ID | Rule |
|---|---|
| CIC-210 | A change to the pipeline (a command, a wrapper, a container definition, an environment description) is a technical story with a persona and a value, follows the same stages and team verification, and is reviewed (WFL-065). |
| CIC-211 | After such a change the canary re-run is required: the skeleton story runs through the whole workflow, green locally and on the first remote server (WFL-203). |
| CIC-212 | A change that makes a step slower, weaker or optional is rejected unless the Architect and the PO accept it in writing. |
| CIC-213 | The pipeline is owned by the DevOps Engineer. The whole team shares its health: a red pipeline on the main branch stops new merges until fixed (CIC-214). |
| CIC-214 | A red main branch is the first priority of the team. The cause is fixed, or the offending change is reverted, before any other merge. |
| CIC-263 | A pipeline story at the MVP asks first "can the step be removed or made simpler?" before it adds a tool or a step (KISS inside the pipeline). |
| CIC-264 | Pipeline documentation is one file, `ci/README.md`: the commands, the variables and secrets, the shortcut register, the template version, and the manual recovery steps of the host. It is kept current by the DevOps Engineer. |
| CIC-215 | Pipeline templates are versioned. A project records the template version it uses in `ci/README.md`. |

## 17. Edge cases

| ID | Situation | Handling |
|---|---|---|
| CIC-E01 | The remote server is not reachable during S14. | Retry (CIC-120). If it stays unreachable, the step fails with the cause; the previous build keeps running; the story is not green (WFL-060). |
| CIC-E02 | The health check passes but the smoke check fails. | A failed deployment: roll back (CIC-124), open a defect story. |
| CIC-E03 | The rollback itself fails. | Stop all deployments to that environment, report at once to the Lead and the PO (blocker procedure, WFL-145). The environment is restored by the documented manual procedure of `infrastructure.md`, by a human. |
| CIC-E04 | A migration passed on the runner but fails on the remote host. | Roll back the containers; the schema is compatible (CIC-132); a defect story for the Database Engineer; in prod the backup is available (CIC-135). |
| CIC-E05 | Two merges happen at once. | The deployment lock (CIC-130): the second waits; the newer build id deploys after the first finished, or replaces a waiting one. |
| CIC-E06 | A flaky test fails on a pull request. | Classified and rerun (CIC-122, CIC-123); if it keeps failing the run fails; the quarantine needs a story. |
| CIC-E07 | The single image and the layered set behave differently. | A defect of the images (CIC-072, WFL-E20); the story is not green. |
| CIC-E08 | The registry is down. | S13 retries; the merge run fails; no deployment from a local build; the build id is not published (CIC-001). |
| CIC-E09 | A secret expires. | S01 or S14 fails with the name of the secret (CIC-114); a human renews it (WFL-171). |
| CIC-E10 | The runner runs out of disk space. | Clean caches and images of earlier runs, retry once; a repeated failure is a finding for the runner size (CIC-195). |
| CIC-E11 | The seed fails on the second run (not idempotent). | The step fails (WFL-182); the Database Engineer fixes the seed. |
| CIC-E12 | The pull request changes only documents. | S02 and S04 run; the other steps are shown as "skipped" with the reason "no code change" only if the project's change filter says so (CIC-103). Documents are checked for consistency in S04 as far as it is defined. |
| CIC-E13 | A hotfix is needed in prod. | The hotfix lane (WFL-240 to WFL-243): the same pipeline, the same steps, an approval of the PO for prod (WFL-242). |
| CIC-E14 | A tag is pushed on a commit that has no green run. | The tag run stops at S02: the commit must have a green run and a published build id (CIC-044). |
| CIC-E15 | A build id was promoted to test but the test run was not green. | It is marked bad (CIC-141) and is not promoted. |
| CIC-E16 | The deployment user cannot write to the deploy directory. | S14 fails at the target check (9.2 step 2); DevOps and the human fix the host definition. |
| CIC-E17 | The wrapper of the provider and the commands disagree about arguments. | A defect of the wrapper (CIC-161); the commands are the source of truth. |
| CIC-E18 | The project moves from one provider to another. | Only the wrapper changes (CIC-003, WFL-E19); the run is compared with the old one on the same commit; the canary re-run (CIC-211) proves it. |
| CIC-E19 | The database of an environment is larger than expected and the backup takes too long. | The backup timeout is a parameter (CP12); a failure to back up blocks the production deployment (CIC-135). |
| CIC-E20 | The scheduled run fails while no change was made. | A defect story with priority (WFL-230); the cause is a missing check, which is added (WFL-232). |
| CIC-E21 | An environment is changed by hand on the host. | A defect of the environment definition (CIC-013). The next deployment restores the defined state; the change is recorded. |
| CIC-E22 | A story has no remote deployment because no server exists yet. | `workflow.md` WFL-173: decided in writing; the steps S14 to S20 report "not available" (CIC-022). |
| CIC-E23 | An agent wants to skip a failing step to finish a story. | Not allowed (WFL-R05, CIC-103). The loop applies (WFL-064). |
| CIC-E24 | Two environments need different build options. | Not allowed. Differences are configuration only (CIC-001, CIC-110). |
| CIC-E26 | The first deployment to a new host fails. | Bootstrap rules (CIC-256): remove the new containers, report; fix the host definition; repeat. |
| CIC-E27 | A pull request comes from a fork or an external contributor. | No secrets, only S01 to S05 (CIC-254); a maintainer reruns it after a review on a trusted branch. |
| CIC-E28 | The runner has no free ports or two runs use the same port. | Ports and the compose project name come from the run id (CIC-253). |
| CIC-E29 | A job hangs. | The timeout ends it (CIC-252); one retry if transient; then it fails with the step named. |
| CIC-E30 | The host disk is full during a pull or a log write. | The target check fails the deployment before it starts (CIC-260); the prune and the log limits (CIC-257) prevent it; the cause is a finding. |
| CIC-E31 | Someone runs `docker compose down -v` on the host. | Not allowed (CIC-258). The volume is lost: restore from the backup (prod) or reseed (dev); the cause is a finding and the command is removed from every script and note. |
| CIC-E32 | The image transfer over SSH is slow or cut off. | The digest comparison fails the step (CIC-242); retry (CIC-120); if the image is too large, the trigger X4 (registry) fires. |
| CIC-E33 | The pipeline works on the provider but not on a developer machine, or the reverse. | A defect of the commands (CIC-030); the commands are fixed and the wrapper is not used to hide it. |
| CIC-E34 | The MVP has one host and one environment; a story asks for a test environment. | Not available (CIC-022); the PO and the Architect decide if G-04 starts earlier (X5). |
| CIC-E35 | The pipeline's own scripts have grown large. | A finding (CP14): remove steps or logic, use the build tool's feature, or split by a story; do not add a tool (CIC-263). |
| CIC-E36 | The provider changes its free limits or its runner images. | The pinned toolchain and the pinned actions (CIC-034, CIC-243) limit the effect; a new run of the canary shows the impact (CIC-211). |
| CIC-E37 | A story's end-to-end test needs a third-party service that is not available in the pipeline. | A stub or a recorded response inside the test environment, owned by the Tester (`tests.md`); never a call to a live third-party service from the pipeline. |
| CIC-E38 | The build id on the page differs from the run's build id after a deployment. | A defect of the pipeline (WFL-183); the deployment is rolled back. |
| CIC-E39 | A required secret is set at the wrong scope (for example a prod secret is visible to a pull request). | A finding: rotate the secret, fix the scope (CIC-112); the run is repeated after the rotation. |
| CIC-E40 | The single container and the layered set both exist but only one is deployed on the host. | Allowed: one mode per environment (CIC-081); the other mode is tested in the pipeline only. |
| CIC-E41 | A merge happens while the previous deployment is still verifying. | The lock (CIC-130): the new run waits or the older waiting run is cancelled; the deployed build is never half-replaced. |
| CIC-E42 | A change touches only `ci/` or the wrapper. | A full run plus the canary re-run (CIC-211). |
| CIC-E43 | The ephemeral environment cannot start because the runner lacks memory. | Resource limits are set (CIC-195); the run fails with the cause; the Lead decides: reduce parallel services or trigger X8. |
| CIC-E44 | The deployment passes but the story's journey fails on dev. | S15 fails; automatic rollback (CIC-124); defect story with the failing criterion. |
| CIC-E45 | The approval for prod is pending for days. | The run waits; it never times out into a deployment (S18); the PO is reminded by the notification (CIC-261). |
| CIC-E46 | An agent proposes adding a pipeline tool "to be safe". | Rejected unless a trigger of 3.3 holds or the Architect decides (CIC-241, CIC-263). |
| CIC-E47 | Two environments need to differ in more than configuration. | Not allowed (CIC-E24); the difference is a requirement for a story. |
| CIC-E25 | The single container starts but the database inside is not ready in time. | The supervisor exits non-zero (CIC-076); the restart policy retries; the deployment fails its health wait if it does not become ready (CP03). |

## 18. Restrictions

| ID | Restriction |
|---|---|
| CIC-R01 | No logic of the project in a provider wrapper (CIC-161). |
| CIC-R02 | No rebuilding of an image for another environment (CIC-001). |
| CIC-R03 | No mutable image tags that change content (CIC-008, CIC-094). |
| CIC-R04 | No secrets in the repository, images, logs, reports or run records (CIC-111). |
| CIC-R05 | No deployment to a remote environment from a pull request run (CIC-042). |
| CIC-R06 | No deployment to prod without the recorded approval of the PO (CIC-142). |
| CIC-R07 | No manual change of a host or a runner (CIC-013, CIC-E21). |
| CIC-R08 | No skipped, reordered or weakened step without a story and the Architect's and the PO's written decision (CIC-103, CIC-104, CIC-212). |
| CIC-R09 | No test, check or gate disabled to make a run green (WFL-045, AGT-044). |
| CIC-R10 | No parallel deployments to one environment (CIC-130). |
| CIC-R11 | No partial deployment left running: the new build runs healthy or the previous one runs (CIC-137). |
| CIC-R12 | No change of application code in the single image beyond the supervisor and the database (CIC-070). |
| CIC-R13 | No embedded database engine different from the one of `db.md` in the single image (CIC-073). |
| CIC-R14 | No data deletion on a shared or prod environment by an automatic reaction (CIC-125). |
| CIC-R15 | No security scan, monitoring stack, alerting or zero-downtime tooling in the MVP (section 15, WFL-R22). |
| CIC-R16 | No `latest` base image (CIC-091). |
| CIC-R17 | No fixed waits instead of health checks for container order (CIC-061). |
| CIC-R18 | No run without a story id and a goal (CIC-014). |
| CIC-R19 | No change of the pipeline without the canary re-run (CIC-211). |
| CIC-R20 | No metric of the pipeline used as a reward or target for agents (WFL-251). |
| CIC-R21 | No agent edits the wrapper, the commands or the secrets of the pipeline outside a pipeline story owned by the DevOps Engineer (AGT-002, AGT-042). |
| CIC-R23 | No second provider, self-hosted runner, orchestrator, IaC tool or GitOps controller at the MVP without a trigger of 3.3 (CIC-240, CIC-244). |
| CIC-R24 | No pipeline tool beyond the budget of CIC-241 without a decision. |
| CIC-R25 | No job without a timeout (CIC-252). |
| CIC-R26 | No fixed ports or fixed compose project names in tests (CIC-253). |
| CIC-R27 | No secrets for runs from forks or untrusted sources (CIC-254). |
| CIC-R28 | No removal of volumes on a remote host (CIC-258). |
| CIC-R29 | No unlimited container logs (CIC-257). |
| CIC-R30 | No call to a live third-party service from the pipeline (CIC-E37). |
| CIC-R31 | No unpinned marketplace action, plugin or base image (CIC-243, CIC-091). |
| CIC-R32 | No unregistered shortcut (CIC-250), and no shortcut that removes an element of the fixed core (CIC-251). |
| CIC-R33 | No change filter or skipped step at the MVP (CIC-248). |
| CIC-R34 | No pipeline step that only one person or one agent can run (CIC-030, CIC-013). |
| CIC-R35 | No growth of the pipeline ahead of its trigger (3.3). |
| CIC-R36 | No notification for every success (CIC-261). |
| CIC-R37 | No pipeline documentation outside `ci/README.md` and this contract (CIC-264). |
| CIC-R38 | No deployment that leaves the host without free disk space for the rollback image (CIC-260, CP07). |
| CIC-R22 | No provider-specific plugin or marketplace action in a wrapper without a command that can replace it (CIC-162). |

## 19. Parameters

| ID | Parameter | Default | Meaning |
|---|---|---|---|
| CP01 | Pull request run time target | 10 minutes (15 at the MVP, CP17) | Fast feedback (CIC-190) |
| CP02 | Retries of a transient failure or flaky test | 3 (WP08) | CIC-120, CIC-123 |
| CP03 | Health wait after a start or a deployment | 120 seconds | Per environment, set in `infrastructure.md` |
| CP04 | Smoke check timeout | 60 seconds | S11, S15, S20 |
| CP05 | Retention of reports and run records | 30 days (release runs: one year) | CIC-182 |
| CP06 | Image retention | Last 10 builds per environment and all release tags | CIC-097 |
| CP07 | Previous builds kept on a host | 3 | Rollback and clean-up (9.2) |
| CP08 | Cache keys | Lock files plus toolchain versions | CIC-191 |
| CP09 | Scheduled run | Daily at a quiet hour | S24 |
| CP10 | Lock timeout for a deployment | 15 minutes | CIC-130 |
| CP11 | End-to-end scope on a pull request | The story's journeys and the smoke set; the full suite on merge | CIC-192, CIC-077 |
| CP13 | Job timeout | 20 minutes per job (build and test), 10 minutes per deployment job | CIC-252 |
| CP14 | Size of the pipeline's own scripts | SHOULD stay under 500 lines in total at the MVP | CIC-245, CIC-E35 |
| CP15 | Cost check | Run minutes and storage reviewed at each epic gate | CIC-262 |
| CP16 | Shortcut register review | At each epic gate | CIC-250 |
| CP17 | Pull request run time target at the MVP | 15 minutes (the 10 minute target of CP01 applies after X1 and X2) | CIC-190 |
| CP12 | Production backup | Before each migration deployment; keep the last 5; timeout set per size | CIC-135 |

## 20. Templates

The templates are skeletons. They contain no project logic; they call `ci/run`. `infrastructure.md` names the registry, hosts and secrets.

### 20.1 GitHub Actions (also Gitea and Forgejo Actions, with the same structure)

```yaml
# .github/workflows/ci.yml
name: ci
on:
  pull_request:
  push:
    branches: [main]
    tags: ['v*']
  schedule:
    - cron: '0 4 * * *'
  workflow_dispatch:

concurrency:
  group: ci-${{ github.ref }}
  cancel-in-progress: ${{ github.event_name == 'pull_request' }}

jobs:
  verify:
    if: github.event_name != 'schedule'
    runs-on: ubuntu-latest
    timeout-minutes: 20                    # CP13
    env:
      COMPOSE_PROJECT_NAME: run-${{ github.run_id }}   # isolation (CIC-253)
    steps:
      - uses: actions/checkout@v4
      - run: ci/run prepare
      - run: ci/run check-inputs
      - run: ci/run build
      - run: ci/run check
      - run: ci/run test-unit
      - run: ci/run images
      - run: ci/run env-up layers
      - run: ci/run migrate
      - run: ci/run seed
      - run: ci/run test-integration
      - run: ci/run test-e2e
      - run: ci/run smoke local
      - run: ci/run env-down
        if: always()
        # single-container check: env-up single, smoke local (CP11)
      - run: ci/run report
        if: always()
      - uses: actions/upload-artifact@v4
        if: always()
        with:
          name: run-${{ github.run_id }}
          path: ci/out/

  publish-and-dev:
    needs: verify
    if: github.event_name == 'push'
    runs-on: ubuntu-latest
    environment: dev
    concurrency: deploy-dev
    timeout-minutes: 10
    steps:
      - uses: actions/checkout@v4
      - run: ci/run prepare
      - run: ci/run publish
      - run: ci/run deploy dev
      - run: ci/run verify dev
      - run: ci/run record
        if: always()

  test:                                   # Level 2
    needs: publish-and-dev
    runs-on: ubuntu-latest
    environment: test
    concurrency: deploy-test
    steps:
      - uses: actions/checkout@v4
      - run: ci/run promote dev test
      - run: ci/run verify test

  prod:                                   # Level 2; the environment needs required reviewers
    needs: test
    if: startsWith(github.ref, 'refs/tags/v')
    runs-on: ubuntu-latest
    environment: prod
    concurrency: deploy-prod
    steps:
      - uses: actions/checkout@v4
      - run: ci/run promote test prod
      - run: ci/run verify prod

  scheduled:
    if: github.event_name == 'schedule'
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - run: ci/run verify dev
      - run: ci/run record
        if: always()
```

Secrets per environment: `DEPLOY_HOST`, `DEPLOY_USER`, `DEPLOY_SSH_KEY`, `REGISTRY_USER`, `REGISTRY_TOKEN`, and the application secrets. The `prod` environment has required reviewers (the PO).

### 20.2 GitLab CI

```yaml
# .gitlab-ci.yml
stages: [prepare, build, test, package, dev, test-env, prod, scheduled]

default:
  timeout: 20 minutes                     # CP13
  retry:
    max: 2
    when: [runner_system_failure, stuck_or_timeout_failure]
  artifacts:
    when: always
    paths: [ci/out/]
    expire_in: 30 days

workflow:
  rules:
    - if: '$CI_PIPELINE_SOURCE == "merge_request_event"'
    - if: '$CI_COMMIT_BRANCH == $CI_DEFAULT_BRANCH'
    - if: '$CI_COMMIT_TAG =~ /^v\d+\.\d+\.\d+$/'
    - if: '$CI_PIPELINE_SOURCE == "schedule"'

prepare:
  stage: prepare
  rules: [{ if: '$CI_PIPELINE_SOURCE != "schedule"' }]
  script: [ci/run prepare, ci/run check-inputs]

build:
  stage: build
  rules: [{ if: '$CI_PIPELINE_SOURCE != "schedule"' }]
  script: [ci/run build, ci/run check]

unit:
  stage: test
  rules: [{ if: '$CI_PIPELINE_SOURCE != "schedule"' }]
  script: [ci/run test-unit]

verify:
  stage: package
  rules: [{ if: '$CI_PIPELINE_SOURCE != "schedule"' }]
  services: [docker:dind]
  script:
    - ci/run images
    - ci/run env-up layers
    - ci/run migrate
    - ci/run seed
    - ci/run test-integration
    - ci/run test-e2e
    - ci/run smoke local
  after_script: [ci/run env-down, ci/run report]

deploy-dev:
  stage: dev
  rules: [{ if: '$CI_COMMIT_BRANCH == $CI_DEFAULT_BRANCH' }]
  environment: { name: dev }
  resource_group: dev
  script: [ci/run publish, ci/run deploy dev, ci/run verify dev]
  after_script: [ci/run record]

promote-test:                              # Level 2
  stage: test-env
  rules: [{ if: '$CI_COMMIT_BRANCH == $CI_DEFAULT_BRANCH' }]
  environment: { name: test }
  resource_group: test
  script: [ci/run promote dev test, ci/run verify test]

deploy-prod:                               # Level 2; protected environment with approvers
  stage: prod
  rules: [{ if: '$CI_COMMIT_TAG =~ /^v\d+\.\d+\.\d+$/', when: manual }]
  environment: { name: prod }
  resource_group: prod
  script: [ci/run promote test prod, ci/run verify prod]

scheduled-verify:
  stage: scheduled
  rules: [{ if: '$CI_PIPELINE_SOURCE == "schedule"' }]
  script: [ci/run verify dev, ci/run record]
```

### 20.3 Jenkins (Gitblit and other Git servers without CI)

```groovy
// Jenkinsfile (declarative)
pipeline {
  agent any
  options { disableConcurrentBuilds(); timestamps(); timeout(time: 20, unit: 'MINUTES') }   // CP13
  triggers { cron('H 4 * * *') }            // scheduled run; pushes arrive through the Git server hook
  stages {
    stage('Prepare')  { steps { sh 'ci/run prepare'; sh 'ci/run check-inputs' } }
    stage('Build')    { steps { sh 'ci/run build'; sh 'ci/run check' } }
    stage('Unit')     { steps { sh 'ci/run test-unit' } }
    stage('Package')  { steps { sh 'ci/run images' } }
    stage('Verify') {
      steps {
        sh 'ci/run env-up layers'
        sh 'ci/run migrate'
        sh 'ci/run seed'
        sh 'ci/run test-integration'
        sh 'ci/run test-e2e'
        sh 'ci/run smoke local'
      }
      post { always { sh 'ci/run env-down'; sh 'ci/run report' } }
    }
    stage('Publish and dev') {
      when { branch 'main' }
      steps {
        withCredentials([sshUserPrivateKey(credentialsId: 'deploy-key', keyFileVariable: 'DEPLOY_SSH_KEY')]) {
          sh 'ci/run publish'; sh 'ci/run deploy dev'; sh 'ci/run verify dev'
        }
      }
    }
    stage('Test')     { when { branch 'main' }  steps { sh 'ci/run promote dev test'; sh 'ci/run verify test' } }
    stage('Approve prod') {
      when { tag 'v*' }
      steps { input message: 'Promote to prod?', submitter: 'product-owner' }
    }
    stage('Prod')     { when { tag 'v*' }  steps { sh 'ci/run promote test prod'; sh 'ci/run verify prod' } }
  }
  post { always { archiveArtifacts artifacts: 'ci/out/**', allowEmptyArchive: true; sh 'ci/run record' } }
}
```

Gitblit hook (a repository or global setting, see CIC-151): enable the sample Jenkins Groovy hook, set the Jenkins server URL and the repository base URL; Jenkins then runs the multibranch pipeline for the pushed branch.

### 20.4 Other providers

| Provider | How |
|---|---|
| Azure DevOps | `azure-pipelines.yml` with stages that call `ci/run`; environments with approvals for test and prod |
| Bitbucket Pipelines | `bitbucket-pipelines.yml` with `pipelines`, `branches`, `tags`, `pull-requests`, deployment environments and manual `trigger` for prod |
| CircleCI | `.circleci/config.yml` with workflows, jobs, a `hold` approval job for prod |
| Woodpecker, Drone | `.woodpecker.yml` or `.drone.yml` with steps that call `ci/run`; promotion through `promote` events |

All follow section 4 and 11: triggers, order, secrets, approval, concurrency and artifacts in the wrapper; everything else in the commands.

### 20.5 Compose definitions (shape, content in `infrastructure.md`)

```yaml
# infrastructure/compose.layers.yaml
services:
  database:
    image: ${IMAGE_REGISTRY}/${IMAGE_PREFIX}-database:${BUILD_ID}
    environment: [DB_NAME, DB_USER, DB_PASSWORD]
    volumes: [dbdata:/var/lib/data]
    healthcheck: { test: ["CMD", "db-ready"], interval: 5s, timeout: 5s, retries: 20 }
    restart: unless-stopped
  migrate:
    image: ${IMAGE_REGISTRY}/${IMAGE_PREFIX}-backend:${BUILD_ID}
    command: ["migrate"]                    # process role migrate
    depends_on: { database: { condition: service_healthy } }
    restart: "no"
  backend:
    image: ${IMAGE_REGISTRY}/${IMAGE_PREFIX}-backend:${BUILD_ID}
    command: ["web"]                        # process role web
    depends_on:
      database: { condition: service_healthy }
      migrate: { condition: service_completed_successfully }
    healthcheck: { test: ["CMD", "app-health"], interval: 5s, timeout: 5s, retries: 20 }
    restart: unless-stopped
    logging: { driver: json-file, options: { max-size: "10m", max-file: "3" } }   # CIC-257
  frontend:
    image: ${IMAGE_REGISTRY}/${IMAGE_PREFIX}-frontend:${BUILD_ID}
    ports: ["${PUBLIC_PORT:-80}:80"]
    depends_on: { backend: { condition: service_healthy } }
    restart: unless-stopped
volumes: { dbdata: {} }
```

```yaml
# infrastructure/compose.single.yaml
services:
  app:
    image: ${IMAGE_REGISTRY}/${IMAGE_PREFIX}-single:${BUILD_ID}
    ports: ["${PUBLIC_PORT:-80}:80"]
    volumes: [appdata:/data]
    healthcheck: { test: ["CMD", "app-health"], interval: 5s, timeout: 5s, retries: 30 }
    restart: unless-stopped
volumes: { appdata: {} }
```

### 20.6 Deployment sketch (`ci/commands/deploy`)

```text
deploy <env> [build id]
  1  lock <env>                              (provider concurrency + host lock)
  2  ssh <deploy user>@<host>: check engine, disk, registry login
  3  verify the images and digests for the build id
  4  record previous build id in releases.log
  5  prod: backup the database and verify the file
  6  write env files from infrastructure/env/<env> and the secret store
  7  compose pull
  8  compose run migrate               (one-shot, wait for it)
  9  compose up -d                      (recreate with the new build id)
 10  wait for healthy (CP03)
 11  smoke <env> (CP04)
 12  success: record current; prune beyond CP07; unlock
     failure: rollback <env>; verify; unlock; exit non-zero
```

## 21. Sources

- Current CI/CD practice summaries: [CI/CD best practices: 13 ways to build faster, safer pipelines](https://brights.io/blog/cicd-best-practices) (trunk-based development, parallelism and caching, build once and promote, test pyramid, simple pipelines and shared templates, on-demand environments, delivery metrics, progressive rollout) and further articles from Harness, Mergify and others found in the search.
- Gitblit integration with Jenkins: [Gitblit sample Jenkins post-receive hook](https://gerrit.googlesource.com/gitblit/+/master/src/main/distrib/data/groovy/jenkins.groovy) (triggers the Jenkins `notifyCommit` URL for the pushed repository; settings `groovy.jenkinsServer`, `groovy.jenkinsGitbaseurl`, `groovy.postReceiveScripts`).
- Provider documentation for GitHub Actions (workflows, environments, concurrency), GitLab CI (stages, rules, environments, resource groups), Jenkins (declarative pipeline, input, lock) and the Compose specification (health checks, `depends_on` conditions), as used for the templates.
- The twelve-factor app (configuration in the environment).
- Uploaded projects: the CI pipeline design (one pipeline for all containers, Docker Compose as the source of truth, images tagged with the commit id, SSH deployment, rollback by tag) and the DevOps phase with GitLab CI lessons (stages, parallel tests, environments, manual gates, migration safety, rollback).
- The documents of this project: `workflow.md` (goals, stages, the loop, deploy targets, self-healing, self-learning, hotfix lane, skeleton page), `team.md`, `agents.md`, `backend.md`, `tests.md`.
