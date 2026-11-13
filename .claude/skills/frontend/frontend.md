# Frontend Architecture

**Status:** Frontend architecture source of truth  
**Scope:** Frontend architecture, UX/UI implementation, backend API integration, and basic component unit tests  
**Related authority:** [`ux-ui.md`](./ux-ui.md)

## 1. Authority

This document is authoritative for the architecture of the independent `frontend/` area of the monorepo.

`ux-ui.md` is authoritative for designing and styling the frontend, including:

- Page and user-facing component structures.
- Component anatomy and interaction behavior.
- Design tokens, colour, typography, spacing, imagery, and themes.
- Responsive layouts and required visual and reading order.
- Navigation, routes, content, and user-facing states.
- Accessibility outcomes and UX acceptance criteria.

`frontend.md` defines how those requirements are implemented and organized in frontend code. It must reference, not duplicate or redefine, the UX/UI contract. If the documents conflict on a user-facing requirement, `ux-ui.md` wins. If they conflict on frontend architecture or code ownership, `frontend.md` wins only when the result still satisfies `ux-ui.md`.

Normative language:

- **Must**, **must not**, **required**, **prohibited**, and **only** are mandatory.
- **Should** is the expected default; deviation requires a documented technical reason.
- **May** is optional.
- A specific rule overrides a general rule. If equally specific rules conflict, the later rule wins.

## 2. Scope

The monorepo has independent frontend and backend areas. Each may organize its own code by corresponding semantic domains. This document plans only `frontend/`.

The frontend communicates with the backend only through documented API contracts. It does not define or depend on backend source structure, database structure, persistence, infrastructure, or internal backend services.

This document does not define:

- Backend or database architecture.
- Internationalisation.
- CI/CD or deployment pipelines.
- A mandatory frontend framework, router, state library, test runner, or build system.
- Test levels beyond the basic component unit-test baseline.
- Visual requirements already governed by `ux-ui.md`.

## 3. Principles

- Organize production code by semantic domain.
- Treat each domain as a bounded frontend context with explicit ownership.
- Build a modular frontend monolith that permits later domain extraction into microfrontends.
- Use one primary frontend framework unless an approved migration or integration need justifies another.
- Keep framework implementation behind browser-standard domain and shared UI contracts.
- Make domains independently understandable, lazy-loadable, and failure-isolated.
- Prefer private domain code over premature shared abstractions.
- Keep shared code generic, stable, and free of domain vocabulary and business rules.
- Keep state close to its owner; do not default to global mutable state.
- Keep HTTP and transport details out of presentation components.
- Treat the backend as authoritative for persistence, permissions, final validation, and business decisions.
- Implement the user-facing outcomes in `ux-ui.md` exactly.
- Enforce architecture with available monorepo, compiler, or lint tooling.

## 4. Structure

Use exactly the following architectural locations. No additional domain folder category is prescribed.

```text
frontend/
├── <domain-name>/
│   ├── domain.ts
│   ├── views/
│   ├── components/
│   ├── store/
│   ├── api/
│   ├── models/
│   ├── mappers/
│   ├── styles/
│   └── utilities/
│
└── shared/
    ├── ui-components/
    ├── logic/
    ├── http/
    │   └── interceptors/
    ├── styles/
    └── utilities/
```

Framework-required files must be placed in the closest approved location. A new architectural folder must not be introduced for convenience, naming preference, or to bypass ownership. Structural expansion requires a documented architectural need.

### Domain locations

- `domain.ts`: the sole static public entry point and root Web Component registration contract.
- `views/`: screen-level and journey-level domain composition.
- `components/`: private framework-native domain components.
- `store/`: domain state, actions, selectors, orchestration, and state transitions when needed.
- `api/`: handwritten frontend adapters for backend APIs.
- `models/`: frontend-owned domain, view, form, and transport representations.
- `mappers/`: handwritten conversion between transport and frontend-owned representations when required.
- `styles/`: domain-local styling and domain-specific token composition.
- `utilities/`: private domain-specific helpers.

### Shared locations

- `shared/ui-components/`: framework-neutral Web Components governed by `ux-ui.md`.
- `shared/logic/`: stable domain-neutral logic.
- `shared/http/`: generic HTTP transport infrastructure.
- `shared/http/interceptors/`: generic request and response interception policies.
- `shared/styles/`: technical representations of design tokens, base styles, and framework styling configuration governed by `ux-ui.md`.
- `shared/utilities/`: small domain-neutral helpers.

## 5. Semantic domains

A domain represents a meaningful product or business capability and owns its frontend language, behavior, state, views, components, API adaptation, models, mappings, styles, and recovery behavior.

A domain must:

- Own all domain-specific frontend code.
- Expose only its root Web Component contract through `domain.ts`.
- Keep views, internal components, stores, adapters, models, mappers, styles, and utilities private.
- Depend only on itself, browser/platform APIs, approved external dependencies, and domain-neutral `shared/` contracts.
- Be independently lazy-loadable and removable without changes inside unrelated domains.
- Be extractable later into an independently built microfrontend without redesigning its public contract.
- Handle its applicable loading, ready, empty, stale, unavailable, error, and recovery states.

A domain must not:

- Import another domain or its internal files directly.
- Access another domain's store, services, components, or models.
- Export internals as shortcuts around its public contract.
- Move domain rules into `shared/` to bypass a boundary.
- Depend on a global event bus for ordinary communication.
- Assume another domain already initialized shared runtime state.
- Access backend internals or a database.

Direct and circular cross-domain dependencies are prohibited. The selected tooling must enforce import directions and public entry points where technically possible.

### Domain naming and size

- A domain is named after a product capability in the product's own vocabulary. Names of technical layers or catch-alls (`common`, `core`, `base`, `misc`, `helpers`, `ui`, `services`) are not domain names.
- Each domain states its purpose in one sentence in its contract documentation. A domain whose purpose needs "and" to be described is a candidate for splitting.
- A domain does not grow into a second `shared/`. Code that other domains need is promoted under Annex D, or the other domains are served through the public contract.
- Cross-domain coordination is translated by the shell. The shell maps one domain's output event to another domain's documented input, so neither domain learns the other's vocabulary or models.
- Storage keys, `BroadcastChannel` names, cookies, custom-event names, URL parameters, and CSS custom properties that a domain owns carry that domain's prefix. A domain does not read or write another domain's.

### Import direction within a domain

Default; deviation requires a documented technical reason. Dependencies run one way, and a lower layer never imports a higher one.

| Location | May import from (same domain) |
|---|---|
| `domain.ts` | `views/`, `components/`, `store/`, `styles/` |
| `views/` | `components/`, `store/`, `models/`, `styles/`, `utilities/` |
| `components/` | other `components/`, `store/`, `models/`, `styles/`, `utilities/` |
| `store/` | `api/`, `models/`, `utilities/` |
| `api/` | `mappers/`, `models/`, `utilities/` |
| `mappers/` | `models/`, `utilities/` |
| `utilities/` | `models/` |
| `models/`, `styles/` | none |

- Presentation code (`views/`, `components/`) never imports `api/` or `mappers/` directly.
- Every location may import the public locations of `shared/`, except that `shared/http/` is imported only by `api/`.
- `shared/` never imports a domain.

## 6. Domain contract

Every semantic domain exposes one large autonomous Web Component through `domain.ts`. It is the domain's framework-neutral browser boundary.

The root Web Component must:

- Use a unique project-owned custom-element name containing a hyphen, prefixed with `app-` or with the project short name (`<project-short-name>-`), chosen once and used for all domain roots and shared UI components.
- Document attributes, properties, slots, events, public methods, configuration, and lifecycle behavior.
- Use attributes for primitive serializable configuration.
- Use properties for structured runtime input.
- Emit semantic, typed `CustomEvent` events for output.
- Use slots for controlled host-provided composition where needed.
- Expose a public method only when properties and events are insufficient.
- Hide framework instances, stores, API clients, dependency containers, and internal components.
- Register idempotently; repeated or concurrent loading must not register the same custom element twice.
- Support safe mounting, unmounting, reconnection, and input changes.
- Isolate initialization and runtime failures from the shell and unrelated domains.

`domain.ts` must not re-export private implementation. Deep imports into a domain are prohibited.

### Lifecycle

- Constructors perform safe initial setup only.
- Constructors must not assume children, attributes, connection, browser storage, or backend data are available.
- Connection initializes resources that require a connected element.
- Disconnection cleans up owned listeners, observers, subscriptions, timers, framework roots, and pending requests.
- Reconnection must not duplicate state, listeners, requests, analytics, or markup.
- Attribute and property changes must work before and after connection.
- Element instances must not leak mutable state into one another.
- Moving an element in the DOM must not corrupt user state.
- Registration occurs only where the Custom Elements registry exists.

### Public contract conventions

These conventions apply to every domain root and every shared UI component.

- Every primitive input is available as both an attribute and a property, and the two stay in sync.
- Rich input (objects, arrays, functions) is a property only and is not reflected to an attribute.
- A property assigned before the element definition is loaded is applied after upgrade and is not lost.
- Setters, getters, and attribute-change handling must not create reflection loops.
- An element must not emit a public event merely because its host set a property. Events report user intent and internal activity.
- State is exposed through attributes or custom states (`ElementInternals.states`, where the support target permits), never by adding classes to the host. The host's `class` and `style` belong to its author.
- An element must not override `role`, `tabindex`, or other global attributes set by its author.
- Public events use lowercase kebab-case names and typed payloads in `detail`. Each event sets `bubbles` and `composed` deliberately; an event that the host must receive across a shadow boundary is `composed`.
- The host declares a default `display` and honours the `hidden` attribute.
- Every input documents its default, and the default applies when the input is absent.

### Contract documentation and versioning

- Every public contract is described in a machine-readable Custom Elements Manifest (`custom-elements.json`, referenced from `package.json` through the `customElements` field). It covers tag name, attributes, properties, events, slots, CSS parts, and CSS custom properties, to the extent the schema version in use supports them.
- The manifest should be generated from source by tooling and checked for drift against the code. A hand-maintained manifest is allowed only where generation is impossible.
- The manifest feeds editor support, the component gallery, and detection of public API changes between versions.
- A change is **breaking** when it removes or renames an element, attribute, property, event, slot, part, or custom property; changes a type, default, or semantic meaning; changes when an event is emitted; or turns optional input into required input. Every other change is **non-breaking**.
- Non-breaking additive change is the default. A breaking change requires a documented migration note and a recorded architectural decision.
- A deprecated contract element keeps working, is marked deprecated in the manifest, and is removed only after the deprecation period the project defines.
- The shell relies on nothing beyond the documented contract: tag name, attributes, properties, events, slots, and mount path.

### Registration, failure, and robustness

