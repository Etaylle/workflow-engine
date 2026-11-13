# API Contract and Design Specification

| Attribute | Value |
|---|---|
| Version / status | 0.1, draft |
| Identifier prefix | `API-` (stable; never renumbered or reused; withdrawn rules stay listed as *withdrawn*) |
| Obligation level | The keyword in the statement: **MUST** (= shall), **SHOULD**, **MAY** |
| Verification methods | **T** test (executed, automated or manual) · **I** inspection (review of an artefact) · **A** analysis · **D** demonstration |
| Basis abbreviations | HTTP = HTTP semantics (RFC 9110) · PD = Problem Details (RFC 9457) · OAS = OpenAPI Specification · IJSON = I-JSON (RFC 7493) · MRG = JSON Merge Patch (RFC 7396) · DEP = Deprecation (RFC 9745) and Sunset (RFC 8594) headers · ZAL = Zalando RESTful API Guidelines · AIP = Google API Improvement Proposals · YAML = YAML 1.2 · GEN = OpenAPI Generator documentation · PP = project practice |
| Defaults and parameters | Values marked **default**, and every table labelled *default*, are non-normative: they apply only until the matching project parameter in section 20 is filled, are overridden by it, and have no power over any rule (section 2.1). |

## 1. Scope and definitions

**Scope.** This document is the single contract for the design, documentation, evolution and verification of every HTTP API a project exposes between a provider (backend) and its consumers (frontend, other services, integrations). It is independent of any product, language, framework or database. It covers the wire contract, not the internal structure of the provider or of a consumer. Event-driven and streaming interfaces are out of scope.

| Term | Definition |
|---|---|
| Provider | The system that implements the API. |
| Consumer | Any system that calls the API. |
| Contract | The OpenAPI document that describes the API, together with the rules in this document. It is the authoritative description of all externally observable behaviour. |
| Operation | One HTTP method on one path, identified by an `operationId`. |
| Resource | A thing the API exposes at a canonical URL. |
| Collection | A resource that contains other resources. |
| Command | An operation that changes state without being a plain create, replace or update of a resource. |
| Problem | An error response body in the format of RFC 9457. |
| Error code | A stable, machine-readable identifier of a problem type. |
| Cursor | An opaque token that marks a position in a collection. |
| Idempotency key | A client-chosen unique value that makes a repeated request recognisable. |
| Breaking change | A change that can make a correctly written consumer fail (section 12.1). |
| Major version | The version number that changes only on breaking changes. |
| Deprecation | The announcement that an operation, parameter or property will be removed. |
| Example | A named, valid sample request or response stored in the contract. |
| Fixture | Fictional sample data used by examples, mocks and tests. |
| Project parameter | A project-specific decision recorded in section 20. |

## 2. Constraints and assumptions

| ID | Statement | Ver. | Basis |
|---|---|---|---|
| API-CON-001 | The API MUST be designed contract-first: the contract is the single source of truth for every externally observable behaviour, and behaviour MUST NOT exist that the contract does not describe. | I | ZAL |
| API-CON-002 | The contract is the agreement between provider and consumers. Neither side's implementation is the agreement. | I | PP |
| API-CON-003 | The contract MUST be independent of programming language, framework, database and product. | I | PP |
| API-CON-004 | Consumers MUST rely only on documented behaviour. The provider MUST NOT leak internal structure (persistence entities, class names, internal identifiers) through the contract. | I | PP |
| API-CON-005 | The API is resource-oriented HTTP with JSON bodies. Other styles (RPC over a single endpoint, GraphQL, gRPC) require a recorded project decision and their own contract. | I | PP |
| API-CON-006 | Business rules live in the provider. The contract exposes outcomes and usability constraints, never implementation names. | I | PP |
| API-CON-007 | Authentication, authorisation, rate-limit policy and hardening are defined elsewhere and deferred (section 15). This document fixes only how they appear on the wire. | I | PP |
| API-CON-008 | Project parameters (section 20) MUST be completed before the operations that depend on them are designed; until then defaults apply. | I | PP |
| API-CON-009 | An agent MUST read the business-requirements document or ask the user when this contract does not answer a question, and MUST NOT invent operations, fields, error codes or policy. | I | PP |

### 2.1 Rule classes and precedence

| Class | Definition | Can be overridden by |
|---|---|---|
| Fixed rule | A requirement whose basis is HTTP, PD, IJSON, MRG, DEP or OAS. | Nothing. A project MAY only tighten it. |
| Rule | Any other requirement (basis ZAL, AIP, PP). | A project parameter that tightens it, or a recorded and human-approved deviation that relaxes it. |
| Parameter | A project value in section 20. | Replaces defaults. Cannot relax a fixed rule; cannot relax a rule without a deviation. |
| Default | A value labelled *default* and every table labelled *default*. | Overridden by any parameter. A default has no power over any rule. |

| ID | Statement | Ver. | Basis |
|---|---|---|---|
| API-PRE-001 | The order of precedence MUST be: fixed rules, then rules, then parameters, then defaults. | I | PP |
| API-PRE-002 | A default MUST NOT be used to justify a violation of a rule; where a default conflicts with a rule, the rule prevails and the default is void. | I | PP |
| API-PRE-003 | Every default MUST be overridable per project through configuration and the contract, without changing rules. | I | PP |
| API-PRE-004 | A value marked *default* in a requirement is overridable; an unmarked value is part of the rule. | I | PP |
| API-PRE-005 | Verification MUST run against the effective values after overrides, never against the defaults. | T | PP |
| API-PRE-006 | If two rules conflict, the more specific rule wins; if equally specific, the stricter rule wins; the conflict MUST be recorded. | I | PP |
| API-PRE-007 | Examples and illustrative snippets in this document are non-normative. | I | PP |
| API-PRE-008 | A deviation from a rule MUST be recorded with the reason, the scope, an owner and an expiry or review date. | I | PP |

## 3. The contract document

| ID | Statement | Ver. | Basis |
|---|---|---|---|
| API-SPC-001 | A change in API behaviour MUST start as a change to the contract, reviewed before the implementation is written. | I | ZAL |
| API-SPC-002 | The published contract MUST be one self-contained, version-controlled document (YAML or JSON) per API. Source files MAY be split and bundled, and the bundled artefact is the contract. | I | ZAL |
| API-SPC-003 | The contract MUST use one OpenAPI version, **default** 3.1. A later 3.x version MAY be used when every tool in the toolchain (linter, diff, renderer, mock, test) supports it. Versions MUST NOT be mixed. | I | OAS |
| API-SPC-004 | `info` MUST contain `title`, `version`, `description` and `contact`. `info.description` MUST summarise the API conventions (authentication overview, errors, pagination, versioning, idempotency) in CommonMark. | I | ZAL |
| API-SPC-005 | `info.version` MUST follow semantic versioning: major for breaking changes, minor for compatible additions, patch for corrections that change no behaviour. | I | ZAL |
| API-SPC-006 | `servers` MUST list only documented environment base URLs. Secrets and internal-only hostnames MUST NOT appear in a contract published to external consumers. | I | PP |
| API-SPC-007 | Every operation MUST have a unique, stable `operationId` (lowerCamelCase, verb then noun, for example `listCases`), a `summary`, at least one tag, and a `description` wherever behaviour is not obvious from the schema. | I | OAS |
| API-SPC-008 | Changing an `operationId` MUST be listed in the changelog, because tools use it. | I | PP |
| API-SPC-009 | Every operation MUST declare every response it can produce: each success status, each documented error status, and a `default` response that references the Problem schema. | T | OAS |
| API-SPC-010 | Request and response bodies MUST reference named schemas in `components`. Anonymous inline object schemas are allowed only for trivial single-use cases. | I | PP |
| API-SPC-011 | Every property MUST declare its type and every constraint the provider actually enforces (`format`, `minimum`, `maximum`, `minLength`, `maxLength`, `pattern`, `enum`, `required`). A constraint in the contract MUST be enforced, and an enforced constraint MUST be in the contract. | T | PP |
| API-SPC-012 | Every `number` and `integer` MUST declare a `format` (`int32`, `int64`, `double`). Exact decimals MUST use a string with a documented pattern (API-REP-007). | I | ZAL |
| API-SPC-013 | Object schemas MUST state `additionalProperties` explicitly: `false` for the request bodies of write operations, and left open for response bodies so they can be extended compatibly. | I | PP |
| API-SPC-014 | Optional and nullable MUST be declared separately: nullable uses a type array such as `["string", "null"]`, optional uses absence from `required`. The meaning of `null` MUST be stated (API-REP-011). | I | OAS |
| API-SPC-015 | An enumeration MUST be declared as closed (`enum`) or open (`type: string` with `examples` listing the known values), and each value's meaning MUST be described. | I | ZAL |
| API-SPC-016 | Security schemes MUST be declared in `components.securitySchemes`, and every operation MUST declare its `security` requirement, with an explicit empty requirement for public operations. | I | OAS |
| API-SPC-017 | The permission or role an operation requires MUST be stated, in `security` scopes where the scheme supports them and otherwise in the description. | I | PP |
| API-SPC-018 | Deprecated operations, parameters and properties MUST carry `deprecated: true` and a description naming the replacement and the sunset date (API-EVO-007). | I | OAS |
| API-SPC-019 | Descriptions MUST use CommonMark and the project's documentation language (**default** US English). | I | ZAL |
| API-SPC-020 | Top-level `tags` MUST be declared with descriptions and ordered as the reference should present them. | I | OAS |
| API-SPC-021 | The contract MUST NOT contain unused components or unresolved references. Remote references MUST point to immutable, versioned locations. | T | ZAL |
| API-SPC-022 | Vendor extensions (`x-` members) MUST be few, documented, and never required to understand behaviour. | I | PP |
| API-SPC-023 | Behaviour that schemas cannot express MUST be written in descriptions: side effects, ordering, asynchronous behaviour, idempotency, required permissions, and consistency. | I | PP |
| API-SPC-024 | The contract MUST be stored at a documented path in version control, and every released version MUST be tagged and retained for comparison. | I | PP |
| API-SPC-025 | The contract MUST validate against the OpenAPI schema of its declared version. | T | OAS |

## 4. Generation and consumer adaptation

| ID | Statement | Ver. | Basis |
|---|---|---|---|
| API-GEN-001 | The contract MUST be usable without code generation: everything a consumer needs MUST be in schemas, descriptions and examples. | I | PP |
| API-GEN-002 | The provider MAY generate server interfaces and transport models from the contract. If it does, generated artefacts MUST be reproducible build outputs, MUST NOT be edited by hand, and MUST NOT contain domain logic. Implementations implement the generated interfaces. | I | PP |
| API-GEN-003 | The domain model of the provider MUST stay separate from generated transport models. | I | PP |
| API-GEN-004 | Deriving the contract from code annotations (code-first) is allowed only when project parameter P12 says so. The derived document MUST then be committed, reviewed, linted and diffed like a handwritten contract, and the diff MUST be reviewed before merge (API-SPC-001). | I | PP |
| API-GEN-005 | Consumer code generation is optional and is a consumer decision. The contract MUST NOT require it or assume it. The default posture (**default**, project parameter P11) is that consumers adapt to the contract by hand so they own their representations. | I | PP |
| API-GEN-006 | Where a consumer generates code from the contract, the generated code MUST be limited to transport shapes, MUST be reproducible, MUST NOT be edited by hand, and MUST NOT reach the consumer's presentation layer. | I | PP |
| API-GEN-007 | Generator configuration and generator-specific flags MUST live outside the contract. | I | PP |
| API-GEN-008 | Mocks and fixtures MAY be derived from contract examples. They MUST be regenerated or validated when the contract changes and MUST NOT replace contract tests. | T | PP |


### 4.1 Authoring the contract in YAML

The contract is written for people first and for tools second, and it MUST satisfy both. These rules make the YAML readable, diffable and generation-ready.

#### 4.1.1 Source layout

Default source layout (non-normative, overridable by project parameter P34):

```text
api/
├── openapi.yaml                    root: info, servers, tags, security, paths, components index
├── paths/<resource-group>.yaml     operations of one resource group, mirroring the URL hierarchy
├── components/
│   ├── schemas/<SchemaName>.yaml   one schema per file, named like the schema
│   ├── parameters/<Name>.yaml
│   ├── responses/<Name>.yaml
│   ├── headers/<Name>.yaml
│   ├── examples/<resource>/<name>.yaml
│   └── security-schemes.yaml
├── overlays/<audience>.yaml        optional audience or environment variants
├── generator/<target>.yaml         generator configuration (outside the contract)
└── dist/openapi.bundled.yaml       bundled contract (build output)
```

