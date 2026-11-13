# Infrastructure contract

| | |
|---|---|
| Document | `.claude/skills/infrastructure/infrastructure.md` |
| Version / status | 0.1, draft for review |
| Purpose | Defines where the product runs and how that place is built: the environments (local, dev, test, prod), the Git branches and promotion that lead to them, the hosts, the containers, the configuration, the data and its backups, the deployment, and the path to grow. It is the source for the stories of Epic 0 (goal G-01) and for the completion of the delivery path (goal G-04). |
| Related | `workflow.md` (goals, Epic 0, environments, gates), `ci-cd.md` (the pipeline that uses this infrastructure: commands, steps, remote deployment), `backend.md` (process roles, statelessness, health endpoints), `db.md` (database host, backups, recovery targets), `tests.md` (environments for tests), `team.md` and `agents.md` (owners and guardrails). |
| Owner | DevOps Engineer, with the Architect (decisions) and the Database Engineer (data and backups). |
| Not covered | Security (access control, hardening, TLS policy, scans) and observability (metrics, tracing, alerting): no-touch until all project phases are finished (`workflow.md` WFL-080, WFL-120). Their places are reserved (section 16). |

Normative words: MUST, MUST NOT, SHOULD, MAY. Rule IDs are `INF-<GROUP>-nnn`; edge cases `INF-Enn`; restrictions `INF-Rnn`; parameters `IPnn`; extension triggers `IXn`.

## Contents and version

| Section | Content |
|---|---|
| 1 | Scope and definitions |
| 2 | Principles |
| 3 | The MVP profile |
| 4 | Environments |
| 5 | Git: branches, promotion and rollback |
| 6 | Repository layout |
| 7 | The local environment |
| 8 | Hosts |
| 9 | Containers |
| 10 | Configuration and secrets |
| 11 | Network and entry point |
| 12 | Data, volumes and backups |
| 13 | Deployment and recovery |
| 14 | Dependability |
| 15 | The growth path (scaling, orchestrator, broker) |
| 16 | Deferred areas (security, observability) |
| 17 | Epic 0: the technical setup stories |
| 18 | Edge cases |
| 19 | Restrictions |
| 20 | Quality gates |
| 21 | Parameters |
| 22 | Templates |
| 23 | Sources and limits |

Version history: 0.1 first version: four environments, trunk plus pointer branches, one host to start, compose as the only runtime, a growth ladder with triggers.

## 1. Scope and definitions

| Term | Meaning |
|---|---|
| Infrastructure | Everything the application needs to run that is not application code: hosts, container definitions, networks, volumes, configuration, backups, and the Git conventions that connect code to environments. |
| Environment | A named place where one build runs: `local`, `ephemeral`, `dev`, `test`, `prod` (section 4). |
| Host | A machine (virtual or physical) that runs containers and is reachable by SSH. |
| Layer | One of database, backend, frontend (`ci-cd.md` 7.1). |
| Mode | `layers` (one container per layer) or `single` (the whole app in one container) (`ci-cd.md` 7.2). |
| Trunk | The branch `main`: the only line on which code is developed and reviewed. |
| Pointer branch | A branch `env/<name>` that holds no own work. It points to the commit that environment runs (5.3). |
| Build id | The commit id of a build; it names the images (`ci-cd.md` 1). |
| Release | A build id that carries a tag `v<major>.<minor>.<patch>`. |
| Runbook | A short, tested, step-by-step procedure for an operation or a failure (13.4). |
| RPO / RTO | The data loss and the downtime that are acceptable after a failure (`db.md` DP10, DP11). |
| Dormant phase | The phase with security and observability, opened only after all other phases (`workflow.md` WFL-080). |

Rule classes: **T** (testable by a command or a check), **I** (checked by inspection), **D** (document or decision), **A** (agreement with the PO).