- The custom-element registry offers no way to remove a definition, and redefining a name or constructor throws `NotSupportedError`. A name is therefore never reused for a different constructor, and neither tests nor development tooling may depend on redefinition.
- Registering a name that the same constructor already defines is a no-op. A name already defined by a different constructor is a failure: the domain enters its failure state with a diagnosable development error and never runs against a foreign definition.
- A domain root that fails after initialization sets a documented failure attribute and emits a documented failure event carrying a stable failure category and no internals, so the shell can show its own fallback.
- Event payloads are plain, structured-cloneable data: no DOM nodes, functions, or framework objects. This keeps a contract usable across a frame or worker boundary later.
- Event names do not collide with native event names.
- An event is cancelable only when its contract documents what cancelling does.
- Boolean attributes follow HTML semantics: presence is true and absence is false. The string value `"false"` is never used to mean false.
- Removing an attribute restores its documented default.
- A public method that depends on initialization returns a promise or is documented as available only after connection. Calling any public method before connection or upgrade follows its documented behavior (queued, or a documented error) and is never silently ignored.
- Elements created by the parser, `createElement`, `cloneNode`, or `importNode` behave equivalently. A clone shares no mutable state with its source. Adoption into another document is handled or documented as unsupported.

## 7. Components

There are three component categories.

| Category | Implementation | Visibility |
|---|---|---|
| Domain root | Autonomous Web Component | Public domain boundary |
| Domain view/component | Native component of the selected framework | Private to the domain |
| Shared UI component | Autonomous Web Component | Public framework-neutral UI contract |

### Domain components

Components under a domain are private, framework-native components. They must not be Web Components unless deliberately promoted to `shared/ui-components/` after proving they are domain-neutral.

Domain components must:

- Have narrow responsibility and explicit ownership.
- Use typed inputs and outputs.
- Prefer one-way data flow and immutable inputs.
- Keep transient state locally and coordinated durable state in `store/` when necessary.
- Avoid hidden HTTP calls and direct transport knowledge.
- Avoid direct sibling, parent-internal, other-domain, and global mutable-state access.
- Use semantic markup and preserve the reading order from `ux-ui.md`.
- Implement applicable loading, empty, error, unavailable, disabled, long-content, and missing-media states.

Physical files follow framework conventions. Behavior, markup, styling, and tests must be identifiable and co-located, but no framework is forced into Angular-style file separation. Every component, view, and shared UI component has its own folder (as in Angular) that holds its files, including its unit and component tests, for example `components/article-card/article-card.component.ts` beside `components/article-card/article-card.component.spec.ts`.

Examples:

```text
Angular: component.ts + component.html + component.scss + component.spec.ts
Vue:     component.vue + component.spec.ts
React:   component.tsx + component.scss + component.spec.tsx
Svelte:  component.svelte + component.spec.ts
Lit:     component.ts + component.spec.ts
```

### Shared UI components

Every component in `shared/ui-components/` must be an autonomous Web Component. Shared UI contains reusable design-system and interaction primitives consumable by any domain or compatible host, regardless of framework.

A shared UI component must:

- Implement anatomy, styling, states, responsiveness, and accessibility from `ux-ui.md`.
- Use browser-standard attributes, properties, slots, methods, and semantic custom events.
- Work without domain stores or framework context supplied by the host.
- Be independently and idempotently registerable.
- Expose only controlled styling hooks required by the UX/UI contract.
- Clean up owned resources on disconnection and reconnect safely.
- Include basic component unit tests and stable `data-testid` hooks.

A shared UI component must not:

- Contain domain vocabulary, business rules, API calls, persistence, routing knowledge, or domain-store access.
- Import a domain.
- Expose framework-specific instances or require the host to use its framework.
- mutate document-global styles or unrelated elements as an import side effect.
- Replace a native HTML control without a justified design-system need.
- Treat Shadow DOM as a security boundary.

### Shadow DOM and light DOM

Shadow DOM encapsulates styles and markup. It does not carry ID-based relationships across its boundary: `aria-labelledby`, `aria-describedby`, `aria-controls`, `aria-activedescendant`, and a label's `for` do not resolve across shadow roots.

- Elements that reference each other by ID must live in the same tree, either one shadow root or one light DOM.
- Mechanisms that attach element references across roots (ARIA element reflection, Reference Target) must not be relied on until they are Baseline under the browser support target. Element reflection also requires JavaScript, so it is unusable for server-rendered markup.
- Choose per component:

| Component situation | Use |
|---|---|
| Self-contained widget whose related parts are all internal | Shadow DOM with slots |
| Needs a label, description, or error text supplied by the host or by sibling markup | Slot that content into the component, or use light DOM |
| Composite widget with managed focus (combobox, listbox, menu, tree) whose references span parts | Keep all parts in one tree; use light DOM when a part must live outside the shadow root |
| Custom form control | Form-associated custom element (`ElementInternals`) or a slotted native control |
| Must be server-rendered without JavaScript | Declarative Shadow DOM (Section 11) or light DOM; no JavaScript-only ARIA references |

- Shadow roots are open by default. A closed root requires a documented reason, because it hinders testing and assistive tooling.
- Focus delegation into the shadow tree is explicit and tested, so pointer and keyboard focus reach the correct internal control.
- Styling hooks are limited to CSS custom properties (which inherit through the boundary), documented `::part()` names, and slots.
- Shared UI components should share one parsed stylesheet across instances (constructable stylesheets) where supported. Declarative shadow roots carry their styles in `<style>` or `<link>` instead.
- A light-DOM component is allowed in `shared/ui-components/` when ID-based relationships or document-level styling must apply. Its style isolation then relies on cascade layers (Section 12) and documented selectors, and the choice is stated in its contract.
- The domain-root rendering target (shadow root or light DOM) is chosen once in the framework profile for all domain roots and must satisfy this section and Section 12.
- Custom form controls must participate in form submission, constraint validation, label association, reset, and the disabled and required states.

## 8. Communication

Use this preference order:

1. URL/router state for navigation and shareable application state.
2. Properties for structured host-to-domain input.
3. Attributes for primitive serializable configuration.
4. Slots for host-controlled composition.
5. Semantic `CustomEvent` events for output.
6. Public methods only when the preceding mechanisms are insufficient.

Rules:

- Events describe user intent or completed facts, not internal commands.
- Public events and payloads must be documented and versionable.
- Payloads use browser and TypeScript data, not framework instances.
- Components must not communicate through arbitrary DOM queries.
- Siblings communicate through their owner, private same-domain state, or a public contract.
- The shell composes and loads domains but contains no domain business logic.
- Shared mutable business state across domains is prohibited.

## 9. State

A domain owns coordinated or durable state in `store/`. The chosen framework may use signals, stores, reducers, observables, atoms, or an equivalent approach.

- Keep transient UI state in its component or view.
- Keep domain state private.
- Do not introduce global state by default.
- Model applicable idle, loading, refreshing, success, empty, stale, unavailable, validation-error, and failure states.
- Derive values instead of storing duplicate authoritative values.
- Prevent stale responses from overwriting newer state.
- Deduplicate equivalent concurrent reads where useful.
- Define state retention or reset behavior on unmount.
- Persist only intentional drafts, preferences, or resilience state.
- Do not persist secrets or sensitive data without explicit approval.
- Treat backend responses as authoritative for backend-owned data.

### State kinds

Each kind of state has exactly one owner.

| Kind | Owner | Examples |
|---|---|---|
| Server state | Domain `store/`, as a cache over `api/` | Fetched lists, records, permission views |
| Client state | Component, or `store/` when coordinated | Selection, open panels, wizard step |
| URL state | URL and router | Filters, sort, page, tab, search term |
| Form state | The form | Draft values, touched and dirty flags, errors |

### Server state

Server state is remote, asynchronous, shared, and can become stale. It is a cache of backend-owned data and never the source of truth.

- A server-state cache is implemented by handwritten code or by an admitted query and cache library (Annex D). The library stays behind `store/` and `api/`; its types and instances never appear in Web Component contracts.
- Each cached resource defines its identity, freshness period, retention period, and invalidation triggers.
- Cache identity includes the resource and every parameter that changes the response, including locale and any identity, role, or tenant on which the response depends.
- Stale data may be shown while revalidating only where the UX permits. It is labelled as stale when the difference matters (Section 14).
- A mutation explicitly invalidates or updates every cached resource it affects.
- Optimistic updates are allowed only for reversible, low-risk actions. They keep the previous state, roll back on failure, and revalidate afterwards. An irreversible or authoritative outcome (payment, permission change, deletion without undo) is never shown as complete before the backend confirms it.
- Caches are cleared or partitioned on sign-out and on identity change.
- Cached data revalidates on reconnection, on return to visibility, and on restore from the back/forward cache when its freshness has expired.
- Paginated and infinite lists keep stable entity identity, retain a bounded number of pages, and define position restoration.
- Unchanged data keeps its referential identity across refreshes where the framework benefits from it.
- A persisted cache requires explicit approval, a schema version, an expiry, and no sensitive data.

### URL state

- Shareable and navigable state lives in the URL: filters, sort order, pagination, search term, selected tab, and the identifier of the viewed entity.
- Parsing is defensive. Unknown, repeated, malformed, or out-of-range values fall back to documented defaults without error.
- User-meaningful navigation creates a history entry. Transient adjustments, such as typing in a search field, replace the current entry.
- The URL carries no sensitive values (Section 15).
- A domain reads and writes only the URL parameters it owns, and ownership is agreed per route between the shell and the domain.

### State modeling

- Domain and component states are discriminated unions (for example `idle`, `loading`, `ready`, `empty`, `failed`), not combinations of boolean flags, so impossible states cannot be represented. Handling is exhaustive.
- Strict type checking is enabled. Type escape hatches (`any`, non-null assertions, `@ts-ignore`, double assertions) are not used at contract, adapter, and model boundaries. Elsewhere each carries an adjacent reason.
- Public contract types (attribute and property types, event payloads) are published as type declarations of the contract, for example through an augmentation of `HTMLElementTagNameMap`. They never expose implementation types.
- A copy of derived state that must exist has one documented source and one update path.

## 10. API integration

Frontend domains communicate with the backend through handwritten adapters.

```text
Domain root/view
    → domain state or orchestration
        → handwritten domain API adapter
            → shared HTTP transport
                → backend API
```

Components must not make ad hoc or hidden HTTP calls. Endpoint paths, methods, headers, authentication transport, request construction, and raw response handling belong in the owning domain's `api/` layer.

`shared/http/` may provide only generic behavior such as request execution, public runtime configuration, credential forwarding, correlation identifiers, timeouts, cancellation, safe retry policies, parsing, and normalized transport failures. It must not contain domain endpoint knowledge or business rules.

### OpenAPI adaptation

The backend OpenAPI specification is the authoritative transport contract. The frontend must adapt to it through handwritten `api/`, `models/`, and `mappers/` code.

Production frontend clients, adapters, models, mappers, forms, stores, views, and components must not be generated directly from OpenAPI.

OpenAPI tooling may be used only for contract reading, documentation, exploration, validation, drift detection, development-time schema checks, and local mocks that do not replace production adaptation.