| ID | Statement | Ver. | Basis |
|---|---|---|---|
| API-YML-001 | The source MUST be split by resource group, mirroring the URL hierarchy, with one schema per file named like the schema. A source file SHOULD NOT exceed 500 lines (**default**). Files SHOULD NOT be split so finely that navigation suffers. | I | PP |
| API-YML-002 | References between source files MUST be relative `$ref` paths. Absolute filesystem paths and remote references MUST NOT be used (API-SPC-021). | T | PP |
| API-YML-003 | A pinned bundling step MUST produce the single published contract (API-SPC-002). The bundle is a reproducible build output, MUST NOT be edited by hand, and is the only input of linters, comparison, generators and the reference. | T | PP |
| API-YML-004 | Audience-specific or environment-specific variants (for example a public subset or a different server list) MUST be derived from the single source by an overlay or filter, never by copy-editing. The derived contract MUST be validated and linted like the source. | T | OAS |
| API-YML-005 | Every source file MUST be UTF-8 with LF line endings and a final newline, without trailing whitespace. | T | PP |

#### 4.1.2 YAML syntax and formatting

| ID | Statement | Ver. | Basis |
|---|---|---|---|
| API-YML-006 | The contract MUST be YAML 1.2 restricted to the JSON-compatible tag set. Custom tags MUST NOT be used, and map keys MUST be scalar strings. | T | OAS |
| API-YML-007 | Anchors, aliases and merge keys (`&`, `*`, `<<`) MUST NOT be used. Reuse MUST be done with `$ref`, because tool support differs. | T | PP |
| API-YML-008 | Indentation MUST be two spaces, tabs MUST NOT be used, and block style MUST be used except for empty collections (`[]`, `{}`). | T | PP |
| API-YML-009 | A string that a YAML parser could read as another type MUST be quoted: boolean-like words (`yes`, `no`, `on`, `off`, `y`, `n`), `null` and `~`, numbers including version strings (`1.10`) and values with leading zeros (`007`), dates and timestamps (`2024-01-01`), and any value containing `: ` or ` #`. Country codes such as `NO` are the typical trap. | T | YAML |
| API-YML-010 | Quoting MUST be consistent (**default** double quotes). Multi-line descriptions MUST use the literal block style (`\|`). The folded style (`>`) MAY be used for prose whose line breaks are irrelevant. Non-description lines SHOULD NOT exceed 120 characters (**default**). | T | PP |
| API-YML-011 | Keys within a map MUST be unique. | T | YAML |
| API-YML-012 | Key order MUST follow one convention, enforced by the formatter. **Default** for an operation: `summary`, `description`, `operationId`, `tags`, `parameters`, `requestBody`, `responses`, `security`, `deprecated`. **Default** for a schema: `description`, `type`, `format`, `required`, `properties`, then constraints and `examples`. | T | PP |
| API-YML-013 | A YAML linter and formatter with committed configuration MUST run in the pipeline, and violations MUST block the merge. | T | PP |
| API-YML-014 | Comments MAY help authors but MUST NOT carry requirements. Behaviour MUST be written in descriptions (API-SPC-023). | I | PP |
| API-YML-015 | The source contract MUST be YAML only. JSON MAY be produced as a build output. | I | PP |

#### 4.1.3 Naming

| ID | Statement | Ver. | Basis |
|---|---|---|---|
| API-YML-016 | Schema names MUST be PascalCase, singular nouns in the domain vocabulary, ASCII letters and digits only, at most 60 characters (**default**), and unique when compared case-insensitively. | T | PP |
| API-YML-017 | Schema names MUST follow one suffix convention. **Default:** `<Resource>` (read representation), `<Resource>Summary`, `Create<Resource>Request`, `Replace<Resource>Request`, `Patch<Resource>Request`, `<Resource>Page` (one page of a collection), `<Command>Request` and `<Command>Result` for commands, `<Name>Problem` for a code-specific problem, `<Resource>Id` for an identifier. | I | PP |
| API-YML-018 | Component names MUST be PascalCase: parameters by meaning (`CaseId`, `PageLimit`, `Cursor`), responses by outcome (`NotFound`, `ValidationFailed`, `Conflict`), headers by header name (`ETag`, `Location`, `RetryAfter`, `RequestId`). Example names MUST be lowercase kebab-case, for example `typical`, `minimal`, `maximal`, `tariff-001-missing-tariff`. | I | PP |
| API-YML-019 | Property names MUST follow the API casing (API-REP-003), MUST use ASCII letters and digits, and MUST start with a letter. They SHOULD NOT be reserved words of common target languages (`class`, `default`, `new`, `package`, `function`, `object`, `import`, `enum`, `interface`, `static`, `void`). | T | PP |
| API-YML-020 | Enumeration values MUST be valid identifiers (**default** UPPER_SNAKE_CASE), unique, starting with a letter, and MUST NOT be empty, numeric-only or contain spaces or symbols, so generators need no name mapping. | T | PP |
| API-YML-021 | An `operationId` MUST be lowerCamelCase and begin with a verb from a fixed set (**default** `list`, `get`, `create`, `replace`, `update`, `delete`, plus documented command verbs). | T | PP |
| API-YML-022 | Every operation MUST have exactly one tag (**default**), a Title Case plural noun naming its resource group. The tag drives grouping in the reference and in generated interfaces, so renaming it is a documented change. | T | PP |
| API-YML-023 | Names MUST use domain vocabulary, not storage or user-interface terms. Abbreviations MUST be limited to domain-standard ones. | I | PP |

#### 4.1.4 Schema design for generation

| ID | Statement | Ver. | Basis |
|---|---|---|---|
| API-YML-024 | Objects, enumerations and arrays of objects MUST be named schemas in `components`. Inline schemas for them MUST NOT be used, because generators invent unstable names for them. Inline schemas are allowed only for single-use scalar constraints. | T | GEN |
| API-YML-025 | Read, create, replace and patch representations MUST be separate schemas. `readOnly` and `writeOnly` MAY be used only for an isolated property where separate schemas would duplicate a large structure, and the exception MUST be noted in the description. | I | PP |
| API-YML-026 | `allOf` MUST be used only to extend a base schema with additional properties, at most one level deep and with at most three members, and MUST NOT redefine inherited properties. Deep inheritance MUST NOT be built. | T | PP |
| API-YML-027 | A union of object types MUST use `oneOf` with a `discriminator`. The discriminator MUST name a required string enumeration property present in every member and MUST have an explicit `mapping` that references each member schema. `anyOf` MUST NOT be used for types that are generated. | T | OAS |
| API-YML-028 | Circular references MUST NOT be used, except to model a tree. A recursive property MUST be optional, and the generation smoke test (API-GPL-008) MUST pass for it. | T | PP |
| API-YML-029 | In a 3.1 contract, nullability MUST be expressed with a type array (`["string", "null"]`). The 3.0 keyword `nullable` MUST NOT appear. | T | OAS |
| API-YML-030 | A `$ref` MUST stand alone, without sibling keywords, because their handling differs between versions and tools. Documentation belongs in the referenced schema. A nullable reference MUST use `oneOf` with `{ type: "null" }`. | T | OAS |
| API-YML-031 | Maps MUST use `additionalProperties` with a typed schema. A free-form object (`type: object` without properties) MUST NOT be used unless its meaning is documented. | I | PP |
| API-YML-032 | Arrays MUST declare `items` and `maxItems`, and `uniqueItems` where they are sets. Strings MUST declare `maxLength`, and `pattern` where the format matters. Numbers MUST declare `minimum` and `maximum`. | T | PP |
| API-YML-033 | Patterns MUST use a portable regular-expression subset: no lookbehind, no backreferences, no inline flags. | T | PP |
| API-YML-034 | `required` MUST list exactly the properties that are always present in that representation. In a response schema, removing a property from `required` is a breaking change. | T | PP |
| API-YML-035 | `default` MUST appear only where the provider applies exactly that value when the property is absent. Generators copy defaults into models, so a mismatch is a defect. | T | PP |
| API-YML-036 | `format` MUST be limited to formats defined by OpenAPI and JSON Schema (`int32`, `int64`, `float`, `double`, `date`, `date-time`, `duration`, `uuid`, `uri`, `email`). A project-specific format MUST NOT be invented; a named schema MUST be used instead. | I | OAS |
| API-YML-037 | A binary file part MUST use the form of the contract's OpenAPI version (3.1: `contentMediaType`, without `type`). One form MUST be used across the API, and the generator mapping for it MUST be documented. | I | OAS |
| API-YML-038 | Each kind of identifier MUST be a named schema (`CaseId`) with type, `maxLength` and `pattern`, reused by every reference to it. | I | PP |
| API-YML-039 | Shared building blocks (`Problem`, `ValidationProblem`, `Money`, `PageInfo`, and the identifier and instant schemas) MUST be defined once in a common set and referenced, never redefined. | I | PP |
| API-YML-040 | Each collection MUST have its own explicit page schema (`CasePage`) with `items` and a reference to `PageInfo`. Generic wrapper types MUST NOT be used. | I | PP |
| API-YML-041 | `title` and vendor extensions MUST NOT be used to steer generator naming (API-GEN-007). | I | PP |

#### 4.1.5 Operations, parameters, responses and examples

| ID | Statement | Ver. | Basis |
|---|---|---|---|
| API-YML-042 | Path parameters MUST be components referenced by every operation that uses them, and every variable in a path template MUST have a matching required path parameter. | T | OAS |
| API-YML-043 | Common query parameters (`limit`, `cursor`, `sort`, `q`, `fields`, `embed`) MUST be components, so their bounds and defaults have one source. | I | PP |
| API-YML-044 | Common error responses MUST be components referenced by every operation, and success responses MUST reference named schemas. | T | PP |
| API-YML-045 | A write operation MUST declare `requestBody.required: true` and list its media types explicitly (`application/json`, or `application/merge-patch+json` for a patch). | T | OAS |
| API-YML-046 | Headers that matter (`ETag`, `Location`, `Retry-After`, `Idempotency-Key`, `X-Request-Id`, `Deprecation`, `Sunset`, `Link`) MUST be declared as header components and referenced wherever they occur. | I | OAS |
| API-YML-047 | `servers` SHOULD use a relative base path (**default** `/api/v1`) so the reference works from the same origin. | I | PP |
| API-YML-048 | Examples MUST live in `components/examples`, one file per example, and be referenced from media types, named by API-YML-018, and validated by API-DOC-007. | T | OAS |
| API-YML-049 | A default `security` requirement MUST be declared at the root, and each operation that differs MUST override it explicitly. | I | OAS |

### 4.2 Generation pipeline

| ID | Statement | Ver. | Basis |
|---|---|---|---|
| API-GPL-001 | The generator and the bundler MUST be pinned to exact versions. An upgrade is a reviewed change. Generator configuration MUST be version-controlled outside the contract (package names, library, type, name and import mappings, output paths). | I | GEN |
| API-GPL-002 | Generation MUST be deterministic: the same contract, configuration and tool version MUST produce identical output. Options that sort output (for example model property sorting) MUST be enabled where the tool offers them. | T | GEN |
| API-GPL-003 | Generation MUST read only the bundled contract and run in the build before compilation. Output MUST go to a build directory and is not committed (**default**). If generated code is committed, a check MUST regenerate it and fail on any difference. | T | PP |
| API-GPL-004 | The generator's validation of the contract MUST stay enabled. Skipping validation or relaxing strict mode MUST NOT be used to make generation pass. A generator warning about the contract is a contract defect and MUST be fixed in the contract. | T | GEN |
| API-GPL-005 | Generator-side adjustments (normalisation, type, name and import mappings) are allowed only for target-language concerns such as reserved words, package names and mapping to existing value types. Each MUST be documented with its reason and MUST NOT change wire behaviour. | I | PP |
| API-GPL-006 | Hand-written code MUST live in separate source sets or packages from generated code. Regeneration MUST NOT overwrite hand-written files. | T | GEN |
| API-GPL-007 | The build MUST compile generated code, and the provider MUST implement the generated interfaces so the contract test exercises the real implementation. | T | PP |
| API-GPL-008 | For every target language the project uses, a generation smoke test (generate and compile) MUST run on every contract change, even where consumers do not ship generated code. It finds constructs a target cannot express. | T | PP |
| API-GPL-009 | Generated models and clients MUST NOT be published as packages unless a project parameter says so. If published, the package version MUST equal `info.version`. | I | PP |
| API-GPL-010 | Customised generator templates SHOULD be avoided. If used, they MUST be version-controlled, minimal, and re-verified at every generator upgrade. | I | GEN |
| API-GPL-011 | Type and import mappings to hand-written classes MUST be limited to shared value types (for example `Money` and instants) and MUST NOT map domain entities. | I | PP |
| API-GPL-012 | Where a consumer generates code, it MUST use the same bundled contract and these rules (API-GEN-006). | I | PP |
| API-GPL-013 | A generator or bundler upgrade MUST regenerate, compare the output with the previous output, run every gate, and record the result. | T | PP |
| API-GPL-014 | The pipeline order MUST be: YAML lint, bundle, contract validation, contract lint, example validation, breaking-change comparison, generation smoke test, provider build and contract test. A step MUST NOT be skipped because an earlier step passed. | T | PP |

