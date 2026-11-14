# Deferred-risk register

Rows are accepted by the Product Owner in writing; dates are filled at acceptance.

| Risk | Source | Accepted by | Reviewed at |
|---|---|---|---|
| No authentication, no roles, no access control (engine has no login; all projects, ZIPs and links visible to and changeable by anyone who opens the engine, ADR-034) | WFL-120, ADR-020, QR-SEC-05 | Product Owner, date | Each goal gate |
| Engine container has the host Docker socket; sandbox uses a privileged Docker-in-Docker sidecar | engine-architecture.md 11 | Product Owner, Architect, date | Each goal gate |
| Coding agents in the sandbox have network access | engine-architecture.md 11 | Product Owner, date | Each goal gate |
| Users' API keys and GitHub tokens held in engine memory for the session and its runs (never stored); anyone can continue a project that pushes to the starter's public GitHub repository | ADR-033, ADR-034, ADR-035, QR-SEC-01 | Product Owner, date | Each goal gate |
| No monitoring or alerting | ADR-021 | Product Owner, date | Each goal gate |
| MVP shortcuts in each area (the registers) | AUD-T13 | Architect | Each epic gate |