Handwritten adaptation must:

- Follow documented paths, methods, parameters, payloads, status codes, and authentication requirements.
- Keep raw transport representations out of presentation components.
- Map transport data to frontend-owned representations when semantics differ.
- Handle optional, nullable, missing, malformed, and unknown fields safely.
- Ignore unknown response fields unless the contract defines them as invalid.
- Normalize errors into stable frontend failure categories.
- Avoid coupling frontend terminology to backend implementation names.

### Request rules

- Abort requests when the owner is destroyed or an operation is superseded.
- Prevent older responses from overwriting newer state.
- Retry automatically only safe operations after transient failure.
- Never retry a non-idempotent write automatically without an idempotency strategy.
- Prevent or safely handle duplicate submissions.
- Distinguish authentication, authorization, validation, conflict, not-found, rate-limit, timeout, offline, and unavailable failures.
- Never show raw backend errors, stack traces, or internals to users.
- Retain safe correlation identifiers for support when provided.

### Runtime validation

- TypeScript types are erased at runtime and do not validate data. Adapters validate untrusted responses at runtime, with handwritten schemas or type guards, before mapping them to frontend-owned models.
- Validation is strict for the fields a feature depends on and tolerant of unknown or non-critical fields.
- A response that fails validation becomes a normalized `malformed-response` failure. Partial or unchecked data does not reach presentation.
- Handwritten schemas stay within the OpenAPI rule above. Contract tooling may check them against the OpenAPI document for drift during development and in automated checks.
- Payloads built from user input are validated for usability before sending (Responsibility boundary).

### Failure mapping

- A single default mapping from HTTP status and transport outcome to the stable failure categories lives in `shared/http/`. Domains refine it.
- When the backend documents `application/problem+json` (RFC 9457), adapters read it as follows:
  - `type` is the primary identifier for mapping.
  - `status` should agree with the HTTP status.
  - `title`, `detail`, and `instance` serve diagnostics and support. They are never parsed for program logic and never shown to users unchanged.
  - Extension members, such as field errors, are read only when the API contract declares them.
  - Unknown extension members and members of an unexpected type are ignored.
  - `type` URIs are never fetched automatically.
- When a response has no recognisable problem body, the category derives from the status code and transport outcome.
- Field-level errors map to form fields in the domain adapter (Annex B). Errors that cannot be mapped appear in the error summary.

### Data representation

- Instants are ISO 8601 values with an offset or in UTC. They are parsed explicitly. Formatting happens in presentation with `Intl`, using the project's locale and time zone, and never relies on implicit local-time parsing of non-ISO strings.
- Date-only values (birth dates, calendar days, deadlines without a time) stay date-only and are never converted through a time zone.
- The client clock is not authoritative for expiry, deadlines, or entitlements. A countdown computes the remaining time from a timestamp on each tick instead of decrementing a counter, because timers are throttled in hidden pages.
- Money and exact decimals are never handled as binary floating point. They stay as strings or integer minor units, as the contract defines, and are formatted with `Intl.NumberFormat`.
- Integer identifiers that can exceed `Number.MAX_SAFE_INTEGER` are strings in models. JSON parsing must not silently lose precision.
- Identifiers are opaque. The frontend does not parse them, sort by them, or derive meaning from them.
- Models distinguish absent, `null`, empty string, and zero, and each mapping states what each case means.
- An unknown enum or union value from the backend maps to an explicit unknown variant with a safe fallback rendering. It never throws and is never coerced to a known value.
- Sorting and filtering of paginated collections happen on the backend. The frontend does not sort or filter a partial page as if it were the full set. Locale-aware sorting (`Intl.Collator`) applies only to fully loaded collections.
- Pages appended to a list are de-duplicated by entity identity, because the data can change between page requests.

### Composite reads and authentication refresh

- Independent reads run in parallel and fail independently. A view renders the parts it has and marks failed parts with a local recovery action. Sequential requests are used only when one needs the result of another.
- Expired authentication triggers one refresh at a time. Requests that failed because of it wait for that refresh and are replayed at most once. A failed refresh moves the application to a signed-out state and preserves unsaved user input where practical.
- A response is interpreted according to its `Content-Type`. A non-JSON body where JSON is expected, such as an HTML error page from a gateway, maps to an unavailable failure and is never parsed blindly.
- A `204` or `205` response is a valid success for operations whose contract declares no content.
- A network error without a response, including a blocked cross-origin request, cannot be told apart from being offline. It maps to the unavailable or offline category without technical wording.
- Aborting a write on unmount or supersession does not cancel it on the server. The outcome is reconciled by revalidation or by an idempotent resend, never assumed.

### Writes: idempotency and concurrency

- The interface prevents duplicate submissions first, through a pending state and an in-flight guard.
- Where the API contract documents an idempotency key (`Idempotency-Key` header), the adapter sends one unique key per user intent. It reuses that key for every resend of the same request and generates a new key for a new intent.
- Key-related error responses (missing key, key in use, key reused with a different request) are handled as the API contract documents and are never retried blindly.
- The header is an IETF draft and not a standard, so it is used only where the contract documents it. Without such support, non-idempotent writes are not retried automatically.
- Where the contract provides entity versions (`ETag` with `If-Match`, or a version field), updates carry the version the user edited. A version or precondition conflict puts the form in a conflict state that preserves the user's input and offers reload and comparison. It never overwrites silently.
- A long-running operation is modelled as an explicit pending state. Polling uses a capped interval with backoff and stops on unmount.

### Retry, timeout, and cancellation

Defaults; a project may override them.

| Rule | Default |
|---|---|
| Eligible failures | Network failure, timeout, 408, 429, 502, 503, and 504 |
| Eligible requests | Safe methods (`GET`, `HEAD`, `OPTIONS`); writes only with an idempotency key or documented idempotent semantics |
| Automatic retries | At most 2 per operation |
| Backoff | Exponential with full jitter, base 500 ms, cap 8 s |
| `Retry-After` | Honoured for 429 and 503 and replaces the computed delay up to the cap; if it exceeds the operation budget, the operation fails with a retry affordance |
| Timeout | Every request has one; reads 10 s, writes 30 s |
| Operation budget | 30 s in total, then retries stop |
| After exhaustion | A manual retry affordance is offered where retry is safe |

- Every request carries an `AbortSignal` bound to its owner and to supersession. The timeout and owner signals are combined.
- An aborted request produces no error state.
- Online and offline events are hints. A failed request, not `navigator.onLine`, establishes the offline state.

### Responsibility boundary

The frontend owns presentation, interaction, client state, safe optimistic feedback, and immediate usability validation.

The backend remains authoritative for authentication, authorization, business invariants, persistence, final validation, permissions, and authoritative calculations or decisions. Frontend validation never replaces backend validation. Hidden or disabled controls are not authorization.

## 11. Rendering and loading

Select rendering per route or domain entry point.

- Prefer CSR for private, browser-dependent, or highly interactive experiences when initial HTML is unnecessary.
- Use SSR, prerendering, or an equivalent initial-HTML strategy where public content, metadata, discovery, sharing, resilience, or performance requires it.
- Use hybrid rendering when supported.
- Do not make a public editorial route client-only merely to hide an unresolved SSR or hydration problem.
- The chosen mode must satisfy `ux-ui.md`.

Every domain must be independently lazy-loadable when the selected framework and target platform support code splitting. Load a domain only when its route or host slot requires it. Lazy-load noncritical child code and heavy dependencies when this does not damage UX, accessibility, or initial content delivery.

### Rendering selection

Defaults; route requirements and the UX/UI contract override them. The chosen mode is documented per route.

| Route characteristic | Default strategy | Note |
|---|---|---|
| Public content that changes rarely; discovery and sharing matter | Prerender at build time | Cache at the edge; rebuild on content change |
| Public content that changes often or needs per-request metadata | SSR, streamed where supported | Define HTTP caching per route |
| Mostly static page with isolated interactive parts | Partial hydration: static HTML, hydrate only interactive domain roots (on visibility, idle, or interaction) | Domain roots are natural hydration islands |
| Authenticated, highly interactive, not discoverable | CSR with a shell and a meaningful loading state | The shell does not wait for domain data |
| Mixed requirements | Hybrid, chosen per route | Document the mode per route |

- First-view content does not wait for client-only scripts where the technology allows it.
- Slow, personalised, or below-the-fold fragments do not block initial HTML. They stream or load after first render into reserved space.

### Server and hydration rules

- Server-executed modules must not access browser-only APIs unconditionally.
- Defer custom-element registration until `customElements` exists.
- Preserve meaningful semantic fallback HTML where required.
- Hydration must not duplicate requests, listeners, state, analytics, or content.
- Document any client-only boundary and preserve required SEO, accessibility, and fallback behavior.
- Activate browser-only behavior progressively.

### Hydration correctness

- The first client render produces markup equivalent to the server markup. Values that differ by time, randomness, locale, viewport, or browser storage are applied after hydration.
- A hydration mismatch is a defect to fix, not a warning to suppress.
- Hydration timing (on load, idle, visible, interaction) is chosen per domain root and does not change the public contract.

### Declarative Shadow DOM

- Server-rendered shadow trees use declarative shadow DOM (`<template shadowrootmode="open">`), so component content is visible before JavaScript runs.
- On connection, an element checks for an existing declarative root (`attachInternals().shadowRoot`) before attaching one. It reuses that root and never creates a second root or discards server-rendered content.
- Declarative roots are created by the HTML parser. `innerHTML` does not create them. Inserting such markup at runtime uses `setHTMLUnsafe()` or `parseHTMLUnsafe()`, and only with trusted, application-generated markup.
- Constructable stylesheets are not available to declarative roots. Server-rendered shadow trees carry their styles in `<style>` or `<link rel="stylesheet">` inside the template.
- Everything required for the server-rendered state is expressible as primitive attributes or slotted content. Structured property input is applied only on the client after upgrade.
- A component that cannot render meaningfully on the server declares this and provides a semantic fallback or a loading state.
- Whether a Declarative Shadow DOM polyfill is required is decided by the browser support target (Annex C).

### Loading failures

Handle concurrent domain-load requests, abandoned navigation, bundle failure, registration failure, initialization failure, backend failure, incompatible cached bundles, and domain failure without breaking unrelated domains or the application shell.

### Navigation-level enhancements

These are progressive enhancements. None is required for function. Each is feature-detected, has a working fallback, and honours the UX/UI contract.

**Speculation Rules**

- Prefetch broadly for safe, read-only pages. Prerender sparingly, only for targets the user is likely to open.
- Never speculate on URLs with side effects: sign-out, language switch, cart mutation, sign-in flows that send one-time codes, or anything that consumes quota or starts a timer.
- Code with side effects (analytics, storage writes, notifications) waits for activation, using `document.prerendering` and the `prerenderingchange` event. The server may inspect `Sec-Purpose` to defer side effects.
- A prerendered page may be stale and refreshes itself on activation. Prerender stays same-origin by default.
- Where the feature is unsupported, fall back to `<link rel="prefetch">` or to nothing.
- Inline rules need a Content Security Policy opt-in once a policy exists (deferred, Annex E).