## 5. Resources and URLs

| ID | Statement | Ver. | Basis |
|---|---|---|---|
| API-RES-001 | URLs MUST identify resources with nouns, not verbs. Path segments MUST be lowercase kebab-case, collection names MUST be plural, and paths MUST NOT contain trailing slashes, empty segments or file extensions. | I | ZAL |
| API-RES-002 | Sub-resource nesting SHOULD NOT exceed three levels (**default**). A deeper relation SHOULD be a top-level resource with a filter. | I | ZAL |
| API-RES-003 | Resource identifiers MUST be opaque, URL-safe strings. Consumers MUST NOT parse or construct them, and the provider MUST NOT assign them meaning. | I | AIP |
| API-RES-004 | An identifier MUST NOT be treated as a secret or as an authorisation. Every operation MUST authorise access to the identified resource. | T | PP |
| API-RES-005 | Each resource MUST have one canonical URL, and its representation MUST carry its `id`. | I | PP |
| API-RES-006 | A state change that is not a plain create, replace or update MUST be modelled as a command. A command SHOULD be a noun sub-resource (`POST /cases/{id}/closure`). A verb as the final path segment (`POST /cases/{id}/close`) is allowed when no natural noun exists. A command MUST use `POST` and MUST be documented as a command. | I | ZAL |
| API-RES-007 | A command that changes workflow state MUST return the updated resource or a documented result. An invalid transition MUST be rejected with 409 and an error code. | T | PP |
| API-RES-008 | Allowed transitions SHOULD be discoverable through a documented read operation or an `allowedTransitions` property. | I | PP |
| API-RES-009 | An alias for the current principal (for example `/me`) MAY be used and MUST be documented as an alias. | I | PP |
| API-RES-010 | Query parameter names MUST use the API's property casing (API-REP-003). Conventional names are `q`, `sort`, `fields`, `embed`, `limit`, `cursor`, `offset`. | I | ZAL |
| API-RES-011 | Array-valued query parameters MUST use one serialisation across the API, **default** repeated parameter (`style: form`, `explode: true`), and MUST declare it. | I | OAS |
| API-RES-012 | Audiences with different permission models (public, application, administrative, preview) SHOULD be separated by path prefix and tag. Preview operations MUST be separate from public operations. | I | PP |
| API-RES-013 | Sensitive values (health, financial, identity data, secrets) MUST NOT appear in URLs, query strings or headers. An operation that needs sensitive criteria MUST take them in a request body, for example `POST /resources/search`, documented as safe and read-only. | T | PP |
| API-RES-014 | A URL SHOULD NOT exceed 2,048 characters (**default**). Long filter lists SHOULD move to a documented search operation with a body. | I | PP |
| API-RES-015 | Resources MUST model capabilities and business concepts, not database tables. A resource MAY aggregate several stored entities. | I | PP |
| API-RES-016 | Every operation MUST trace to a documented requirement. An operation without one MUST NOT be added. | I | PP |
| API-RES-017 | View or aggregation operations MAY be added when the contract documents their source resources and their behaviour on partial failure. | I | PP |

## 6. HTTP semantics

| Method | Meaning | Safe | Idempotent | Request body | Typical success |
|---|---|---|---|---|---|
| GET | Read a resource or collection | yes | yes | no | 200, 304 |
| HEAD | Read headers only | yes | yes | no | 200 |
| POST | Create in a collection, or run a command | no | not by default | yes | 201, 202, 200, 204 |
| PUT | Replace a resource completely, or create at a client-chosen URL | no | yes | yes | 200, 201, 204 |
| PATCH | Change part of a resource | no | not guaranteed | yes | 200, 204 |
| DELETE | Remove a resource | no | yes | no | 204 |
| OPTIONS | Describe communication options | yes | yes | no | 204 |

| Status | Use |
|---|---|
| 200 | Success with a body. |
| 201 | A resource was created. Carries `Location` and the representation. |
| 202 | Accepted for asynchronous processing. Carries `Location` of a status resource. |
| 204 | Success without a body. |
| 304 | Conditional read; the cached representation is current. |
| 400 | The request cannot be understood, or violates the schema or a parameter constraint, including field validation. |
| 401 | Authentication is missing, invalid or expired. |
| 403 | The caller is authenticated but not permitted. |
| 404 | The resource does not exist, or its existence must not be revealed. |
| 405 | The method is not supported on this path. Carries `Allow`. |
| 406 | The requested representation or locale is not available. |
| 409 | The request conflicts with the current state: invalid transition, duplicate unique value, request in flight with the same idempotency key. |
| 410 | The resource was removed permanently. |
| 412 | A precondition (`If-Match`) failed. |
| 413 | The body is larger than the documented limit. |
| 415 | The request content type is not supported. |
| 422 | The request is valid against the schema but violates a business rule. |
| 428 | A precondition is required. |
| 429 | Too many requests. Carries `Retry-After`. |
| 500 | Unexpected provider failure. |
| 502 / 504 | An upstream or gateway failure or timeout. |
| 503 | The service is temporarily unavailable. Carries `Retry-After` where known. |

| ID | Statement | Ver. | Basis |
|---|---|---|---|
| API-HTTP-001 | Methods MUST be used with their HTTP semantics. Safe methods MUST NOT change state, and `PUT` and `DELETE` MUST be idempotent. | T | HTTP |
| API-HTTP-002 | `GET`, `HEAD` and `DELETE` requests MUST NOT rely on a request body. | T | HTTP |
| API-HTTP-003 | `PUT` MUST replace the full representation. `PATCH` MUST change only the named parts. | T | HTTP |
| API-HTTP-004 | The default `PATCH` media type is `application/merge-patch+json`. Its rules apply: `null` removes a member and arrays are replaced as a whole. A resource that needs explicit `null` values or element-wise array changes MUST use another documented mechanism. | T | MRG |
| API-HTTP-005 | The most specific status code MUST be used, and only standard codes. An error MUST NOT be carried in a 2xx response. | T | HTTP |
| API-HTTP-006 | Field validation failures MUST use 400. Business-rule violations on a valid request MUST use 422. State-dependent conflicts MUST use 409. These three MUST NOT be interchanged within one API. | T | PP |
| API-HTTP-007 | An unsupported method MUST return 405 with `Allow`. An unsupported `Accept` MUST return 406. An unsupported `Content-Type` MUST return 415. | T | HTTP |
| API-HTTP-008 | 401 MUST mean missing, invalid or expired authentication and MUST carry `WWW-Authenticate` for the scheme in use. 403 MUST mean authenticated but not permitted. 404 MAY replace 403 where existence must not be revealed; this decision MUST be recorded per resource type. | T | HTTP |
| API-HTTP-009 | An empty collection MUST return 200 with an empty array. A missing single resource MUST return 404, never 200 with `null`. | T | PP |
| API-HTTP-010 | The API SHOULD NOT answer a call with a redirect. A documented redirect MUST use 307 or 308 so the method is preserved. | I | ZAL |
| API-HTTP-011 | Media types MUST be honoured. The default request and response type is `application/json` in UTF-8, and errors use `application/problem+json`. | T | HTTP |
| API-HTTP-012 | A bulk operation MUST report a result per item, with the item's own status and, on failure, its own problem. Whether processing is all-or-nothing or partial MUST be documented. | T | PP |
| API-HTTP-013 | An operation that can exceed the provider timeout (**default** 30 s) MUST be asynchronous (API-WRT-010). | I | PP |

## 7. Representations

| ID | Statement | Ver. | Basis |
|---|---|---|---|
| API-REP-001 | Request and response bodies MUST be JSON objects in UTF-8. The top-level value MUST be an object, never an array or a primitive, so it can be extended. | T | ZAL |
| API-REP-002 | Documents MUST conform to I-JSON: member names are unique, and numbers beyond IEEE 754 double precision are not used. | T | IJSON |
| API-REP-003 | Property names MUST use one casing convention across the whole API, **default** camelCase (project parameter P04). | I | PP |
| API-REP-004 | Common names MUST be used consistently: `id` for the resource's own identifier, `<resource>Id` for references, `<event>At` for instants, `<name>Date` for calendar days, `<name>Duration` for durations. | I | ZAL |
| API-REP-005 | Instants MUST be RFC 3339 `date-time` strings in UTC with `Z`, uppercase `T` and seconds. Numeric timestamps MUST NOT be used. | T | IJSON |
| API-REP-006 | Calendar days MUST use `date` (`YYYY-MM-DD`) and MUST NOT carry a time or zone. The provider MUST NOT convert them through a time zone. A local date-time MUST be accompanied by a time-zone identifier. Durations MUST use ISO 8601. | T | ZAL |
| API-REP-007 | Money and exact decimals MUST NOT use floating point. They MUST be an object with `amount` as a decimal string and `currency` as an ISO 4217 code, or integer minor units with the currency, as project parameter P23 says (**default** decimal string). | T | PP |
| API-REP-008 | Codes MUST use standards: countries ISO 3166-1 alpha-2, languages BCP 47, currencies ISO 4217. | I | ZAL |
| API-REP-009 | Identifiers MUST be strings. An integer that can exceed 2^53 − 1 MUST be a string. | T | IJSON |
| API-REP-010 | Response enumerations are open unless the contract declares them closed. Consumers MUST tolerate unknown values of an open enumeration. Adding a value to a closed enumeration used in responses is a breaking change. Enumeration values SHOULD be UPPER_SNAKE_CASE strings (**default**). | I | ZAL |
| API-REP-011 | Booleans MUST NOT be `null`, and arrays MUST NOT be `null`; an empty array is `[]`. For an optional nullable property, absence and `null` mean the same, except in a merge-patch request where `null` removes the member. | T | ZAL |
| API-REP-012 | Responses MUST be explicit representations designed for the contract. They MUST NOT expose persistence entities, internal identifiers, audit fields, deletion markers, storage keys or internal staff identities unless the contract defines them. | I | PP |
| API-REP-013 | Request bodies MUST accept only documented fields (no mass assignment). Unknown fields in a write request MUST be rejected with 400 (**default**, project parameter P25). Consumers MUST ignore unknown fields in responses. | T | PP |
| API-REP-014 | Properties marked `readOnly` MUST be rejected or ignored in requests as documented, and properties marked `writeOnly` MUST NOT appear in responses. | T | OAS |
| API-REP-015 | The content locale MUST be selected by one documented mechanism. An unsupported locale MUST return a defined error and MUST NOT silently substitute another language, unless a fallback is documented and signalled with `Content-Language`. | T | PP |
| API-REP-016 | Status and error information MUST be code-based. Human-readable text from the API is content only. Consumers map codes to user-facing messages. | I | PP |
| API-REP-017 | File uploads MUST use `multipart/form-data` with documented maximum size and accepted types per file. File downloads MUST declare `Content-Type` and `Content-Disposition`, or return a time-limited URL. An oversize upload MUST return 413. | T | PP |
| API-REP-018 | Binary data inside JSON, where unavoidable, MUST be base64url-encoded. | I | IJSON |
| API-REP-019 | The meaning of absent, `null`, empty string and zero MUST be documented where they differ. | I | PP |
| API-REP-020 | Property order and whitespace are insignificant, and neither side MAY depend on them. | I | IJSON |
| API-REP-021 | Maximum request and response sizes MUST be documented (**default** table 17.2). | I | PP |
| API-REP-022 | A summary representation and a detail representation of one resource MUST be separate named schemas. | I | PP |
| API-REP-023 | The acting principal MUST NOT be supplied in a request body. It is taken from the authentication context. | T | PP |
| API-REP-024 | Boolean properties MUST be named positively (`enabled`, not `disabled`) and MUST NOT use double negatives. | I | PP |
| API-REP-025 | A quantity property MUST name its unit or reference a documented unit (`timeoutSeconds`, `sizeBytes`). | I | PP |
| API-REP-026 | A field the caller may not see MUST be omitted, not returned as `null`. | T | PP |
| API-REP-027 | Values compared for uniqueness MUST be Unicode-normalised (NFC) before comparison, and the contract MUST state the rule. | T | PP |
| API-REP-028 | A CSV export MUST follow RFC 4180, use UTF-8 with a header row, apply the same filters as the matching collection, and neutralise cells starting with `=`, `+`, `-`, `@`, tab or carriage return. | T | PP |
| API-REP-029 | A large export MUST run as an asynchronous operation (API-WRT) and be delivered through a time-limited download URL (default 5 min). | T | PP |
| API-REP-030 | A download that supports resumption MUST document `Range` support and answer with 206 or 416 as in HTTP. | T | HTTP |