Basis codes: **12F** twelve-factor app; **GIT** Git practice (single trunk, short branches, tags); **DEP** dependability (Tanenbaum's attributes and fault-tolerance); **K8S** keep the Kubernetes path open; **EVT** event and broker decision criteria; **YAG** you are not going to need it yet; **REL** reliability and recovery.

## 2. Principles

| ID | Principle | Rule | Class | Basis |
|---|---|---|---|---|
| INF-PRN-001 | The simplest thing that runs | One host, one container engine, compose files. Every other tool (registry, proxy, orchestrator, broker, infrastructure-as-code) has a trigger in section 3 and is not added before it. | I | YAG |
| INF-PRN-002 | Everything in Git, except secrets and data | Container definitions, compose files, environment descriptions, host bootstrap scripts, backup scripts and runbooks are files in the repository. The state of a host is what the files say, not what someone typed there. | I | GIT |
| INF-PRN-003 | Build once, run anywhere | One image set per commit runs in every environment. Environments differ only by configuration (`ci-cd.md` CIC-001, INF-CFG-001). | T | 12F |
| INF-PRN-004 | The same shape everywhere | Local, dev, test and prod use the same compose structure, the same images, the same commands and the same health checks. A difference is a value in an environment file, never a different file layout. | T | 12F |
| INF-PRN-005 | Disposable containers, durable data | A container can be killed and recreated at any moment without loss. All state is in named volumes (the database) or in the database. | T | REL |
| INF-PRN-006 | Recovery is practised, not hoped for | A backup counts only after a restore from it has worked. The restore is drilled at the intervals of section 12. | T | REL |
| INF-PRN-007 | Fail visibly and early | A missing variable, an unreachable host, a full disk or an unhealthy container stops the operation with a clear message before anything is changed (`ci-cd.md` 9.2 step 2). | T | REL |
| INF-PRN-008 | Orchestrator-ready, not orchestrator-first | The images and the configuration obey the rules that Kubernetes also needs (stateless backend, configuration by environment, health endpoints, signal handling, one process per container), so that a later move changes the deployment files, not the application (section 15). | I | K8S |
| INF-PRN-009 | Growth by trigger | Each extension is a story with a measurable trigger (3.3). The infrastructure does not grow ahead of its trigger, and it is not frozen behind its trigger. | D | YAG |
| INF-PRN-010 | Agents change files, humans hold the keys | Agents write and change the infrastructure files. Credentials (SSH keys, tokens, passwords) are created and placed by a human and are never visible to agents (`ci-cd.md` CIC-113, `workflow.md` WFL-171). | I | REL |

## 3. The MVP profile

The MVP infrastructure is the smallest one that can carry the skeleton page and every story after it, locally and on one remote server, and that can grow to test and prod without a redesign.

### 3.1 The fixed core

| # | Element | Rule |
|---|---|---|
| 1 | The four environment names and their purpose | INF-ENV-001 |
| 2 | One trunk `main`, short-lived story branches, and pointer branches `env/dev`, `env/test`, `env/prod` | INF-BRN-001 to INF-BRN-004 |
| 3 | Container definitions in `infrastructure/`, one compose file per mode | INF-CTR-001 |
| 4 | Configuration by environment variables, non-secret values in Git, secrets outside Git | INF-CFG-001 to INF-CFG-004 |
| 5 | Named volumes for data, never removed by a deployment | INF-DAT-001, `ci-cd.md` CIC-258 |
| 6 | A backup and a restore procedure for every environment that holds data that matters, tested once before the first production data | INF-DAT-010, INF-DAT-014 |
| 7 | Health checks on every container and restart policy `unless-stopped` | INF-CTR-008 |
| 8 | A written bootstrap of a host and a written manual recovery procedure | INF-HST-006, INF-DEP-010 |
| 9 | The local environment starts from nothing with one command | INF-LOC-001 |
| 10 | No real personal data in any environment before the dormant phase is finished | INF-DAT-015 |

### 3.2 Simplifications at the MVP

| Topic | Full form (grows to) | MVP profile |
|---|---|---|
| Hosts | One host per environment, prod separate | One remote host for dev; test and prod are defined and created at G-04. Until the first real user, dev, test and prod MAY share one host as separate compose projects (INF-HST-003) |
| Mode | Layers in all environments | `layers` everywhere; `single` for a demo, a review environment and the smoke test |
| Image transport | A registry | Image transfer over SSH for one host (`ci-cd.md` CIC-242) |
| Entry point | A reverse proxy in front of several services and hosts | The frontend container is the entry point and passes `/api` to the backend (INF-NET-002) |
| Domain names | One name per environment, certificates | The host address and a port per environment; names and certificates are in the dormant phase (INF-NET-006) |
| Database | A managed database or a replica | One container with a named volume (INF-DAT-002) |
| Backups | Physical backups with point-in-time recovery, off-site copy, automatic restore test | A scheduled logical dump on the host, copied to a second place, restore tested by hand (INF-DAT-010 to INF-DAT-014) |
| Infrastructure as code | A tool that creates hosts | A host is created by a human at the provider; its setup is a script in the repository (INF-HST-006) |
| File and object storage | An object store | None until a story needs files (IX7) |
| Mail | A mail relay | None; a mail catcher container only for local and ephemeral environments when a story sends mail (IX8) |
| Message broker | A broker | None; the outbox table of `db.md` and `backend.md` (BKD-MVP-007) |
| Orchestrator | Kubernetes or a managed container service | None (IX9) |

### 3.3 Extension triggers

Each extension is a story with a trigger. The Architect decides; the decision is recorded in `architecture.md`.

| # | Extension | Trigger |
|---|---|---|
| IX1 | Separate host for prod | The first real user, or any data that matters is in prod |
| IX2 | Separate hosts for test and dev | Test runs disturb dev, or the host is short of memory or disk (IP05, IP06) |
| IX3 | A registry | A second host, or an image too large to transfer within the deployment time (`ci-cd.md` X4) |
| IX4 | A reverse proxy at the host (names, several environments on one port) | More than one environment must be reachable on the same port, or names are required |
| IX5 | Off-site and point-in-time backups | The RPO of the PO is shorter than IP08, or the data cannot be recreated |
| IX6 | Separate database host or a managed database | The database needs more memory or disk than the shared host can give, or the availability target requires it |
| IX7 | Object storage | A story stores files that must not live in the database or on a container disk |
| IX8 | Mail relay | A story sends mail outside local use |
| IX9 | Orchestrator | Two or more of the five criteria in 15.3 are true |
| IX10 | Message broker | Two or more of the five criteria in 15.4 are true |
| IX11 | Zero-downtime release | An availability requirement of the PO (`ci-cd.md` X10) |
| IX12 | Infrastructure as code for hosts | More than three hosts, or a host was lost and its rebuild took longer than IP14 |
| IX13 | Security and observability | The dormant phase is opened (section 16) |

| ID | Rule |
|---|---|
| INF-MVP-001 | Every element of 3.2 that the project actually uses in its simple form is registered in `infrastructure/README.md` with the date, the reason and the trigger that ends it. An unregistered shortcut is a finding of the Auditor (`workflow.md` 10.3, shortcut clearing). |
| INF-MVP-002 | A shortcut never removes an element of the fixed core (3.1). |
| INF-MVP-003 | An extension keeps the interfaces that exist: the commands of `ci-cd.md` 4.1, the variables of 10.2, the health endpoints and the image names. A trigger that fires changes the files behind them. |
| INF-MVP-004 | Cost is a design input: the MVP runs on the smallest host class that keeps the checks of IP05 and IP06 green. A larger host needs a reason in the run record. |

## 4. Environments

| Environment | Purpose | Where | Mode | Data | Created by | Deployed from |
|---|---|---|---|---|---|---|
| **local** | A person (or an agent) builds and checks a story | The developer machine or the agent workspace | `layers`; `single` optional | Reference and demo | `ci/run all-local`, `ci/run env-up` | The working tree |
| **ephemeral** | Verdict of a pull request | The runner of the provider | `layers` for tests; `single` for the smoke check | Reference and demo | Each pull request run (S07), removed at S21 | The pull request commit |
| **dev** | The latest merged work, for the team and the PO to look at | Remote host | As set in section 8 (default `layers`) | Reference and demo | The merge run | `main` |
| **test** | Full verification: the whole pyramid, cross-domain journeys, the Auditor's checks | Remote host | `layers` | Reference and demo | Promotion from dev | A build green on dev |
| **prod** | The product for its users | Remote host | `layers`, or `single` for a small installation | Reference only, then real data after the dormant phase | Promotion from test after approval | A build green on test |

| ID | Rule |
|---|---|
| INF-ENV-001 | The environment names are `local`, `ephemeral`, `dev`, `test`, `prod`. Other names (`demo`, `review`, `staging`) are added only through a story and an entry in this table (INF-MVP-003). |
| INF-ENV-002 | The environment is given to the application as `APP_ENV` (`ci-cd.md` 8). The application MUST NOT change its behaviour on the name, except for the seed set (`SEED_SET`) and the display of the build id. Behaviour that depends on the name is a defect (INF-PRN-004). |
| INF-ENV-003 | Every environment shows the build id and the environment name on the skeleton page and in `GET /health`'s non-public detail, so that anyone can see what runs (`workflow.md` WFL-183). |
| INF-ENV-004 | Environments are independent: each has its own database volume, its own configuration and its own compose project name. No environment reads the database or the files of another. |
| INF-ENV-005 | Promotion order is dev, test, prod (`ci-cd.md` CIC-140). The same build id moves; nothing is rebuilt. |
| INF-ENV-006 | Dev and test hold only reference and demo data. They are rebuilt from migrations and seeds at any time (`db.md` DBS-SED rules). Their database volume MAY be dropped and recreated by a human decision when a migration needs it. A volume of prod is never dropped by a pipeline. |
| INF-ENV-007 | Prod is the only environment where a rollback needs a backup first (`ci-cd.md` CIC-135). |
| INF-ENV-008 | An environment that is defined but not yet created is reported "not available" by the pipeline, not skipped silently (`ci-cd.md` CIC-022). |
| INF-ENV-009 | The ephemeral environment on the runner needs no host. Its ports and names derive from the run id (`ci-cd.md` CIC-253). |
| INF-ENV-010 | The environment descriptions live in `infrastructure/env/<env>.env` (non-secret values only) and are changed by pull request (`ci-cd.md` CIC-144). |

## 5. Git: branches, promotion and rollback

Git is the record of what the product is and of what runs where. Two ideas are combined: **one trunk** on which all code lives (so that every environment runs code with one history), and **pointer branches** that show, in Git, which commit each environment runs.

### 5.1 The branches

| Branch | Holds | Who writes | Lives |
|---|---|---|---|
| `main` | All reviewed code, documents, infrastructure files and migrations | Pull request merges only | Permanent |
| `<story-id>-<title>` (for example `us-02.03-order-list`) | The work of one story | The author (agent or human) | Hours to days; deleted after the merge |
| `hotfix-<id>-<title>` | A fix for a defect in prod that cannot wait (`workflow.md` hotfix lane) | The author | Hours; deleted after the merge |
| `env/dev`, `env/test`, `env/prod` | Nothing of its own: a pointer to the commit that runs in that environment | The pipeline only, after a successful deployment | Permanent |

```text
local work ─► story branch ─► pull request ─► main ─┬─► deploy ─► env/dev   (pointer moves after green)
                                  ▲                  ├─► promote ─► env/test (pointer moves after green)
                                  │                  └─► approve ─► env/prod (pointer moves after green)
                         ephemeral run (S01-S12)

tags:  v1.2.0 on main = a release;  the same commit carries the pointer of every environment that runs it
```

### 5.2 Rules for trunk and story branches

| ID | Rule | Class | Basis |
|---|---|---|---|
| INF-BRN-001 | There is one trunk, `main`. All code is developed on short-lived branches from `main` and merged back by pull request. No long-lived branch holds code that differs between environments. | T | GIT |
| INF-BRN-002 | `main` is protected: no direct push, no force push, no deletion; a merge needs a green pull request run and the Reviewer's approval (`ci-cd.md` CIC-040). | T | GIT |
| INF-BRN-003 | A story branch is named with the story id and a short title, starts from the current `main`, is rebased or merged from `main` before the pull request is merged, and is deleted after the merge (`ci-cd.md` CIC-041). | T | GIT |
| INF-BRN-004 | The history of `main` is linear (squash or rebase merge). One merged story is one commit or a short series, each with the story id in the message (`ci-cd.md` S02). | T | GIT |
| INF-BRN-005 | A commit message has a subject (what changed), a body (why, when it is not obvious) and the story id. A commit that changes infrastructure, a migration or a seed names this in the subject. | I | GIT |
| INF-BRN-006 | The repository holds source, documents, configuration without secrets, and small test fixtures. It does not hold secrets, build outputs, dependency folders, database dumps, large binaries or personal data. A `.gitignore` and a size check in S04 enforce this. | T | GIT |
| INF-BRN-007 | A large binary that the product needs (a font, an image set) is allowed when small; a growing set of binaries is an architecture decision (an asset store) and not a repository habit. | D | GIT |
| INF-BRN-008 | Tags are annotated and immutable. A release tag `v<major>.<minor>.<patch>` is placed on a commit of `main` that is green on test. A tag is never moved or deleted (`ci-cd.md` CIC-044, CIC-045). | T | GIT |
| INF-BRN-009 | A secret that reached Git is a defect of the highest class: the secret is replaced at its source at once, then the history is cleaned by a human with the Lead. Removing the file in a later commit does not remove the secret. | I | GIT |

### 5.3 Pointer branches and promotion

| ID | Rule | Class | Basis |
|---|---|---|---|
| INF-BRN-010 | `env/dev`, `env/test` and `env/prod` hold no commits of their own. Each points to a commit of `main`: the build id that runs in that environment now. | T | GIT |
| INF-BRN-011 | Only the pipeline moves a pointer, and only after the deployment and the smoke check of that environment succeeded. A person does not push to a pointer branch. The pointer branches are protected against pushes by people. | T | GIT |
| INF-BRN-012 | Promotion moves the same commit: `promote <from> <to>` checks that the build id is green on `<from>`, deploys it to `<to>`, and moves `env/<to>` to that commit. No merge between environment branches exists, and no change is made on a pointer branch. | T | GIT |
| INF-BRN-013 | The pointer of `env/dev` follows the merge run. A newer merge moves it forward after its deployment is green. The pointers of test and prod move only by promotion. | T | GIT |
| INF-BRN-014 | `env/prod` moves only after the approval of the PO is recorded (`ci-cd.md` CIC-142). | T | GIT |
| INF-BRN-015 | A rollback returns the environment to its previous build id (`ci-cd.md` 9.2, CIC-124) and moves the pointer back to that commit. The pointer is the one place where a non-fast-forward update by the pipeline is allowed. The deployment record and `releases.log` keep the history of what ran. | T | GIT |
| INF-BRN-016 | A pointer branch always names a commit that exists on `main` or in a hotfix that is merged to `main` within IP13 hours. A pointer to a commit that is not on `main` is a defect found by the Auditor. | I | GIT |
| INF-BRN-017 | A provider that cannot start a run by hand (a bare Git server with an external CI server, `ci-cd.md` 11.1) MAY start the promotion by a push of the pipeline's own identity to `env/<name>`, which the hook of the Git server turns into a deployment run. The checks of INF-BRN-012 and INF-BRN-014 still apply. | D | GIT |
| INF-BRN-018 | Environment-specific code, configuration or data is not kept on a branch. It is a value in `infrastructure/env/<env>.env` on `main` (INF-CFG-001) or a seed set (`db.md`). | T | GIT |

### 5.4 Hotfix and rollback paths

| Situation | Path |
|---|---|
| A defect in prod that cannot wait | `hotfix-` branch from the commit that runs in prod (or from `main`); pull request with the full run; merge to `main`; promote the new build id dev, test, prod with the shortened path of `workflow.md` (hotfix lane). The fix is on `main` first, never only on a hotfix branch. |
| A bad deployment | Automatic rollback of the failed environment (`ci-cd.md` CIC-124); the pointer moves back; the build id is marked bad (`ci-cd.md` CIC-141). |
| A bad build found later in prod | `rollback prod` as a manual run with the person and the reason recorded (`ci-cd.md` CIC-047); the database follows expand and contract so that the previous build still works (`db.md`, `ci-cd.md` CIC-132). |
| A bad migration | Not reverted. A forward fix is made as a new migration; a restore from backup is the last resort (13.3). |

## 6. Repository layout

The structure is a decision of the project; this is the default (`ci-cd.md` 4 shows the same folders).

```text
project/
  backend/  frontend/  api/  db/                 area folders (area contracts)
  ci/                                            commands (ci/run), see ci-cd.md
  infrastructure/
    README.md                                    the shortcut register, the variable list, host list (no secrets)
    compose.layers.yaml                          layered deployment (database, backend, frontend, helpers)
    compose.single.yaml                          single-container deployment
    images/
      backend/Dockerfile   frontend/Dockerfile   single/Dockerfile   database/Dockerfile (only if needed)
      frontend/nginx.conf                        entry point: static files and /api proxy
    env/
      local.env  dev.env  test.env  prod.env     non-secret values per environment
    host/
      bootstrap.sh                               prepares a new host (INF-HST-006)
      backup.sh  restore.sh                      logical dump and restore (section 12)
      cron.example                               schedule of the backup and the prune
    runbooks/
      recover-host.md  restore-database.md  rollback.md  first-deploy.md
  ai-docs/                                          the project documents
```

| ID | Rule |
|---|---|
| INF-REP-001 | All files of the infrastructure live under `infrastructure/`, except the provider wrappers (`ci-cd.md` 11) and the commands (`ci/`). |
| INF-REP-002 | A file in `infrastructure/` is small and readable. A compose file over about 150 lines or a script over about 100 lines is split or simplified (IP12). |
| INF-REP-003 | `infrastructure/README.md` lists: the hosts (name, role, address class), the variables and secrets (names only), the registered shortcuts (INF-MVP-001) and the versions of the pinned base images. |
| INF-REP-004 | No file in `infrastructure/` contains a secret, a real address of a private person, or a password, not even a default that looks harmless. Defaults for local use are generated at the first local start (INF-LOC-005). |
| INF-REP-005 | Base images and tool versions are pinned (`ci-cd.md` CIC-091). The list is in `infrastructure/README.md`. |

## 7. The local environment

| ID | Rule | Class |
|---|---|---|
| INF-LOC-001 | From a clean checkout, one command starts the whole local environment: `ci/run env-up layers` (and `ci/run all-local` for the full local pipeline). It needs only the tools of `ci-cd.md` CIC-241. | T |
| INF-LOC-002 | The first start creates the database, runs the migrations from empty, loads the seed (reference and demo) and shows the address of the page. A second start changes nothing (`workflow.md` WFL-182). | T |
| INF-LOC-003 | The local environment uses the same images, the same compose file and the same health checks as the remote ones (INF-PRN-004). A developer MAY also start only the database container and run the backend and frontend from the build tools for a fast edit loop; the result of a story is always checked in the container form. | T |
| INF-LOC-004 | The local reset is one command: `ci/run env-down` removes containers and the local volumes. It runs only on `local` and `ephemeral` (INF-R05). | T |
| INF-LOC-005 | Local passwords and keys are generated at the first start into an ignored file and are never committed. | T |
| INF-LOC-006 | Local ports are set in `infrastructure/env/local.env` (default: page on 8080, database on none). Two local environments (for example two stories at once) do not clash: the compose project name and ports derive from a name or the run id (`ci-cd.md` CIC-253). | T |
| INF-LOC-007 | The database port is not published to the host in any environment by default. A tool that needs it uses `docker exec` or a temporary, explicit override that is not committed. | T |
| INF-LOC-008 | An agent workspace is a local environment. An agent starts it with the same command and reads the same health check. It does not install system packages beyond the pinned toolchain (`agents.md` guardrails). | I |
| INF-LOC-009 | Disk and memory needs of the local environment are written in `infrastructure/README.md` (IP05, IP06 apply as minimum). A local run that cannot start for lack of resources reports it by name. | I |

## 8. Hosts

A host runs containers and is reached over SSH. The MVP needs one.

| Item | MVP default |
|---|---|
| Kind | One small virtual server from any provider, Linux, with a container engine and the compose plugin |
| Size | At least IP05 CPU cores, IP06 GB memory, IP07 GB disk for one environment; more when dev, test and prod share it |
| Access | The deploy user by SSH key only; no password login (`ci-cd.md` 9.1) |
| Directory | `/opt/<product>/<env>/` per environment (`ci-cd.md` 9.1) |
| Time | Synchronised clock |

| ID | Rule | Class |
|---|---|---|
| INF-HST-001 | The MVP starts with one remote host named in `infrastructure/README.md` by role (`dev-host`), not by a private address. | I |
| INF-HST-002 | A host has one role per environment it carries. The host list in the README says which environments each host carries. | I |
| INF-HST-003 | Until the first real user (IX1), dev, test and prod MAY share one host as separate compose projects with separate directories, volumes and published ports (section 11). This is a registered shortcut (INF-MVP-001). When IX1 fires, prod moves to its own host through the bootstrap and a restore. | D |
| INF-HST-004 | A host that carries prod does not carry test or dev. A host that carries only dev and test MAY be rebuilt at any time. | D |
| INF-HST-005 | The host runs only what the environments need: the container engine, SSH, the clock service, the backup schedule and the system updates of its operating system. No other service runs on it. | I |
| INF-HST-006 | A new host is prepared by `infrastructure/host/bootstrap.sh`, which is idempotent: it installs the engine, creates the deploy user and the directories of `ci-cd.md` 9.1, sets the log limits (INF-CTR-010), installs the schedule of the backup and the prune, and prints what it did. The creation of the machine and the SSH key is done by a human (INF-PRN-010). | T |
| INF-HST-007 | After the bootstrap, the first deployment is a story (`ci-cd.md` CIC-256) with the runbook `first-deploy.md`: deploy, health check, smoke check, restore drill (INF-DAT-014). | D |
| INF-HST-008 | Operating-system updates are applied on a schedule by a human or by the host's own mechanism. A restart of the host is a planned event: all containers come back by the restart policy (INF-CTR-008), and the smoke check runs afterwards. | I |
| INF-HST-009 | A host is replaceable. Its loss is repaired by a new host, the bootstrap, a deployment of the last build id, and a restore of the last backup (`recover-host.md`). The time this takes is measured in the first drill and recorded (IP14 and the RTO of `db.md` DP11). | T |
| INF-HST-010 | Free disk space is checked before a deployment and reported after it (`ci-cd.md` CIC-260). A host below IP09 percent free disk space refuses a deployment. | T |
| INF-HST-011 | The deploy user can run the deployment commands in its own directory and nothing else; it is not an administrator of the host (`ci-cd.md` CIC-113). Administration is done by a human with a separate account. | I |

## 9. Containers

The container contents and the build rules of the images are in `ci-cd.md` 7. This section fixes what the infrastructure files must contain.

| Container | Image base (pinned) | Publishes | Health check | Volume | Memory limit |
|---|---|---|---|---|---|
| `database` | The engine of `db.md` (default PostgreSQL), a minimal variant | Nothing to the host | The engine's ready check | `db-data` (named) | IP02 |
| `migrate` (one-shot) | The backend image, role `migrate` | Nothing | Exit code 0 | None | IP01 |
| `backend` | The backend image, role `web` | Nothing to the host | `GET /health` | None | IP01 |
| `worker` (when a story needs it) | The backend image, role `worker` | Nothing | The role's own check | None | IP01 |
| `frontend` | A small web server with the built frontend | `PUBLIC_PORT` | `GET /` | None | IP03 |
| `single` | The single image (`ci-cd.md` 7.2) | `PUBLIC_PORT` | `GET /health` ready only when database, migration and backend are ready | `/data` | IP02 + IP01 |

| ID | Rule | Class | Basis |
|---|---|---|---|
| INF-CTR-001 | `compose.layers.yaml` and `compose.single.yaml` are the source of truth for the two modes. Both are valid on every environment; the mode is a parameter (`APP_MODE`, `ci-cd.md` CIC-080). | T | 12F |
| INF-CTR-002 | Images are named `<IMAGE_PREFIX>/<layer>:<build id>`. The compose files take the build id from `BUILD_ID`; they never name `latest` (`ci-cd.md` CIC-091, CIC-094). | T | 12F |
| INF-CTR-003 | One process per container (the single image is the documented exception, with a supervisor, `ci-cd.md` CIC-070). A helper process inside a layer container is a defect. | I | K8S |
| INF-CTR-004 | Containers start in dependency order by health conditions, not by fixed waits: database healthy, `migrate` completed, `backend` healthy, `frontend` started (`ci-cd.md` CIC-061). | T | REL |
| INF-CTR-005 | The backend and the frontend are stateless. They write no business data to their own disk. Temporary files go to a temporary directory and are deleted (`backend.md` BKD-DEP-009). | T | 12F |
| INF-CTR-006 | Every container handles the termination signal: it stops taking work, finishes what runs within the grace period (IP15), and exits (`backend.md` BKD-DEP-012). The compose files set the same stop period. | T | K8S |
| INF-CTR-007 | A container runs as a non-root user and with a read-only root file system where the runtime allows it; writable places are named volumes or temporary file systems. This is image hygiene, not a security feature. | I | K8S |
| INF-CTR-008 | Every container has a health check and `restart: unless-stopped` (the one-shot `migrate` has `restart: "no"`). A container that fails its check is restarted by the engine; repeated failure stops the deployment (`ci-cd.md` CIC-064). | T | REL |
| INF-CTR-009 | Every container has a memory limit (IP01 to IP03). A container that exceeds it is restarted alone and does not take the host down. | T | DEP |
| INF-CTR-010 | Log size is limited per container (maximum size and number of files, IP04), so that logs cannot fill the disk (`ci-cd.md` CIC-257). Logs go to the standard output and are collected by the engine. | T | 12F |
| INF-CTR-011 | Only the entry point publishes a port (`PUBLIC_PORT`). The database and the backend are reachable only inside the compose network (`ci-cd.md` CIC-062). | T | REL |
| INF-CTR-012 | Compose projects have a fixed name per environment (`<product>-<env>`) so that two environments on one host never share containers, networks or volumes. Ephemeral projects use the run id. | T | REL |
| INF-CTR-013 | Named volumes have fixed names with the project prefix (`<product>-<env>_db-data`). A volume is created by compose on first start and is never removed by a deployment (`ci-cd.md` CIC-258). | T | REL |
| INF-CTR-014 | A compose file contains no value that differs between environments except through variables. A value that is the same everywhere is written once. | T | 12F |
| INF-CTR-015 | A new container (a mail catcher, an object store, a cache, a broker) needs a story, a trigger of 3.3 and an entry in the README. The existing compose structure and the names of the others are not changed by it (INF-MVP-003). Adding backend modules (domains) adds no container: they are in the same artefact (`backend.md` BKD-ARC-003). | I | YAG |
| INF-CTR-016 | The database container is not recreated by a normal deployment (`ci-cd.md` CIC-133). A change of its engine version is a story with a dump, a restore and a rollback path. | T | REL |

## 10. Configuration and secrets

`ci-cd.md` 8 fixes the principles and the standard variables. This section adds the infrastructure's own variables and the places.

### 10.1 Where configuration lives

| Kind | Place | In Git? |
|---|---|---|
| Non-secret values per environment (mode, ports, names, limits, seed set) | `infrastructure/env/<env>.env` | Yes |
| Secrets (database password, registry token, keys) | The provider's secret store for the pipeline; `.env.secret` (mode 600) on the host, written at deployment | No |
| Build-time values (build id, date) | Set by the pipeline, written to labels and to the page | No (generated) |
| Local passwords | An ignored file generated at the first start | No |

### 10.2 Variables added by this document

| Variable | Meaning | Example |
|---|---|---|
| `COMPOSE_PROJECT_NAME` | Project name of the environment (INF-CTR-012) | `shop-dev` |
| `PUBLIC_PORT` | The one published port | `8081` |
| `BACKUP_DIR` | Directory of the dumps on the host | `/opt/shop/dev/backups` |
| `BACKUP_KEEP_DAYS` | Retention of dumps (IP10) | `14` |
| `BACKUP_COPY_TARGET` | Where the second copy goes (a path or an SSH target) | set per host |
| `MEM_LIMIT_BACKEND`, `MEM_LIMIT_DB`, `MEM_LIMIT_FRONTEND` | Container limits (IP01 to IP03) | `512m` |

| ID | Rule | Class |
|---|---|---|
| INF-CFG-001 | The same image runs in every environment. Everything that differs is read from variables at start. The images contain no environment name, address or limit (`ci-cd.md` CIC-063). | T |
| INF-CFG-002 | `infrastructure/env/<env>.env` holds only non-secret values and is reviewed like code. A secret in it is a defect (INF-BRN-009). | T |
| INF-CFG-003 | A missing or malformed required variable stops the start with its name; the application and the pipeline never use a hidden default for a required value (`backend.md` BKD-DEP-002, `ci-cd.md` CIC-114). | T |
| INF-CFG-004 | Secrets are written to the host at deployment from the pipeline's secret store, with mode 600, owned by the deploy user, and are not printed in logs or reports (`ci-cd.md` CIC-111). | T |
| INF-CFG-005 | The list of variables (names, meaning, required or optional, where set) is in `infrastructure/README.md` and is checked against the compose files in S04: a variable used and not listed fails the check. | T |
| INF-CFG-006 | Secrets are scoped per environment (`ci-cd.md` CIC-112). The secrets of prod are not available to dev or test runs. | I |
| INF-CFG-007 | A change of configuration on a host is made by changing the environment description and deploying. A manual edit on the host is an incident to be repaired by redeploying the file (INF-PRN-002). | I |
| INF-CFG-008 | The database account names follow `db.md` (a migrate account and a runtime account); their passwords are secrets and differ per environment. | I |

## 11. Network and entry point

| ID | Rule | Class |
|---|---|---|
| INF-NET-001 | One compose network per project. Containers reach each other by service name (`database`, `backend`, `frontend`). No container uses the host network. | T |
| INF-NET-002 | The frontend container is the entry point: it serves the built page and passes `/api` to the backend (the shape is in 22.3). The frontend knows only `API_BASE_URL` (default `/api`), so that the same image works behind any later proxy. | T |
| INF-NET-003 | Operational endpoints (`/health`) are not routed through the public path (`backend.md` BKD-IFC-012). The smoke check reaches the health detail through the published port's documented route, or through the engine inside the host. | T |
| INF-NET-004 | The published ports per environment are fixed in the environment descriptions (defaults in the table below). Ports of two environments on one host never overlap. | T |
| INF-NET-005 | The page is reached by the address of the host and the port. The same page address works for the end-to-end tests of that environment (`PUBLIC_URL`). | T |
| INF-NET-006 | Domain names, certificates and HTTPS are part of the dormant phase (section 16). The environments of the MVP are plain HTTP and are not announced publicly; the PO accepts this for projects without real data (INF-DAT-015, `workflow.md` WFL-127). | A |
| INF-NET-007 | Time-outs, body-size limits and the request deadline at the entry point equal those of the backend (`backend.md` BKD-IFC-010, BKD-DEP-004), so that the entry point does not cut a request earlier or later than the backend expects. | T |
| INF-NET-008 | A reverse proxy at the host (IX4) is added in front of the frontend containers without changing them. The environments are then reached by name on one port, and the published ports of 11 disappear from the outside. | D |

| Environment | Published port (default) | On a shared host |
|---|---|---|
| local | 8080 | not applicable |
| ephemeral | derived from the run id | not applicable |
| dev | 8081 | its own port |
| test | 8082 | its own port |
| prod | 80 on its own host; 8083 when shared (INF-HST-003) | its own port |

## 12. Data, volumes and backups

`db.md` owns the database design, migrations, seeds and import. This section owns the place of the data, the backup and the restore.

| Data | Where | Backup | Loss tolerated |
|---|---|---|---|
| Database | The named volume `db-data` | A scheduled logical dump (and later a physical backup, IX5) | The RPO (IP08) |
| Uploaded files (when a story adds them) | Database, or the object store after IX7 | With the database dump, or the object store's own | The RPO |
| Environment descriptions, code, migrations | Git | Git itself and its remote | None |
| Secrets | Provider secret store and host file | A human keeps a record outside Git | None |
| Container images | Built from Git; kept per `ci-cd.md` CP06 | Not backed up; rebuilt | None |
| Demo and reference seed | Git (`db.md`) | Not backed up; reloaded | None |

| ID | Rule | Class | Basis |
|---|---|---|---|
| INF-DAT-001 | Data that matters lives only in the database volume. Containers, images and the working directory are disposable (INF-PRN-005). | T | REL |
| INF-DAT-002 | The database is one container with one named volume per environment. The volume lives on the host's disk, not on a network share without a decision of the Architect. | T | REL |
| INF-DAT-003 | The volume is created on the first start and is never removed by a deployment or a clean-up (`ci-cd.md` CIC-258). Only `local` and `ephemeral` are removed with their volumes. | T | REL |
| INF-DAT-004 | A volume of prod, and of any environment that the PO marks as keeping data, is protected by a label and by the rule in the deployment command that refuses to touch it. | T | REL |
| INF-DAT-005 | The database server's settings for memory, connections and logging are set through the environment and checked in the health check. They fit the container limit (IP02) and the backend's pool (`backend.md` BKD-PER rules). | T | DEP |
| INF-DAT-010 | Backups are made by `infrastructure/host/backup.sh`, which writes a logical dump of the whole database (all domain schemas) with the build id and the time in its name, verifies that the file is complete (size above zero, the dump tool's own check, a test read of the table of contents), and writes the result to a log. | T | REL |
| INF-DAT-011 | The schedule is set at the host (`cron.example`): production at least every IP08 hours; dev and test only when the PO wants their data kept. A backup is also made before any production deployment that has a migration (`ci-cd.md` CIC-135). | T | REL |
| INF-DAT-012 | A backup is copied to a second place away from the host's disk (`BACKUP_COPY_TARGET`) after it is verified. A backup only on the disk that it protects is not a backup. | T | REL |
| INF-DAT-013 | Retention: IP10 daily dumps for prod and for kept environments. Older dumps are removed by `backup.sh` after the new one is verified and copied. A dump is never the only copy when removed. | T | REL |
| INF-DAT-014 | A restore drill is done by hand with `restore.sh` into a scratch environment, before the first production data and then every IP11 days. The drill checks that the application starts on the restored database, that the migrations are at the expected version and that the skeleton check is green. The result and the time are recorded (`db.md` DBS-QGT-008). | T | REL |
| INF-DAT-015 | Until the dormant phase is finished, no environment holds real personal data. Demo data is invented. A project that must hold real data first opens the dormant phase for the parts it needs (`workflow.md` WFL-080). | A | REL |
| INF-DAT-016 | The restore procedure and the recovery steps are in `restore-database.md`. They name the order: stop the application, restore into a new volume, run the migration check, start, smoke check, only then drop the old volume (by a human). | D | REL |
| INF-DAT-017 | A database upgrade (a new minor or major version of the engine) is rehearsed on a restored copy first (INF-DAT-014 environment), then done as a story with a dump and a measured downtime. | T | REL |
| INF-DAT-018 | Dumps contain data: they are treated like data (kept on the host and the second place only, not in Git, not in artifacts of the pipeline). | I | REL |

## 13. Deployment and recovery

The algorithm of a deployment, the lock, the smoke check and the automatic rollback are in `ci-cd.md` 9. This section states what the infrastructure must provide for them, and the manual procedures for the cases that the automation does not cover.

### 13.1 What the infrastructure provides to `deploy`

| Need of `deploy` (`ci-cd.md` 9.2) | Provided by |
|---|---|
| A reachable target, free disk, running engine | The host (section 8) and INF-HST-010 |
| One deployment at a time | A lock file in the environment directory (`ci-cd.md` CIC-130) |
| Image transfer without a registry | The deploy user can load images; the host has enough disk (`ci-cd.md` CIC-242) |
| `previous` and `current` build ids | `releases.log` in the environment directory |
| Environment files | `env` and `.env.secret` (section 10) |
| A backup before a risky prod deployment | `backup.sh` (INF-DAT-010) |
| Recreate with health wait | The compose files and health checks (section 9) |
| Pointer update | The pipeline, after the smoke check (INF-BRN-011) |

### 13.2 Rules

| ID | Rule | Class |
|---|---|---|
| INF-DEP-001 | Deployment to every remote environment is only by `ci/run deploy <env>` (`ci-cd.md` CIC-131). The compose files on a host are the ones written by the last deployment. | T |
| INF-DEP-002 | A deployment is complete when the containers are healthy, the smoke check passed, `releases.log` and the deployment record are written, and the pointer branch moved. Any other end is a failure with a rollback. | T |
| INF-DEP-003 | The release of a build to a new environment is the same operation as to an old one. The first deployment (a bootstrap) has no previous build and is a story (`ci-cd.md` CIC-256). | T |
| INF-DEP-004 | A deployment recreates containers briefly (a short interruption behind the health check). Zero-downtime release is deferred (IX11, `ci-cd.md` CIC-138). The interruption is announced to the team in dev and test and agreed with the PO in prod. | A |
| INF-DEP-005 | The deployment never touches the volume of the database (INF-DAT-003) and never runs a destructive command on a remote host (INF-R05). | T |
| INF-DEP-006 | The migration runs as the one-shot `migrate` container before the new backend starts, under the migration lock (`backend.md` BKD-MIG-009). The database is therefore at the new version before the new backend runs; the old backend, if rolled back to, still works because the migration follows expand and contract (`ci-cd.md` CIC-132). | T |
| INF-DEP-007 | A deployment of the single mode recreates the container and keeps `/data` (`ci-cd.md` 9.2 step 9). | T |
| INF-DEP-008 | Changes to the infrastructure files themselves (compose, scripts, environment descriptions) are deployed by the same path: pull request, merge, deploy dev, promote. An infrastructure change for prod is tested on dev and test first. | T |
| INF-DEP-009 | The bootstrap script, the backup script and the restore script are tested by the pipeline in the ephemeral environment (a dry run with a container as the host, or the first-deploy story on a scratch host) whenever they change. | T |
| INF-DEP-010 | The manual recovery procedure of `ci-cd.md` CIC-E03 (a failed rollback) is the runbook `rollback.md`: stop deployments; on the host, start the last known good build id with its compose file and environment from the record; run the smoke check; restore the database only if the data is damaged; report; then repair the cause by a story. | D |

### 13.3 Recovery cases

| Case | Procedure | Runbook |
|---|---|---|
| A container fails | The engine restarts it (INF-CTR-008). Repeated failure: the deployment's rollback, or a human looks at the container log | none |
| The new build is unhealthy | Automatic rollback to the previous build; pointer moved back | `rollback.md` |
| The database data is damaged or lost | Restore from the last verified dump into a new volume; run the migration check; smoke check; the loss is at most the RPO | `restore-database.md` |
| A migration fails halfway | The failed migration leaves the database as it was (a migration is one transaction where the engine allows, `db.md` DBS-MIG rules); fix forward with a new migration; restore only if the data is damaged | `restore-database.md` |
| The host is lost | A new host, the bootstrap, a deployment of the last build id of each environment, a restore of the last backup from the second place | `recover-host.md` |
| The disk is full | Free space by pruning unused images and old dumps (never a volume), then deploy again; if the data volume itself is full, increase the disk first | `recover-host.md` |
| The second backup place is lost | Make a new copy at once from the host; investigate; this is a finding | `restore-database.md` |
| The repository host is down | Running environments keep running; no deployment is possible; the team waits or uses the manual procedure for a hotfix with the Lead | `rollback.md` |

## 14. Dependability

Dependability has four attributes (Tanenbaum and van Steen): **availability** (the system is ready when used), **reliability** (it runs continuously without failure), **safety** (a failure has no catastrophic effect) and **maintainability** (how easily it is repaired). The infrastructure serves them in a simple, measurable form. Fault tolerance rests on masking failures by redundancy of information, time or physical resources; at the MVP the redundancy is of **information** (backups) and **time** (retries, restarts, repeated deployment), not of hardware.

| Attribute | MVP target | How it is reached | Measured by |
|---|---|---|---|
| Availability | Prod: best effort in business hours at first; the PO sets a target (IP16 is a placeholder of `workflow.md` non-functional requirements) | Restart policy, health checks, a quick rollback, a short recovery time | The smoke check; the downtime recorded in each incident |
| Reliability | The same build behaves the same in every environment | One image set, same configuration shape (INF-PRN-003, INF-PRN-004), idempotent deployment | The skeleton check on every environment; the run record |
| Safety | A failed change does not destroy data | Backups before risky changes, expand and contract, no destructive commands on remote hosts, volumes protected | The restore drill; the restrictions of section 19 |
| Maintainability | A new host is ready, and a failure repaired, within the RTO | Everything in Git, a bootstrap script, runbooks, small files | The measured rebuild time (IP14); the age of the runbooks |

### 14.1 Failure model

| Failure | Likelihood at the MVP | Effect | Masked by | Residual risk |
|---|---|---|---|---|
| A process crashes | High | A short outage of a container | Restart policy | None |
| A bad release | Medium | The page or the API is broken | Health and smoke check, automatic rollback | A short outage |
| A bad migration | Medium | The database is changed wrongly | Expand and contract, backup before prod, forward fix | A data repair by hand |
| A full disk | Medium | The host stops writing | Log limits, prune, the disk check before a deployment | Data volume growth: watch by the deployment's report |
| Operator error on a host | Medium | A wrong state | Everything by pipeline, no manual edit (INF-CFG-007), restrictions | A rebuild |
| Loss of the host or its disk | Low | Everything on it is gone | Bootstrap, redeploy, restore from the second copy | Loss up to the RPO, downtime up to the RTO |
| Loss of the network | Low | The environment is unreachable | None at the MVP | Wait |
| A fault in the provider's zone | Low | As the loss of a host | None at the MVP; a second host or zone is an extension (IX6, IX11) | Accepted by the PO |

| ID | Rule | Class | Basis |
|---|---|---|---|
| INF-DPD-001 | The recovery targets are written down: RPO IP08 hours and RTO IP14 hours as the MVP defaults (`db.md` DP10, DP11), changed only with the PO. The backup schedule and the drills are derived from them. | A | DEP |
| INF-DPD-002 | The availability target of prod is agreed with the PO before the first production release and recorded in `requirements.md`. Without a target, the MVP gives best effort and says so. | A | DEP |
| INF-DPD-003 | Failures that the MVP does not mask are listed in the failure model (14.1) with the PO's acceptance. A new known risk is added to the table at the epic gate (`workflow.md` 10.3). | A | DEP |
| INF-DPD-004 | A single point of failure is allowed when it is listed, accepted and has a recovery runbook. The host and the database volume are the known single points of the MVP. | D | DEP |
| INF-DPD-005 | Retry and restart are bounded: containers restart with the engine's back-off; a deployment is retried once for a transient fault; no component retries forever (`ci-cd.md` CIC-252, `backend.md` BKD-DEP-008). | T | DEP |
| INF-DPD-006 | Operations are idempotent so that repeating them is the recovery for a crash in the middle: deployment (`ci-cd.md` CIC-134), bootstrap (INF-HST-006), backup (INF-DAT-010), seed (`db.md`). | T | DEP |
| INF-DPD-007 | A change that could reduce availability (an engine upgrade, a host change, a mode change) is a story with a rollback path, a measured downtime and a window agreed with the PO (INF-DEP-004). | A | DEP |
| INF-DPD-008 | Each runbook is tested once when written and again after any change of the procedure it describes. A runbook that was never run is marked "untested" at its head. | T | DEP |
| INF-DPD-009 | Incidents get a short written record (what happened, the time to detect and repair, the cause, the follow-up story). Without observability tools, the record is the learning loop (`workflow.md` self-learning). | D | DEP |
| INF-DPD-010 | Scalability at the MVP is vertical (a larger host) and by running more identical backend instances only after the triggers of section 15. Size, geographic and administrative scalability are not goals of the MVP. | D | DEP |

## 15. The growth path

The infrastructure grows by a ladder. A step is taken when its trigger is true and not before (INF-PRN-009). Each step keeps the images, variables, commands and health endpoints (INF-MVP-003).

### 15.1 The ladder

| Step | Change | Trigger | Changes only |
|---|---|---|---|
| 0 | One host, compose, layers or single (the MVP) | Start | |
| 1 | Prod on its own host (IX1); test and dev on theirs when needed (IX2) | First real user, or disturbance between environments | Host list, environment files |
| 2 | A registry instead of image transfer (IX3) | A second host | `publish` and `deploy` commands |
| 3 | A reverse proxy at the host (IX4); names and certificates arrive with the dormant phase | More than one environment per port, or names required | A new container or host service in front |
| 4 | A separate or managed database (IX6) | Memory or disk of the shared host, or availability | Database connection variables, backup method |
| 5 | A second backend instance behind the proxy; the `worker` role as its own container (`backend.md` BKD-TRG-011) | Load, or a different scaling profile for the worker | Compose or deployment files |
| 6 | Off-site and point-in-time backups (IX5) | A shorter RPO | `backup.sh` or a managed service |
| 7 | An orchestrator or a managed container service (IX9) | Section 15.3 | Deployment files (15.2); the images stay |
| 8 | A message broker (IX10) | Section 15.4 | An adapter behind the dispatcher (`backend.md` BKD-TRG-003) |

### 15.2 Staying Kubernetes-ready without Kubernetes

Kubernetes runs containers in pods, restarts them, scales them and routes to them by services. It needs the application to behave in the ways listed in INF-K8S. Compose files map to its objects as follows.

| Compose today | Kubernetes object later |
|---|---|
| `service` (backend, frontend) | `Deployment` and `Service` |
| Database container with a named volume | A managed database, or a `StatefulSet` with a persistent volume claim (a managed database is preferred) |
| `env` file and `.env.secret` | `ConfigMap` and `Secret` |
| `healthcheck` | Liveness and readiness probes |
| `restart: unless-stopped` | The controller's restart behaviour |
| `mem_limit` | Resource requests and limits |
| `migrate` one-shot | A `Job`, or an init step, run before the new `Deployment` |
| `worker` role container | A second `Deployment` of the same image |
| The frontend's `/api` pass-through | An `Ingress` or a gateway |
| Published port | A `Service` of type load balancer, or the ingress |
| `ci/run deploy` over SSH | `ci/run deploy` calling the orchestrator's client or a GitOps controller that reads the pointer branch |

| ID | Rule | Class | Basis |
|---|---|---|---|
| INF-K8S-001 | The backend and the frontend are stateless and run as many identical copies without coordination (`backend.md` BKD-ARC-007, BKD-DEP-009). | T | K8S |
| INF-K8S-002 | Configuration comes only from the environment; secrets are separate from configuration (INF-CFG-001, INF-CFG-004). | T | K8S |
| INF-K8S-003 | Every container has separate liveness and readiness behaviour: not ready while starting or while a dependency is down; alive while the process can work (`backend.md` BKD-IFC-012). | T | K8S |
| INF-K8S-004 | Containers log to the standard output, handle the termination signal in the grace period and are started and stopped quickly (`backend.md` BKD-DEP-011, BKD-DEP-012). | T | K8S |
| INF-K8S-005 | Migrations run as a one-shot before the new version and are compatible with the previous one, so that rolling updates and rollbacks work (`backend.md` BKD-MIG-009, INF-DEP-006). | T | K8S |
| INF-K8S-006 | The pointer branch of an environment is the desired state of that environment. A GitOps controller, if added, reads the pointer and applies it; it does not require environment branches that differ in content (INF-BRN-018). | D | K8S |
| INF-K8S-007 | An orchestrator is never introduced to solve a problem that a bigger host, a second instance behind a proxy or a better index would solve (`backend.md` BKD-MVP-007). | D | YAG |

### 15.3 Trigger for an orchestrator (IX9)

Two or more of these five are true, measured or contracted, not guessed. They are heuristics from practice reports and are adjusted by the Architect.

| # | Criterion |
|---|---|
| 1 | More than 1,000 users at the same time, or traffic spikes that one host cannot absorb, expected within six months |
| 2 | An availability target of 99.9 % or higher is contracted or business-critical |
| 3 | The team has, or will hire, a person who can run an orchestrator (a managed service lowers this need) |
| 4 | More than two to three hours a week go into manual deployment or infrastructure care |
| 5 | More than ten separately deployed services need independent scaling and networking |

A **managed** container service or a managed orchestrator is preferred to a self-run one, because the cost is in operating it (INF-K8S-007). The decision is recorded in `architecture.md` with the criteria that were true.

### 15.4 Trigger for a message broker such as Kafka (IX10)

The MVP uses the outbox table with a relay in the `worker` role (`db.md`, `backend.md` BKD-MVP-007). A database-backed outbox is enough until about five consumers or about a thousand messages a second. A broker is considered when two or more of these five are true.

| # | Criterion |
|---|---|
| 1 | Several independent systems must read the same stream, each at its own pace |
| 2 | Historical events must be replayed or reprocessed |
| 3 | The sustained rate is above about 10,000 events a second |
| 4 | Strict order within a key is required at a scale the outbox relay cannot give |
| 5 | The need is a data pipeline (change data capture, analytics) rather than point-to-point messaging |

| ID | Rule | Class | Basis |
|---|---|---|---|
| INF-EVT-001 | A work queue where each message is handled once and deleted, a notification, or a job needs no streaming platform: the job table of `backend.md` BKD-TRG-008 is used. | D | EVT |
| INF-EVT-002 | A broker is operated by a person or bought as a managed service. A self-run cluster costs the work of one or two people in a production setting; below that, a managed service or no broker is the choice. | D | EVT |
| INF-EVT-003 | A broker is added behind the dispatcher interface and the outbox: the domains publish as before, and a relay publishes to the broker. The producers do not change (`backend.md` BKD-TRG-003). | D | EVT |
| INF-EVT-004 | Events keep the contract of `api.md` and `db.md`: versioned, idempotent consumers, a business key for deduplication. A broker does not remove the need for idempotency (`backend.md` BKD-DEP-015). | T | EVT |

## 16. Deferred areas

| Area | State | What is reserved now |
|---|---|---|
| Security: access control, hardening, TLS and certificates, firewall policy, secret rotation, scans, audit | Not created; no-touch until all other phases are finished (`workflow.md` WFL-080, WFL-120). The document `security.md` is not created; the decision and its trigger are recorded in `architecture.md` | The host's SSH-key-only deploy user and the secret scope rules (INF-CFG-006) are kept as basic hygiene that already exists. A place for TLS is the reverse proxy (INF-NET-008); a place for secrets rotation is `.env.secret` (INF-CFG-004) |
| Observability: metrics, tracing, alerting, dashboards | Not created; no-touch. The document `observability.md` is not created; recorded in `architecture.md` | Health endpoints, container logs with limits (INF-CTR-010), the run record and the incident record (INF-DPD-009) |

| ID | Rule |
|---|---|
| INF-DEF-001 | No agent adds security or monitoring tools, containers, host services or scans to the infrastructure before the dormant phase is opened. An attempt is stopped and recorded (`workflow.md` WFL-120). |
| INF-DEF-002 | The rules above that look like security (SSH key only, no secrets in Git, non-root containers, secret scoping, log masking) are correctness and hygiene rules that stay active. They are not a security phase. |
| INF-DEF-003 | The Auditor lists, at each epic gate, what is deferred and what risk the PO accepted (INF-NET-006, INF-DAT-015, `workflow.md` WFL-127). |
| INF-DEF-004 | When the dormant phase opens, the first steps are: names and certificates at the proxy, access to the environments, rotation of secrets, then monitoring and alerting. Each is a story and a trigger-driven extension (IX13). |

## 17. Epic 0: the technical setup stories

Goal G-01 has Epic E-00. Its stories come from this document (`workflow.md` WFL-073). The order is the order of the dependencies; each story passes the 16 stages of `workflow.md` and ends green and deployable. The aim is the skeleton page deployed locally and on one remote server (`workflow.md` 9.2).

| Story | Title | Outcome | Acceptance (suggested) |
|---|---|---|---|
| US-00.01 | Repository and layout | The monorepo with the area folders, `infrastructure/`, `ci/`, the project documents, the protected `main` and the pointer branches (empty) | `main` is protected; a story branch and a pull request work; the layout matches section 6 |
| US-00.02 | Toolchain and commands | Pinned toolchains and `ci/run` with the Level 1 commands as thin wrappers | `ci/run prepare` passes on a clean checkout |
| US-00.03 | Database container and first migration | The database container, one migration, the seed with the skeleton row, idempotent | Migrate from empty, seed twice, row present once |
| US-00.04 | Backend skeleton | The backend with `GET /health` and one endpoint that reads the skeleton row | Health and the endpoint answer; roles `web` and `migrate` start |
| US-00.05 | Frontend skeleton page | The header, body with placeholders, footer and the one element from the backend, build id and environment name | The page shows the backend value and the build id |
| US-00.06 | Layered compose and local environment | `compose.layers.yaml` and `ci/run env-up layers` | One command, page works, second start changes nothing |
| US-00.07 | Single image | `compose.single.yaml` and the single image | Smoke check green in the single container |
| US-00.08 | Local pipeline | `ci/run all-local` with the eight local steps and the test skeleton | The skeleton tests pass; reports are written |
| US-00.09 | Minimal central pipeline | The wrapper of the chosen provider: pull request run and merge run | A pull request gets a verdict; a merge builds and publishes |
| US-00.10 | Host bootstrap and first deployment | `bootstrap.sh`, the first deployment to the remote host, the smoke check, the pointer `env/dev` | The skeleton page is reachable on the remote host with the build id |
| US-00.11 | Rollback | The rollback tested once on the first host | A deliberately broken build is rolled back; the page runs the previous build |
| US-00.12 | Backup and restore drill | `backup.sh`, `restore.sh`, the schedule and the first drill | A restore into a scratch environment passes the skeleton check; the time is recorded |
| US-00.13 | Agents and rules | `CLAUDE.md`, the agent files and the project's decision records are in place | Setup readiness of `workflow.md` is complete |

| ID | Rule |
|---|---|
| INF-EP0-001 | Epic 0 ends when the setup readiness items of `workflow.md` are complete and the skeleton page runs locally and on the first remote host, deployed by the pipeline, with its tests green on both. |
| INF-EP0-002 | Test and prod are not created in Epic 0. They are created at G-04 or earlier when a trigger of 3.3 fires (`workflow.md` G-04). Their descriptions in `infrastructure/env/` MAY exist from Epic 0 and are reported "not available" until the hosts exist (INF-ENV-008). |
| INF-EP0-003 | The stories above are the minimum. A story may be split, but none is dropped without an Architect decision that is recorded. |
| INF-EP0-004 | The order may change when a dependency allows it. US-00.12 (backup and restore) MAY follow US-00.10 at once, and MUST be complete before the first data that matters. |

## 18. Edge cases

| ID | Case | Handling |
|---|---|---|
| INF-E01 | A person asks for branches `dev`, `test`, `prod` that hold their own code. | Not created. The pointer branches `env/dev`, `env/test`, `env/prod` give the same view in Git (what runs where) without different code per environment (INF-BRN-001, INF-BRN-018). |
| INF-E02 | A change is pushed directly to a pointer branch by a person. | Rejected by the branch protection. If it got through, the pipeline resets the pointer to the recorded build id at the next deployment and the Auditor lists it (INF-BRN-011). |
| INF-E03 | A hotfix is merged to `main` and the pointer of prod points to a commit that is not an ancestor of the new `main`. | Allowed: a pointer need only name a commit that exists on `main` (INF-BRN-016). The promotion moves it forward. |
| INF-E04 | Two merges follow each other quickly and the deployment of the first is still running. | The second waits for the environment lock or cancels the first if it has not started deploying (`ci-cd.md` CIC-043, CIC-130). The pointer moves only after the last green deployment. |
| INF-E05 | The pointer branch cannot be pushed (the Git server is down) after a successful deployment. | The deployment is still valid; `releases.log` and the deployment record are the proof. The pointer is moved by the next run or by a manual run; the difference is reported until fixed. |
| INF-E06 | The smoke check passes but the pointer update fails because of a permission fault. | The step reports a failure of the pointer; the environment keeps the new build; a human fixes the permission; the pointer is moved by a manual run. |
| INF-E07 | A rollback moves a pointer backwards and a "force" protection of the server rejects it. | The pointer branches are exempt from the force rule for the pipeline's identity only (INF-BRN-015). If the server cannot exempt one identity, the pointer is a file `env/<name>` in a pointer repository or a tag set by the pipeline; the semantics stay the same. |
| INF-E08 | A provider has no manual run button. | The promotion is started by the push of the pipeline's identity to the pointer branch (INF-BRN-017). |
| INF-E09 | Dev, test and prod share one host and test uses too much memory. | The memory limits keep the other environments alive (INF-CTR-009). If the limits are hit often, IX2 and IX1 fire; the move is a story. |
| INF-E10 | Two environments on one host get the same published port by mistake. | The start of the second fails with a clear message; the check in S04 compares the ports of all environment files (INF-NET-004). |
| INF-E11 | The host runs out of disk during a deployment. | The deployment checks the disk first and refuses (INF-HST-010). If it happens in the middle, the lock is released, the previous build keeps running, images and old dumps are pruned, and the deployment is repeated. |
| INF-E12 | The image transfer is interrupted. | The digest check fails; the deployment stops before any change; it is repeated (`ci-cd.md` CIC-242, CIC-137). |
| INF-E13 | The host is rebooted at the wrong time. | All containers return by the restart policy; the health checks decide readiness; the scheduled smoke check or the next deployment finds a fault (INF-HST-008). |
| INF-E14 | The database container is started before its volume's permission fits. | The container fails its health check; the deployment stops. The bootstrap sets the volume's owner correctly (INF-HST-006). |
| INF-E15 | The backup script succeeds but the dump is empty or truncated. | The verification step fails the backup and reports it (INF-DAT-010). A failed backup is never treated as a backup. |
| INF-E16 | The second backup place is full or unreachable. | The backup is marked "not copied" and reported; the old dumps are not removed (INF-DAT-013). |
| INF-E17 | The restore drill shows that the dump cannot be restored. | A finding of the highest class: the backup method is changed first, and no production data is accepted until a drill passes (INF-DAT-014). |
| INF-E18 | A restore is needed during a running deployment. | The lock stops new deployments. The restore follows `restore-database.md`; the deployment is abandoned and repeated after. |
| INF-E19 | A migration changes the database in a way that the previous build cannot read, and the new build must be rolled back. | The rule forbids it (INF-DEP-006). If it happened, the rollback restores the previous build and the database from the backup taken before the deployment (INF-DAT-011). |
| INF-E20 | A new domain is added to the backend. | No new container (INF-CTR-015). Its schema and migrations are in the same database and deployment. |
| INF-E21 | A story needs file upload. | The Architect decides: files in the database for small sizes, or IX7 (object storage) with a story; the compose structure is extended, not rewritten. |
| INF-E22 | A story needs to send mail. | Local and ephemeral use a mail catcher container; remote mail is IX8 and the dormant phase if it needs credentials. |
| INF-E23 | A story needs a scheduled task. | A job in the `worker` role under the database lease (`backend.md` BKD-DEP-019); not a cron entry in the container. Host cron is used only for backups and prune (INF-DAT-011). |
| INF-E24 | A developer wants to publish the database port for a tool. | Allowed locally by an uncommitted override (INF-LOC-007); never on remote hosts. |
| INF-E25 | The first environment is made on a host with an old container engine. | The bootstrap checks the engine and compose versions and stops with a message (INF-HST-006). |
| INF-E26 | A host has a different CPU architecture than the build machine. | The images are built for the architecture of the hosts; the architecture is a value in the README and the build; a mismatch fails S06 or the load. |
| INF-E27 | A base image is withdrawn or changed upstream. | Images are pinned by version and by digest where possible; a rebuild fails visibly; the dependency register is updated by a story. |
| INF-E28 | The clock of the host is wrong. | Time synchronisation is a bootstrap item; expiry and logs depend on it. A host with a drifting clock fails the check of the deployment (INF-HST-005). |
| INF-E29 | A person changes a file on the host by hand to fix an incident. | The next deployment overwrites it (INF-CFG-007); the change is written into an incident record and, if right, made in Git. |
| INF-E30 | Dev data was filled with real data by a tester. | Against INF-DAT-015: the data is removed, the volume of that environment is recreated by a human decision, and the incident is recorded. |
| INF-E31 | The PO wants a stable demo for a customer from dev. | A `demo` environment is added through a story (INF-ENV-001); dev stays free for work. |
| INF-E32 | A request to run a new tool on the host (a monitoring agent, a scanner). | Refused (INF-DEF-001) until the dormant phase; recorded as a request for that phase. |
| INF-E33 | The team grows or the load grows and the host is too small. | A larger host (vertical growth) first; step 1 and step 5 of the ladder only on their triggers (INF-K8S-007). |
| INF-E34 | A person proposes Kubernetes or Kafka for the MVP. | The criteria of 15.3 or 15.4 are applied with measured values; below two true criteria, the answer is recorded and no change is made (INF-K8S-007, INF-EVT-001). |
| INF-E35 | The `single` mode is chosen for prod and the product later needs separate scaling. | A mode change is a migration with a dump and a restore (`ci-cd.md` CIC-081); planned as a story with a measured downtime. |
| INF-E36 | The same build must run in an environment with a different base path. | Only `API_BASE_URL` and `PUBLIC_URL` change (INF-NET-002); a rebuild for a base path is a defect. |
| INF-E37 | The repository grows large because of binary files. | The size check in S04 reports it (INF-BRN-006); the Architect decides on an asset store (INF-BRN-007). |
| INF-E38 | A secret was committed. | INF-BRN-009: replace the secret first, then clean the history with the Lead. |
| INF-E39 | The provider's secret store is lost or its value is unknown. | A human creates new values, places them at the store and at the host, and deploys; agents never see them (INF-PRN-010). |
| INF-E40 | The remote credentials expire. | The deployment fails at the check of the target (`ci-cd.md` 9.2 step 2) with a clear message; a human renews them. |
| INF-E41 | Two people start a manual rollback at once. | The lock lets one proceed; the second sees the lock and stops (`ci-cd.md` CIC-130). |
| INF-E42 | The ephemeral environment is not removed after a cancelled run. | The cleanup runs on cancellation; leftovers are found by name prefix and removed by the scheduled clean-up of the runner or a manual command (`ci-cd.md` CIC-255). |
| INF-E43 | A restore is done into an environment with a newer migration version than the dump. | The migration runs forward at start (`migrate` role) and is checked; a dump from a newer version than the code is refused with a message. |
| INF-E44 | The PO asks for a different engine than the default database. | Allowed by `db.md`'s engine section; the database container, the backup and the restore scripts change for that engine; the rest is unchanged. |

## 19. Restrictions

| ID | Restriction |
|---|---|
| INF-R01 | No branch holds code that differs between environments. Environment branches with their own commits are forbidden. |
| INF-R02 | No person pushes to a pointer branch. |
| INF-R03 | No secret, credential, key, token or dump is committed, built into an image or written to a log or report. |
| INF-R04 | No manual change on a remote host as the way to change the state of an environment. Hosts change by deployment or by the bootstrap. |
| INF-R05 | No destructive command on a remote host from a pipeline or agent: no volume removal, no `down -v`, no `volume prune`, no deletion of the data directory, no drop of a database (`ci-cd.md` CIC-258). Only `local` and `ephemeral` are removed with their volumes. |
| INF-R06 | No agent creates or holds the credentials of a host, a registry or the secret store (INF-PRN-010). |
| INF-R07 | No image uses `latest` or an unpinned base. |
| INF-R08 | No environment-specific value inside an image. |
| INF-R09 | No real personal data in any environment before the dormant phase is finished. |
| INF-R10 | No security or observability tool, host service or container before the dormant phase is opened. |
| INF-R11 | No orchestrator, broker, cache, search engine, object store, mail relay or second database engine without a trigger of 3.3 and a recorded decision. |
| INF-R12 | No publication of the database port on a remote host. |
| INF-R13 | No deployment to prod without the PO's approval and a verified backup before a migration. |
| INF-R14 | No deployment that skips health and smoke checks, and no pointer update without them. |
| INF-R15 | No container without a memory limit, a health check and a log limit. |
| INF-R16 | No build for a single environment: one image set is promoted (INF-PRN-003). |
| INF-R17 | No second copy of an environment's compose structure for another environment: differences are variables. |
| INF-R18 | No backup counted as valid before it is verified and copied away from the host; no restore procedure counted as valid before it has been run. |
| INF-R19 | No shared host for prod and other environments after the first real user. |
| INF-R20 | No change of the database engine version without a dump, a rehearsal and a rollback path. |
| INF-R21 | No infrastructure file over the size limits of INF-REP-002 without a decision to keep it. |
| INF-R22 | No new environment name, host, port or variable that is not listed in the README and, where needed, in section 4 or 11. |

## 20. Quality gates

| Gate | Checked | When | By |
|---|---|---|---|
| Layout and README present, shortcuts registered | `infrastructure/` matches section 6; INF-MVP-001 | Every pull request that touches `infrastructure/` | Reviewer |
| Variables listed and used | Variable list equals variables in compose and environment files (INF-CFG-005) | S04 | DevOps, pipeline |
| Ports and names unique | INF-NET-004, INF-CTR-012 | S04 | Pipeline |
| Images pinned, no `latest` | INF-R07 | S04 | Pipeline |
| One command local start works from clean | INF-LOC-001, INF-LOC-002 | The canary story, each change of compose or scripts | DevOps |
| Scripts tested | INF-DEP-009 | Each change of a script | Pipeline |
| Deployment and rollback tested | `ci-cd.md` CIC-143 | First host; once per epic on test | Tester, Auditor |
| Restore drill recorded | INF-DAT-014 | Before first data that matters; then every IP11 days | DevOps, Database Engineer |
| Pointers match the records | INF-BRN-016 | Each epic gate | Auditor |
| Shortcut clearing | INF-MVP-001 | Each epic gate | Auditor |
| Failure model and recovery targets current | INF-DPD-001, INF-DPD-003 | Each epic gate and before the first production release | Architect, PO |
| Runbooks tested | INF-DPD-008 | When written; after change | DevOps |

Definition of Done for an infrastructure story: the files are in Git and reviewed; the change ran through the pull request run; it is deployed to dev by the pipeline; the affected runbook is updated and tested; the README lists new variables, ports and shortcuts; no restriction of section 19 is broken.

## 21. Parameters

| ID | Parameter | Default | Note |
|---|---|---|---|
| IP01 | Memory limit of backend and worker containers | 512 MB | Fit to the runtime's setting (`backend.md` BKD-DEP-023) |
| IP02 | Memory limit of the database container | 1 GB | Raise by measured need |
| IP03 | Memory limit of the frontend container | 128 MB | |
| IP04 | Log limit per container | 10 MB, 3 files | |
| IP05 | Minimum CPU cores of a host for one environment | 2 | More for a shared host |
| IP06 | Minimum memory of a host for one environment | 4 GB | About 8 GB for a shared host |
| IP07 | Minimum disk of a host for one environment | 40 GB | Includes images and dumps |
| IP08 | Backup interval, equal to the default RPO | 24 hours | `db.md` DP10 |
| IP09 | Minimum free disk to allow a deployment | 20 % | |
| IP10 | Retention of daily dumps | 14 (prod), 7 (kept dev and test) | |
| IP11 | Restore drill interval after the first | 90 days | |
| IP12 | Size limits of an infrastructure file | Compose about 150 lines; script about 100 lines | Signal to simplify |
| IP13 | Maximum time before a hotfix is merged to `main` | 24 hours | |
| IP14 | Host rebuild time, equal to the default RTO | 4 hours | `db.md` DP11 |
| IP15 | Stop grace period of containers | 30 seconds | `backend.md` BKD-DEP-012 |
| IP16 | Availability target of prod | Best effort until the PO sets one | INF-DPD-002 |

## 22. Templates

The templates are shapes. They name no product and carry no secret; the real files are written from them by the DevOps Engineer and reviewed. The database shown is PostgreSQL, the default of `db.md`; another engine changes the `database` service only.

### 22.1 `compose.layers.yaml`

```yaml
name: ${COMPOSE_PROJECT_NAME}
services:
  database:
    image: postgres:16-alpine            # pinned version; one engine, `db.md`
    restart: unless-stopped
    environment:
      POSTGRES_DB: ${DB_NAME}
      POSTGRES_USER: ${DB_USER}
      POSTGRES_PASSWORD: ${DB_PASSWORD}  # from .env.secret
    volumes: [ "db-data:/var/lib/postgresql/data" ]
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U ${DB_USER} -d ${DB_NAME}"]
      interval: 5s
      timeout: 3s
      retries: 20
    mem_limit: ${MEM_LIMIT_DB}
    stop_grace_period: 30s
    logging: { driver: json-file, options: { max-size: "10m", max-file: "3" } }

  migrate:                               # one-shot, backend image in the role `migrate`
    image: ${IMAGE_PREFIX}/backend:${BUILD_ID}
    restart: "no"
    command: ["migrate"]
    env_file: [ env, .env.secret ]
    depends_on: { database: { condition: service_healthy } }

  backend:
    image: ${IMAGE_PREFIX}/backend:${BUILD_ID}
    restart: unless-stopped
    command: ["web"]
    env_file: [ env, .env.secret ]
    depends_on: { migrate: { condition: service_completed_successfully } }
    healthcheck:
      test: ["CMD", "/app/healthcheck"]  # calls GET /health inside the container
      interval: 10s
      timeout: 3s
      retries: 12
    mem_limit: ${MEM_LIMIT_BACKEND}
    stop_grace_period: 30s
    logging: { driver: json-file, options: { max-size: "10m", max-file: "3" } }

  frontend:
    image: ${IMAGE_PREFIX}/frontend:${BUILD_ID}
    restart: unless-stopped
    ports: [ "${PUBLIC_PORT}:80" ]       # the only published port
    depends_on: { backend: { condition: service_healthy } }
    healthcheck:
      test: ["CMD", "wget", "-qO-", "http://localhost/"]
      interval: 10s
      timeout: 3s
      retries: 6
    mem_limit: ${MEM_LIMIT_FRONTEND}
    logging: { driver: json-file, options: { max-size: "10m", max-file: "3" } }

volumes:
  db-data: {}                            # named; never removed by a deployment
```

### 22.2 `infrastructure/env/dev.env`

```text
APP_ENV=dev
APP_MODE=layers
COMPOSE_PROJECT_NAME=<product>-dev
IMAGE_PREFIX=<registry-or-local-prefix>/<product>
PUBLIC_PORT=8081
PUBLIC_URL=http://<host-address>:8081
API_BASE_URL=/api
SEED_SET=reference+demo
DB_HOST=database
DB_PORT=5432
DB_NAME=<product>
DB_USER=<runtime-account>
MEM_LIMIT_DB=1g
MEM_LIMIT_BACKEND=512m
MEM_LIMIT_FRONTEND=128m
BACKUP_KEEP_DAYS=7
# no secrets here: DB_PASSWORD and the migrate account's password are in .env.secret
```

### 22.3 The entry point of the frontend container (shape)

```text
server {
  listen 80;
  root /usr/share/web;                    # the built frontend
  location /api/ {                        # same prefix as API_BASE_URL
    proxy_pass http://backend:8080;       # service name inside the compose network
    proxy_set_header Host $host;
    proxy_set_header X-Forwarded-For $remote_addr;
    proxy_set_header X-Forwarded-Proto $scheme;
    proxy_read_timeout 30s;               # equals the backend deadline (INF-NET-007)
    client_max_body_size 10m;             # equals the backend limit (INF-NET-007)
  }
  location / { try_files $uri /index.html; }   # single-page fallback
}
```

### 22.4 `infrastructure/host/backup.sh` (shape)

```text
set -eu
stamp=$(date -u +%Y%m%dT%H%M%SZ)
file="$BACKUP_DIR/<product>-<env>-$BUILD_ID-$stamp.dump"
docker exec <project>-database-1 pg_dump -U "$DB_USER" -Fc "$DB_NAME" > "$file"   # all schemas
test -s "$file"                                         # not empty
docker exec -i <project>-database-1 pg_restore -l < "$file" > /dev/null   # readable table of contents
copy "$file" "$BACKUP_COPY_TARGET"                      # a second place; scp, rsync or a mount
find "$BACKUP_DIR" -name '*.dump' -mtime +"$BACKUP_KEEP_DAYS" -delete   # only after the copy
echo "backup ok $file"
```

### 22.5 `infrastructure/host/bootstrap.sh` (outline)

| # | Action | Idempotent check |
|---|---|---|
| 1 | Verify the container engine and compose plugin versions (install if the OS allows) | Version output |
| 2 | Create the deploy user without a password and with the authorised key supplied by the human | User exists |
| 3 | Create `/opt/<product>/<env>/` with `backups/`, owned by the deploy user | Directory exists |
| 4 | Set the engine's default log limits (IP04) | File content equal |
| 5 | Install the backup and prune schedule from `cron.example` | Entry exists once |
| 6 | Check time synchronisation | Service active |
| 7 | Print the free disk space and the list of what was done | Output |

### 22.6 The pointer update (after a green deployment)

```text
git fetch origin
git push --force-with-lease origin ${BUILD_ID}:refs/heads/env/${ENV}
# the pipeline's identity only; people cannot push to env/* (INF-BRN-011)
```

### 22.7 Runbook head

```text
# Runbook: <title>        Status: tested <date> | untested
Purpose: ...
When to use: ...
Before: (who, what to stop)
Steps: 1 ... 2 ... (each step with the expected result)
After: (the checks: smoke, version, data)
If it fails: (where to stop, whom to tell)
Last changed by story: <id>
```

## 23. Sources and limits

| Source | Used for |
|---|---|
| [Harness: how Git strategy can break your database pipeline](https://www.harness.io/blog/how-git-strategy-can-break-your-database-pipeline) | One trunk, environments promoted from the same history, the risk of environment branches for database changes |
| [Codefresh: stop using branches for deploying to different GitOps environments](https://codefresh.io/about-gitops/branches-gitops-environments/) and the Octopus and Cloudogu articles on GitOps promotion | Why environment branches with own content are discouraged (found by search; the page itself was not read) |
| [ThirstySprout: Docker Compose vs Kubernetes](https://www.thirstysprout.com/post/docker-compose-vs-kubernetes) | The five criteria of 15.3. The article's own numbers are not sourced and partly inconsistent; they are used as adjustable heuristics only |
| [Confluent: when to use Apache Kafka, and when not](https://www.confluent.io/blog/architectural-decision-guide-when-to-use-apache-kafka-and-when-you-shouldnt/) | The five criteria of 15.4 and the operating cost. A vendor's article: read with that in mind |
| Tanenbaum and van Steen, *Distributed Systems* (the dependability attributes and fault tolerance by redundancy) | Section 14. The book was not available; the attributes are taken from the standard description of the book's chapter on fault tolerance and from search results |
| Twelve-factor app principles | Configuration by environment, stateless processes, disposability, logs to the output |
| The uploaded infrastructure tasks (Docker Compose, reverse proxy, environment variables, init scripts, no new container for new backend modules) | The service shapes and the "no extra container per module" rule. The identity-provider and object-store parts were not taken: security and storage are deferred |
| `ci-cd.md`, `db.md`, `backend.md`, `workflow.md` | The interfaces this document must fit |

Limits of this version. The books named in the request (a Kubernetes book, Kafka: The Definitive Guide, Tanenbaum's book on dependable and scalable systems, and a Git book) were not read in full: only web sources and summaries were. No host was created and no compose file was run, so the templates are shapes that need testing in Epic 0. The growth ladder and its numeric criteria are heuristics for the Architect to adjust.