**View Transitions**

- Same-document transitions use `document.startViewTransition()`. Cross-document transitions use `@view-transition { navigation: auto }` on both pages.
- The DOM update happens whether or not the API exists.
- Reduced motion disables the transition animations.
- A transition does not change reading order, focus order, or live-region behavior. Durations and easing come from motion tokens.

**Back/forward cache**

- Pages stay eligible for the back/forward cache. Do not register `unload` listeners; use `pagehide`.
- Register `beforeunload` only while unsaved changes exist and remove it afterwards.
- Use `Cache-Control: no-store` on documents only when required.
- Close connections and observers on `pagehide` and reopen them on `pageshow`.
- On `pageshow` with `event.persisted`, revalidate stale or sensitive data.
- Windows opened by the application use `rel="noopener"`.
- A domain root treats restoration from the cache like reconnection (Lifecycle).

### Upgrade and first paint

- Before a custom element is defined, its content is readable (slotted or light-DOM fallback) or its final space is reserved, so upgrading causes no layout shift. `:not(:defined)` rules reserve the size, and code that needs a definition waits with `customElements.whenDefined()`.
- A loading indicator appears only after a short delay and, once shown, stays for a minimum time, so fast responses do not flash. Defaults: 200 ms delay and 400 ms minimum. The UX/UI contract overrides them.
- Generated IDs used for `for` and ARIA references are unique per instance and identical on server and client.

### Domain load recovery

- A failed dynamic import of a domain, for example after a deployment replaced its files, is retried once. A second failure shows a controlled recovery state with a reload action.
- Automatic reload happens at most once per navigation. A guard prevents reload loops, and unsaved state is preserved before any reload.
- The error view has no dependency on the failed domain and cannot throw.

### Service workers and offline caches

- Service workers and persistent offline caches are not introduced without an explicit requirement.
- When required, caches are versioned. An update flow never leaves users on a bundle that is incompatible with the backend. Authenticated and API responses are cached only where the API contract allows it, and a tested way to disable the worker exists.

## 12. UX and styling

`ux-ui.md` defines design, component structures, design tokens, visual styles, responsive rules, interaction states, and accessibility outcomes. Frontend implementation consumes those decisions and does not redefine them.

```text
ux-ui.md
    → semantic design tokens
        → CSS custom properties
            → framework or Tailwind theme mapping
                → component styles
```

- Prefer Tailwind where it integrates cleanly with the selected framework and Web Component build.
- Use SCSS or another maintainable local mechanism for complex component styling.
- Use approved semantic tokens, not arbitrary values, when a token exists.
- Configure utility-class scanning for all relevant domain and shared UI sources.
- Keep domain styles private and shared styles domain-neutral.
- Do not rely on document-global selectors to style component internals.
- Preserve tokens, fonts, focus, accessible relationships, forms, and overlays across Shadow DOM boundaries.
- Preserve DOM reading and focus order during responsive rearrangement.
- Reserve media dimensions and use responsive images.
- Handle long content, missing media, reduced motion, zoom, and narrow viewports without overlap or loss of function.

### Styling inside Shadow DOM