## 8. Errors

| ID | Statement | Ver. | Basis |
|---|---|---|---|
| API-ERR-001 | Every error response (4xx, 5xx) the provider produces MUST use `application/problem+json` per RFC 9457, including errors generated by the framework for unknown paths, unsupported methods and unsupported media types. | T | PD |
| API-ERR-002 | A problem MUST carry `type`, `title`, `status` and `code`, and SHOULD carry `detail`, `instance` and `traceId`. `status` MUST equal the HTTP status. | T | PD |
| API-ERR-003 | `type` MUST identify the problem type with a stable URI reference. `title` MUST be short and stable for the type, and MUST NOT be localised. `detail` is a human-readable diagnostic for this occurrence and MAY vary. | I | PD |
| API-ERR-004 | `code` is a stable machine-readable string (project parameter P09, **default** pattern `^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$`, for example `TARIFF_001`) and corresponds one-to-one to `type`. | T | PP |
| API-ERR-005 | A code MUST NOT be reused or change meaning. A withdrawn code stays in the catalogue as *withdrawn*. | I | PP |
| API-ERR-006 | Consumers MUST map errors by `code` or `type`, never by `title`, `detail` or status text. Consumers MUST ignore unknown extension members and fall back to the HTTP status for unknown codes. | T | PD |
| API-ERR-007 | `type` URIs are identifiers. They are not required to resolve, and consumers MUST NOT fetch them automatically. | I | PD |
| API-ERR-008 | A validation problem MUST carry an `errors` array. Each entry MUST have `code`, `detail`, and either `pointer` (a JSON Pointer into the request body) or `parameter` (a query or header name). All violations MUST be reported together. | T | PP |
| API-ERR-009 | Names in `errors` MUST be contract names, not internal names. The values of sensitive fields MUST NOT be echoed. | T | PP |
| API-ERR-010 | `traceId` SHOULD be the correlation identifier of the request, also returned in a response header (project parameter P18), and MUST be safe to show to a user for support. | T | PP |
| API-ERR-011 | Errors MUST NOT expose stack traces, SQL, table or schema names, class names, file paths, hostnames, secrets or raw upstream error bodies. | T | PP |
| API-ERR-012 | A 5xx problem MUST use a generic `title` and `detail` and a generic code (**default** `INTERNAL_001`). 503 SHOULD carry `Retry-After`. | T | PP |
| API-ERR-013 | Error responses MUST carry `Cache-Control: no-store` unless the contract states otherwise for a specific code. | T | HTTP |
| API-ERR-014 | Every error code MUST be documented in the contract with: code, HTTP status, meaning, whether the user can correct it, whether a retry can succeed, and the expected consumer action. | I | PP |
| API-ERR-015 | Extension members that carry structured context (for example `allowedTransitions`, `retryAfterSeconds`) MUST be declared in the Problem schema of that code. | I | PP |
| API-ERR-016 | Every error response in the contract MUST reference the shared Problem schema. Ad hoc error shapes MUST NOT be defined. | I | PP |
| API-ERR-017 | `title` and `detail` MUST be written in the contract language (**default** English). The consumer localises by code. | I | PP |
| API-ERR-018 | A rate-limit rejection MUST be 429 with `Retry-After` and a problem code (**default** `RATE_LIMIT_001`). The policy values are deferred (section 15). | T | HTTP |
| API-ERR-019 | An error produced by infrastructure outside the provider's control MAY have another body. Consumers MUST handle that case (API-EDG-021). | I | PP |
| API-ERR-020 | The `errors` array of one problem MUST NOT exceed the documented cap (**default** 100). When truncated, the problem MUST say so. | T | PP |

## 9. Collections

| ID | Statement | Ver. | Basis |
|---|---|---|---|
| API-COL-001 | Every operation that returns a collection of unbounded size MUST be paginated from its first release. Adding pagination later is a breaking change. | T | AIP |
| API-COL-002 | Pagination is cursor-based by default. The request takes `limit` (default 25, maximum 100, both **default**, project parameter P06) and `cursor`. The response is an object with `items` and `page`. `page` contains `limit` and, when more items may exist, `nextCursor`. | T | ZAL |
| API-COL-003 | The absence of `nextCursor` is the only signal for the end of a collection. A page MAY contain fewer items than `limit` before the end. | T | AIP |
| API-COL-004 | A `limit` above the maximum MUST be reduced to the maximum (**default**) or rejected with 400, as documented. A `limit` below 1 MUST be rejected with 400. The contract MUST declare `minimum` and `maximum`. | T | AIP |
| API-COL-005 | Cursors MUST be opaque and URL-safe. Consumers MUST NOT parse, build or store them beyond the traversal. Encoding alone is not opacity. | I | AIP |
| API-COL-006 | A cursor is bound to the filters and sort of the request that produced it. Using it with different filters or sort MUST return 400 with a code. An expired cursor MUST return a defined error. Cursors MUST stay valid for at least 24 hours (**default**). | T | AIP |
| API-COL-007 | A cursor never confers access. Authorisation MUST be checked on every request. | T | AIP |
| API-COL-008 | A consumer MAY change `limit` between pages. | T | AIP |
| API-COL-009 | Offset pagination (`offset`, `limit`) is allowed only for small, stable sets or when page jumping is required. It MUST be documented per operation, together with the consequence that changing data can repeat or skip items. | I | PP |
| API-COL-010 | Every paginated collection MUST have a deterministic default order with a unique tie-breaker, for example `createdAt` then `id`. | T | PP |
| API-COL-011 | `sort` MUST take a comma-separated list of property names, with a leading `-` for descending, limited to a documented allowlist. An unknown property MUST return 400. | T | ZAL |
| API-COL-012 | Filters MUST be named query parameters per filterable property. Range filters MUST use paired parameters with documented inclusive or exclusive bounds. An unknown filter or an invalid value MUST return 400. | T | PP |
| API-COL-013 | Free-text search (`q`) MUST document which properties it covers and how results are ranked. | I | PP |
| API-COL-014 | A total count SHOULD NOT be returned by default. If it is offered (**default** opt-in through `includeTotal=true`), it MUST be documented as exact or estimated. | I | ZAL |
| API-COL-015 | Where partial responses (`fields`) or embedding (`embed`) are supported, the permitted names MUST be an allowlist, an unknown name MUST return 400, and an embedded item MUST have the same representation as the standalone resource. | T | ZAL |
| API-COL-016 | List items MAY be summary representations. The summary and the detail schemas MUST be separate (API-REP-022). | I | PP |
| API-COL-017 | The consistency of traversal (live data or snapshot) MUST be documented. The default is live data. Consumers MUST de-duplicate appended pages by `id`. | I | PP |
| API-COL-018 | The default order, default limit and maximum limit are part of the contract. Changing them is a breaking change. | I | PP |
| API-COL-019 | Sub-resource collections MUST be paginated by the same rules as top-level collections. | T | PP |
| API-COL-020 | The collation used for sorting text MUST be documented. | I | PP |

## 10. Writes, idempotency and concurrency

| ID | Statement | Ver. | Basis |
|---|---|---|---|
| API-WRT-001 | `PUT` and `DELETE` MUST be idempotent. The response to a repeated `DELETE` (**default** 404 after the first 204) MUST be documented. Consumers MUST treat a 404 on a retried `DELETE` as success. | T | HTTP |
| API-WRT-002 | Operations that create resources or trigger effects SHOULD be made idempotent, in this order of preference: a client-chosen identifier with `PUT`, then a documented business key with 409 on duplicates, then an idempotency key. | I | ZAL |
| API-WRT-003 | The contract MUST state for every write operation whether it is idempotent by nature, idempotent through an idempotency key, or not idempotent and MUST NOT be retried automatically. | I | PP |
| API-WRT-004 | Where an operation supports an idempotency key, it MUST be the `Idempotency-Key` request header, declared in the contract as optional or required. The key is a unique string of at most 255 characters (UUID **default**), scoped to the authenticated principal and the operation, and retained for 24 hours (**default**, project parameter P07). | T | PP |
| API-WRT-005 | A repeated request with the same key and the same request MUST return the original status and body. The same key with a different request MUST return 422 with a code. The same key while the first request is still in progress MUST return 409 with a code. A missing required key MUST return 400 with a code. | T | PP |
| API-WRT-006 | The `Idempotency-Key` header is an IETF draft and not a standard. It MUST be used only where the contract declares it. | I | PP |
| API-WRT-007 | Resources that can be updated concurrently MUST expose a strong `ETag` on reads and on write responses, and updates MUST support `If-Match`. A mismatch MUST return 412 with a code. Where lost updates are unacceptable the contract MUST require `If-Match`, and a request without it MUST return 428. | T | HTTP |
| API-WRT-008 | Weak ETags MUST NOT be used for `If-Match`. ETag values are opaque. | T | HTTP |
| API-WRT-009 | Where ETags are not used, a `version` property in the body MUST provide the same protection and a mismatch MUST return 409 with a code. One mechanism MUST be chosen per API (project parameter P08). | I | PP |
| API-WRT-010 | An operation that can exceed the provider timeout MUST return 202 with the `Location` of a status resource. The status resource MUST expose a state (`PENDING`, `RUNNING`, `SUCCEEDED`, `FAILED`, `CANCELLED`), `Retry-After` while running, and on completion a link to the result or a problem. It MUST remain readable for a documented period. | T | PP |
| API-WRT-011 | A create operation MUST return 201 with `Location` set to the canonical URL and the created representation. A duplicate business key MUST return 409 with a code. | T | HTTP |
| API-WRT-012 | An update operation MUST return 200 with the updated representation (**default**) or 204 as documented. | T | PP |
| API-WRT-013 | A delete operation MUST return 204. A deleted resource MUST behave as removed (404 or 410). | T | HTTP |
| API-WRT-014 | The provider MUST validate every request against the contract constraints before any side effect. A write MUST either apply completely or not at all, except documented bulk operations with per-item results. | T | PP |
| API-WRT-015 | Server-side validation is authoritative. Constraints in the contract exist so consumers can offer usability validation and never replace the provider's checks. | I | PP |
| API-WRT-016 | Of several concurrent creations with the same business key, exactly one MUST succeed. | T | PP |
| API-WRT-017 | Side effects (messages, jobs, notifications) MUST be described in the operation. Their timing MUST NOT be part of the response time contract. | I | PP |
| API-WRT-018 | After a successful write, a read by the same principal MUST reflect it. Eventual consistency MUST be documented with the expected delay and signalled with 202. | T | PP |
| API-WRT-019 | `DELETE` on a resource that uses entity tags SHOULD require `If-Match`. | T | HTTP |
| API-WRT-020 | Server-assigned values (identifiers, timestamps, versions, state) MUST be ignored or rejected when sent in a request, as the contract states. | T | PP |
| API-WRT-021 | Defaults applied by the provider MUST be documented and echoed in the response. | T | PP |
| API-WRT-022 | A command repeated after success MUST return the current state or 409, as the contract documents. | T | PP |
| API-WRT-023 | Method-override headers (`X-HTTP-Method-Override` and similar) MUST NOT be accepted. | T | HTTP |

## 11. Caching and conditional requests

| ID | Statement | Ver. | Basis |
|---|---|---|---|
| API-CCH-001 | Every `GET` response MUST state its cacheability with an explicit `Cache-Control`, so no heuristic caching applies. The default for authenticated data is `private, no-cache` with revalidation. | T | HTTP |
| API-CCH-002 | `public` caching MUST be used only for data that is public, stable and identical for every caller. Personal or sensitive data MUST use `no-store`. | T | HTTP |
| API-CCH-003 | `GET` resources SHOULD provide an `ETag` derived from the representation. The provider MUST honour `If-None-Match` with 304 and no body. | T | HTTP |
| API-CCH-004 | `Vary` MUST name every request header that changes the representation (`Accept`, `Accept-Language`, and others where relevant). | T | HTTP |
| API-CCH-005 | A response that differs by principal MUST NOT be cacheable by shared caches. | T | HTTP |
| API-CCH-006 | Responses to `POST` and `PATCH` MUST NOT be cached unless the contract documents a cacheable `POST`, for example a search. | I | HTTP |
| API-CCH-007 | Cacheable responses MUST NOT contain sensitive data. | T | PP |
| API-CCH-008 | The contract MUST NOT rely on non-standard cache headers. | I | HTTP |
| API-CCH-009 | Responses larger than 1 KiB SHOULD be compressible (gzip or br) with `Vary: Accept-Encoding`. An unsupported encoding MUST yield an uncompressed response. | T | HTTP |

## 12. Evolution, versioning and deprecation

### 12.1 Classification of changes

| Change | Class |
|---|---|
| Add an operation | Compatible |
| Add an optional request property or parameter | Compatible |
| Add a response property | Compatible |
| Add a value to an open response enumeration | Compatible |
| Add a new error code to an existing status | Compatible |
| Relax a constraint (longer maximum length, wider range) | Compatible |
| Raise the maximum page size | Compatible |
| Add an optional header | Compatible |
| Add `Deprecation` or `Sunset` headers | Compatible |
| Remove or rename an operation, path, parameter or property | Breaking |
| Change a type, format, unit or meaning | Breaking |
| Make an optional property or parameter required | Breaking |
| Add a required request property or parameter | Breaking |
| Tighten a constraint (shorter maximum length, narrower pattern, smaller range) | Breaking |
| Remove a value from an enumeration used in requests | Breaking |
| Add a value to a closed enumeration used in responses | Breaking |
| Change the status code of an existing outcome | Breaking |
| Remove an error code or change its meaning | Breaking |
| Change default order, default limit or maximum limit | Breaking |
| Add pagination to an unpaginated collection | Breaking |
| Change the identifier format | Breaking |
| Make authentication or permission requirements stricter | Breaking |
| Change idempotency or concurrency semantics | Breaking |

| ID | Statement | Ver. | Basis |
|---|---|---|---|
| API-EVO-001 | Within a major version the provider MUST NOT make a breaking change (section 12.1). | T | ZAL |
| API-EVO-002 | Consumers MUST apply the tolerant-reader rule: ignore unknown response properties, tolerate unknown values of open enumerations, not depend on property order, and not rely on undocumented behaviour. | I | ZAL |
| API-EVO-003 | The major version MUST be part of the base path (**default** `/api/v{major}`, project parameter P03). Minor and patch versions MUST NOT appear in URLs. Media-type versioning is allowed when the parameter says so. | I | PP |
| API-EVO-004 | A compatible extension SHOULD be preferred to a new major version. A new major version MUST be created only for breaking changes and MUST run in parallel with the previous one for the deprecation period. | I | ZAL |
| API-EVO-005 | A request for an unsupported major version MUST return 404 or 410 with a problem and a code. | T | PP |
| API-EVO-006 | Preview operations MUST be tagged `preview`, carry no compatibility guarantee, and MUST NOT be used by production features without approval. | I | PP |
| API-EVO-007 | Deprecation MUST follow these steps: mark `deprecated: true` in the contract with the replacement and date; send `Deprecation` and `Sunset` on responses of the operation and a `Link` with `rel="deprecation"` to the documentation; add a changelog entry; notify consumers; remove only after the sunset date and after consumers confirm. | T | DEP |
| API-EVO-008 | The `Deprecation` value MUST be a structured date (`@` followed by a Unix timestamp), and `Sunset` MUST be an HTTP date not earlier than the deprecation date. Deprecation is a hint and MUST NOT change the behaviour of the operation. | T | DEP |
| API-EVO-009 | The minimum deprecation period for consumers outside the repository is 90 days (**default**, project parameter P10). | I | PP |
| API-EVO-010 | Consumers MUST NOT start using a deprecated operation, parameter or property. | I | ZAL |
| API-EVO-011 | After the sunset date a removed operation MUST return 410 with a code for a documented period and then 404. | T | PP |
| API-EVO-012 | Every released contract MUST be retained and comparable. Automated breaking-change detection against the last released contract is a quality gate (API-QLT-003). | T | PP |
| API-EVO-013 | A changelog MUST list for each release what was added, changed, deprecated and removed, with breaking changes marked. | I | PP |
| API-EVO-014 | A breaking change MAY ship without a new major version when provider and all consumers live in one repository and release together (coordinated change), every consumer changes in the same release, no external consumer exists, and the decision is recorded. Otherwise a new major version is required. | I | PP |

## 13. Documentation and the interactive reference

| ID | Statement | Ver. | Basis |
|---|---|---|---|
| API-DOC-001 | An interactive API reference rendered from the contract (**default** Swagger UI) MUST be provided for every released contract version. It MUST be generated from the contract at build or run time and MUST NOT be written by hand. | D | PP |
| API-DOC-002 | The contract file and the reference MUST be served from stable, documented paths (project parameter P05), and the reference MUST offer the contract for download in YAML or JSON. | T | PP |
| API-DOC-003 | The reference and the contract it shows MUST come from the same build artefact as the deployed API, so documentation cannot be stale. The reference MUST show `info.version`. | T | PP |
| API-DOC-004 | Every operation MUST have a summary and a description covering purpose, side effects, idempotency statement, required permission and consistency. Every parameter MUST have a description and an example. | I | OAS |
| API-DOC-005 | Every operation with a body MUST have at least one request example. Every operation MUST have at least one success response example and one example per documented error response, each a complete problem with its code. | I | OAS |
| API-DOC-006 | Examples MUST use the named `examples` map with a `summary` per example and MUST cover the typical case, the minimal case, and edge cases (longest values, Unicode, empty and maximum collections). | I | OAS |
| API-DOC-007 | Every example MUST validate against its schema. The check MUST run automatically, and an invalid example MUST fail the quality gate. | T | OAS |
| API-DOC-008 | Examples MUST use fictional fixture data only: no real personal data, credentials or production identifiers. Identifiers and dates MUST be fixed and obviously synthetic. | I | PP |
| API-DOC-009 | Fixtures MUST be coherent across operations, so the same entities reappear and a flow (create, read, update, delete) can be followed from example to example. | I | PP |
| API-DOC-010 | Where practical, examples MUST be replayed against the provider in tests, so documented behaviour is actual behaviour. | T | PP |
| API-DOC-011 | Schemas MUST have descriptions. Each enumeration value MUST be explained. | I | OAS |
| API-DOC-012 | "Try it out" MUST be enabled by default only in non-production environments and against non-production servers. Authorisation persistence MUST be off by default. The reference MUST NOT call an external validator service. | D | PP |
| API-DOC-013 | Whether the reference is reachable in production is project parameter P13 (**default** not reachable by the public). The decision MUST be recorded. Hardening of that exposure is deferred (section 15). | I | PP |
| API-DOC-014 | The reference MUST explain authentication: which schemes exist and how to obtain a test credential in non-production environments. | I | PP |
| API-DOC-015 | Operations MUST be grouped by tag in a logical order with tag descriptions. Deprecated operations MUST be visibly marked. | D | OAS |
| API-DOC-016 | A conventions guide (in `info.description` or linked through `externalDocs`) MUST cover base URL, versioning, authentication overview, content types, naming, dates and money, pagination, filtering and sorting, errors and the error catalogue, idempotency, concurrency, caching, rate limits where defined, the deprecation policy and the changelog. | I | PP |
| API-DOC-017 | The error catalogue (API-ERR-014) MUST be visible in the reference as a table generated from the contract. | D | PP |
| API-DOC-018 | Multi-call flows (upload then attach, asynchronous polling, workflow transitions) MUST be described step by step in the guide with example calls. | I | PP |
| API-DOC-019 | A mock server for local consumer development MAY be provided from the contract examples, documented with its start command, and MUST NOT run outside development. | I | PP |
| API-DOC-020 | Code samples generated by the renderer MAY be shown. Handwritten samples MUST be tested. | T | PP |

## 14. Quality gates

| ID | Statement | Ver. | Basis |
|---|---|---|---|
| API-QLT-001 | The contract MUST validate against the OpenAPI schema of its version (API-SPC-025). A validation failure MUST block the merge. | T | OAS |
| API-QLT-002 | The contract MUST pass a linter ruleset (**default** Spectral with the OpenAPI ruleset, extended with project rules for operation identifiers, tags, descriptions, examples, error responses, pagination parameters and casing). Lint errors MUST block the merge. Warnings MUST be tracked. | T | PP |
| API-QLT-003 | A comparison against the last released contract MUST detect breaking changes (**default** oasdiff). A breaking change without a new major version or a recorded coordinated change (API-EVO-014) MUST fail the gate. | T | PP |
| API-QLT-004 | A provider contract test MUST verify, for every operation: the operation exists with its documented method and path; each documented status is produced by at least one test; real responses, including error responses, validate against the documented schemas; no route exists that the contract does not describe; and no undocumented status code is returned. | T | PP |
| API-QLT-005 | Every example MUST validate against its schema (API-DOC-007), and request examples MUST be accepted by the provider. | T | OAS |
| API-QLT-006 | Property-based testing from the contract (**default** Schemathesis) SHOULD run against a test environment to find server errors on edge inputs, responses that violate the schema, and invalid data that is accepted. | T | PP |
| API-QLT-007 | Consumer-driven contract tests (**default** Pact) MAY be added when several independent consumers exist. They complement the provider-side checks and do not replace them. | T | PP |
| API-QLT-008 | Behaviour tests MUST cover the business scenarios of each operation, and every error code in the catalogue MUST be covered by at least one test. | T | PP |
| API-QLT-009 | The interactive reference MUST build from the contract without errors, and its links MUST resolve. | T | PP |
| API-QLT-010 | A change that alters observable behaviour without a contract change MUST fail the contract test. A contract change and its implementation MUST be in the same change set. | T | PP |
| API-QLT-011 | The gates run in the pipeline once one exists. Until then they MUST be runnable locally with documented commands, and their results MUST be recorded. | I | PP |
| API-QLT-012 | YAML lint and format, bundling and the generation smoke test (API-GPL-009) MUST be gates. | T | PP |

## 15. Operational aspects and deferred items

| ID | Statement | Ver. | Basis |
|---|---|---|---|
| API-OPS-001 | The design of authentication and authorisation is out of scope here and deferred. This contract requires only: security schemes and per-operation `security` declared (API-SPC-016), the semantics of 401 and 403 (API-HTTP-008), and required permissions documented per operation (API-SPC-017). | I | PP |
| API-OPS-002 | The wire form of rate limiting is fixed: 429 with `Retry-After` (seconds or HTTP date) and a problem code (API-ERR-018). The policy values are deferred. The IETF `RateLimit` header fields are a draft, MUST NOT be required, and MAY be adopted when they are published as a standard or a parameter says so. | I | PP |
| API-OPS-003 | The provider MUST accept a W3C `traceparent` request header when present and ignore a malformed one. It MUST return a correlation identifier in a response header (project parameter P18, **default** `X-Request-Id`) and in problems (`traceId`). Consumers retain it for support. Collection and analysis of this data is deferred. | T | PP |
| API-OPS-004 | Operational endpoints (health, readiness) MUST be outside the versioned business contract, MUST be documented separately, and MUST NOT expose internals publicly. | I | PP |
| API-OPS-005 | Browser consumers SHOULD reach the API from the same origin through the routing layer, so cross-origin policy is not needed. A cross-origin policy is deferred unless development needs it, and then a local development proxy MUST be used. | I | PP |
| API-OPS-006 | Sensitive values MUST NOT appear in URLs, query strings, headers or error messages. They travel in request bodies (API-RES-013). | T | PP |
| API-OPS-007 | The following are deferred to the final project phase, and agents MUST NOT implement, configure, scaffold or modify them earlier: rate-limit values and enforcement, gateway and edge hardening, cross-origin policy design, security response headers, abuse detection, request signing, audit logging of API calls, usage analytics and telemetry, usage monitoring of deprecated operations, and service-level monitoring. | I | PP |
| API-OPS-008 | Until usage monitoring exists, deprecation (API-EVO-007) MUST be coordinated directly with the known consumers. | I | PP |

## 16. Edge cases and error conditions

Each row is a condition that MUST be handled. The required behaviour is binding; values marked *default* may be overridden by a project parameter (API-PRE-004).

### 16.1 Contract and documents