- Design tokens are defined as CSS custom properties on `:root` and `:host`. Inherited properties and custom properties cross the shadow boundary, so components consume tokens through `var()`.
- Rules that style shadow-hosted markup are delivered inside the shadow root (static styles, the framework's styles option, or `<style>` in a declarative root). The document stylesheet does not reach it.
- `@font-face` rules are declared at document level.
- Compensating for isolation with `!important` or document-wide resets is prohibited.
- Tailwind v4: the `@theme` layer applies to shadow roots because it includes `:host` (change merged January 2025), but `@property` registrations cannot be scoped to a shadow root, so the generated stylesheet must also exist at document level. Utility CSS is generated once per build and injected into every shadow root that uses it. Verify this against the pinned Tailwind version before relying on it.

### Cascade and platform features

- One cascade-layer order (`@layer`) is declared in `shared/styles/`. Later layers beat earlier ones regardless of specificity, and unlayered styles beat layered ones. Default order: `reset`, `tokens`, `base`, `components`, `utilities`. Domain and component styles belong to `components`, utilities come last, and third-party CSS is imported into the lowest layer.
- Component-level responsiveness prefers container queries (`container-type`). Page-level layout uses media queries.
- A native platform feature is preferred over a script or library that emulates it, whenever it meets the browser support target (Annex C) and the UX/UI contract:
  - `<dialog>` for modal content.
  - The Popover API for non-modal layered content. It provides no role or name, does not move focus into the popover unless `autofocus` is used, and hover-only triggers are never the only way to reach its content.
  - `inert` for background content while a modal is open. Nothing indicates inertness by default, so the background must look inactive.
  - Logical properties (`margin-inline`, `inset-inline`) instead of physical left and right, unless a physical direction is meant.
- Features that are not yet Baseline, such as CSS anchor positioning, follow Annex C: progressive enhancement behind detection, with a working fallback.

### Layout and rendering robustness

- `z-index` values come from a documented token scale. Overlays use the top layer (`<dialog>`, popover) instead of escalating values.
- Inline `style` sets dynamic values only, preferably CSS custom properties. Static presentation lives in stylesheets.
- `!important` is allowed only in the utilities layer and for user-preference overrides such as reduced motion.
- Animations use `transform` and `opacity`. Layout-affecting properties are not animated, and `will-change` is used briefly and sparingly.
- Forced colors mode (`forced-colors: active`) replaces author colors and removes `box-shadow`, `text-shadow`, and non-URL background images. Components stay usable in it:
  - Boundaries and focus indication use borders or outlines, whose style and width remain while the browser sets the color.
  - Meaning is never carried by a shadow or background image alone.
  - Icons use `currentColor`.
  - `forced-color-adjust: none` needs a documented reason. Adapt with small adjustments, not a separate design.
- Hover styles apply under `(hover: hover)`, and nothing depends on hover to be reachable.
- Full-height layouts do not rely on `100vh` alone where browser chrome changes the viewport, and fixed UI respects safe-area insets (`env(safe-area-inset-*)`), within the browser support target.
- Opening a modal causes no layout shift and keeps scroll position. Closing restores both.
- User-generated and mixed-direction text uses `dir="auto"` or `<bdi>` where its direction is unknown.

## 13. Accessibility

All implementation must meet the accessibility contract in `ux-ui.md`.

- Use native semantic HTML before ARIA.
- Use links for navigation and buttons for actions.
- Do not use clickable generic elements when a native control exists.
- Do not nest interactive controls or links.
- Preserve visible focus and logical keyboard order.
- Provide accessible names, labels, errors, descriptions, and status announcements.
- Do not communicate meaning by colour alone.
- Support keyboard operation and reduced motion.
- Preserve semantics across Web Component boundaries.
- Do not let Shadow DOM hide required labels, validation, descriptions, or focus behavior.

### Web Components and assistive technology

- A label, description, error text, or controlled element lives in the same tree as the element that references it (Section 7).
- An interactive custom element provides role, name, and state through native elements inside it, or through `ElementInternals` default semantics that its author can override. It never overwrites attributes its author set.
- Focus is managed explicitly at the component boundary: focus delegation into the shadow tree, `tabindex` only where needed, no focus traps outside modal contexts, and focus restored on close.
- Route changes and dynamic replacement follow Annex A.
- A status message uses a live region that exists before its content changes.
- Automated accessibility checks run against the rendered tree, including open shadow roots.

### Dynamic content, focus, and announcements

- Live-region updates are coalesced. Rapidly changing content (typing, progress, streaming) announces summarized changes, not every change. `assertive` is reserved for urgent errors, and everything else is `polite`.
- `autofocus` is not used on page load of content routes. Initial focus inside a dialog follows the UX/UI contract.
- Focus survives re-rendering. Updating a list or replacing a region never drops focus to `body`. When the focused element is removed, focus moves to a logical neighbor or to the container, and the change is announced when it matters.
- When the element that opened a dialog no longer exists on close, focus moves to the nearest logical container.
- A custom widget implements the full keyboard pattern of its role, or a native control is used instead.
- Expiring sessions and time-limited content warn before data is lost and preserve unsaved input, as the UX/UI contract requires.

## 14. Errors and resilience

Errors are normalized by generic transport code and interpreted by the owning domain.

- Present safe, understandable, actionable messages.
- Preserve user input after recoverable failures where practical.
- Distinguish loading, empty, unavailable, stale, expired, offline, denied, and error states when recovery differs.
- Offer retry only when safe and useful.
- Avoid infinite retries and synchronized retry storms.
- Restore focus after dialogs, errors, navigation, or dynamic replacement.
- Isolate local failure instead of replacing the entire page.
- Prevent shared UI failure from corrupting domain state.
- Label cached or estimated backend-owned values and revalidate them when possible.

### Error containment

- Unhandled errors and promise rejections inside a domain are caught at the domain boundary and mapped to its failure state. The shell has a last-resort fallback for failures that escape.
- Containment transmits nothing to external services. Error reporting is deferred to the final phase (Annex E).
- Error and fallback views are small, have no dependency on the failing code, and cannot themselves throw.
- A domain recovers from its failure state by re-initialising, without a full page reload, where recovery is safe.

### Browser environment

- Browser storage (local storage, session storage, IndexedDB, cookies) may be unavailable, blocked, full, or cleared. Every access is guarded, and failure degrades to in-memory behavior without breaking the interface.
- Persisted data carries a schema version. Data of an unknown version is discarded or migrated.
- Permission-gated APIs (geolocation, notifications, clipboard, camera, microphone) are requested only in response to a user action and in context, never on load. Denial, dismissal, and unavailability are handled states with an alternative path, and a denied prompt is not repeated.
- Media preferences (reduced motion, color scheme, contrast, forced colors, hover) can change while the page is open. Components react to changes instead of reading them once.
- Automatic page translation and browser extensions may rewrite text nodes. Dynamic text renders inside its own element, and tokens that must not be translated (codes, names, brand) carry `translate="no"`.
- Third-party embeds have a title, a reserved size, lazy loading, and the least permission they need. A failed embed leaves a placeholder without layout shift.
- When state or drafts are shared across tabs, messages are versioned and conflicts resolve by an explicit rule. A tab never overwrites newer data silently.
- A sign-out or session change in one tab is detected by the others on return to visibility or on the next interaction, so stale authenticated content does not stay on screen.

## 15. Security and privacy

- Provide API endpoints and public deployment settings through environment or runtime configuration.
- Treat all browser code and delivered configuration as public.
- Never store secrets, private credentials, privileged keys, or authorization decisions in frontend code.
- Treat backend and external content as untrusted.
- Avoid unsafe HTML injection; use only an approved sanitization mechanism where rich HTML is required.
- Do not construct URLs, queries, or HTML through unsafe concatenation.
- Do not expose sensitive values in URLs, logs, analytics, custom events, attributes, test IDs, DOM text, or browser storage.
- Avoid long-lived token or personal-data storage unless explicitly required and reviewed.
- Validate external navigation targets and prevent open redirects.
- Enforce permissions on the backend; frontend guards improve UX only.
- Shadow DOM, hidden routes, disabled controls, and minification are not security boundaries.

### Runtime configuration and feature flags

- Public runtime configuration is read once by the shell, validated at startup, typed, and passed to domains through documented inputs. Domain code does not read global configuration objects or `window` globals.
- Missing or invalid required configuration produces a controlled failure state, never requests to undefined URLs.
- A feature flag is declared per domain in typed form, with an owner and a removal criterion. Its default is the safe state, and an unknown or unavailable flag evaluates to that default.
- Both states of a flag are tested. A flag gates behavior behind an existing contract and does not change the shape of a public contract.
- A flag is never used as authorization, and it is removed after rollout.

### Code hygiene

- `eval`, `new Function`, string-based timers, `document.write`, and `javascript:` URLs are not used.
- Untrusted text is rendered as text (`textContent` or framework text binding), not parsed as HTML.
- A URL that comes from data is checked against an allow-list of schemes (`https`, `http`, `mailto`, `tel`) before it reaches `href` or `src`.
- A `message` event handler verifies `event.origin` and `event.source` and validates the payload like an API response. A wildcard target origin is never used for non-public data.
- A link with `target="_blank"` carries `rel="noopener"`, and `noreferrer` where the referrer must not leak.
- Debug output and test data are removed before completion. `console` is not a logging mechanism.

## 16. Performance

- Split bundles at domain boundaries.
- Avoid imports that load another domain as a side effect.
- Load noncritical code only when needed.
- Keep the shell and initial shared runtime small.
- Do not ship multiple frontend frameworks without approval.
- Share runtime dependencies deliberately.
- Define freshness and invalidation for cached backend data.
- Prevent unbounded memory caches and listener growth.
- Avoid duplicate framework runtimes in fine-grained components.
- Follow performance requirements and budgets from `ux-ui.md`.
- Introduce Module Federation or another remote-loading system only when independent build or deployment is a real requirement.

### Metrics and budgets

- Thresholds come from the UX/UI contract. Without them, use the Core Web Vitals "good" targets at the 75th percentile: Largest Contentful Paint at most 2.5 s, Interaction to Next Paint at most 200 ms, Cumulative Layout Shift at most 0.1.
- Budgets exist per route and per domain chunk (initial JavaScript, CSS, total transfer), are measured on the production build, and are checked automatically once a pipeline exists.
- Lab measurement guides development. Field data decides compliance. Collecting field data is deferred to the final phase (Annex E).

### Responsiveness

Interaction to Next Paint has three phases: input delay, processing time, and presentation delay.

- An interaction handler does only what visible feedback needs and defers the rest.
- Long tasks are split by yielding to the main thread (`scheduler.yield()` where available, `setTimeout` otherwise).
- Heavy computation moves to Web Workers.
- Layout reads and writes are batched to avoid layout thrashing.
- The DOM stays small. Long off-screen sections use `content-visibility: auto` with a reserved size, and very long lists are virtualised.
- Constructors and connection callbacks run no long work and force no layout.
- Every interaction gives visible feedback within the frame budget. Debouncing never delays the visual acknowledgement.
- Startup script evaluation stays small because domain code loads only when needed (Section 11).

### Loading efficiency

- The LCP image carries `fetchpriority="high"` and is never lazy-loaded. Below-the-fold images use native lazy loading. Non-critical above-the-fold images may use `fetchpriority="low"`.
- Preload only resources the parser discovers late and that matter for the first view, such as a background-image LCP candidate or a critical font. Verify the resulting priority in the browser's network panel.
- Images use responsive `srcset` and `sizes` and efficient formats with fallbacks through `<picture>`.
- Noncritical and third-party scripts load with `async` or `defer`, or after interaction. They never block first render.
- Each domain chunk is analysed for duplicated dependencies. One framework runtime serves all domain roots wherever the framework allows externalising it.

### Runtime efficiency

- Scroll, touch, and wheel listeners are passive unless they must call `preventDefault`.
- Observers (`IntersectionObserver`, `ResizeObserver`) replace polling and scroll or resize handlers for visibility and size logic.
- Domain code for the likely next navigation may be preloaded on intent (hover, focus, idle) without joining the initial bundle. Chains of dependent lazy chunks (waterfalls) are avoided.

### Hidden pages

- Work that does not need to run while the page is hidden (polling, animation, subscriptions, timers) pauses when `visibilitychange` reports `hidden` and resumes on `visible`, revalidating stale data.
- State that would be lost if the page is discarded is saved when the page becomes `hidden`, which is the last event the page can reliably observe.
- Correctness never depends on a timer firing on time in a background tab.

## 17. Basic component unit tests

Basic component unit tests are mandatory. Use tooling compatible with the selected framework and capable of testing DOM behavior and Web Component contracts.

Test:

- Domain root Web Components.
- Domain views and components that own behavior.
- Every shared UI Web Component.

Tests cover applicable public behavior:

- Creation, mounting, and initial rendering.
- Required inputs, properties, and attributes.
- Loading, content, empty, error, unavailable, disabled, and validation states.
- User interaction and emitted outputs or `CustomEvent` events.
- Web Component registration and public contracts.
- Cleanup of listeners, observers, subscriptions, timers, and owned pending work.
- Safe remounting or reconnection.
- Critical semantic, focus, and keyboard behavior from `ux-ui.md`.

### Test IDs

Stable `data-testid` attributes are required for unit-test targets that expose interactive behavior, state containers, or content requiring reliable assertion.

- Name test IDs by stable semantic purpose, never CSS position, style, implementation type, or array index.
- Keep them stable through visual refactoring unless public behavior changes.
- Repeated entities use a stable content/entity identifier when uniqueness is needed; array indexes are prohibited.
- Never include personal, sensitive, secret, or volatile data.
- Use test IDs only as testing hooks, never for production logic, styling, analytics, or component communication.
- Tests may additionally use roles, labels, names, properties, attributes, or visible text.

```html
<button data-testid="article-card-open">Read article</button>
<section data-testid="article-card-loading" aria-busy="true"></section>
```

### Test boundaries

- Test public behavior, not framework internals or private methods.
- Mock the domain adapter boundary rather than coupling component tests to real backend services.
- Do not use frontend component tests as substitutes for backend validation.
- No coverage percentage is mandatory initially.
- Integration, API-contract, end-to-end, automated accessibility, visual-regression, SSR/hydration, performance, and cross-framework tests are optional future extensions.
- Architecture and tooling must not prevent those levels from being added later.
- Test files follow framework conventions such as `.spec.ts`, `.spec.tsx`, `.test.ts`, or an equivalent. Unit and component tests are always located in the folder of the component, view, or unit they test, beside the file they test, and never in a separate test tree. Test files are excluded from the production output.
- Where the UX/UI contract requires a test level, such as automated accessibility checks, that requirement prevails over the optionality above (Section 1).
- Tests that depend on real browser behavior (focus, Shadow DOM, layout, form association) should run in a real browser engine. Simulated DOM environments suit logic-only component tests (Annex G).
- When contract tests are introduced, the shell tests each domain through its public contract, and each domain tests against the shell's documented expectations.

### Test hygiene

- The custom-element registry is global and cannot be cleared. Tests register an element once per run, create fresh instances in each test, and clean up the DOM and listeners afterwards. Test doubles use unique tag names.
- Tests do not depend on execution order, shared mutable fixtures, real time, the real network, randomness, or the locale and time zone of the machine. Time is controlled with a fake clock, and locale and time zone are fixed.
- Waiting uses conditions (retrying assertions, `customElements.whenDefined()`, awaited events), never arbitrary sleeps.
- Assertions target roles, names, states, and emitted events. Snapshots of large DOM trees are not primary assertions, and snapshot updates are reviewed.
- Skipped, focused (`only`), or commented-out tests are not committed without an owner and a recorded reason. A failing test is fixed, not weakened.
- A flaky test is fixed, or quarantined with an owner and a reason, within the period the project defines.
- Test factories are domain-local. Shared factories contain no domain vocabulary.
- A defect fix includes a test that fails without the fix, where practical.

## 18. Edge cases

| Area | Required handling |
|---|---|
| Domain loading | Concurrent requests resolve to one load and registration. |
| Registration | Detect an existing custom-element definition before registration. |
| Missing input | Optional input uses a safe default; invalid required input produces a controlled state. |
| Reconnection | No duplicate listeners, requests, state, analytics, or markup. |
| Destruction | Owned requests and resources are cancelled or released. |
| Navigation | Late responses from an abandoned route cannot update the new route. |
| Concurrency | Stale responses cannot overwrite newer results. |
| Submission | Rapid repeated actions cannot cause unsafe duplicate writes. |
| API evolution | Optional, unknown, nullable, missing, and malformed fields degrade safely. |
| Authentication | Missing/expired authentication differs from authorization denial. |
| Offline | Preserve user intent and drafts when practical; fail unsupported actions clearly. |
| Server rendering | Do not access browser-only APIs during server execution. |
| Hydration | Do not duplicate HTML, requests, state, listeners, or events. |
| Content | Long labels, headlines, values, and unbroken strings do not overlap controls. |
| Media | Missing, slow, or failed media preserves layout and useful fallback. |
| Responsive layout | Visual reordering preserves reading and keyboard order. |
| Shadow DOM | Tokens, fonts, focus, overlays, forms, and accessibility still work. |
| Domain failure | The shell and unrelated domains remain operational. |
| Sensitive data | No leakage through URLs, test IDs, logs, events, analytics, or storage. |
| Tests | Test IDs never depend on visual position or array index. |
| Upgrade | Properties set before the element definition loads are applied after upgrade and are not lost. |
| Declarative shadow root | An existing server-rendered root is reused; no second root and no duplicated content. |
| Cross-root accessibility | Labels, descriptions, errors, and controlled elements resolve within one tree. |
| Server-rendered input | State needed for the first render arrives through attributes or slotted content, not object properties. |
| Route change | Title, focus, announcement, and scroll position are handled (Annex A). |
| Back/forward cache restore | Connections reopen, stale or sensitive data is revalidated, and no listeners or events are duplicated. |
| Speculative load | A prefetched or prerendered page defers analytics, storage writes, and side effects until activation. |
| View transition | Without support, or under reduced motion, the update happens immediately and function is unchanged. |
| Malformed response | A validation failure becomes a normalized failure; no partial data reaches presentation. |
| Problem details | Unknown extension members and members of an unexpected type are ignored; `detail` is never shown unchanged. |
| Write retry | A resend reuses its idempotency key, a new intent gets a new key, and key conflicts are not retried. |
| Version conflict | A precondition failure preserves the user's input and offers reload and comparison. |
| Rate limiting | `Retry-After` is honoured and retries stop at the operation budget. |
| Identity change | Cached server state, drafts, and URL-derived state of the previous identity are cleared or partitioned. |
| Dirty form | Leaving by link, back button, or reload warns, and the `beforeunload` listener exists only while changes exist. |
| Unsupported feature | A feature outside the browser support target degrades to its documented fallback. |
| Long task | An interaction that triggers heavy work still gives visible feedback within the frame budget. |
| Third-party failure | A slow or failing third-party script does not block first render or domain function. |

### Edge cases: contracts and registration

| Area | Required handling |
|---|---|
| Name conflict | A name already defined by a different constructor puts the domain in its failure state with a diagnosable error. There is no silent redefinition and no partial run. |
| Clone or import | A cloned or imported element shares no mutable state with its source. |
| Early method call | A public method called before connection or upgrade follows its documented behavior. |
| Cancelled event | Cancelling a non-cancelable event has no effect. A cancelable event's documented effect applies. |
| Failure after initialization | The domain shows its failure state and emits its failure event. The shell and other domains stay operational. |
| Attribute value `"false"` | Presence means true. The contract does not interpret the string value. |
| Attribute removal | Removing an attribute restores its documented default. |
| Cross-domain coordination | The shell's mapping between two domains leaves both free of the other's vocabulary and models. |

### Edge cases: data

| Area | Required handling |
|---|---|
| Date-only value | A calendar date displays unchanged in every time zone. |
| Daylight-saving change | Day boundaries, durations, and recurring times stay correct around clock changes. |
| Large identifier | An identifier above 2^53 − 1 is neither rounded nor altered. |
| Money | Amounts keep their exact decimal value through display and submission. |
| Unknown enum value | The interface renders a safe fallback without an error. |
| Absent, null, empty, zero | Each is displayed and submitted according to the contract and is never conflated. |
| Partial page | Sorting and filtering of paginated data happen on the backend. |
| Page overlap | A list appended across pages shows no duplicate entity when data shifted between requests. |
| Wrong client clock | Expiry and deadline decisions do not change when the client clock is wrong. |

### Edge cases: API and authentication

| Area | Required handling |
|---|---|
| HTML error page | A non-JSON gateway response maps to unavailable. No parse error reaches the user. |
| No-content success | `204` and `205` count as success where the contract declares no content. |
| Concurrent expiry | Several requests failing on expired authentication cause one refresh and at most one replay each. |
| Refresh failure | The application moves to a signed-out state and preserves unsaved input where practical. |
| Partial failure | One failed independent read shows a local failure while the other parts render. |
| Blocked cross-origin request | It appears as unavailable, without technical wording. |
| Aborted write | The server may still have applied it. The next read or an idempotent resend reconciles the outcome. |

### Edge cases: loading and rendering

| Area | Required handling |
|---|---|
| Fast response | A loading indicator does not flash for responses faster than its delay. |
| Chunk load failure | One retry, then a controlled recovery state. There is no reload loop and state is preserved. |
| Stale service worker | An update never leaves users on a bundle incompatible with the backend, and the worker can be disabled. |
| Undefined element | Before upgrade, content is readable or space is reserved. Upgrading causes no layout shift. |
| Script failure | Public routes keep their essential content and navigation where progressive enhancement applies. |
| Hidden tab | Polling, animation, and timers pause. Resuming revalidates, and countdowns recompute from timestamps. |
| Generated IDs | IDs are unique per instance and identical on server and client. |
| Error view failure | The error and fallback views do not depend on the failed code and cannot throw. |

### Edge cases: styling and accessibility

| Area | Required handling |
|---|---|
| Forced colors | Controls keep visible boundaries and focus indication when shadows and backgrounds are removed. |
| No hover | Content and actions are reachable without hover. |
| Browser chrome and notches | Fixed UI is not hidden behind browser chrome or safe-area regions. |
| Modal open and close | There is no layout shift, and scroll position and focus are restored. |
| Mixed-direction text | User-generated text shows in the correct direction without breaking the surrounding layout. |
| Rapid updates | Live regions announce coalesced changes, not every change. |
| Removed focused element | Focus moves to a logical neighbor or container, never to `body`. |
| Removed dialog trigger | Closing the dialog moves focus to the nearest logical container. |
| Preference change | Changing reduced motion, color scheme, or contrast while the page is open takes effect without a reload. |
| Page translation | Automatic translation or extensions that rewrite text do not break the interface, and tokens marked `translate="no"` stay unchanged. |

### Edge cases: forms

| Area | Required handling |
|---|---|
| Autofill and paste | Values changed without key events are validated and submitted correctly. |
| IME composition | Interim text is not validated, counted, or submitted. Confirming a candidate with Enter does not submit the form. |
| Counter and backend limit | Counters use the backend's unit, so emoji and combined characters count as the backend counts them. |
| Paste over the limit | Pasted text is never silently truncated. |
| Number-like identifiers | Leading zeros, spaces, and long digit strings survive. |
| Disabled and read-only | A value that must be submitted is not held in a disabled control. |
| Draft in two tabs | Two tabs holding the same draft never silently overwrite each other. |

### Edge cases: environment, configuration, navigation, and tests

| Area | Required handling |
|---|---|
| Storage unavailable | Blocked, full, or cleared storage degrades to in-memory behavior without errors. |
| Old stored schema | Persisted data of an unknown version is discarded or migrated. |
| Permission denied | An alternative path is offered, and the prompt is not repeated. |
| Failed embed | A placeholder remains and the layout does not shift. |
| Sign-out in another tab | Other tabs revalidate on return and stop showing stale authenticated content. |
| Missing configuration | Missing or invalid required configuration yields a controlled failure state, not requests to undefined URLs. |
| Flag source unavailable | Flags evaluate to their documented defaults. |
| New tab or reload | The URL alone reproduces the view, without in-memory navigation state. |
| Encoded URL parts | Encoded, Unicode, and reserved characters in paths and parameters round-trip without corruption. |
| Fragment link | A fragment link scrolls to and focuses its target. |
| Double navigation | Activating a link twice, or navigating while another navigation is pending, ends on the last requested route. |
| Registry persistence | Tests do not rely on removing definitions. They use fresh instances and unique test-double names. |
| Machine time zone and locale | Tests pass in any time zone and locale. |

## 19. Prohibited patterns

- Direct imports into another domain.
- Circular domain dependencies.
- Domain business code in `shared/`.
- Global mutable business state shared by domains.
- Hidden HTTP calls in presentation components.
- Direct database or backend-internal access.
- Production client, adapter, model, mapper, form, store, view, or component generation from OpenAPI.
- Raw transport objects used directly in presentation where adaptation is needed.
- Framework instances or stores exposed through Web Component contracts.
- Domain-internal components registered as Web Components by default.
- Shared UI components that know routes, domain models, stores, or APIs.
- Non-idempotent custom-element registration.
- Global event buses for ordinary feature communication.
- DOM-query-based component communication.
- Array indexes as identity, keys, event identity, or test-ID identity.
- Production logic, styling, or analytics that depend on `data-testid`.
- Secrets or authorization rules in browser code.
- Client-only rendering used to hide an unresolved public-route SSR defect.
- Premature Module Federation or multiple frameworks without a documented need.
- New architectural folder categories without approval.
- Reimplementation, weakening, or contradiction of `ux-ui.md`.
- ID-based accessibility references (`aria-labelledby`, `aria-describedby`, `aria-controls`, `aria-activedescendant`, a label's `for`) that span shadow roots.
- Adding classes to a custom-element host to express state, or overriding `role`, `tabindex`, or other attributes set by the host's author.
- Reflecting object or array properties to attributes.
- Closed shadow roots without a documented reason.
- TypeScript types as the only validation of untrusted response data.
- Parsing the `title` or `detail` of a problem response for program logic, or showing it to users unchanged.
- Unbounded retries, or retries without backoff and jitter.
- `unload` event listeners.
- Speculative loading of URLs with side effects.
- Any feature that depends on View Transitions, Speculation Rules, or another non-Baseline platform feature for function.
- A new runtime dependency without admission (Annex D).
- Removing a deprecated contract element without a migration path.
- Implementing, configuring, or scaffolding final-phase items before the final phase (Annex E).
- Catch-all domain names (`common`, `core`, `misc`, `helpers`) and domains that act as a second `shared/`.
- Reading or writing another domain's storage keys, URL parameters, cookies, or custom-event names.
- Reusing a custom-element name for a different constructor, or relying on redefinition.
- Event payloads that contain DOM nodes, functions, or framework objects.
- Boolean state modeled as combinations of flags where a discriminated union fits, and type escape hatches at contract, adapter, and model boundaries.
- Binary floating point for money or exact decimals.
- Converting date-only values through a time zone, or using the client clock as authority for expiry or deadlines.
- Parsing identifiers, sorting by them, or deriving meaning from them.
- Sorting or filtering a partial page as if it were the full set.
- Parsing a response without regard to its `Content-Type`.
- Concurrent token refreshes, or replaying a request more than once after a refresh.
- Assuming that aborting a write cancelled it on the server.
- `type="number"` for identifiers and codes, and `maxlength` that silently truncates or blocks pasting.
- Validating, transforming, counting, or submitting interim text during IME composition.
- Unguarded access to browser storage, and permission prompts on page load.
- `eval`, `new Function`, string-based timers, `document.write`, `javascript:` URLs, unchecked URL schemes, and `message` handlers that skip origin checks.
- Arbitrary `z-index` values, inline styles for static presentation, `!important` outside the utilities layer and user-preference overrides, and animation of layout-affecting properties.
- Meaning carried only by shadows or background images, which forced colors mode removes.
- `autofocus` on page load of content routes.
- Dropping focus to `body` when a focused element is removed.
- Service workers or persistent offline caches without an explicit requirement.
- Feature flags without an owner and a removal criterion, and flags used as authorization.
- Domain code that reads global configuration objects or `window` globals.
- Arbitrary sleeps in tests, order-dependent tests, large DOM snapshots as primary assertions, and committed skipped or focused tests without an owner and a reason.
- Disabling, loosening, or bypassing boundary, lint, type, or test rules to make a change pass (Section 21).
- Inventing endpoints, fields, event names, or design values that no contract defines.
- Floating dependency ranges such as `latest` or `*` in application dependencies.

## 20. Framework profile

The architecture is framework-neutral; an implementation is framework-specific. The selected framework profile defines:

- Component syntax and physical file conventions.
- Domain-root custom-element adapter.
- Shared UI Web Component implementation.
- Routing and rendering mechanisms.
- State implementation.
- Lazy loading and code splitting.
- Tailwind and SCSS integration.
- Server/browser and hydration boundaries.
- Unit/component test runner and utilities.
- Dependency-boundary enforcement tooling.

A framework is suitable only if it can satisfy the Web Component contracts, domain boundaries, rendering needs, UX/UI contract, lazy loading, styling, accessibility, API adaptation, and unit-test requirements in this document.

Framework neutrality does not permit uncontrolled framework diversity. Different frameworks may coexist only behind Web Component boundaries and with documented justification.

## 21. Workflow

Before implementing or changing frontend behavior:

1. Read the relevant `ux-ui.md` requirements.
2. Identify the owning frontend domain.
3. Confirm whether code is domain-owned or genuinely domain-neutral.
4. Define or update the domain-root or shared UI public contract when necessary.
5. Confirm backend behavior against OpenAPI and implement handwritten adaptation.
6. Select CSR, SSR, prerendering, or hybrid rendering from route requirements.
7. Implement with framework-native internal components and approved shared Web Components.
8. Add or update stable `data-testid` hooks and component unit tests.
9. Validate lifecycle, concurrency, errors, accessibility, responsiveness, security, and edge cases.
10. Confirm automated boundary checks report no prohibited dependencies.
11. Confirm new platform features meet the browser support target (Annex C) and new dependencies pass admission (Annex D).
12. Update the contract manifest and apply the versioning rules when a public contract changes.

### Change restrictions

These apply to every change, whether made by a person or by an automated agent.

- Boundary, lint, type, and test rules are never disabled, loosened, or bypassed to make a change pass. Ignore comments, configuration exclusions, type escape hatches, and skipped tests count as bypasses. An exception requires a recorded decision.
- A change stays inside the domain or `shared/` area it was scoped to. A change that must touch another domain is split into separate, reviewable changes that go through public contracts.
- A change to a public contract updates its manifest and its tests in the same change.
- No new architectural folder, dependency, framework, or build tool is added as a side effect of a feature change.
- Generated or vendored code is not edited by hand. Generated files are reproducible from their source and marked as generated. Production code generated from OpenAPI remains prohibited.
- Dead code, commented-out code, and `TODO` markers without an owner or reference are not left behind.
- Endpoints, fields, event names, attribute names, and design values are taken from their contracts and never invented. When a contract is missing or unclear, the implementer uses a documented safe default and flags the gap.
- When a requirement conflicts with this document or with the UX/UI contract, the implementer stops, states the conflict, and does not resolve it silently by choosing one side (Section 1).
- A failing check is fixed at its cause. It is not silenced, retried until it passes, or removed.

## 22. Definition of done

Frontend work is complete only when:

- It belongs to the correct semantic domain or justified shared location.
- It uses only the approved structure.
- Domain boundaries and public entry points are preserved.
- Root and shared UI Web Component contracts are explicit and stable.
- Domain-internal components remain private and framework-native.
- The implementation conforms to `ux-ui.md`.
- Backend communication uses handwritten adapters and OpenAPI adaptation.
- Transport details do not leak into presentation.
- Rendering and lazy loading fit the route and content.
- Applicable loading, empty, unavailable, error, stale, long-content, media, and recovery states exist.
- Cleanup, cancellation, stale-response protection, and failure isolation work.
- Security and privacy restrictions are satisfied.
- Required `data-testid` hooks are stable and contain no sensitive data.
- Basic component unit tests pass with framework-compatible tooling.
- No prohibited dependency, deep import, cycle, or undocumented structural extension remains.
- Future test levels and domain extraction remain possible without redesigning the public contract.
- Public contracts are described in the manifest, and breaking changes follow the versioning rules.
- Routing, focus, title, and scroll behavior follow Annex A, and forms follow Annex B.
- Platform features and dependencies conform to the browser support target (Annex C) and admission rules (Annex D).
- Performance thresholds and budgets are met.
- No final-phase item has been implemented early (Annex E).
- Data representation, authentication refresh, error containment, and browser-environment rules are satisfied.
- Test hygiene rules are satisfied, and every defect fix has a regression test where practical.
- Change restrictions are respected, and no rule was suppressed or weakened to pass a check.

## Annex A (normative): Routing and navigation

### Ownership

- The shell owns the top-level router and maps each route to one domain root.
- A domain owns the routes below its mount path. It receives its base path from the host, works under any base path, and does not hard-code the paths of other domains.
- Navigation to another domain uses a real link with a canonical URL, or a documented navigation `CustomEvent` that the shell handles. A domain never imports the shell's router or another domain's route table.
- Route guards improve UX only. Authorization belongs to the backend (Section 15).

### Links

- Navigation uses `<a href>` with the canonical URL, so open in new tab, copy link, and middle click work.
- Client-side interception applies only to same-origin primary-button clicks without modifier keys and without `target` or `download`.

### Route change behavior

| Concern | Required behavior |
|---|---|
| Document title | Updated on every route change to the new page's title. |
| Focus | Moved to a defined target of the new view (the page heading, or the main landmark with `tabindex="-1"`). On back navigation, a sensible position is restored. |
| Announcement | If focus does not move, the change is announced through an existing polite live region. |
| Scroll | A new navigation scrolls to the top or to the in-page anchor target. Back and forward restore the previous position. |
| Loading | A pending navigation shows progress feedback. An abandoned navigation cancels the requests it owns (Section 10). |
| Not found | An unknown route renders a defined not-found state. A server-rendered route returns the matching HTTP status. |
| Redirects | They preserve the intended destination and query, and they never create open redirects (Section 15). |
| URL hygiene | Each resource has one canonical URL. Trailing-slash and case policy is defined once. |
| Unsaved changes | In-app navigation away from a dirty form asks for confirmation through an accessible dialog. |
| Deep links | Every route loads from a direct URL and survives reload. The hosting configuration serves the shell for client-routed paths. |

### Additional routing rules

- Opening a URL in a new tab, or reloading, reproduces the same view from the URL alone. No view depends on in-memory navigation state.
- Characters in paths and query values are encoded and decoded explicitly. Encoded, Unicode, and reserved characters round-trip unchanged.
- A fragment link scrolls to its target and moves focus to it.
- Activating a link twice, or navigating while another navigation is pending, ends on the last requested route. A superseded navigation cancels the requests it owns.
- Links and assets resolve correctly under a non-root base path.

## Annex B (normative): Forms

- Start from native markup: `<form>`, `<label>`, native input types, `autocomplete`, `inputmode`, and constraint attributes. `novalidate` is used only when custom validation gives equivalent accessible messages.
- Credential fields use native inputs with correct `autocomplete` values, and pasting is never blocked, so password managers work.
- The domain owns each form's validation schema. It runs on blur and on submit and mirrors backend rules for usability only. Generic validators (format, length) may live in `shared/logic/`.
- Error presentation follows the UX/UI contract: inline messages, and an error summary that receives focus after a failed submit. The implementation keeps each field, its label, its help text, and its error message in one tree so ID references resolve (Section 7).
- Server errors map to form fields in the domain adapter. Errors that cannot be mapped appear in the summary. The form stays editable and keeps the user's entries.
- A form has at most one submission in flight. The pending state is announced and the submit control keeps its focus, instead of silently becoming unavailable. Idempotency follows Section 10.
- Drafts and autosave exist only for intentional drafts. They are keyed by identity and form version, discarded on success, sign-out, and expiry, and never overwrite newer server data. Sensitive data is excluded without explicit approval.
- Dirty tracking registers `beforeunload` only while changes exist (Section 11).
- File inputs check type and size for usability only, and provide progress, cancellation, and retry. The backend validates.
- Multi-step flows keep state in the domain store or in the URL. Each step is addressable, the back button works, and information already given is not requested again.

### Input handling

- Identifiers and codes (postal codes, phone numbers, account or reference numbers, IDs) use a text-like input (`type="text"`, or `type="tel"` for phone numbers) with `inputmode`, not `type="number"`. Number inputs change value when scrolled and give poor feedback on invalid entry, and they drop leading zeros and spaces. Use `type="number"` only where user research shows a need.
- `maxlength` is not used to truncate or block pasted text silently. Limits are stated before input, longer input is accepted, and the value is validated after normalisation.
- Length checks and counters use the same unit as the backend rule (characters, bytes, or grapheme clusters), so emoji and combined characters count as the backend counts them.
- Characters are not restricted without need. When a character is rejected, the message names it or lists the allowed ones.
- `spellcheck="false"` is set for reference numbers, names, and email addresses. `autocomplete` states the input's purpose where one applies.
- Interim text during IME composition is not validated, transformed, counted, or submitted. Pressing Enter to confirm a composition candidate does not submit the form. Browsers order composition and key events differently, so test this in each target engine.
- Autofill and paste can change a value without key events. Values are read from the control on `change` and at submit, not only from state updated by key events.
- Normalisation (trimming, Unicode form) happens on blur and on submit, not on every keystroke. The user's raw entry is preserved when errors are shown.
- A disabled control is not submitted. A value that must be submitted but not edited is shown in a read-only control.
- Reset restores initial values and clears errors and touched state.
- Two tabs holding the same draft never overwrite each other silently. A newer version is detected before saving.

## Annex C (normative): Browser support and progressive enhancement

The support target is a project parameter in the UX/UI contract. Without one, the default target is the current Baseline "Widely available" set across Chrome, Edge, Firefox, and Safari on desktop and mobile. A Baseline year target, such as "Baseline 2024", may replace it.

| Feature status | Rule |
|---|---|
| Baseline Widely available | May be used without a fallback. |
| Baseline Newly available | May be used when the support target includes it; otherwise only as an enhancement with a fallback. |
| Not Baseline | Only as a progressive enhancement behind feature detection, with a working fallback, and never required for function. |
| Experimental, origin trial, or vendor-prefixed | Not used in production. |

- Baseline status is checked against Baseline data (web.dev, MDN) when a feature is adopted.
- Features are detected, not browsers.
- Where the toolchain supports it, configure the browser list with Baseline queries and lint CSS and JavaScript compatibility in automated checks.
- Core content and primary actions of public routes work from semantic HTML before JavaScript where the technology allows it.
- A polyfill is allowed only for a feature the target requires and lacks. It loads conditionally, has an owner and a removal trigger, and is never used for an enhancement that can degrade.

Status snapshot as read from MDN and web.dev in October 2026. Re-verify at adoption.

| Feature | Status |
|---|---|
| `@layer` | Widely available (since March 2022) |
| `inert` | Widely available (since April 2023) |
| `ElementInternals` | Widely available (since March 2023) |
| Declarative Shadow DOM | Newly available (since August 2024) |
| Popover API | Newly available (since January 2025) |
| Speculation Rules | Not Baseline |
| View Transitions, CSS anchor positioning, container style queries | Not confirmed in this review; treat as enhancements until verified |

## Annex D (normative): Dependency and shared-code governance

A new runtime dependency is admitted only after these questions are answered. The decision is recorded as an architectural decision with the reason, the alternatives considered, and an owner.

| Criterion | Question |
|---|---|
| Need | Can the platform or existing code do this? |
| Maintenance | Are releases recent, issues answered, and a support policy documented? |
| Licence | Is it compatible with how the project is distributed? |
| Weight | What is the measured compressed size on the affected chunks, and does it fit the budget? Is it ESM and tree-shakeable? |
| Compatibility | Does it work with server rendering, Shadow DOM, custom elements, and the framework profile? |
| Accessibility | Does its output meet the UX/UI contract? |
| Types | Does it ship a typed API? |
| Boundary | If it shapes state, queries, validation, or routing, is it used behind an adapter? |
| Duplication | Is there one version across domains? Are duplicates measured? |
| Exit | What does removal or replacement cost? |

- Development-only dependencies need a lighter review: need, maintenance, and licence.
- Every `shared/` area has a named custodian who owns its contract, reviews its changes, and removes unused code.
- Shared code contains no domain logic. Code moves from a domain to `shared/` only after a proven need in more than one domain.
- Shared runtime dependencies are versioned together, with one framework and one version of each shared runtime library.
- Removing a shared export follows the versioning rules in Section 6.
- The lockfile is committed and installs are reproducible. Application dependencies do not use floating ranges such as `latest` or `*`.
- A dependency update is a separate change, verified by the test suite. A major upgrade of the framework or of a shared runtime library is recorded as an architectural decision.
- A dependency that is no longer used is removed in the same change that stops using it.

## Annex E (normative): Readiness and deferred items

### Domain extraction readiness

- Extracting a domain replaces the dynamic import of its `domain.ts` with runtime loading of a separately built bundle. The public contract does not change.
- Integration stays runtime-based and contract-based. Build-time package coupling between shell and domains is not introduced for extraction, because it re-couples releases.
- Domains share no state and no domain models. Communication stays minimal: attributes, properties, events, and URL.
- A domain root can be mounted in an otherwise empty host page using only its documented inputs.
- Domain styles depend only on shared CSS custom properties, never on global selectors.
- The costs of extraction are accepted knowingly: duplicated dependencies, coupling through shared runtimes, and operational complexity. Extraction happens only when independent build or deployment is a real requirement (Section 16) and the organisation can operate it.
- When extraction happens, the shell and each domain verify the contract with consumer-driven contract tests.

### Internationalisation readiness

Internationalisation is not defined here. The implementation must not prevent it.

- User-facing text is not assembled by concatenation in logic and is separable from code. Failure categories map to message keys in the presentation layer, never to backend text.
- Dates, numbers, currency, and lists are formatted through one shared utility based on `Intl` in `shared/utilities/`, with the locale as an input.
- Layouts tolerate longer text and use logical CSS properties, so a change of language or direction needs no structural rework.
- `lang` and `dir` are set on the document and honoured by Web Components.

### Test-level readiness

- Public contracts are testable from outside through tag, attributes, properties, events, and slots.
- Network access can be replaced at the adapter or transport boundary.
- No global singleton prevents running a domain in isolation.

### Deferred to the final phase

Security hardening and observability are specified outside this document and are implemented only in the final project phase, after all other phases. Until then, agents must not implement, configure, scaffold, or modify any of the following:

- Content Security Policy and other response-header policies, Trusted Types, and subresource integrity.
- Automated dependency vulnerability gating.
- Real-user monitoring, performance telemetry, and error or crash reporting.
- Tracing, logging pipelines, and alerting.

The security and privacy rules in Section 15 are basic code hygiene and apply from the start. The correlation-identifier forwarding defined in Section 10 remains. A rule in this document that names a deferred item takes effect only when that item is implemented.

## Annex F (informative): Framework profiles

These notes record what vendor documentation states about producing and consuming custom elements. They are inputs to the framework profile (Section 20), not rules. Frameworks not listed were not reviewed.

| Framework | Producing a domain root or shared component | Caveats |
|---|---|---|
| Lit | The component class is the custom element. Lit SSR renders shadow-DOM components to static HTML without a full browser DOM and emits declarative shadow DOM. | SSR supports only shadow-DOM components. Async component work was unsupported and the library was under active development when read. Integrations exist for Eleventy, Astro, Next.js (pages router), and Nuxt 3. |
| Vue 3 | `defineCustomElement` returns an `HTMLElement` constructor. The instance mounts in the shadow root on first connection and unmounts a microtask after disconnection unless the element was moved. `.ce.vue` styles are inlined into the shadow root. | Props become properties and primitives reflect to attributes. `$emit` dispatches a `CustomEvent` whose `detail` is an array. Slots use native syntax only and scoped slots are unsupported. `provide` and `inject` work only between custom elements. The runtime adds about 16 kB and can be externalised. Styles embedded in JavaScript duplicate under SSR. No SSR setup for Vue custom elements is documented. |
| React 19 | React has no authoring API for custom elements. The profile supplies a thin wrapper element that creates a React root on connection and unmounts it on disconnection. React 19 consumes custom elements and passes Custom Elements Everywhere. | On the client, a prop becomes a property when the element has one and an attribute otherwise. On the server, primitive props render as attributes and object, symbol, function, and `false` props are omitted. |
| Angular | Angular Elements: `createCustomElement` converts a component. Inputs become dash-case attributes and properties. Outputs become `CustomEvent` with the value in `detail`. The element bootstraps on connection and is destroyed on removal. | The component's selector must not be reused as the tag name. A documented issue affects destroying and re-attaching elements, so the reconnection rules in Section 6 must be tested. The guide does not cover SSR, bundle size, or zone behavior; verify these in the profile. |
| Svelte | `customElement: true` with `<svelte:options customElement="tag" />`. Props are properties and, where possible, attributes; `reflect` is off by default. `extend` can add form association through `ElementInternals`. | Styles are inlined as a JavaScript string. `shadow: "none"` removes encapsulation and slots. Context does not cross custom elements. The documentation calls custom elements generally unsuitable for SSR because the shadow DOM is invisible until JavaScript loads. |

Where a framework's custom-element output creates its shadow root with JavaScript, the first-view content is not server-rendered by that mechanism. Such routes render the root without a shadow root, use another server-rendering path, or render on the client (Section 11).

Selection criteria for a framework profile, in addition to Section 20:

- It can both author and consume custom elements.
- It has a server-rendering story for the routes that need one, including declarative shadow DOM.
- It delivers styles inside shadow roots.
- Its lifecycle behaves correctly when elements move or reattach.
- Its runtime weight per domain root is acceptable, and one runtime can be shared.
- It supports form association and the accessibility contract.
- It has test tooling, active maintenance, and a clear support policy.

## Annex G (informative): Tooling options

Options only. The selected tooling is recorded in the framework profile.

| Purpose | Options and findings |
|---|---|
| Import boundaries | dependency-cruiser validates rules for forbidden, circular, and orphaned dependencies in JavaScript, TypeScript, Vue, and Svelte files and reports as text, DOT, Mermaid, JSON, CSV, or HTML. eslint-plugin-boundaries classifies files into element types by path pattern and allows or disallows imports between them, with flat-config examples. `@nx/enforce-module-boundaries` matches projects by tags and applies allow and deny constraints, detects circular dependencies, and can control external imports. |
| Contract manifest | `@custom-elements-manifest/analyzer` generates the manifest from source (`npx cem analyze`) and has a plugin system. The schema is published as JSON Schema (version 2.1.0 when read). Consumers include editors, documentation, wrapper generators, and tools that detect public API changes between versions. |
| Component tests | Vitest Browser Mode runs tests in a real browser. Its Playwright provider is recommended for CI; WebdriverIO is also supported; the `preview` provider simulates events. Official component packages exist for Vue, Svelte, React, and Angular, and community packages for Lit. Locators such as `getByRole` and `getByLabelText` and `userEvent` from `vitest/browser` drive interaction. Module exports cannot be spied on with `vi.spyOn`. |
| Network mocking | Mock at the adapter boundary (Section 17), or at the network layer with a request-interception library. Mocks are checked against OpenAPI for drift (Section 10). |
| Browser support | Browser-list Baseline queries and Baseline-aware CSS linting (web.dev). |
| Performance | Lighthouse for lab runs, the browser's back/forward cache test panel, and a bundle analyser for per-domain chunks. |
| Later test levels | End-to-end, visual-regression, and cross-framework tools are selected when those levels are introduced. |

## Annex H (informative): References

- [Declarative Shadow DOM (web.dev)](https://web.dev/articles/declarative-shadow-dom)
- [Shadow DOM and accessibility: the trouble with ARIA (Nolan Lawson)](https://nolanlawson.com/2022/11/28/shadow-dom-and-accessibility-the-trouble-with-aria/)
- [Optimize Interaction to Next Paint (web.dev)](https://web.dev/articles/optimize-inp)
- [Back/forward cache (web.dev)](https://web.dev/articles/bfcache)
- [Fetch Priority API (web.dev)](https://web.dev/articles/fetch-priority)
- [Custom element best practices (web.dev)](https://web.dev/articles/custom-elements-best-practices)
- [Baseline (web.dev)](https://web.dev/baseline)
- [Speculation Rules API (MDN)](https://developer.mozilla.org/en-US/docs/Web/API/Speculation_Rules_API)
- [View Transition API (MDN)](https://developer.mozilla.org/en-US/docs/Web/API/View_Transition_API)
- [Popover API (MDN)](https://developer.mozilla.org/en-US/docs/Web/API/Popover_API)
- [ElementInternals (MDN)](https://developer.mozilla.org/en-US/docs/Web/API/ElementInternals)
- [`@layer` (MDN)](https://developer.mozilla.org/en-US/docs/Web/CSS/@layer)
- [`inert` (MDN)](https://developer.mozilla.org/en-US/docs/Web/HTML/Reference/Global_attributes/inert)
- [Idempotency-Key header (MDN)](https://developer.mozilla.org/en-US/docs/Web/HTTP/Reference/Headers/Idempotency-Key)
- [RFC 9457: Problem Details for HTTP APIs](https://www.rfc-editor.org/rfc/rfc9457)
- [Micro Frontends (Martin Fowler)](https://martinfowler.com/articles/micro-frontends.html)
- [TanStack Query overview](https://tanstack.com/query/latest/docs/framework/react/overview)
- [Custom Elements Manifest](https://github.com/webcomponents/custom-elements-manifest)
- [Vue and Web Components](https://vuejs.org/guide/extras/web-components)
- [React 19: support for custom elements](https://react.dev/blog/2024/12/05/react-19)
- [Angular Elements](https://angular.dev/guide/elements)
- [Svelte custom elements](https://svelte.dev/docs/svelte/custom-elements)
- [Lit SSR](https://lit.dev/docs/ssr/overview/)
- [Vitest Browser Mode](https://vitest.dev/guide/browser/)
- [dependency-cruiser](https://github.com/sverweij/dependency-cruiser)
- [eslint-plugin-boundaries](https://github.com/javierbrea/eslint-plugin-boundaries)
- [Nx enforce-module-boundaries](https://nx.dev/docs/technologies/eslint/eslint-plugin/guides/enforce-module-boundaries)
- [Tailwind CSS pull request 15975: `:host` in `@theme`](https://github.com/tailwindlabs/tailwindcss/pull/15975)