| ID | Condition | Required behaviour | Ver. | Basis |
|---|---|---|---|---|
| API-EDG-001 | The contract declares a constraint the provider does not enforce, or the reverse | The mismatch is a defect. The contract test fails until contract and provider agree. | T | PP |
| API-EDG-002 | The provider returns a status code the contract does not list | A defect. The contract test fails. | T | PP |
| API-EDG-003 | The provider has a route the contract does not describe | The route is removed or documented. The contract test detects it. | T | PP |
| API-EDG-004 | Two operations share an `operationId` | The contract is invalid. Lint fails. | T | OAS |
| API-EDG-005 | An example no longer validates after a schema change | The gate fails until the example is corrected. | T | OAS |
| API-EDG-006 | A generator needs a flag or vendor extension to work | The flag moves to generator configuration outside the contract. | I | PP |
| API-EDG-007 | A reference points to a mutable remote location | It is replaced by a local or immutable versioned reference. | I | ZAL |
| API-EDG-008 | The reference shows an older contract than the deployed API | A defect. Reference and API come from the same build artefact. | T | PP |

### 16.2 Requests

| ID | Condition | Required behaviour | Ver. | Basis |
|---|---|---|---|---|
| API-EDG-009 | The body is not valid JSON | 400 with a problem and code. Nothing is processed. | T | HTTP |
| API-EDG-010 | The body repeats a member name | 400 where the parser can detect it. The behaviour is documented otherwise. | T | IJSON |
| API-EDG-011 | A write request contains an unknown field | 400 with the field's JSON Pointer (**default**). | T | PP |
| API-EDG-012 | The body exceeds the size limit | 413. | T | HTTP |
| API-EDG-013 | The `Content-Type` is not supported | 415. | T | HTTP |
| API-EDG-014 | `Accept` cannot be satisfied | 406. | T | HTTP |
| API-EDG-015 | The requested locale is unsupported | A defined error with a code. No silent fallback. | T | PP |
| API-EDG-016 | A query parameter is unknown | 400 naming the parameter (**default**). | T | PP |
| API-EDG-017 | A single-valued query parameter is repeated | 400 naming the parameter. | T | PP |
| API-EDG-018 | A parameter value is out of range or malformed | 400 with `parameter` and a code. | T | PP |
| API-EDG-019 | A string has leading or trailing whitespace | It is not changed silently. Any normalisation is documented in the contract. | T | PP |
| API-EDG-020 | A string contains combining characters, emoji, right-to-left text or zero-width characters | Stored and returned unchanged. `maxLength` counts characters as JSON Schema does, and the provider's storage limit agrees with it. | T | PP |

### 16.3 Responses and errors

| ID | Condition | Required behaviour | Ver. | Basis |
|---|---|---|---|---|
| API-EDG-021 | A gateway, proxy or load balancer returns an error that is not a problem body | The consumer treats it as unavailable. Provider-owned layers SHOULD return `application/problem+json` where they can. | T | PD |
| API-EDG-022 | An unexpected exception occurs in the provider | 500 with a generic problem, `INTERNAL_001` (**default**) and `traceId`. No internals. | T | PD |
| API-EDG-023 | Several fields fail validation | All violations are returned together, each with a pointer. | T | PP |
| API-EDG-024 | An upstream service fails | 502, 503 or 504 with a generic problem. The upstream body is never forwarded. | T | PP |
| API-EDG-025 | The provider is overloaded | 503 with `Retry-After`. | T | HTTP |
| API-EDG-026 | A consumer receives an unknown error code | It falls back to the category of the HTTP status. | T | PD |
| API-EDG-027 | A resource exists but its existence must not be revealed | 404, identical to the response for a resource that does not exist. | T | PP |
| API-EDG-028 | Authentication has expired | 401 with `WWW-Authenticate` and a code that differs from the code for 403. | T | HTTP |
| API-EDG-029 | A rule is violated on a valid request, or the state conflicts | 422 for the rule and 409 for the state. They are not mixed. | T | PP |
| API-EDG-030 | A money amount has more decimals than its currency allows | Rejected with a code. Never rounded silently. | T | PP |
| API-EDG-031 | An identifier or number exceeds 2^53 − 1 | It is a string and is neither rounded nor altered. | T | IJSON |
| API-EDG-032 | A calendar date is exchanged | It is a `date` without time or zone and shows the same day in every time zone. | T | ZAL |
| API-EDG-033 | A request instant carries a non-UTC offset | It is rejected with 400, or normalised to UTC as documented. Responses always use UTC. | T | PP |
| API-EDG-034 | A local date-time falls in a daylight-saving gap or overlap | The contract documents the resolution rule. The time-zone identifier is required. | T | PP |

### 16.4 Collections

| ID | Condition | Required behaviour | Ver. | Basis |
|---|---|---|---|---|
| API-EDG-035 | A cursor is used with different filters or sort | 400 with a code. | T | AIP |
| API-EDG-036 | A cursor has expired | A defined error. The consumer restarts the traversal. | T | AIP |
| API-EDG-037 | Data changes between page requests | Items may repeat or be skipped as documented. Consumers de-duplicate by `id`. | T | PP |
| API-EDG-038 | `limit` is 0, negative or not an integer | 400. | T | AIP |
| API-EDG-039 | `limit` exceeds the maximum | Reduced to the maximum (**default**) or 400, as documented. | T | AIP |
| API-EDG-040 | The number of items is an exact multiple of `limit` | A page with `nextCursor` MAY be followed by an empty page. The end is signalled only by the absence of `nextCursor`. | T | AIP |
| API-EDG-041 | `sort` names a property outside the allowlist | 400. | T | ZAL |
| API-EDG-042 | Items have equal sort values | The tie-breaker keeps the order stable across pages. | T | PP |
| API-EDG-043 | A collection is empty | 200 with `items: []`. | T | PP |
| API-EDG-044 | A filter matches nothing | 200 with an empty collection, never 404. | T | PP |
| API-EDG-045 | A search term contains special characters | It is treated literally unless the contract documents operators. No injection into the query engine. | T | PP |
| API-EDG-046 | An offset is beyond the allowed maximum | 400 or an empty result, as documented. | T | PP |

### 16.5 Writes and concurrency

| ID | Condition | Required behaviour | Ver. | Basis |
|---|---|---|---|---|
| API-EDG-047 | A non-idempotent `POST` is repeated after a lost response | It is not retried automatically. A documented business key returns 409 for the duplicate. | T | HTTP |
| API-EDG-048 | The same idempotency key arrives with a different request | 422 with a code. | T | PP |
| API-EDG-049 | The same idempotency key arrives while the first request is in progress | 409 with a code. | T | PP |
| API-EDG-050 | An idempotency key arrives after its retention period | It is treated as a new request. The contract documents the period. | T | PP |
| API-EDG-051 | The response was lost and the consumer resends with the same key | The original status and body are returned. | T | PP |
| API-EDG-052 | `If-Match` is stale | 412 with a code. The write is not applied. | T | HTTP |
| API-EDG-053 | `If-Match` is missing where required | 428. | T | HTTP |
| API-EDG-054 | An update targets a deleted resource | 404 or 410. | T | PP |
| API-EDG-055 | A merge patch sets `null` on a property that is not nullable | 400 with a pointer. | T | MRG |
| API-EDG-056 | A merge patch changes an array | The whole array is replaced. The contract documents this. | T | MRG |
| API-EDG-057 | A `PUT` omits an optional property | The property is reset or removed as documented. | T | HTTP |
| API-EDG-058 | A request sets a read-only property | Rejected with 400 (**default**) or ignored, as documented. | T | OAS |
| API-EDG-059 | Two creations with the same business key run at once | One succeeds and the other returns 409. | T | PP |
| API-EDG-060 | A state transition is not allowed | 409 with a code and, where documented, the allowed transitions. | T | PP |
| API-EDG-061 | An asynchronous operation fails | The status resource shows `FAILED` with a problem. No partial result is visible. | T | PP |
| API-EDG-062 | An asynchronous status is read after its retention period | 404 or 410 as documented. | T | PP |
| API-EDG-063 | A completed asynchronous operation is cancelled | 409 with a code. | T | PP |
| API-EDG-064 | A bulk operation partly fails | Per-item results. The overall status is documented. | T | PP |
| API-EDG-065 | An upload is too large or has an unsupported type | 413 or 415. | T | HTTP |
| API-EDG-066 | An upload is interrupted | No partial resource becomes visible. A retry is safe. | T | PP |
| API-EDG-067 | A resource is deleted twice | The documented response (**default** 404). Consumers treat it as success. | T | HTTP |
| API-EDG-068 | A resource with dependants is deleted | 409 with a code, unless the contract documents a cascade. | T | PP |

### 16.6 Evolution

| ID | Condition | Required behaviour | Ver. | Basis |
|---|---|---|---|---|
| API-EDG-069 | An open response enumeration gains a value | Consumers handle the unknown value safely. | T | ZAL |
| API-EDG-070 | A new request field would be required | It is optional with a documented default, or a new major version is created. | T | ZAL |
| API-EDG-071 | An optional property becomes required | A breaking change. New major version or recorded coordinated change. | T | PP |
| API-EDG-072 | A deprecated operation is called after its sunset date | 410 with a code for the documented period, then 404. | T | PP |
| API-EDG-073 | A sunset date is earlier than the deprecation date | Invalid. The gate fails. | T | DEP |
| API-EDG-074 | A consumer still uses a deprecated operation near its sunset | The consumer is notified and migrates before the date. | I | PP |
| API-EDG-075 | Two major versions run in parallel | Each has its own contract, reference and changelog. | I | PP |
| API-EDG-076 | The contract changes without a changelog entry | The review gate fails. | I | PP |

### 16.7 Documentation and the interactive reference

| ID | Condition | Required behaviour | Ver. | Basis |
|---|---|---|---|---|
| API-EDG-077 | "Try it out" is enabled against production | Not allowed by default. Only by a recorded decision. | I | PP |
| API-EDG-078 | An example contains real or personal data | Rejected in review. | I | PP |
| API-EDG-079 | An error response has no example | The quality gate fails. | T | PP |
| API-EDG-080 | The reference service is unavailable | The API is unaffected. The contract file remains available from the artefact. | D | PP |
| API-EDG-081 | The contract is very large | Sources are split and bundled. The reference still loads. | D | PP |
| API-EDG-082 | An operation does not state its required permission | The review gate fails. | I | PP |
| API-EDG-083 | An example differs from a real response | The example replay test fails. | T | PP |
| API-EDG-084 | The reference shows an internal hostname | Removed (API-SPC-006). | I | PP |
| API-EDG-085 | Authorisation persists in a shared browser | Persistence is off by default. | D | PP |
| API-EDG-086 | A mock returns an outdated shape | The mock is regenerated or validated against the contract. | T | PP |

### 16.8 Operations and environment

| ID | Condition | Required behaviour | Ver. | Basis |
|---|---|---|---|---|
| API-EDG-087 | A design puts sensitive data in a query string | Rejected in review. The data moves to a request body. | I | PP |
| API-EDG-088 | Client and server clocks differ | Server instants are authoritative. Consumers do not decide expiry or deadlines from their own clock. | T | PP |
| API-EDG-089 | The `traceparent` header is absent | The provider generates a correlation identifier. | T | PP |
| API-EDG-090 | The `traceparent` header is malformed | It is ignored, a new identifier is generated, and no error is returned. | T | PP |
| API-EDG-091 | A `HEAD` request is received | The same headers as `GET`, without a body. | T | HTTP |
| API-EDG-092 | A browser sends a cross-origin preflight | Handled by the routing layer. The policy design is deferred; the default is same-origin access. | I | PP |
### 16.9 Authoring and generation

| ID | Condition | Required behaviour | Ver. | Basis |
|---|---|---|---|---|
| API-EDG-093 | The YAML source contains an unquoted `NO`, `1.10`, `007` or a date-like value | Quoted, or typed as the schema requires. Lint fails otherwise. | T | PP |
| API-EDG-094 | A source file contains a tab for indentation | Lint fails. | T | PP |
| API-EDG-095 | A mapping contains a duplicate key | Lint fails. The bundle is not produced. | T | PP |
| API-EDG-096 | A source file uses an anchor, alias or merge key | Rejected. `$ref` or `allOf` is used instead. | T | PP |
| API-EDG-097 | A `$ref` has sibling keywords | Rejected in review. A wrapper schema with `allOf` is used. | I | PP |
| API-EDG-098 | `nullable: true` appears in a 3.1 contract | Rejected. A type array is used. | T | PP |
| API-EDG-099 | A schema is defined inline and the generator invents a name | Rejected. The schema moves to `components/schemas` with a stable name. | T | PP |
| API-EDG-100 | Two schema names differ only by case | Rejected. Names are case-insensitively unique. | T | PP |
| API-EDG-101 | A property is named with a reserved word of a target language | Renamed in the contract. A generator-side rename is documented only for target-language concerns. | I | PP |
| API-EDG-102 | An enum value is not identifier-safe | Rejected. Values are UPPER_SNAKE_CASE. | T | PP |
| API-EDG-103 | A `oneOf` has no discriminator | Rejected. | T | PP |
| API-EDG-104 | A discriminator mapping names a missing schema | The bundle fails validation. | T | PP |
| API-EDG-105 | Schemas reference each other circularly outside a tree | Redesigned, or the cycle is documented as a tree. | I | PP |
| API-EDG-106 | A `default` does not match the provider's behaviour | Fixed. The provider test fails. | T | PP |
| API-EDG-107 | `required` differs from what the provider returns | Contract test fails. The contract is fixed, not the test. | T | PP |
| API-EDG-108 | The generator prints a warning | The warning is fixed or recorded as a documented exception. It is not suppressed. | I | PP |
| API-EDG-109 | A generator upgrade changes the output | The diff is reviewed and recorded before merge (API-GPL-013). | T | PP |
| API-EDG-110 | Two runs produce different output | Deterministic ordering is enabled. Otherwise the pipeline fails. | T | PP |
| API-EDG-111 | Generated code does not compile in one target language | The contract or documented adjustments are fixed. The change is not merged. | T | PP |
| API-EDG-112 | A hand-written file collides with a generated file | Hand-written sources live in a separate location (API-GPL-008). The collision is a defect. | I | PP |
| API-EDG-113 | A custom `format` is unknown to the generator | Replaced by a limited format or a documented mapping. | I | PP |
| API-EDG-114 | The bundle is older than its sources | The pipeline rebuilds it. A stale bundle is never published. | T | PP |
| API-EDG-115 | An overlay target matches nothing | The pipeline verifies the overlay's effect and fails. | T | PP |
| API-EDG-116 | An overlay leaves an internal operation exposed | Fails the audience check. The overlay is corrected. | T | PP |

### 16.10 Data, exports and requests

| ID | Condition | Required behaviour | Ver. | Basis |
|---|---|---|---|---|
| API-EDG-117 | A caller may not see a field | The field is omitted, not null. | T | PP |
| API-EDG-118 | Two values differ only in Unicode normalisation | Treated as equal for uniqueness. | T | PP |
| API-EDG-119 | A CSV cell starts with a formula character | Neutralised on export. | T | PP |
| API-EDG-120 | A presigned download URL has expired | 403 or 410 with a problem body. A new URL is requested from the operation. | T | PP |
| API-EDG-121 | A `Range` request is sent to a download that does not support it | The full body is returned with 200. | T | PP |
| API-EDG-122 | `Accept-Encoding` lists only unsupported encodings | An uncompressed response. | T | PP |
| API-EDG-123 | A command is repeated after success | Current state or 409, as documented. | T | PP |
| API-EDG-124 | An aggregation operation partly fails | Documented behaviour: partial result with a marker, or a problem. Never a silent gap. | T | PP |
| API-EDG-125 | A request body sets a server-assigned field | Ignored or rejected as documented. | T | PP |
| API-EDG-126 | A problem would carry more than the error cap | Truncated at the cap and marked as truncated. | T | PP |
| API-EDG-127 | Text sorting meets mixed case or accents | The documented collation decides. | T | PP |
| API-EDG-128 | An operation has no traceable requirement | Rejected in review. | I | PP |
| API-EDG-129 | `DELETE` is sent with a stale `If-Match` | 412 with a problem body. | T | PP |

## 17. Restrictions

### 17.1 Prohibited patterns

| ID | Prohibited | Basis |
|---|---|---|
| API-RST-001 | Behaviour that the contract does not describe. | ZAL |
| API-RST-002 | Verbs in resource paths, except documented commands (API-RES-006). | ZAL |
| API-RST-003 | Errors carried in 2xx responses. | HTTP |
| API-RST-004 | Error bodies that are not Problem objects, and ad hoc error shapes. | PD |
| API-RST-005 | Stack traces, SQL, schema names, paths or secrets in any response. | PP |
| API-RST-006 | Persistence entities, internal identifiers, audit fields or deletion markers in responses. | PP |
| API-RST-007 | Mass assignment, and silently accepting unknown or read-only fields in writes. | PP |
| API-RST-008 | Unbounded collections without pagination. | AIP |
| API-RST-009 | Offset pagination for large or frequently changing collections. | PP |
| API-RST-010 | Parsing or constructing cursors or identifiers. | AIP |
| API-RST-011 | `GET` with side effects, and `GET` with a body. | HTTP |
| API-RST-012 | Sensitive data in URLs, query strings or headers. | PP |
| API-RST-013 | Floating point for money, numeric timestamps, and converting calendar dates through time zones. | IJSON |
| API-RST-014 | Integers beyond 2^53 − 1 as JSON numbers. | IJSON |
| API-RST-015 | `null` booleans and `null` arrays. | ZAL |
| API-RST-016 | Reusing or redefining an error code. | PP |
| API-RST-017 | Localised `title`, and user-interface strings carried in status or error information. | PP |
| API-RST-018 | A breaking change within a major version. | ZAL |
| API-RST-019 | Versioning through query parameters, or minor and patch versions in URLs. | PP |
| API-RST-020 | Removal without deprecation and notice, except a recorded coordinated change. | DEP |
| API-RST-021 | A handwritten reference page. | PP |
| API-RST-022 | Generator flags or vendor extensions that are needed to understand the contract. | PP |
| API-RST-023 | Hand-editing generated artefacts, and domain logic in generated code. | PP |
| API-RST-024 | Code-first contract derivation without project parameter P12. | PP |
| API-RST-025 | Real data in examples. | PP |
| API-RST-026 | "Try it out" enabled against production by default. | PP |
| API-RST-027 | The acting principal in a request body. | PP |
| API-RST-028 | Authorisation that depends on the secrecy of an identifier. | PP |
| API-RST-029 | Automatic retry of a non-idempotent write. | HTTP |
| API-RST-030 | Weak ETags with `If-Match`. | HTTP |
| API-RST-031 | Silent truncation, rounding or other alteration of user data without documentation. | PP |
| API-RST-032 | Unbounded page sizes, payload sizes, nesting depth or bulk sizes. | PP |
| API-RST-033 | Implementing deferred items (API-OPS-007) before the final phase. | PP |
| API-RST-034 | Suppressing lint, breaking-change or contract-test failures to merge. | PP |
| API-RST-035 | Inventing operations, fields, error codes or policy (API-CON-009). | PP |
| API-RST-036 | Mixed casing conventions within one API. | PP |
| API-RST-037 | Redirects in answer to API calls, except documented 307 and 308. | ZAL |
| API-RST-038 | Secrets or internal hostnames in the contract. | PP |
| API-RST-039 | YAML anchors, aliases, merge keys, tabs or custom tags in contract sources. | PP |
| API-RST-040 | Inline object or enum schemas. | PP |
| API-RST-041 | `$ref` with sibling keywords, or `nullable` in a 3.1 contract. | PP |
| API-RST-042 | Generator flags that skip or relax validation, or suppressed generator warnings. | PP |
| API-RST-043 | Unpinned generator or bundler versions. | PP |
| API-RST-044 | Non-identifier enum values and reserved-word property names. | PP |
| API-RST-045 | `oneOf` without a discriminator, or `anyOf` for generated types. | PP |
| API-RST-046 | Free-form objects without a documented reason. | PP |
| API-RST-047 | Copy-edited audience variants instead of overlays. | PP |
| API-RST-048 | Table-shaped resources and speculative operations. | PP |
| API-RST-049 | Masking invisible fields with `null`. | PP |
| API-RST-050 | CSV exports without formula neutralisation. | PP |
| API-RST-051 | Method-override headers. | PP |
| API-RST-052 | Hand-edited bundles. | PP |
| API-RST-053 | Undocumented custom generator templates. | PP |

### 17.2 Default limits (non-normative, overridable; project parameters P06, P16, P36–P38)

| Limit | Default |
|---|---|
| Default page size | 25 |
| Maximum page size | 100 |
| Maximum sort properties | 3 |
| Maximum values in one array-valued filter | 50 |
| Maximum URL length | 2,048 characters |
| Maximum JSON request body | 1 MiB |
| Maximum JSON nesting depth | 32 |
| Maximum upload size per file | 10 MiB |
| Maximum items in a bulk request | 100 |
| Provider timeout before 202 is required | 30 s |
| Cursor lifetime | at least 24 h |
| Idempotency key retention | 24 h |
| Maximum idempotency key length | 255 characters |
| Minimum deprecation period for outside consumers | 90 days |
| Asynchronous status retention | 24 h |
| Maximum problems in one `errors` array | 100 |
| Download URL lifetime | 5 min |
| Source file length | 500 lines |
| Maximum `allOf` members | 3 |
| Maximum schema name length | 60 characters |

## 18. Quality, verification and acceptance

### 18.1 Review gate

A change to the API MUST be rejected when any of these is true.

| ID | Rejection criterion | Ver. |
|---|---|---|
| API-REV-001 | The implementation changes behaviour without a contract change, or the contract changes without the implementation (API-QLT-010). | T |
| API-REV-002 | The contract fails validation, lint, the breaking-change check or the contract test (API-QLT-001 to API-QLT-004). | T |
| API-REV-003 | A breaking change has no new major version and no recorded coordinated change (API-EVO-014). | T |
| API-REV-004 | An operation lacks a description, an idempotency statement, a required permission, a success example or an example per documented error (API-DOC-004, API-DOC-005). | I |
| API-REV-005 | An error response is not a Problem, has no code, or exposes internals (API-ERR-001, API-ERR-011). | T |
| API-REV-006 | A collection is unpaginated, has no deterministic order, or an unbounded limit (API-COL-001, API-COL-010). | T |
| API-REV-007 | A write has no idempotency statement, or retries a non-idempotent operation (API-WRT-003). | I |
| API-REV-008 | A representation exposes persistence or internal details (API-REP-012). | I |
| API-REV-009 | Examples use real data, fail validation, or are incoherent across operations (API-DOC-007 to API-DOC-009). | I |
| API-REV-010 | A deferred item was implemented early (API-OPS-007). | I |
| API-REV-011 | An error code was reused, removed without deprecation, or is missing from the catalogue (API-ERR-005, API-ERR-014). | I |
| API-REV-012 | A deprecation lacks the contract marker, the headers, the changelog entry or the notice period (API-EVO-007, API-EVO-009). | I |
| API-REV-013 | A contract source breaks the authoring rules (API-YML) or the generation pipeline (API-GPL). | I |
| API-REV-014 | An operation has no traceable requirement (API-RES-016). | I |

### 18.2 Definition of done

An API change is done only when:

- The contract describes the change completely, including descriptions, constraints, examples and every error response.
- The contract validates, lints and passes the breaking-change check.
- YAML lint, bundling and the generation smoke test pass, and the authoring rules hold (API-YML, API-GPL).
- The provider implements exactly what the contract says, and the contract test passes.
- Every new error code is in the catalogue and covered by a test.
- Examples validate and are coherent with the fixture set.
- The interactive reference builds from the contract and shows the change.
- The changelog is updated, and deprecations follow API-EVO-007.
- Consumers affected by the change are identified and informed.
- No deferred item was implemented.

### 18.3 Agent workflow for an API change

1. Read the business requirement. If it does not answer a question, ask the user (API-CON-009).
2. Identify the resource, the operation, its audience and its permission.
3. Change the contract first, following the YAML authoring rules (4.1): path, method, `operationId`, parameters, schemas, constraints, responses, error responses, descriptions and examples.
4. Decide and write down the idempotency, concurrency, pagination, caching and asynchronous behaviour of the operation.
5. Add or reuse error codes and update the catalogue.
6. Lint the YAML, bundle, validate the contract, lint it, validate the examples, run the breaking-change check and the generation smoke test (4.2).
7. If the change is breaking, stop and decide between a compatible alternative, a new major version and a recorded coordinated change.
8. Implement the provider against the contract. Generated code is not edited.
9. Run the contract test and the behaviour tests, and cover every new error code.
10. Update the changelog, the conventions guide where behaviour is new, and the deprecation headers where relevant.
11. Identify affected consumers and describe the change for them.
12. Check the review gate (18.1) and the definition of done (18.2).

### 18.4 Acceptance criteria

```gherkin
Feature: Contract first                                         # API-CON-001, API-SPC-001, API-QLT-010
  Scenario: Behaviour without a contract change is rejected
    Given an operation returns a new property that the contract does not describe
    When the contract test runs
    Then it fails

Feature: Problem responses                                      # API-ERR-001, API-ERR-002, API-ERR-011
  Scenario: Unknown path
    When a consumer requests a path that does not exist
    Then the response is 404 with content type "application/problem+json"
    And the body has "type", "title", "status" and "code"
    And it contains no stack trace or internal name

Feature: Validation errors                                      # API-ERR-008, API-HTTP-006
  Scenario: Several invalid fields
    When a consumer posts a body with two invalid fields
    Then the response is 400
    And "errors" contains two entries, each with a pointer, a code and a detail

Feature: Pagination                                             # API-COL-001 to API-COL-006
  Scenario: Cursor traversal
    Given a collection of 60 items and a default limit of 25
    When a consumer follows "nextCursor" until it is absent
    Then it receives 25, 25 and 10 items in a stable order
    And a cursor used with different filters returns 400

Feature: Idempotent create                                      # API-WRT-004, API-WRT-005
  Scenario: Repeated request with the same key
    Given a create operation that supports "Idempotency-Key"
    When a consumer sends the same request twice with one key
    Then both responses have the same status and body
    And one resource exists
    When the key is reused with a different body
    Then the response is 422 with a code

Feature: Optimistic concurrency                                 # API-WRT-007
  Scenario: Stale update
    Given a resource whose ETag has changed since a consumer read it
    When the consumer updates it with the old ETag in "If-Match"
    Then the response is 412 and the resource is unchanged

Feature: Deprecation                                            # API-EVO-007, API-EVO-008
  Scenario: Deprecated operation
    Given an operation is deprecated with a sunset date
    When a consumer calls it
    Then the response carries "Deprecation", "Sunset" and a "Link" with rel "deprecation"
    And its behaviour is unchanged

Feature: Interactive reference                                  # API-DOC-001, API-DOC-005, API-DOC-007, API-DOC-012
  Scenario: Reference matches the deployed contract
    When a reader opens the reference of a deployed version
    Then it shows the same "info.version" as the deployed API
    And every operation has a request example, a success example and an error example
    And "Try it out" is disabled in production
```

**Feature: a generation-ready contract**

```gherkin
Scenario: A contract change passes the pipeline in order
  Given a change to the contract sources
  When the pipeline runs
  Then YAML lint, bundling, validation, lint, example validation, the breaking-change comparison and the generation smoke test pass in that order
  And generation twice from the same bundle gives identical output
  And no generator warning is suppressed
```

### 18.5 Verification methods

| Method | Meaning | Typical evidence |
|---|---|---|
| T | Test | Contract test, lint, diff, example validation, behaviour test, property-based test |
| I | Inspection | Review of the contract, the changelog and the decisions |
| A | Analysis | Impact analysis of a change on consumers |
| D | Demonstration | Opening the reference and walking through a flow |

## 19. References

- [OpenAPI Specification versions](https://spec.openapis.org/oas/)
- [Learn OpenAPI: documentation, descriptions and examples](https://learn.openapis.org/specification/docs.html)
- [RFC 9457: Problem Details for HTTP APIs](https://www.rfc-editor.org/rfc/rfc9457)
- [RFC 9745: The Deprecation HTTP Response Header Field](https://www.rfc-editor.org/rfc/rfc9745.html)
- [RFC 7493: The I-JSON Message Format](https://www.rfc-editor.org/rfc/rfc7493.html)
- [RFC 7396: JSON Merge Patch](https://www.rfc-editor.org/rfc/rfc7396.html)
- [RFC 9110: HTTP Semantics](https://www.rfc-editor.org/rfc/rfc9110.html)
- [Zalando RESTful API and Event Guidelines](https://opensource.zalando.com/restful-api-guidelines/)
- [Google AIP-158: Pagination](https://google.aip.dev/158)
- [MDN: Idempotency-Key header](https://developer.mozilla.org/en-US/docs/Web/HTTP/Reference/Headers/Idempotency-Key)
- [MDN: If-Match header](https://developer.mozilla.org/en-US/docs/Web/HTTP/Reference/Headers/If-Match)
- [MDN: Idempotent](https://developer.mozilla.org/en-US/docs/Glossary/Idempotent)
- [IETF draft: RateLimit header fields for HTTP](https://datatracker.ietf.org/doc/draft-ietf-httpapi-ratelimit-headers/)
- [Swagger UI configuration](https://swagger.io/docs/open-source-tools/swagger-ui/usage/configuration/)
- [Spectral](https://github.com/stoplightio/spectral)
- [oasdiff](https://github.com/oasdiff/oasdiff)
- [Schemathesis](https://github.com/schemathesis/schemathesis)
- [Pact](https://docs.pact.io/)

## 20. Project parameters (to be completed by the agent)

Procedure: (1) read the business-requirements document and fill every parameter it answers, set Status to *from requirements* and name the source; (2) ask the user each remaining question, one at a time, using the question text below; (3) if the user accepts the default, copy it and set Status to *default*; (4) record the answer in **Value** and set Status to *confirmed*. A filled value overrides the default in this document. Parameters that stay *open* keep the default.

### 20.1 Parameters

| # | Parameter | Question the agent asks | Check first in the business requirements | Default if unanswered | Value | Status |
|---|---|---|---|---|---|---|
| P01 | Consumers and audiences | Who calls the API (own frontend, partners, mobile apps, other services) and which audiences exist (public, application, administrative, preview)? | Goals, user groups, integrations | Own frontend; audiences public, application, administrative | | open |
| P02 | OpenAPI version and renderer | Which OpenAPI version and which interactive reference tool should be used? | Technical constraints | OpenAPI 3.1, Swagger UI | | open |
| P03 | Versioning mechanism | Should the major version be in the path or in the media type? | Integration and release requirements | Path: `/api/v{major}` | Path: `/api/v{major}` | answered |
| P04 | Property casing | Which casing do JSON properties and query parameters use? | Conventions, team practice | camelCase | camelCase | answered |
| P05 | Paths of contract and reference | At which paths are the contract file and the interactive reference served? | Infrastructure, routing | `/openapi.yaml` and `/docs` | | open |
| P06 | Pagination | Which default and maximum page size apply, and where is offset pagination justified? | Performance, data volumes | Cursor; 25 default, 100 maximum | | open |
| P07 | Idempotency scope | Which write operations must support an idempotency key, and how long are keys kept? | Payments, orders, messages, other critical creates | Creating operations that trigger effects; 24 h | | open |
| P08 | Concurrency mechanism | Should concurrent updates be protected with ETag and `If-Match`, or with a version property? Which resources? | Collaborative editing, workflows | ETag and `If-Match` on every updatable resource | | open |
| P09 | Error code format and ownership | Which pattern do error codes follow, and who owns the catalogue? | Conventions | `^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$`; provider team | | open |
| P10 | Deprecation policy | How long is the notice period, and who must confirm removal? | Consumer agreements | 90 days for outside consumers; zero for coordinated changes | | open |
| P11 | Consumer generation policy | May consumers generate code from the contract, or do they adapt by hand? | Frontend and integration architecture | Consumers adapt by hand | Consumers adapt by hand | answered |
| P12 | Provider generation or code-first | Does the provider generate server interfaces from the contract, or derive the contract from code? | Backend architecture | Contract-first; generated server interfaces allowed | | open |
| P13 | Reference exposure in production | Is the interactive reference reachable in production, and by whom? | Security and operations requirements | Not reachable by the public | | open |
| P14 | "Try it out" environments | In which environments may "Try it out" send requests? | Environments | Non-production only | | open |
| P15 | Locales | Which locales does the API serve, how is the locale selected, and what happens when it is unsupported? | Localisation, markets | One locale; unsupported locale returns a defined error | | open |
| P16 | Size limits and files | Which request, upload and bulk limits apply, and which file types are accepted? | Documents, media, imports | Table 17.2 | | open |
| P17 | Authentication scheme (record only) | Which authentication scheme will the API use? The design itself is deferred. | Security requirements | Not decided | | open |
| P18 | Correlation header | Which response header carries the correlation identifier? | Operations | `X-Request-Id` | | open |
| P19 | Asynchronous operations | Which operations are long-running, and how long are results kept? | Imports, exports, reports, generation | None; results kept 24 h | | open |
| P20 | Bulk operations | Which operations accept several items, and are they all-or-nothing or partial? | Imports, batch actions | None; partial with per-item results | | open |
| P21 | Deletion model | Are resources deleted permanently or marked as deleted, and what do consumers see? | Retention and legal requirements | Marked deleted; consumers see 404 | | open |
| P22 | Time zones | Which time zone applies to calendar logic (deadlines, business days), and where is it stored? | Business rules, markets | Instants in UTC; calendar logic in the business time zone given by the requirements | | open |
| P23 | Money format | Are amounts decimal strings with a currency or integer minor units? | Pricing, invoicing | Decimal string with ISO 4217 currency | | open |
| P24 | Enumeration policy | Which enumerations are closed? | Domain model | All response enumerations open | | open |
| P25 | Unknown request fields | Are unknown fields in write requests rejected or ignored? | Integration practice | Rejected with 400 | | open |
| P26 | Lint ruleset | Which linter and ruleset apply, and which rules are errors? | Quality requirements | Spectral OpenAPI ruleset plus project rules | | open |
| P27 | Mock server | Is a mock server needed for consumer development, and which tool? | Team workflow | None | | open |
| P28 | Changelog format and place | Where and in which format is the changelog kept? | Documentation practice | Markdown file beside the contract | | open |
| P29 | Coordinated deployment | May breaking changes ship as coordinated changes, and which consumers are inside the repository? | Release process | Allowed only when all consumers are in the repository | | open |
| P30 | Operational endpoints | Which health and readiness endpoints exist, and where are they documented? | Operations | One health endpoint outside the versioned path | | open |
| P31 | Search and filtering | Which collections need free-text search, which filters and sort properties, and how are results ranked? | Functional requirements | Documented per operation | | open |
| P32 | Reference examples | Are there existing API descriptions, Postman collections or partner contracts to follow? Please share links or files. | Attachments | None | | open |
| P33 | Generator toolchain | Which generator, bundler and versions, and which target languages? | Text | None | | open |
| P34 | YAML tooling and layout | Which lint and format tools, and does the default source layout apply? | Text | Default layout | | open |
| P35 | Audience variants | Are variants for partners or environments needed (overlays)? | Yes/No | No | | open |
| P36 | Export formats | Which export formats (CSV, other) are required? | List | None | | open |
| P37 | Download URL lifetime | How long may a download URL stay valid? | Duration | 5 min | | open |
| P38 | Error list cap | What is the maximum number of items in one `errors` array? | Number | 100 | | open |

### 20.2 Operation inventory

| Operation ID | Method and path | Resource | Audience | Permission | Idempotency | Pagination | Async | Notes |
|---|---|---|---|---|---|---|---|---|
| | | | | | | | | |

### 20.3 Error code catalogue

| Code | Type URI | HTTP status | Meaning | User-correctable | Retry can succeed | Consumer action | Status |
|---|---|---|---|---|---|---|---|
| `INTERNAL_001` | | 500 | Unexpected provider failure | no | maybe | Show generic failure, offer retry, keep `traceId` | active |
| `RATE_LIMIT_001` | | 429 | Too many requests | no | yes, after `Retry-After` | Wait, then retry | active |
| | | | | | | | |

### 20.4 Project-specific rules

| ID | Type (mandatory operation · forbidden pattern · rejection criterion · naming rule) | Statement | Ver. | Source |
|---|---|---|---|---|
| API-PRJ-001 | | | | |
| API-PRJ-002 | | | | |
| API-PRJ-003 | | | | |

Rules added here are numbered `API-PRJ-nnn`, follow the same statement form as sections 3 to 15, and extend the review gate (18.1).

### 20.5 Open questions log

| # | Question | Raised by | Answer | Date | Status |
|---|---|---|---|---|---|
| 1 | Should frontend consumer generation be allowed by `frontend.md`? | Claude | No. `frontend.md` keeps its prohibition; hand adaptation stays the posture (P11). | 2026-10-11 | closed |
| 2 | Keep path versioning (`/api/v1`)? | Claude | Yes. | 2026-10-11 | closed |
| 3 | Property casing? | Claude | camelCase (P04). | 2026-10-11 | closed |
| 4 | Precedence of the UX/UI contract over optional accessibility tests? | Claude | No decision needed; the existing precedence stands. | 2026-10-11 | closed |
