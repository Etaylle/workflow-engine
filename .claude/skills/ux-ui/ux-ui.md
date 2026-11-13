# UX/UI Contract and Design System Specification

| Attribute | Value |
|---|---|
| Version / status | 0.3, draft |
| Identifier prefix | `UX-` (stable; never renumbered or reused; withdrawn rules stay listed as *withdrawn*) |
| Obligation level | The keyword in the statement: **MUST** (= shall), **SHOULD**, **MAY** |
| Verification methods | **T** test (executed, automated or manual) · **I** inspection (review of an artefact) · **A** analysis · **D** demonstration |
| Basis abbreviations | WCAG = WCAG 2.2 success criterion · H# = Nielsen Norman Group heuristic number · APG = WAI-ARIA Authoring Practices · DTCG = W3C Design Tokens Format · CWV = Core Web Vitals · GOV.UK = GOV.UK Design System · PP = project practice |
| Defaults and parameters | Values marked **default**, and every table labelled *default*, are non-normative: they apply only until the matching project parameter in section 17 is filled, are overridden by it, and have no power over any rule (section 2.1). |

## 1. Scope and definitions

**Scope.** This document is the single contract for the look, behaviour, structure and verification of every user interface surface (public, application, administrative) and the specification of the design system from which those surfaces are built. It is independent of any product, brand, framework or programming language.

| Term | Definition |
|---|---|
| Surface | Any rendered user interface. |
| Design system | The governed set of tokens, components, patterns and templates, with documentation, from which all surfaces are built. |
| Token | A named design value. *Primitive* (raw scale or palette), *semantic* (purpose, e.g. `color.text.primary`), *component* (component-specific, e.g. `button.padding-x`). |
| Mode | A variant of the semantic tokens for a viewing condition (light, dark, high contrast). |
| Theme | A variant of the semantic tokens for brand or section identity. |
| Density | A variant of sizing and spacing tokens (comfortable, compact). |
| Component | A reusable UI element with one owner, defined inputs, events, variants and states. |
| Pattern | A documented, reusable solution that combines components for a recurring task (e.g. form validation). |
| Template | A page-level layout that arranges patterns and components for a class of pages. |
| Slice | A coherent, independently verifiable increment of UI work. |
| Fixture | Fictional sample data for components, pages and tests. |
| Canonical route | The one authoritative URL of a destination. |
| State matrix | The states every component defines (UX-CMP-004). |
| Project parameter | A project-specific decision recorded in section 17. |

## 2. Constraints and assumptions

| ID | Statement | Ver. | Basis |
|---|---|---|---|
| UX-CON-001 | The contract and the design system MUST be framework-neutral; the visual and interaction rules are identical across implementations. | I | PP |
| UX-CON-002 | Design MUST be delivered as version-controlled tokens and production components, not as screenshots. | I | PP |
| UX-CON-003 | Mockups MUST NOT override this contract or the project parameters. | I | PP |
| UX-CON-004 | The accessibility baseline is WCAG 2.2 level AA. | I | WCAG |
| UX-CON-005 | User-facing text MUST come from a localisation layer, never from component code. | I | PP |
| UX-CON-006 | The default presentation is light mode only. Dark and high-contrast modes are enabled per project; the token structure (UX-TOK-007) MUST allow adding them without changing components. | I | PP |
| UX-CON-007 | The default text direction is left-to-right. Right-to-left is enabled per project and then MUST use direction-neutral (logical) layout properties and be covered by the state matrix. | I | PP |
| UX-CON-008 | Project parameters (section 17) MUST be completed before the UI slices that depend on them start; until then defaults apply. | I | PP |
| UX-CON-009 | An agent MUST ask the user, or read the business-requirements document, when this contract does not answer a question, and MUST NOT invent product policy. | I | PP |

### 2.1 Rule classes and precedence

| Class | Definition | Can be overridden by |
|---|---|---|
| Fixed rule | A requirement whose basis is WCAG or APG, except a basis marked *(adopted)*, which is a criterion taken from a higher conformance level and counts as a Rule. | Nothing. A project MAY only tighten it (for example adopt AAA criteria). |
| Rule | Any other requirement (basis PP, H#, GOV.UK, CWV). | A project parameter that tightens it, or a recorded and human-approved deviation (UX-DSA-017, UX-DSA-019) that relaxes it. |
| Parameter | A project value in section 17. | Replaces defaults. Cannot relax a fixed rule; cannot relax a rule without a deviation. |
| Default | A value labelled *default* and every table labelled *default*. | Overridden by any parameter. A default has no power over any rule. |

| ID | Statement | Ver. | Basis |
|---|---|---|---|
| UX-PRE-001 | The order of precedence MUST be: fixed rules, then rules, then parameters, then defaults. | I | PP |
| UX-PRE-002 | A default MUST NOT be used to justify a violation of a rule; where a default conflicts with a rule, the rule prevails and the default is void. | I | PP |
| UX-PRE-003 | A parameter value that would violate a fixed rule (for example a brand colour that fails contrast as text) MUST be adjusted by deriving a compliant variant before use; the original value MAY remain only where no requirement is violated. | T | WCAG |
| UX-PRE-004 | Every default MUST be overridable per project through token files and configuration, without changing component code. | I | PP |
| UX-PRE-005 | Overriding a default MUST NOT alter any requirement, requirement ID or the semantic meaning of status, focus and destructive tokens (UX-TOK-008). | I | PP |
| UX-PRE-006 | A value marked *default* in a requirement is overridable; an unmarked value (for example 4.5:1) is part of the rule. | I | PP |
| UX-PRE-007 | Verification MUST run against the effective values after overrides, never against the defaults. | T | PP |
| UX-PRE-008 | If two rules conflict, the more specific rule wins; if equally specific, the stricter rule wins; the conflict MUST be recorded. | I | PP |
| UX-PRE-009 | Examples, wireframes and illustrative snippets are non-normative. | I | PP |

## 3. Design system architecture and governance

### 3.1 Layers and ownership

| Layer | Contents | Depends on |
|---|---|---|
| 1 Foundations | Tokens: colour, type, space, size, shape, elevation, motion, breakpoints, z-index | nothing |
| 2 Primitives | Button, Link, Input, Icon, Text, Box, Stack, Grid | tokens |
| 3 Components | Composed, stateful elements (Dialog, Table, Tabs, Menu …) | primitives |
| 4 Patterns | Documented solutions (validation, bulk actions, empty states …) | components |
| 5 Templates | Page layouts | patterns |
| 6 Pages | Content bound to templates | templates |

| ID | Statement | Ver. | Basis |
|---|---|---|---|
| UX-DSA-001 | A layer MUST depend only on layers below it. | I | PP |
| UX-DSA-002 | Every element MUST have exactly one owning component, pattern or template. | I | PP |
| UX-DSA-003 | A parent MUST own layout, ordering, data boundaries and section-level loading and error states; a child MUST own rendering, local interaction, accessibility and variants. | I | PP |
| UX-DSA-004 | Data MUST flow down and semantic events up; a component MUST NOT mutate its inputs or access sibling components or their DOM. | I | PP |
| UX-DSA-005 | Page files MUST compose components and MUST NOT contain repeated interactive markup. | I | PP |
| UX-DSA-006 | Lists MUST use stable content IDs as keys, never positions. | I | PP |
| UX-DSA-007 | Repeated modules of the same kind (for example sections, cards, rows) MUST have identical anatomy and equal visual status unless a project parameter says otherwise. | I | PP |
| UX-DSA-008 | Required content of a module MUST NOT be hidden or dropped to solve a layout or content problem; the layout MUST adapt. | I | PP |

### 3.2 Lifecycle, versioning, documentation

| ID | Statement | Ver. | Basis |
|---|---|---|---|
| UX-DSA-010 | Each component MUST carry a status: *experimental*, *stable* or *deprecated*. | I | PP |
| UX-DSA-011 | The design system MUST be versioned with semantic versioning; breaking changes to tokens, component APIs or behaviour increment the major version. | I | PP |
| UX-DSA-012 | A deprecated element MUST name its replacement and removal version, and tokens MUST carry the deprecation flag before removal. | I | DTCG |
| UX-DSA-013 | Each component MUST be documented with: purpose and when not to use, anatomy, variants and sizes, state matrix, tokens used, accessibility (roles, keyboard, focus, naming), content guidelines, responsive behaviour and examples. | I | PP |
| UX-DSA-014 | A browsable component gallery MUST render every component in every variant, state, mode and supported locale using fixtures. | D | PP |
| UX-DSA-015 | Before a component, token, variant or pattern is added, an existing one MUST be reused or adapted where possible. | I | PP |
| UX-DSA-016 | A proposal for a new element MUST record the user need, the items evaluated, the proposed name and location, variants and states, accessibility and responsive impact, and whether it is local or global. | I | PP |
| UX-DSA-017 | Human approval MUST be obtained before a change to a global primitive, semantic token, mode or theme meaning, navigation model, routing convention, shared module boundary or frontend dependency is merged. | I | PP |
| UX-DSA-018 | An agent MUST NOT make the changes listed in UX-DSA-017 autonomously. | I | PP |
| UX-DSA-019 | Design decisions that deviate from this contract MUST be recorded with an identifier, status, rationale and consequences. | I | PP |
| UX-DSA-020 | A superseded pattern MUST be listed as withdrawn with the version that replaced it. | I | PP |

## 4. Foundations

*Tables and values labelled **default** in this section are non-normative and overridable (UX-PRE-004). They never relax a requirement (UX-PRE-002). Requirement rows are binding.*

### 4.1 Token architecture

| ID | Statement | Ver. | Basis |
|---|---|---|---|
| UX-TOK-001 | All visual values (colour, type, space, size, radius, border, elevation, motion, z-index, breakpoints) MUST be defined as tokens in version-controlled files. | I | PP |
| UX-TOK-002 | Components MUST NOT contain raw colour values, one-off spacing values or ad hoc shadows. | T | PP |
| UX-TOK-003 | Tokens MUST be organised in primitive, semantic and component tiers. | I | PP |
| UX-TOK-004 | Components MUST consume semantic or component tokens only, never primitives. | T | PP |
| UX-TOK-005 | Token source files MUST use the W3C Design Tokens Format (JSON with `$value`, `$type`, `$description`, `{group.token}` aliases) unless a deviation is recorded (UX-DSA-019). | I | DTCG |
| UX-TOK-006 | Token names MUST follow `category.role.variant.state` (e.g. `color.action.primary.hover`) and MUST describe purpose, not appearance (`color.text.secondary`, not `color.grey`). | I | PP |
| UX-TOK-007 | Modes, themes and densities MUST be expressed by remapping semantic tokens only. | I | PP |
| UX-TOK-008 | The meaning of status (success, info, warning, error), focus and destructive actions MUST be identical in every mode and theme. | T | PP |
| UX-TOK-009 | Platform outputs (e.g. CSS custom properties) MUST be generated from the token files and MUST NOT be edited by hand. | I | PP |

Token file shape (illustrative):

```json
{
  "color": {
    "$type": "color",
    "neutral": { "0": { "$value": "#FFFFFF" }, "900": { "$value": "#161B21" } },
    "text": {
      "primary":   { "$value": "{color.neutral.900}", "$description": "Default text on any surface" }
    }
  },
  "space": { "$type": "dimension", "4": { "$value": "16px" } }
}
```

### 4.2 Colour

**Role set (every project MUST define all roles):**

| Group | Roles |
|---|---|
| Surface | `page`, `default`, `subtle`, `raised`, `inverse`, `scrim` |
| Text | `primary`, `secondary`, `tertiary`, `placeholder`, `disabled`, `inverse`, `link`, `link-hover`, `on-action` |
| Border | `subtle` (decorative), `default`, `strong` (control boundaries), `focus` |
| Action | `primary`, `primary-hover`, `primary-pressed`, `secondary`, `danger`, `danger-hover` |
| Status | `success`, `info`, `warning`, `error`, each with `background`, `base` (icons, borders), `strong` (text) |
| Data | categorical series (≥ 6), sequential, diverging |
| Brand | `brand.primary`, `brand.secondary`, `brand.accent` (project parameters P05) |

**Default primitives** (non-normative, overridable; the brand scale is a placeholder replaced by project parameter P05; any replacement must still pass the contrast rules, UX-PRE-003):

| Scale | 0 | 50 | 100 | 200 | 300 | 400 | 500 | 600 | 700 | 800 | 900 | 950 |
|---|---|---|---|---|---|---|---|---|---|---|---|---|
| Neutral | #FFFFFF | #F8F9FA | #F1F3F5 | #E3E7EB | #CDD3D9 | #9AA4AE | #6B7682 | #4F5964 | #3A434D | #262D35 | #161B21 | #0B0E11 |

| Scale | 50 | 100 | 200 | 300 | 400 | 500 | 600 | 700 | 800 | 900 |
|---|---|---|---|---|---|---|---|---|---|---|
| Brand (placeholder) | #EEF4FF | #DCE8FF | #B9D0FF | #8AB0FF | #5B8CF5 | #3A6FE0 | #2658C4 | #1E459C | #193776 | #142B5C |

| Status | background (50) | base (600) | strong (800) |
|---|---|---|---|
| Success | #E8F5EC | #2F7D4A | #1F5C35 |
| Info | #E8F0FA | #2D68A6 | #1F4F7A |
| Warning | #FFF3D6 | #9A5A00 | #6B3F00 |
| Error | #FDECEC | #B4232C | #7F1720 |

**Default semantic mapping** (non-normative, overridable; light; the dark column is used when dark mode is enabled; contrast ratios were calculated with the WCAG relative-luminance formula):

| Token | Light | Dark | Used on | Contrast (light / dark) |
|---|---|---|---|---|
| `surface.page` | N0 | N900 | – | – |
| `surface.default` | N0 | N800 | – | – |
| `surface.subtle` | N50 | N800 | – | – |
| `text.primary` | N900 | N50 | page, default, subtle | 17.3 / 16.4 |
| `text.secondary` | N700 | N300 | page, default, subtle | ≥ 9.0 / 9.2 |
| `text.tertiary`, `text.placeholder` | N600 | N400 | page, default, subtle | 7.1 / 5.5 |
| `text.disabled` | N400 | N600 | – (exempt from contrast, MUST remain perceivable) | 2.5 |
| `text.link` | B700 | B200 | page, default | 8.8 / 11.2 |
| `text.on-action` | N0 | N950 | action.primary | 8.8 (on B700) / 9.0 |
| `border.subtle` | N300 | N700 | decorative dividers only | 1.5 / 1.7 |
| `border.strong` | N500 | N400 | control boundaries | 4.6 / 5.5 |
| `border.focus`, `focus.ring` | B600 | B300 | page, default | 6.4 / 8.0 |
| `action.primary` | B700 | B300 | page, default | 8.8 / 8.0 |
| `action.danger` | E600 | dark variant of E | page, default | 6.5 |
| `status.*.strong` on `status.*.background` | 800 on 50 | – | – | 7.1 to 9.0 |
| `status.*.base` on `surface.default` (icons, borders) | 600 on N0 | – | – | ≥ 5.0 |

| ID | Statement | Ver. | Basis |
|---|---|---|---|
| UX-COL-001 | Normal text MUST have a contrast ratio of at least 4.5:1 against its background in every mode. | T | WCAG 1.4.3 |
| UX-COL-002 | Large text (≥ 24 px, or ≥ 19 px bold) MUST have a contrast ratio of at least 3:1. | T | WCAG 1.4.3 |
| UX-COL-003 | UI component boundaries, icons and graphical objects needed to understand content MUST have a contrast ratio of at least 3:1 against adjacent colours. | T | WCAG 1.4.11 |
| UX-COL-004 | Text over images MUST meet UX-COL-001/002 for every crop in use, using an opaque surface or a verified overlay. | T | WCAG 1.4.3 |
| UX-COL-005 | Colour MUST NOT be the only means of conveying status, selection, error, priority or active state. | T | WCAG 1.4.1 |
| UX-COL-006 | A status message MUST combine colour with an icon, a title and text. | I | WCAG 1.4.1 |
| UX-COL-007 | The brand accent colour MUST NOT be used for body text or status meaning. | I | PP |
| UX-COL-008 | Text MUST NOT sit on detailed imagery without protection (UX-COL-004). | I | PP |
| UX-COL-009 | Chart series MUST NOT rely on colour alone (direct labels, patterns or markers are required) and the categorical palette MUST be distinguishable under common colour-vision deficiencies. | T | WCAG 1.4.1 |
| UX-COL-010 | State changes (hover, pressed, selected) MUST use defined state-layer opacities (default: hover 8 %, pressed 12 %, selected 12 %). | I | PP |
| UX-COL-011 | Disabled elements MUST be recognisable by more than colour (for example by style and a non-interactive cursor). | I | PP |
| UX-COL-012 | Decorative layers (gradients, shapes, patterns) MUST be non-interactive, have empty alternative text or be CSS backgrounds, and MUST NOT sit behind body text or functional controls. | I | PP |

### 4.3 Typography

**Default roles and stacks** (non-normative, overridable):

| Role | Default stack | Use |
|---|---|---|
| Functional UI | `system-ui, -apple-system, "Segoe UI", Roboto, "Helvetica Neue", Arial, sans-serif` | Navigation, controls, labels, forms, tables |
| Reading (optional) | `Georgia, "Times New Roman", serif` | Long-form text, if the project defines a reading face |
| Brand / display (optional) | project parameter P07 | Wordmark, display headings |
| Mono | `ui-monospace, "SFMono-Regular", Menlo, Consolas, monospace` | Code, identifiers |

**Default type scale** (non-normative, overridable; modular ratio 1.25 on a 16 px base; mobile / desktop px, line height as a unitless multiplier):

| Token | Mobile | Desktop | Weight | Line height | Use |
|---|---:|---:|---:|---:|---|
| `type.display` | 40 | 56 | 600 | 1.10 | Hero statements |
| `type.h1` | 32 | 44 | 600 | 1.15 | Page title (one per page) |
| `type.h2` | 26 | 34 | 600 | 1.20 | Section |
| `type.h3` | 22 | 26 | 600 | 1.25 | Sub-section |
| `type.h4` | 18 | 20 | 600 | 1.30 | Card and panel titles |
| `type.lead` | 18 | 20 | 400 | 1.50 | Standfirst |
| `type.body` | 16 | 16 | 400 | 1.50 | UI and general text |
| `type.body-long` | 17 | 18 | 400 | 1.65 | Long-form reading |
| `type.label` | 14 | 14 | 500 | 1.40 | Field labels, buttons, tabs |
| `type.small` | 14 | 14 | 400 | 1.40 | Secondary metadata |
| `type.caption` | 12 | 12 | 400 | 1.40 | Non-essential captions only |

Weights: 400, 500, 600, 700. Letter spacing: 0 by default, −0.01em for display, +0.04em for uppercase labels.

| ID | Statement | Ver. | Basis |
|---|---|---|---|
| UX-TYP-001 | The type system MUST define at most three typefaces (functional, reading, brand/display) plus a monospace face. | I | PP |
| UX-TYP-002 | A type scale with size, weight and line height per breakpoint MUST be defined as tokens. | I | PP |
| UX-TYP-003 | Body and functional text MUST NOT be smaller than the defined minimum (default: 16 px; 14 px for labels and secondary metadata; 12 px only for non-essential captions). | T | PP |
| UX-TYP-004 | Body line height MUST be at least 1.5. | T | WCAG 1.4.12 |
| UX-TYP-005 | Long-form line length SHOULD be within a defined range (default: 60–80 characters; `measure.article` = 44 rem). | T | PP |
| UX-TYP-006 | Text MUST remain readable, without loss of content or function, at 200 % zoom. | T | WCAG 1.4.4 |
| UX-TYP-007 | Content MUST tolerate user text-spacing overrides (line height 1.5, paragraph spacing 2×, letter spacing 0.12 em, word spacing 0.16 em) without clipping or overlap. | T | WCAG 1.4.12 |
| UX-TYP-008 | Long words and labels in every supported locale MUST wrap or be deliberately clamped; text MUST NOT be scaled down to fit. | T | PP |
| UX-TYP-009 | Font sizes MUST be specified in relative units so that user font-size settings apply. | T | WCAG 1.4.4 |
| UX-TYP-010 | Each web font MUST have a fallback stack with matching metrics and a loading strategy that keeps layout shift within UX-PRF-003. | T | CWV |
| UX-TYP-011 | There MUST be exactly one `h1` per page and heading levels MUST NOT be chosen for visual size; style is applied through classes or tokens, not by level. | T | WCAG 1.3.1 |
| UX-TYP-012 | Uppercase transforms MUST be limited to short labels; running text MUST NOT be set in all capitals. | I | PP |

### 4.4 Spacing and sizing

**Default spacing scale** (non-normative, overridable; base unit 4 px):

| Token | `space.1` | `.2` | `.3` | `.4` | `.5` | `.6` | `.7` | `.8` | `.9` | `.10` | `.11` | `.12` |
|---|---|---|---|---|---|---|---|---|---|---|---|---|
| px | 4 | 8 | 12 | 16 | 24 | 32 | 40 | 48 | 64 | 80 | 96 | 128 |

Layout spacing roles: **inset** (padding inside a container), **stack** (vertical gap), **inline** (horizontal gap), **section** (default 48–64 px on mobile, 72–96 px on desktop).

**Default control sizes** (non-normative, overridable; height, px): `size.control.sm` 32 (compact density only), `size.control.md` 40, `size.control.lg` 48. On coarse pointers every interactive control is at least 44 × 44 CSS px.

| ID | Statement | Ver. | Basis |
|---|---|---|---|
| UX-SPC-001 | One spacing scale on one base unit MUST be used for all margins, paddings and gaps. | T | PP |
| UX-SPC-002 | Section spacing MUST be defined per breakpoint. | I | PP |
| UX-SPC-003 | Pointer targets MUST be at least 24 × 24 CSS px or have equivalent spacing. | T | WCAG 2.5.8 |
| UX-SPC-004 | Touch targets SHOULD be at least 44 × 44 CSS px. | T | PP |
| UX-SPC-005 | Related items MUST be closer together than unrelated items (proximity encodes grouping). | I | PP |
| UX-SPC-006 | Density modes, where offered, MUST be applied only by remapping size and space tokens and MUST keep UX-SPC-003. | T | PP |

### 4.5 Grid, breakpoints, layout

**Default breakpoints and grid** (non-normative, overridable; min-width):

| Name | Range (px) | Columns | Gutter | Outer margin |
|---|---|---:|---:|---:|
| `xs` | 0–599 | 4 | 16 | 16 |
| `sm` | 600–839 | 8 | 24 | 24 |
| `md` | 840–1199 | 12 | 24 | 32 |
| `lg` | 1200–1599 | 12 | 32 | 40 |
| `xl` | ≥ 1600 | 12 | 32 | centred, max content width |

Default measures (non-normative, overridable): `layout.max` 80 rem, `measure.article` 44 rem, `measure.compact` 36 rem. Default verification widths: 320, 375, 768, 1024, 1280, 1440, 1920 px, plus 200 % zoom and landscape.

| ID | Statement | Ver. | Basis |
|---|---|---|---|
| UX-LAY-001 | Layouts MUST be designed mobile-first and verified at the documented verification widths. | T | PP |
| UX-LAY-002 | Content MUST reflow to one column at 320 CSS px without two-dimensional scrolling, except content that needs two dimensions (data tables, maps). | T | WCAG 1.4.10 |
| UX-LAY-003 | DOM order MUST equal reading order and keyboard order at every breakpoint. | T | WCAG 1.3.2, 2.4.3 |
| UX-LAY-004 | Content MUST NOT be duplicated in the DOM to achieve a different order on small screens; the visual order MUST be achieved by layout, with DOM order preserved. | I | PP |
| UX-LAY-005 | Content containers MUST NOT have fixed heights. | I | PP |
| UX-LAY-006 | Sticky or fixed elements MUST NOT fully hide the element that has keyboard focus. | T | WCAG 2.4.11 |
| UX-LAY-007 | Essential content and actions MUST NOT depend on hover, swipe or drag alone. | T | WCAG 2.5.1, 2.5.7 |
| UX-LAY-008 | Infinite scrolling MUST NOT be the only way to reach content or the footer. | T | PP |
| UX-LAY-009 | A mobile order that differs from the desktop order MUST be defined per module in the template. | I | PP |
| UX-LAY-010 | Layouts MUST NOT depend on a specific orientation. | T | WCAG 1.3.4 |
| UX-LAY-011 | Layout MUST use a documented container, grid and spacing system; arbitrary per-page pixel offsets MUST NOT be used. | I | PP |

### 4.6 Shape, borders, elevation, z-index

| Token set | Default values (non-normative, overridable) |
|---|---|
| `radius` | none 0 · xs 2 · sm 4 · md 8 · lg 12 · xl 16 · full 9999 (px) |
| `border.width` | thin 1 · thick 2 (px) |
| `elevation` | 0 none · 1 `0 1px 2px rgb(11 14 17 / 8%)` · 2 `0 4px 12px rgb(11 14 17 / 10%)` · 3 `0 8px 24px rgb(11 14 17 / 14%)` (menus, popovers) · 4 `0 16px 40px rgb(11 14 17 / 18%)` (modals) |
| `z` | base 0 · sticky 100 · dropdown 200 · scrim 300 · modal 400 · popover 500 · toast 600 · tooltip 700 |

| ID | Statement | Ver. | Basis |
|---|---|---|---|
| UX-SHP-001 | Radii, border widths, elevations and z-indexes MUST be taken from the token sets above (or project values), never ad hoc. | T | PP |
| UX-SHP-002 | Elevation MUST express layering (raised, overlay) and MUST NOT be used as decoration. | I | PP |
| UX-SHP-003 | Fine borders SHOULD be preferred to heavy shadows for grouping. | I | PP |
| UX-SHP-004 | Cards SHOULD be used only where they improve grouping, scanning or interaction. | I | PP |
| UX-SHP-005 | Overlays MUST stack in the z-index order defined above. | T | PP |

### 4.7 Motion

| Token | Default (non-normative, overridable) |
|---|---|
| `motion.duration` | instant 0 · fast 100 · base 200 · slow 300 · slower 500 (page-level only), in ms |
| `motion.easing` | standard `cubic-bezier(0.2, 0, 0, 1)` · decelerate (entering) `cubic-bezier(0, 0, 0, 1)` · accelerate (leaving) `cubic-bezier(0.3, 0, 1, 1)` |

| Interaction | Default timing (non-normative, overridable) |
|---|---|
| Hover, press, focus feedback | fast (100) |
| Menu, tooltip, popover open | base (200), close fast (100) |
| Dialog, drawer enter | slow (300), exit base (200) |
| Skeleton shimmer | ≥ 1.2 s loop, off under reduced motion |

| ID | Statement | Ver. | Basis |
|---|---|---|---|
| UX-MOT-001 | Motion MUST serve orientation or feedback and MUST NOT be required to understand content. | I | PP |
| UX-MOT-002 | Non-essential motion MUST be removed or reduced when the user sets reduced motion. | T | WCAG 2.3.3 (adopted) |
| UX-MOT-003 | Durations and easing MUST come from motion tokens; leaving transitions SHOULD be shorter than entering transitions. | I | PP |
| UX-MOT-004 | Moving, blinking or auto-updating content lasting more than 5 s MUST offer pause, stop or hide. | T | WCAG 2.2.2 |
| UX-MOT-005 | Auto-playing video and auto-rotating carousels MUST NOT be used unless a project parameter demands them, and then MUST follow UX-C30. | I | WCAG 2.2.2 |
| UX-MOT-006 | Content MUST NOT flash more than three times per second. | T | WCAG 2.3.1 |
| UX-MOT-007 | Parallax and scroll-linked animation MUST be disabled under reduced motion. | T | WCAG 2.3.3 |

### 4.8 Iconography

| ID | Statement | Ver. | Basis |
|---|---|---|---|
| UX-ICO-001 | One icon set with one stroke style MUST be used; default grid 24 px with sizes 16, 20, 24 and 32. | I | H4 |
| UX-ICO-002 | An icon that carries meaning MUST have an accessible name or adjacent visible text. | T | WCAG 1.1.1 |
| UX-ICO-003 | Decorative icons MUST be hidden from assistive technology. | T | WCAG 1.1.1 |
| UX-ICO-004 | Icons MUST inherit colour from text or status tokens (`currentColor`) and MUST NOT carry fixed colours. | T | PP |
| UX-ICO-005 | The same meaning MUST use the same icon everywhere, and one icon MUST NOT carry two meanings. | I | H4 |
| UX-ICO-006 | Icon-only controls MUST expose a text label to assistive technology and a visible label or tooltip for sighted users. | T | WCAG 2.5.3 |
| UX-ICO-007 | External-link and social icons MUST link only to confirmed destinations; an unconfirmed destination blocks release instead of using a placeholder. | I | PP |

### 4.9 Imagery and identity

| ID | Statement | Ver. | Basis |
|---|---|---|---|
| UX-IMG-001 | A meaningful image MUST have alternative text specific to its context of use. | I | WCAG 1.1.1 |
| UX-IMG-002 | A decorative image MUST be marked decorative and render with empty alternative text. | T | WCAG 1.1.1 |
| UX-IMG-003 | Images MUST have reserved dimensions (aspect ratio) so loading causes no layout shift. | T | CWV |
| UX-IMG-004 | Images MUST be served in responsive sizes and modern formats; images below the first view MUST load lazily. | T | CWV |
| UX-IMG-005 | The set of aspect ratios MUST be limited and defined (default: 1:1, 4:3, 3:2, 16:9, 21:9). | I | PP |
| UX-IMG-006 | A missing or failed image MUST show a defined fallback that preserves layout. | T | PP |
| UX-IMG-007 | Where images are cropped, a focal point MUST be settable so key content survives every crop in use. | D | PP |
| UX-IMG-008 | Text placed on an image MUST sit in a protected zone (opaque surface or verified gradient) that holds for every crop in use. | T | WCAG 1.4.3 |
| UX-IMG-009 | Credit, source and rights information MUST be displayed as the project parameters specify. | I | PP |
| UX-IMG-010 | Brand logos and emblems MUST be rendered only from approved assets and MUST NOT be recreated with CSS, emoji or generic icons. | I | PP |
| UX-IMG-011 | Video MUST NOT autoplay with sound, MUST have controls, captions and a text alternative. | T | WCAG 1.2.2, 1.4.2 |

## 5. Semantic structure

### 5.1 Element mapping

| Intent | Element | Rule |
|---|---|---|
| Navigate to a destination | `<a href>` | Never a `div`/`span`/`button` with a click handler. |
| Perform an action | `<button type="button\|submit">` | Never a link without a destination. |
| Site and section navigation | `<nav aria-label>` containing a list | Every `nav` on a page has a distinct label. |
| Page regions | `<header>`, `<main>`, `<aside>`, `<footer>`, `<search>` / `role="search"` | Exactly one `main`; landmarks are not nested redundantly. |
| Group of related content | `<section>` with a heading | A `section` without an accessible name is a `div`. |
| Self-contained item (card, article) | `<article>` or `<li>` in a list | One primary link per item. |
| Lists | `<ul>`, `<ol>`, `<dl>` | Visual lists are real lists. |
| Tabular data | `<table>` with `<caption>`, `<th scope>` | Never for layout. |
| Image with caption | `<figure>` / `<figcaption>` | Alt text is independent of the caption. |
| Dates and times | `<time datetime>` | Display is locale-formatted. |
| Form group | `<fieldset>` / `<legend>` | Required for radio groups and related checkboxes. |
| Expandable content | `<button aria-expanded aria-controls>` | Native `<details>` MAY be used where its behaviour suffices. |
| Quotation | `<blockquote>` with `<cite>` | – |

### 5.2 Requirements

| ID | Statement | Ver. | Basis |
|---|---|---|---|
| UX-SEM-001 | The element that matches the intent (section 5.1) MUST be used; ARIA MUST only fill gaps that native semantics cannot. | I | APG |
| UX-SEM-002 | Every page MUST provide landmarks and, as the first focusable element, a skip link to the main content. | T | WCAG 2.4.1 |
| UX-SEM-003 | Every page MUST have a unique descriptive title in the form *page – section – site*. | T | WCAG 2.4.2 |
| UX-SEM-004 | Heading levels MUST NOT be skipped. | T | WCAG 1.3.1 |
| UX-SEM-005 | Every interactive element MUST have an accessible name, and that name MUST contain its visible label text. | T | WCAG 2.5.3, 4.1.2 |
| UX-SEM-006 | Element IDs MUST be unique and ARIA references (`aria-labelledby`, `aria-describedby`, `aria-controls`) MUST resolve. | T | WCAG 4.1.2 |
| UX-SEM-007 | Dynamic state MUST be exposed with ARIA state attributes (`aria-expanded`, `aria-pressed`, `aria-current`, `aria-selected`, `aria-busy`, `aria-invalid`, `aria-sort`). | T | WCAG 4.1.2 |
| UX-SEM-008 | Status messages that do not move focus MUST use a live region (`role="status"` for information, `role="alert"` only for urgent errors). | T | WCAG 4.1.3 |
| UX-SEM-009 | The page language MUST be declared and language changes inside content MUST be marked. | T | WCAG 3.1.1, 3.1.2 |
| UX-SEM-010 | Interactive elements and key regions MUST expose a stable test identifier attribute (default `data-testid`) that does not depend on styling or position. | I | PP |
| UX-SEM-011 | Content MUST NOT rely on CSS-generated text for meaning. | I | WCAG 1.3.1 |

## 6. Components

### 6.1 Component contract

| ID | Statement | Ver. | Basis |
|---|---|---|---|
| UX-CMP-001 | A component API MUST expose enumerated variants and sizes, not free-form class names or style overrides. | I | PP |
| UX-CMP-002 | A component MUST emit typed semantic events (for example `select`, `dismiss`, `submit`), not raw DOM events as its public contract. | I | PP |
| UX-CMP-003 | A component MUST accept content through defined slots or inputs and MUST NOT contain hard-coded user-facing text. | T | PP |
| UX-CMP-004 | Every component MUST define each applicable state: default, hover, focus-visible, active, selected, disabled, loading, empty, error, success, read-only, long-content, missing-media, and each mode, theme, density and direction in scope. | T | H1 |
| UX-CMP-005 | A component that renders remote data MUST implement loading, empty and error states, and the error state MUST offer a retry where retrying is meaningful. | T | H1, H9 |
| UX-CMP-006 | A disabled control MUST expose the reason it is disabled (adjacent text or accessible description). | I | H5 |
| UX-CMP-007 | Actions MUST use native buttons and navigation MUST use links; non-interactive elements MUST NOT be made clickable; interactive elements MUST NOT be nested. | T | WCAG 4.1.2 |
| UX-CMP-008 | A card MUST have one primary destination; secondary actions MUST sit outside the primary link. | I | PP |
| UX-CMP-009 | An icon or arrow MUST NOT be the only accessible name of a link or button. | T | WCAG 2.4.4 |
| UX-CMP-010 | Custom widgets MUST follow the roles, states and keyboard interaction of the matching APG pattern; native elements MUST be preferred to custom widgets. | T | APG |
| UX-CMP-011 | Components that open overlays MUST manage focus: move focus in, return it to the trigger on close, and never leave it on a hidden element. | T | APG |
| UX-CMP-012 | Every component MUST be operable by keyboard with the same capabilities as by pointer. | T | WCAG 2.1.1 |
| UX-CMP-013 | The fixture set MUST cover long text, missing media, many items, right-to-left text (where in scope) and every state. | I | PP |
| UX-CMP-014 | Fixtures MUST NOT contain real personal or sensitive data. | I | PP |
| UX-CMP-015 | Components MUST NOT depend on the page they are placed in (no hidden coupling to global CSS, global state or page order). | I | PP |

### 6.2 Component catalogue

Every row is verified by component tests (T) for keyboard behaviour, semantics, states and tokens. Each component also meets UX-CMP-001 to UX-CMP-015.

| ID | Component · variants · sizes | Anatomy | Semantics and keyboard | Mandatory rules |
|---|---|---|---|---|
| UX-C01 | **Button** · primary, secondary, tertiary, danger · sm, md, lg | container, label, optional icon, loading indicator | Native `<button>`; Enter and Space activate | Label states the outcome ("Save draft"). One primary per view region. Loading keeps width, sets `aria-busy` and blocks repeat activation. `type` always explicit. |
| UX-C02 | **Link** · inline, standalone, external, download | text, optional icon | Native `<a href>`; Enter activates | Inline links are underlined (not colour alone). External links show an indicator and, if opening a new context, say so. Link text is meaningful out of context. File links show type and size. |
| UX-C03 | **IconButton** · primary, secondary, tertiary · md, lg | icon, accessible name, tooltip | Native `<button>`; toggles use `aria-pressed` | Accessible name mandatory. Target ≥ 24 px (≥ 44 px on coarse pointers). |
| UX-C04 | **TextInput / TextArea** · default, with prefix or suffix, search · md, lg | label, hint, control, counter, error | Native input with associated `<label>`; `aria-describedby` for hint and error; `aria-invalid` on error | Visible label above the field. Placeholder never replaces a label. Correct `type`, `autocomplete`, `inputmode`. Counter announces politely at thresholds only. |
| UX-C05 | **Select / Combobox** · native select, searchable combobox · md, lg | label, control, popup list, empty message | Native `<select>` by default. Combobox per APG: `role="combobox"`, `aria-expanded`, `aria-controls`, `aria-activedescendant`; Down/Up move, Enter selects, Esc closes | Use native select up to 15 options (default threshold), combobox beyond. Announce result count; show a "no results" message. |
| UX-C06 | **Checkbox / Radio group / Switch** · default, indeterminate (checkbox) | control, label, hint, error | Native inputs; radio groups in `fieldset`/`legend`; Space toggles; arrows move within a radio group; switch `role="switch"` | Hit area includes the label. Switch applies immediately; checkbox applies on submit. Error is shown at group level. |
| UX-C07 | **FormField** · vertical (default), horizontal (wide screens) | label, hint, control, error | Wires `for`/`id`, `aria-describedby`; order: label, hint, control, error | One column by default. Required and optional marked consistently. |
| UX-C08 | **ErrorSummary** | heading, list of links | Focus moves to it on failed submit; each link targets its field (first field for multi-field questions) | Top of main content, above the `h1`. Wording identical to inline messages. |
| UX-C09 | **DatePicker** · date, date range | text input, format hint, optional calendar dialog | Typed entry always available; calendar grid: arrows move day, PageUp/PageDown change month, Home/End move within week, Esc closes | Locale-aware format shown as hint. Min and max enforced with clear messages. No picker-only entry. |
| UX-C10 | **FileUpload** · button, button + drop zone | control, constraints text, file list, progress, errors | Native `<input type="file">`; drop zone is an enhancement; progress uses `role="progressbar"` | Allowed types and size stated before upload. Errors per file. Remove control has a per-file accessible name. |
| UX-C11 | **Alert (inline banner)** · info, success, warning, error · dismissible optional | icon, title, text, optional action | `role="status"` (info, success); `role="alert"` only for urgent errors | Persistent unless dismissible. Never colour alone (UX-COL-006). Not used for field errors. |
| UX-C12 | **Toast** · info, success (errors only with a persistent alternative) | icon, message, optional dismiss | `role="status"`; reachable by keyboard | Default auto-dismiss 8 s, paused on hover and focus. Critical errors never only in a toast. At most 3 stacked. No essential action inside. |
| UX-C13 | **Dialog (modal)** · sm, md, lg (default 400, 560, 800 px); alertdialog for confirmations | scrim, container, title, body, footer actions, close | `role="dialog"` or `alertdialog`, `aria-modal`, `aria-labelledby`; focus moves in, Tab is trapped, Esc closes (except forced decisions), focus returns to trigger; background inert | Confirmation dialogs state effect and reversibility. Primary action at the end of the footer. Scroll lock on the page. |
| UX-C14 | **Drawer / Sheet** · side, bottom (small screens); modal or non-modal | scrim (modal), container, title, body, close | Modal: as dialog. Non-modal detail panel: `complementary` region, no focus trap | Non-modal panels never cover the focused element of the main content. |
| UX-C15 | **Menu button (action menu)** · default | trigger, popup, items, separators | APG menu button: Enter, Space or Down opens; arrows move; Home/End; typeahead; Esc closes and returns focus; Tab closes | `role="menu"` only for actions. Site navigation uses link lists, not menus. |
| UX-C16 | **Tabs** · horizontal, vertical | tablist, tabs, panels | `tablist`/`tab`/`tabpanel`; Left/Right (Up/Down vertical), Home/End; roving tabindex; automatic activation unless panel load is slow | Tabs switch content on the same page, never pages. Panel has `tabindex="0"` when it has no focusable content. |
| UX-C17 | **Accordion / Disclosure** · single, multiple | heading, trigger button, panel | Button inside a heading with `aria-expanded` and `aria-controls`; Enter and Space toggle | Multiple open allowed by default. The heading level fits the page outline. |
| UX-C18 | **Tooltip / Popover** | trigger, bubble | Tooltip `role="tooltip"`, shown on focus and hover, Esc dismisses, hoverable and persistent. Popover with interactive content follows dialog semantics (non-modal) | A tooltip never carries essential information or interactive content. A touch alternative exists. |
| UX-C19 | **Breadcrumbs** | `nav`, ordered list, separators | `aria-label="Breadcrumb"`, current item `aria-current="page"` | Collapse long trails with an accessible expander. Small screens may show the parent only. |
| UX-C20 | **Pagination** · numbered, previous/next | `nav`, list of links, range text | `aria-label`, current page `aria-current="page"` | Preserves filters and sort in the URL. After a page change, focus moves to the results heading. Shows range and total. |
| UX-C21 | **Table** · static, sortable, selectable · comfortable, compact | caption, header, body, optional toolbar, footer | Native table, `<th scope>`; sortable headers contain a button and set `aria-sort`; selection column with select-all | Numeric columns right-aligned. Sticky header on long tables. Defined empty, no-match, loading and error states. Small-screen strategy defined (reflow, card view or scroll with hint). An interactive grid is used only when cell-level navigation is required (APG grid). |
| UX-C22 | **Card** · standard, horizontal, compact, feature | media, eyebrow, title (the link), summary, meta, actions | `<article>` or `<li>`; title link may cover the card via a stretched area | Equal heights come from layout, never fixed heights. Defined truncation and missing-media behaviour. |
| UX-C23 | **Badge / Tag / Chip** · status, count, filter, removable | label, optional icon | Filter chips are toggle buttons (`aria-pressed`); removable chips have a remove button named "Remove *label*" | Status badges use text and icon (UX-COL-006). Contrast per UX-COL-001. |
| UX-C24 | **Avatar** · image, initials · sm, md, lg | image or initials, optional presence | Image has alt text; decorative when the name is adjacent | Initials fallback when the image fails. Overflow count in groups. |
| UX-C25 | **Skeleton / Spinner / Progress** | placeholder shapes, spinner, bar | Skeleton is `aria-hidden` and the container has `aria-busy="true"`; progress uses `role="progressbar"` with value attributes | Skeleton mirrors the final layout. Spinner only for short waits of unknown length and has a status text. Long waits show progress or an explanation. |
| UX-C26 | **EmptyState** · first use, no results, no access, error | decorative visual, title, explanation, primary action, secondary link | Heading and text; action is a button or link | States why it is empty and what to do next. The three empty cases are never conflated (UX-STA-005). |
| UX-C27 | **Header and primary navigation** | skip link, brand, navigation, utilities (search, account, menu) | `<header>`, `nav aria-label`; current item `aria-current`; small-screen menu is a disclosure button with `aria-expanded` | Brand link returns home. Dropdowns are disclosures, not menus. One navigation system per task. |
| UX-C28 | **Footer** | brand block, link groups with headings, legal row | `<footer>`; groups are lists with headings; small-screen groups MAY use native disclosure buttons | Complete content, no placeholder links, no empty groups. Legal row separate. |
| UX-C29 | **Stepper** | ordered list of steps, state text, back/next | `<ol>`, current step `aria-current="step"`; state shown as text | Back never loses data. Each step has a title. Progress persists. |
| UX-C30 | **Carousel** (only if a project parameter requires it) | slides, pause/play, previous/next, slide picker | APG carousel; controls first in order; slides labelled "n of m" | No auto-rotation by default; rotation stops on focus, hover and under reduced motion. All content is reachable without the carousel. Swipe is never the only way. |
| UX-C31 | **Search** | `search` landmark, labelled input, submit, clear, optional suggestions | Suggestions use the combobox pattern; results count announced through a live region or heading | Handles empty, broad and no-result queries (UX-IA-010). Query preserved in the field. |
| UX-C32 | **Rich-text / article renderer** | headings, paragraphs, lists, quotes, figures, tables, embeds | Output derived from structured, sanitised content; headings below the page `h1` | Only approved block types. Embeds load after consent. No user-supplied CSS or markup. |
| UX-C33 | **Chart** · line, bar, area, scatter, donut | title, description, plot, legend, data table alternative | Focusable data points with text equivalents; data table available | Direct labels or patterns in addition to colour (UX-COL-009). Units and period stated. Empty, loading and error states. |

## 7. Patterns

### 7.1 Forms and validation

| ID | Statement | Ver. | Basis |
|---|---|---|---|
| UX-FRM-001 | Every field MUST have a persistent visible label; placeholders MUST NOT replace labels. | T | WCAG 3.3.2 |
| UX-FRM-002 | Required and optional fields MUST be marked consistently. | I | WCAG 3.3.2 |
| UX-FRM-003 | Help text MUST be programmatically associated with its field. | T | WCAG 1.3.1 |
| UX-FRM-004 | Inputs MUST use the appropriate type, autocomplete attribute and input mode; pasting MUST NOT be blocked. | T | WCAG 1.3.5, 3.3.8 |
| UX-FRM-005 | A validation message MUST state what is wrong and how to correct it, in plain language, and MUST NOT expose internals (SQL, stack traces, schema names). | T | WCAG 3.3.1, 3.3.3 |
| UX-FRM-006 | After a failed submit, an error summary MUST appear at the top of the main content with a clear heading and one entry per error linking to its field, and keyboard focus MUST move to it. | T | GOV.UK |
| UX-FRM-007 | Each invalid field MUST show an inline message worded identically to its summary entry. | T | GOV.UK |
| UX-FRM-008 | The page title SHOULD carry an error indicator when the page shows errors. | I | GOV.UK |
| UX-FRM-009 | Entered data MUST be preserved after failed validation. | T | H9 |
| UX-FRM-010 | Leaving a form with unsaved changes MUST trigger a warning. | T | H3 |
| UX-FRM-011 | Validation SHOULD run on blur or submit, not on every keystroke, except guidance such as character counters or password strength. | I | PP |
| UX-FRM-012 | Submitting a form twice MUST be harmless (pending state and safe repeat handling). | T | H5 |
| UX-FRM-013 | Autosave, where present, MUST be visible, MUST NOT publish or change workflow state and MUST NOT overwrite newer server data. | T | H1 |
| UX-FRM-014 | Information already provided MUST NOT be requested again in the same flow. | T | WCAG 3.3.7 |
| UX-FRM-015 | Forms MUST default to a single column; multi-column layouts are limited to short, closely related fields. | I | PP |
| UX-FRM-016 | Constraints (format, length, allowed values) MUST be stated before input, not only on error. | I | H5 |
| UX-FRM-017 | Destructive or irreversible submissions MUST be preceded by a review or confirmation step that states the effect. | T | H5 |

### 7.2 Authentication and account

| ID | Statement | Ver. | Basis |
|---|---|---|---|
| UX-AUT-001 | Authentication MUST NOT require a cognitive function test unless an accessible alternative exists. | T | WCAG 3.3.8 |
| UX-AUT-002 | Password managers MUST work in login and registration forms. | T | WCAG 3.3.8 |
| UX-AUT-003 | Password fields SHOULD offer a show/hide control with an accessible name. | T | PP |
| UX-AUT-004 | Login, reset and registration messages MUST NOT reveal whether an account exists. | T | PP |
| UX-AUT-005 | Session expiry MUST warn the user and preserve work where feasible. | T | WCAG 2.2.1 |
| UX-AUT-006 | The signed-in identity and a sign-out control MUST be visible in the shell. | I | H1 |

### 7.3 Feedback and system states

| Situation | Pattern |
|---|---|
| Invalid field | Inline message + error summary (UX-C08) |
| Action succeeded, low stakes | Toast (UX-C12) or inline confirmation |
| Action succeeded, user needs the result | Inline alert or navigation to the result |
| Persistent condition (maintenance, degraded mode, read-only) | Banner (UX-C11) |
| Recoverable failure | Inline alert with retry |
| Critical failure or data-loss risk | Inline alert (persistent) or alertdialog |
| Irreversible decision | Confirmation dialog (UX-C13) |
| Long-running work | Progress with queued, running, completed, failed |

| ID | Statement | Ver. | Basis |
|---|---|---|---|
| UX-STA-001 | Content with a known layout MUST show skeletons while loading; short waits of unknown shape MAY use spinners. | I | H1 |
| UX-STA-002 | A loading state MUST reserve the space of the final content. | T | CWV |
| UX-STA-003 | Replacement of loading content MUST be announced to assistive technology. | T | WCAG 4.1.3 |
| UX-STA-004 | An empty state MUST explain why it is empty and offer a next step. | T | H9 |
| UX-STA-005 | Empty states MUST distinguish "nothing yet", "no match" and "no access". | T | H1 |
| UX-STA-006 | An error state MUST describe the problem in plain language and offer a recovery action. | T | H9 |
| UX-STA-007 | A success message MUST confirm what happened and where to find the result. | I | H1 |
| UX-STA-008 | A critical error MUST NOT be shown only in an auto-dismissing message. | T | WCAG 2.2.1 |
| UX-STA-009 | A long-running action MUST show queued, running, completed and failed states and MUST NOT be lost when the user leaves the page. | T | H1, H3 |
| UX-STA-010 | In degraded mode the UI MUST state what still works and MUST block writes that cannot be completed safely. | T | H1, H5 |
| UX-STA-011 | Forbidden and not-found pages MUST offer a next step and MUST NOT reveal whether a restricted record exists. | T | H9 |
| UX-STA-012 | Offline and slow-network conditions MUST be detected and communicated without losing entered data. | T | H1 |
| UX-STA-013 | Optimistic updates MUST roll back visibly if the server rejects the change. | T | H1, H9 |

### 7.4 Navigation and information architecture

| ID | Statement | Ver. | Basis |
|---|---|---|---|
| UX-IA-001 | The navigation model (primary, secondary, utility, footer, in-page) MUST be defined in the project parameters. | I | PP |
| UX-IA-002 | A view MUST NOT contain two competing navigation systems for the same task. | I | H4 |
| UX-IA-003 | Every visible link, button, card and menu item MUST resolve to a working destination or perform a working action; placeholder routes and non-functional controls MUST NOT be released. | T | PP |
| UX-IA-004 | Canonical routes and slugs MUST come from content or API data and MUST NOT be generated from display titles in UI code; a changed route MUST produce a redirect. | T | PP |
| UX-IA-005 | The same destination MUST carry the same label everywhere it appears. | I | H4 |
| UX-IA-006 | Navigation MUST list only implemented modules. | I | PP |
| UX-IA-007 | The current location MUST be indicated by more than colour. | T | WCAG 1.4.1 |
| UX-IA-008 | Pages below the first level MUST offer breadcrumbs or an equivalent back path. | I | H3 |
| UX-IA-009 | Where search is provided, it MUST be reachable from every page. | T | PP |
| UX-IA-010 | Search MUST define behaviour for empty, overly broad and no-result queries. | T | PP |
| UX-IA-011 | Repeated help or contact entry points MUST keep the same relative place and order across pages. | I | WCAG 3.2.6 |
| UX-IA-012 | Navigation labels MUST use the audience's vocabulary and MUST be unique within one navigation. | I | H2 |
| UX-IA-013 | The number of top-level navigation items SHOULD NOT exceed seven (default); the rest moves to a defined overflow destination. | I | PP |
| UX-IA-014 | Role-based navigation MUST hide items the user cannot use, and hiding MUST NOT replace server-side authorization. | T | PP |

### 7.5 Data display, filtering and bulk actions

| ID | Statement | Ver. | Basis |
|---|---|---|---|
| UX-DAT-001 | A listing MUST show the minimum needed to decide (title, key metadata, status, thumbnail where useful). | I | H6 |
| UX-DAT-002 | Lists and tables MUST be paginated or explicitly "load more"; page size MUST be bounded. | T | PP |
| UX-DAT-003 | Sorting MUST be stable with a deterministic tie-breaker. | T | PP |
| UX-DAT-004 | Filters MUST show the active state, allow clearing each and all, and be reflected in the URL. | T | PP |
| UX-DAT-005 | Empty and no-match states MUST be defined for every list. | T | H1 |
| UX-DAT-006 | Row actions MUST be discoverable without hover. | T | WCAG 1.4.13 |
| UX-DAT-007 | A bulk action MUST re-validate the current state of each selected record, report partial failures per record and MUST NOT skip records silently. | T | H5, H9 |
| UX-DAT-008 | Entity state (for example draft, active, archived, deleted) MUST be shown in lists as text and icon. | I | WCAG 1.4.1 |
| UX-DAT-009 | Numeric and date columns MUST be aligned and formatted by locale. | I | PP |
| UX-DAT-010 | A table MUST define a small-viewport strategy. | I | WCAG 1.4.10 |

### 7.6 Editing, destructive actions and concurrency

| ID | Statement | Ver. | Basis |
|---|---|---|---|
| UX-EDT-001 | A destructive action MUST state its effect and its reversibility. | T | H5 |
| UX-EDT-002 | Soft delete, undo or restore SHOULD be preferred to irreversible deletion. | I | H3 |
| UX-EDT-003 | A concurrent-edit conflict MUST show a clear message with reload, compare or copy options; silent last-write-wins MUST NOT occur. | T | H9 |
| UX-EDT-004 | Every drag interaction MUST have a keyboard and single-pointer alternative. | T | WCAG 2.5.7 |
| UX-EDT-005 | Status and workflow state of the edited record MUST be visible while editing. | I | H1 |
| UX-EDT-006 | An action disabled by state or permission MUST say why (UX-CMP-006). | I | H5 |

### 7.7 Multi-step flows

| ID | Statement | Ver. | Basis |
|---|---|---|---|
| UX-WIZ-001 | A multi-step flow MUST show progress and the current step by text. | I | H1 |
| UX-WIZ-002 | Going back MUST NOT lose entered data. | T | H3 |
| UX-WIZ-003 | A review step MUST precede irreversible submission. | T | H5 |
| UX-WIZ-004 | Each step MUST be addressable and resumable. | T | PP |

## 8. Page templates

### 8.1 Template requirements

| ID | Statement | Ver. | Basis |
|---|---|---|---|
| UX-TPL-001 | Every template MUST define its regions, their order, the mobile order, the DOM order, and the states of each region (loading, empty, error, partial). | I | PP |
| UX-TPL-002 | Each view MUST have one dominant element and a clear secondary and tertiary hierarchy. | I | H8 |
| UX-TPL-003 | Equal-weight card grids MUST NOT present content of unequal importance. | I | H8 |
| UX-TPL-004 | A hero or lead module, where used, MUST be a single static module whose text sits in a protected zone (UX-IMG-008) and which has one primary action. | I | PP |
| UX-TPL-005 | Repeated sections MUST keep identical anatomy at every breakpoint; on small screens they stack but lose no required element (UX-DSA-007, UX-DSA-008). | T | PP |
| UX-TPL-006 | A detail page MUST show title, key metadata, main content, related items and a clear next action. | I | H6 |
| UX-TPL-007 | A dashboard MUST show actionable information (what needs attention, what failed, what is due); tiles MUST define loading, empty and error states. | T | H1 |
| UX-TPL-008 | Administrative templates MUST use the same tokens and meet the same accessibility requirements as public templates; they MAY be denser (compact density). | I | PP |
| UX-TPL-009 | Navigation items shown by a template MUST exist (UX-IA-006); a template MUST NOT show modules that are not implemented. | I | PP |
| UX-TPL-010 | Content of a template MUST survive the content limits in 9.2 without loss of required elements. | T | PP |
| UX-TPL-011 | A preview or staging view of non-public content MUST reuse the production template and components and MUST add only a visible banner and controls. | T | PP |

### 8.2 Wireframes (default structures)

**T1 Public landing / home**

```text
┌──────────────────────────────────────────────────────────────────────────┐
│ skip link                                                                │
│ HEADER   brand · primary navigation · search · utilities/menu            │
├──────────────────────────────────────────────────────────────────────────┤
│ HERO / LEAD MODULE   one dominant element, one primary action            │
├──────────────────────────────────────────────────────────────────────────┤
│ SECTION  title · "view all" link                                         │
│   ┌ lead item (dominant) ┐  ┌ supporting item ┐ ┌ supporting item ┐ …    │
│ SECTION  (same anatomy, same status)                                     │
├──────────────────────────────────────────────────────────────────────────┤
│ CALL-TO-ACTION BAND  (optional, one)                                     │
├──────────────────────────────────────────────────────────────────────────┤
│ FOOTER   brand block · link groups · legal row                           │
└──────────────────────────────────────────────────────────────────────────┘
Small screens: one column in DOM order: header, hero, sections…, band, footer.
```

**T2 Listing / search results**

```text
HEADER
PAGE TITLE (h1)  · result count (live region)
┌ filters (panel on md+, disclosure on xs/sm) ┐ ┌ sort · active-filter chips ┐
│ facet groups, clear all                      │ │ result items (cards/rows)  │
│                                              │ │ pagination                 │
└──────────────────────────────────────────────┘ └────────────────────────────┘
FOOTER
```

**T3 Detail / article**

```text
HEADER · breadcrumbs
TITLE (h1) · meta (author, date, category) · lead
MEDIA (figure)
BODY (measure.article)                       │ aside: related · actions (optional)
CONTEXT BOXES (notes, sources, disclaimers)
RELATED ITEMS · next action
FOOTER
```

**T4 Application shell / dashboard**

```text
┌ header: product · global search · notifications · account ┐
├ sidebar navigation ┬ page header: title · primary action  ┤
│ (collapses to menu │ KPI tiles (loading/empty/error each) │
│  on xs/sm)         │ attention list · activity · charts    │
└────────────────────┴──────────────────────────────────────┘
```

**T5 Administrative list + detail**

```text
page header: title · primary action
toolbar: search · filters · bulk-action bar (when rows selected)
table (sortable, selectable)  ── row opens ──▶  detail panel or detail page
pagination · range text
```

**T6 Administrative edit form**

```text
page header: title · status badge · save / publish actions (sticky)
┌ main canvas: grouped fields (single column) ┐ ┌ settings panel ┐
│ error summary (top, on failed save)          │ │ metadata       │
└──────────────────────────────────────────────┘ │ history        │
                                                  └────────────────┘
conflict, unsaved-changes and autosave states are visible here
```

**T7 Authentication**

```text
brand · centred card (max 400 px): heading · form · primary action · secondary links
no navigation competing with the task; errors do not reveal account existence
```

**T8 Error / empty-page**

```text
HEADER · heading explaining what happened · next steps (search, home, retry) · FOOTER
```

## 9. Content and language

### 9.1 Requirements

| ID | Statement | Ver. | Basis |
|---|---|---|---|
| UX-CNT-001 | UI copy MUST use the terminology of a project glossary and the audience's vocabulary. | I | H2, H4 |
| UX-CNT-002 | UI strings MUST NOT be hard-coded in components; all come from the localisation layer. | T | PP |
| UX-CNT-003 | Dates, numbers, currency, plurals and sort order MUST follow the active locale. | T | PP |
| UX-CNT-004 | Layouts MUST tolerate text at least 40 % longer than the source language, compound words and names of unusual length. | T | PP |
| UX-CNT-005 | Titles, labels and metadata MUST have defined maximum lengths and truncation behaviour; truncated text MUST remain fully accessible. | T | WCAG 1.4.4 |
| UX-CNT-006 | A call to action MUST describe its outcome; link text MUST be understandable out of context. | I | H2, WCAG 2.4.4 |
| UX-CNT-007 | Content in sensitive domains (health, legal, finance, safety) MUST show the limitation statements the project parameters demand and MUST NOT imply diagnosis, advice or guarantees the product does not give. | I | PP |
| UX-CNT-008 | Dark patterns (hidden costs, forced continuity, confirmshaming, disguised ads, pre-ticked consent) MUST NOT be used. | I | PP |
| UX-CNT-009 | Missing optional data MUST NOT render placeholder text or break the layout. | T | PP |
| UX-CNT-010 | UI text MUST be written in plain language at the audience's reading level, in active voice, with one sentence per idea. | I | H2 |
| UX-CNT-011 | Error, empty and confirmation texts MUST follow the microcopy patterns in 9.3. | I | H9 |
| UX-CNT-012 | Text MUST NOT be embedded in images unless it is also available as real text. | T | WCAG 1.4.5 |
| UX-CNT-013 | Abbreviations and jargon MUST be explained at first use or avoided. | I | WCAG 3.1.4 |
| UX-CNT-014 | Fixtures and examples MUST use realistic content in the primary language of the product, including long and short cases. | I | PP |

### 9.2 Default content limits (non-normative, overridable; layouts MUST tolerate the effective limits)

| Element | Limit (characters) |
|---|---|
| Page title / headline | 140 |
| Card summary | 200 |
| Navigation label | 24 |
| Button label | 30 |
| Field label | 60 |
| Hint text | 120 |
| Error message | 120 |
| Toast message | 140 |
| Table cell (before truncation) | 80 |
| Alt text (recommended) | 150 |

### 9.3 Microcopy patterns

| Pattern | Rule | Example shape |
|---|---|---|
| Button | Verb + object | "Save draft", "Delete account" |
| Field error | What is wrong + how to fix | "Enter a date in the format 31.12.2026." |
| Summary heading | Fixed, short | "There is a problem" |
| Empty state | Why + next step | "No documents yet. Upload your first document." |
| No match | State the query + offer reset | "No results for “x”. Clear filters." |
| Confirmation dialog | Effect + reversibility; buttons repeat the verb | "Delete 3 files? This cannot be undone." → "Delete files" / "Cancel" |
| Success | What happened + where | "Draft saved. View it in Drafts." |
| Loading (long) | What is happening | "Importing 120 rows…" |
| Permission | Reason without leaking | "You do not have access to this page." |

## 10. Accessibility

| ID | Statement | Ver. | Basis |
|---|---|---|---|
| UX-A11Y-001 | Every surface MUST meet WCAG 2.2 level AA. | T | WCAG |
| UX-A11Y-002 | All functionality MUST be operable by keyboard without traps, with a logical focus order. | T | WCAG 2.1.1, 2.1.2, 2.4.3 |
| UX-A11Y-003 | Keyboard focus MUST be visible using the focus ring specification: 2 px solid `border.focus`, 2 px offset, contrast of at least 3:1 against adjacent colours, applied with `:focus-visible`; where one colour cannot reach 3:1 on all backgrounds a two-tone ring MUST be used. | T | WCAG 2.4.7, 2.4.13 (adopted) |
| UX-A11Y-004 | The focus indicator MUST NOT be removed without an equivalent replacement. | T | WCAG 2.4.7 |
| UX-A11Y-005 | Content shown on hover or focus MUST be dismissible, hoverable and persistent. | T | WCAG 1.4.13 |
| UX-A11Y-006 | Keyboard shortcuts of single characters MUST be configurable, disableable or active only on focus. | T | WCAG 2.1.4 |
| UX-A11Y-007 | Navigation and identically functioning components MUST appear and be named consistently across pages. | I | WCAG 3.2.3, 3.2.4 |
| UX-A11Y-008 | Prerecorded video MUST have captions and audio-only content MUST have a transcript. | I | WCAG 1.2.1, 1.2.2 |
| UX-A11Y-009 | Time limits MUST be avoidable, adjustable or extendable. | T | WCAG 2.2.1 |
| UX-A11Y-010 | Content MUST NOT be restricted to a single orientation or input modality. | T | WCAG 1.3.4, 2.5.1 |
| UX-A11Y-011 | An accessibility defect MUST receive the same severity as a functional defect that blocks the same user task. | I | PP |
| UX-A11Y-012 | Key flows MUST be checked with keyboard and screen reader before release. | D | PP |
| UX-A11Y-013 | Automated accessibility checks MUST run on every component and key page in the automated test suite. | T | PP |
| UX-A11Y-014 | Known accessibility limitations SHOULD be recorded per release with a planned fix. | I | PP |
| UX-A11Y-015 | An accessibility statement MUST be published where law or the project parameters demand it. | I | PP |
| UX-A11Y-016 | Authentication, forms and flows MUST meet WCAG 2.2 criteria 3.3.7 and 3.3.8; drag interactions MUST meet 2.5.7; targets MUST meet 2.5.8; help placement MUST meet 3.2.6; sticky content MUST meet 2.4.11. | T | WCAG 2.2 |

## 11. Performance

| ID | Statement | Ver. | Basis |
|---|---|---|---|
| UX-PRF-001 | Largest Contentful Paint MUST be at most 2.5 s at the 75th percentile. | T | CWV |
| UX-PRF-002 | Interaction to Next Paint MUST be at most 200 ms at the 75th percentile. | T | CWV |
| UX-PRF-003 | Cumulative Layout Shift MUST be at most 0.1 at the 75th percentile. | T | CWV |
| UX-PRF-004 | First-view content SHOULD be available without waiting for client-only scripts where the technology allows it. | I | PP |
| UX-PRF-005 | Critical fonts SHOULD be subset and preloaded; the number of font files MUST be bounded (default ≤ 4). | I | CWV |
| UX-PRF-006 | Third-party scripts and embeds MUST be justified and MUST NOT block first render. | I | CWV |
| UX-PRF-007 | A page-weight budget MUST be set per template (default: ≤ 1.5 MB transferred and ≤ 200 KB compressed JavaScript on first load of content templates) and checked automatically. | T | PP |
| UX-PRF-008 | A budget regression MUST have a recorded reason. | I | PP |
| UX-PRF-009 | Expensive decorative effects (large blurs, parallax, full-viewport video) MUST NOT be used on content templates. | I | PP |
| UX-PRF-010 | Perceived performance MUST be supported by skeletons, immediate feedback (default: within 100 ms) and progress for waits above a threshold (default: 1 s). | T | H1 |

## 12. Consent, privacy and sensitive-data UX

| ID | Statement | Ver. | Basis |
|---|---|---|---|
| UX-PRV-001 | Optional cookies, analytics and third-party embeds MUST stay inactive until consent is given. | T | PP |
| UX-PRV-002 | Rejecting optional consent MUST be as easy as accepting it, and choices MUST be granular and changeable later. | I | PP |
| UX-PRV-003 | Sensitive inputs (health, financial, identity) MUST NOT appear in URLs, previews, thumbnails, share links or analytics events. | T | PP |
| UX-PRV-004 | Before personal data is stored, the UI MUST state what is stored, why and for how long. | I | PP |
| UX-PRV-005 | A preview or staging view of non-public content MUST show a persistent "not public" indicator. | T | PP |
| UX-PRV-006 | Fixtures, screenshots and demos MUST use fictional data only. | I | PP |
| UX-PRV-007 | Results of sensitive calculators or tests MUST be shareable only through an explicit user action. | T | PP |

## 13. Edge cases and error conditions

Each row is a condition that MUST be handled. The required behaviour is binding; values marked *default* may be overridden by a project parameter (UX-PRE-006).

### 13.1 Text and content

| ID | Condition | Required behaviour | Ver. | Basis |
|---|---|---|---|---|
| UX-EDG-001 | A displayed value is empty, whitespace-only or null | The defined fallback is shown or the optional element is omitted. The UI never shows "null", "undefined", "NaN" or an empty gap. | T | PP |
| UX-EDG-002 | A title or label exceeds its limit (9.2) | It wraps up to a defined number of lines, then clamps; the full text stays available through an accessible expansion or a detail view (the `title` attribute alone is not sufficient). | T | WCAG 1.4.4 |
| UX-EDG-003 | An unbreakable string (URL, identifier, e-mail address) is longer than its container | It wraps or breaks inside the container and never causes horizontal overflow. | T | WCAG 1.4.10 |
| UX-EDG-004 | User-generated text contains markup, scripts or control characters | It is rendered as text or sanitised, never executed. | T | PP |
| UX-EDG-005 | Right-to-left text appears in a left-to-right UI, or the reverse | The direction is set per text run (`dir="auto"` or bidirectional isolation); punctuation, numbers and icons are placed correctly. | T | PP |
| UX-EDG-006 | Text contains emoji, combining characters or scripts outside the font coverage | Fallback fonts are defined, no missing-glyph boxes appear, and limits and counters count user-perceived characters. | T | PP |
| UX-EDG-007 | A name has one word, is very long, or contains apostrophes, hyphens, diacritics or non-Latin script | A single full-name field is the default; avatars and initials fall back correctly for single-word and non-Latin names. | I | PP |
| UX-EDG-008 | Several list items have identical titles | Items stay distinguishable by metadata, and actions on them have unique accessible names (for example "Delete *Report*, uploaded 3 May"). | T | WCAG 2.4.6 |
| UX-EDG-009 | A translation is much longer or shorter than the source text | Layouts hold; no component has a hard-coded text width. | T | PP |
| UX-EDG-010 | A count is zero, one, two, few, many or other | Every plural category of each supported locale is supported; sentences are never built by concatenation. | T | PP |
| UX-EDG-011 | Content references an item that was deleted or is unavailable | An "unavailable" state with an explanation is shown; there is no broken link and no raw identifier. | T | PP |
| UX-EDG-012 | A page or document is very long | In-page navigation or anchors exist, headings are navigable, and anchor targets account for sticky elements (`scroll-margin`). | T | WCAG 2.4.6 |

### 13.2 Layout and viewport

| ID | Condition | Required behaviour | Ver. | Basis |
|---|---|---|---|---|
| UX-EDG-020 | Viewport is 320 CSS px wide or zoom is 400 % | Everything reflows, including dialogs, menus and toasts; nothing is off-screen. | T | WCAG 1.4.10 |
| UX-EDG-021 | User sets text size up to 200 % | No clipping, overlap or loss of function. | T | WCAG 1.4.4 |
| UX-EDG-022 | The viewport is short (landscape phone, split screen) | Sticky elements (header, banners, bars) together occupy at most 20 % (default) of the viewport height; otherwise they become static. | T | WCAG 2.4.11 |
| UX-EDG-023 | The on-screen keyboard is open | The focused input stays visible; fixed bars do not cover it; sizing accounts for dynamic browser interface (visual viewport, dynamic viewport units). | T | WCAG 2.4.11 |
| UX-EDG-024 | The screen is very wide (≥ 1920 px or ultrawide) | Content is constrained to the maximum width; lines are not stretched. | T | PP |
| UX-EDG-025 | The device has a notch, safe-area insets or a fold | Content stays inside safe areas; layout adapts when the posture changes. | T | PP |
| UX-EDG-026 | The window is resized or the device rotated while an overlay or form is open | State is preserved and the overlay is repositioned. | T | WCAG 1.3.4 |
| UX-EDG-027 | A classic scrollbar appears or disappears | No layout shift (scrollbar gutter reserved). | T | CWV |
| UX-EDG-028 | Scroll containers are nested | Nesting is avoided; where unavoidable, the inner container is keyboard-scrollable and causes no scroll trap. | T | WCAG 2.1.1, 2.1.2 |
| UX-EDG-029 | Focus or an anchor target would sit behind a sticky element | The scroll offset keeps it visible. | T | WCAG 2.4.11 |
| UX-EDG-030 | A flex or grid child contains long content | Minimum-size and overflow rules prevent the layout from blowing out. | T | PP |
| UX-EDG-031 | A page is printed | Where a project parameter requires print, a print style exists; otherwise content stays legible, controls and navigation are hidden and nothing is clipped. | T | PP |

### 13.3 Input, forms and validation

| ID | Condition | Required behaviour | Ver. | Basis |
|---|---|---|---|---|
| UX-EDG-040 | A browser or password manager autofills a field | Styles stay legible and validation accepts the filled value; no input mask may break autofill. | T | WCAG 1.3.5 |
| UX-EDG-041 | A value has leading or trailing whitespace, or exceeds `maxlength` | Whitespace is trimmed where meaning is unchanged. Input is never silently truncated; over-length is reported. | T | PP |
| UX-EDG-042 | Text is being composed with an input method editor | Validation, autocomplete and key handlers do not act during composition. | T | PP |
| UX-EDG-043 | Numbers use a decimal comma, group separators or non-Latin digits | Input is accepted per locale, the expected format is shown, and parse errors are explained. | T | PP |
| UX-EDG-044 | A date is ambiguous (01/02), 29 February, a month end, or out of range | An unambiguous format with an example is shown, and errors are specific. | T | PP |
| UX-EDG-045 | The input is an identifier or code (postal code, phone number, card number) | A text input with `inputmode` is used instead of a spinner number input; leading zeros are preserved and spacing is tolerated. | T | PP |
| UX-EDG-046 | An e-mail address, phone number or postal address comes from any country | No country-specific format is assumed unless a project parameter says so; validation is lenient on format and strict on required content. | I | PP |
| UX-EDG-047 | A character counter is shown | It counts as the server does, announces politely only at thresholds, and never blocks paste (it shows an over-limit state). | T | PP |
| UX-EDG-048 | A form is submitted twice (double click, Enter plus click, slow network) | The effect happens once and the UI shows a pending state. | T | H5 |
| UX-EDG-049 | The user presses Back, refreshes or restores a tab mid-form | Entered data is restored where feasible or the user is warned; a destructive request is never resent silently. | T | H3 |
| UX-EDG-050 | The same record is open in two tabs | The second save is handled as a conflict (UX-EDT-003). | T | H9 |
| UX-EDG-051 | The session expires during editing or submission | Data is kept on the client and offered again after re-authentication. | T | WCAG 2.2.1 |
| UX-EDG-052 | A field is conditional on another | Hidden fields are excluded from validation and submission; revealed fields are announced; values survive toggling back. | T | WCAG 4.1.3 |
| UX-EDG-053 | The server rejects a value the client accepted | The server message is mapped to the field and the summary, never only a generic failure. | T | WCAG 3.3.1 |
| UX-EDG-054 | A password is set | Rules are shown beforehand, paste is allowed, and the maximum length is at least 64 characters (default). | I | PP |
| UX-EDG-055 | A select has zero, one or thousands of options | Zero: explanation and action. One: preselected or shown as text. Many: searchable combobox with a virtualised list. | T | PP |
| UX-EDG-056 | Autosave fails or the device is offline | The failure is visible and a local draft, scoped to user and record, is kept. | T | H1 |
| UX-EDG-057 | A long form is cleared or reset | The user confirms or can undo. | T | H3 |

### 13.4 Interaction, timing and state

| ID | Condition | Required behaviour | Ver. | Basis |
|---|---|---|---|---|
| UX-EDG-060 | An action is activated repeatedly (double click, key repeat) | It is idempotent; destructive actions are confirmed. | T | H5 |
| UX-EDG-061 | An earlier request returns after a later one | Only the result of the latest request is shown; stale responses are discarded. | T | PP |
| UX-EDG-062 | The user leaves while a request, upload or job is pending | The user is warned if data would be lost; background jobs continue and their status stays retrievable. | T | H3 |
| UX-EDG-063 | A response is faster than the loading indicator | The indicator appears only after 200 ms (default) and, once shown, stays at least 400 ms (default). | T | PP |
| UX-EDG-064 | Data changes while the user is viewing or editing it | No layout jump under pointer or focus; changes are announced politely; edits are not overwritten. | T | WCAG 4.1.3 |
| UX-EDG-065 | The user's role or permissions change during a session | The UI updates on the next request; actions fail safely with an explanation; no stale enabled control remains. | T | PP |
| UX-EDG-066 | A record is deleted or archived by someone else while open | A clear message and a path back are shown; unsaved changes remain available to copy. | T | H9 |
| UX-EDG-067 | A batch or multi-step operation partly succeeds | Results are reported per item and only failed items are retried. | T | H9 |
| UX-EDG-068 | An automatic retry fails | Retries are bounded (3, default) with back-off; the user can retry manually; the error persists after the last attempt. | T | PP |
| UX-EDG-069 | The device supports touch, mouse and keyboard together | Modality is not inferred from device type; hover-only features have equivalents. | T | WCAG 2.1.1 |
| UX-EDG-070 | A press is moved away from the control before release | The action fires on release only and can be aborted. | T | WCAG 2.5.2 |
| UX-EDG-071 | A drag is cancelled with Escape or dropped outside a target | The item returns to its origin and no data changes. | T | WCAG 2.5.7 |
| UX-EDG-072 | A function is triggered by motion (shake, tilt) | An equivalent control exists and the motion trigger can be disabled. | T | WCAG 2.5.4 |
| UX-EDG-073 | A feature needs long press or hover | A touch- and keyboard-accessible alternative exists. | T | WCAG 1.4.13 |
| UX-EDG-074 | The focused element is removed (row deleted, panel closed, toast dismissed) | Focus moves to the next logical element or the container heading, never to the document body. | T | WCAG 2.4.3 |
| UX-EDG-075 | The route changes in a client-rendered flow | Focus moves to the new `h1` or main region, the title updates, the change is announced, and scroll resets (or is restored on Back). | T | WCAG 2.4.2, 4.1.3 |
| UX-EDG-076 | A hash or anchor targets collapsed or hidden content | The content is expanded and the target is focusable. | T | PP |
| UX-EDG-077 | A counter exceeds its display width ("99+") | The accessible name carries the full number or "more than 99". | T | WCAG 1.1.1 |
| UX-EDG-078 | A notification arrives while a dialog is open or a field is focused | It is queued, does not steal focus and is announced politely. | T | WCAG 3.2.1 |
| UX-EDG-079 | Time-dependent text (countdown, "5 minutes ago") updates | Focus is not lost; assistive technology is not flooded (coarse live-region updates); an absolute time is available. | T | WCAG 4.1.3 |
| UX-EDG-080 | An idle timeout approaches | A warning appears at least 60 s (default) before, with an option to extend. | T | WCAG 2.2.1 |

### 13.5 Data, lists and numbers

| ID | Condition | Required behaviour | Ver. | Basis |
|---|---|---|---|---|
| UX-EDG-090 | There are zero items, one item, exactly one page, or a page-size boundary | Counts, pagination and empty states are correct and singular forms are used ("1 result"). | T | PP |
| UX-EDG-091 | A list has 1,000 items or more | Server-side paging, filtering and sorting or virtualisation is used; virtualised lists keep keyboard navigation, `aria-setsize`/`aria-posinset` and an alternative to find-in-page. | T | APG |
| UX-EDG-092 | The displayed count differs from the items available (permissions, filters, stale cache) | The count states its scope ("12 of 40 you can access") or is omitted. | I | H1 |
| UX-EDG-093 | Sorted values are null, mixed case, accented, or numbers inside strings | Locale-aware collation, a defined null position and natural numeric order where labelled. | T | PP |
| UX-EDG-094 | A filter yields no results | A "no match" state shows the active filters and a one-step clear. | T | H1 |
| UX-EDG-095 | The user navigates Back or shares a link with filters, sort and page | State is restored from the URL; a selection is cleared on filter change with a notice. | T | PP |
| UX-EDG-096 | A selection spans several pages | The scope is explicit ("3 selected on this page" / "Select all 120") and bulk actions state their scope. | T | H5 |
| UX-EDG-097 | A number is very large, very small, negative, zero or needs rounding or units | It is formatted per locale with its unit, aligned consistently, and never shown in exponent notation unless scientific. | T | PP |
| UX-EDG-098 | A ratio or percentage has a zero denominator | A dash or explanation is shown, never NaN or infinity. | T | PP |
| UX-EDG-099 | A chart has no data, one point, outliers, thousands of points or identical values | Each case has a defined state; aggregation or sampling is disclosed; truncated axes are marked. | T | PP |
| UX-EDG-100 | A table has many columns | Column priority or user-controlled hiding applies; a sticky first column and keyboard-accessible horizontal scroll with a visible hint are provided. | T | WCAG 1.4.10 |
| UX-EDG-101 | A table cell overflows (long text, many tags) | It is clamped with accessible expansion. | T | PP |
| UX-EDG-102 | Hierarchical data is deeply nested | Displayed depth is limited, and expand/collapse follows the tree-view pattern. | I | APG |

### 13.6 Internationalisation, time and format

| ID | Condition | Required behaviour | Ver. | Basis |
|---|---|---|---|---|
| UX-EDG-110 | A time is scheduled or shared across zones | The zone is shown explicitly; storage is in UTC; display uses the user's or the configured zone. | T | PP |
| UX-EDG-111 | A daylight-saving transition affects a time | Non-existent local times are rejected with guidance; ambiguous local times require the user to choose. | T | PP |
| UX-EDG-112 | Week start, calendar, number system, address or name format differs by locale | Formats follow the locale; no hard-coded Monday or Gregorian calendar unless a project parameter says so. | T | PP |
| UX-EDG-113 | A translation is missing | The source language is shown and marked with a language attribute; translation keys are never shown. | T | WCAG 3.1.2 |
| UX-EDG-114 | A language switcher is present | Languages are named in their own language (no flags as language); switching preserves location and state and updates the page language. | T | PP |
| UX-EDG-115 | The UI is mirrored for right-to-left | Directional icons (arrows, chevrons, progress) mirror; clocks, media controls and logos do not; layout uses logical properties. | T | PP |
| UX-EDG-116 | Search or sort meets diacritics, case or non-Latin scripts | Locale collation and tolerant search apply. | T | PP |
| UX-EDG-117 | A relative date ("yesterday") is shown | The absolute date is also available in a `time` element. | T | WCAG 1.3.1 |
| UX-EDG-118 | A currency or unit is converted | The source and the time of the rate are shown. | I | H1 |

### 13.7 Accessibility and assistive technology

| ID | Condition | Required behaviour | Ver. | Basis |
|---|---|---|---|---|
| UX-EDG-120 | Forced-colours (high-contrast) mode is active | The UI stays usable: controls keep borders, focus indicators remain visible, icons use system colours, and no information exists only in background images. | T | WCAG 1.4.11 |
| UX-EDG-121 | The user prefers more contrast | Where the project enables a high-contrast mode it is applied automatically; otherwise the default already meets the contrast rules. | T | WCAG 1.4.3 |
| UX-EDG-122 | Live regions fire often | Announcements are throttled and de-duplicated; decorative changes are not announced. | T | WCAG 4.1.3 |
| UX-EDG-123 | Voice-control users address a control | The visible label text is contained in the accessible name. | T | WCAG 2.5.3 |
| UX-EDG-124 | Switch or sequential-access devices are used | Skip links and landmarks reduce steps; there are no tab stops inside long static content. | T | WCAG 2.4.1 |
| UX-EDG-125 | Bot protection is required | A non-cognitive method or an accessible alternative is provided. | T | WCAG 3.3.8 |
| UX-EDG-126 | Content is shown in an iframe or embed | It has a title, is keyboard-operable, has no trap, and an alternative exists if it is not accessible. | T | WCAG 4.1.2 |
| UX-EDG-127 | A document is generated for download (PDF, spreadsheet) | It has an accessible structure (tags, headers) or an accessible alternative is offered. | I | PP |
| UX-EDG-128 | A tooltip or hover content is needed on touch | An alternative opens on tap, is dismissible without moving the pointer, and does not obscure the trigger. | T | WCAG 1.4.13 |
| UX-EDG-129 | Animated media is present (GIF, Lottie, video loops) | It is paused or static under reduced motion and can be paused by the user. | T | WCAG 2.2.2 |
| UX-EDG-130 | A magnification user changes the viewport | Messages and changes appear near the trigger or focus, or are announced and moved into view. | T | WCAG 1.4.10 |
| UX-EDG-131 | An image is complex (chart, diagram, infographic) | A long description or data table is available adjacent to it. | I | WCAG 1.1.1 |

### 13.8 Media, files and uploads

| ID | Condition | Required behaviour | Ver. | Basis |
|---|---|---|---|---|
| UX-EDG-140 | An image fails, is slow, is blocked, is huge, has the wrong aspect ratio or is transparent | A fallback, reserved space, focal-point cropping and a defined background apply. | T | CWV |
| UX-EDG-141 | An image has an extreme aspect ratio (panorama, very tall) | It is cropped by focal point in the layout and the full image is viewable on request. | T | PP |
| UX-EDG-142 | A file is empty, has the wrong type, an extension that does not match its content, is too large, is a duplicate, or exceeds the file count | It is checked on the client for feedback and on the server for authority; the message is per file and other files are unaffected. | T | PP |
| UX-EDG-143 | An upload is cancelled or the network drops | The upload is resumable or cleanly cancelled; no half-ready item is shown as complete. | T | H1 |
| UX-EDG-144 | File processing is pending or failed after upload | Status (processing, ready, failed) is visible; failed items can be retried or removed; dependent actions are blocked with a reason. | T | H1 |
| UX-EDG-145 | A download link is shown | File type, size and open behaviour are stated; large downloads show progress or a warning. | T | WCAG 2.4.4 |
| UX-EDG-146 | Captions are missing for video | Publication is blocked or a clear note is shown; controls remain keyboard-operable. | I | WCAG 1.2.2 |
| UX-EDG-147 | An embed is blocked by consent | A placeholder explains what is blocked and how to allow it; the layout is reserved. | T | PP |
| UX-EDG-148 | Inline SVG or icons come from an untrusted source | They are sanitised before rendering. | I | PP |

### 13.9 Network, loading and failure

| ID | Condition | Required behaviour | Ver. | Basis |
|---|---|---|---|---|
| UX-EDG-160 | The device is offline | Offline state is detected; cached read-only content is marked; writes are queued or blocked with a message; recovery is automatic with a notice. | T | H1 |
| UX-EDG-161 | The network is slow | Skeletons and progressive rendering apply; timeouts are defined; the user can cancel. | T | H1 |
| UX-EDG-162 | A request times out | A timeout message distinct from a server error is shown, with retry. | T | H9 |
| UX-EDG-163 | The server returns an error class | Validation → field messages. 401 → re-authenticate and return to context. 403 → forbidden page. 404 → not-found page. 429 → "try again in *n* seconds". 5xx → retry without internals. | T | H9 |
| UX-EDG-164 | One of several independent sections fails | That section shows its own error and the rest stays usable. | T | UX-DSA-003 |
| UX-EDG-165 | Decision-critical data is older than its freshness threshold | A last-updated time and a refresh control are shown. | I | H1 |
| UX-EDG-166 | A new version is deployed while the user has the old client | The mismatch is detected and a reload is offered without data loss. | T | PP |
| UX-EDG-167 | A third-party service (maps, fonts, analytics, embeds) is down | The page stays usable and degrades gracefully; no blocking spinner. | T | CWV |
| UX-EDG-168 | Scripts fail or are disabled | Critical content and navigation remain readable where the technology allows; otherwise a clear message replaces a blank page. | T | PP |
| UX-EDG-169 | Maintenance mode is active | A message with the expected return time is shown and writes are blocked. | T | H1 |
| UX-EDG-170 | Rate limiting applies (for example repeated login attempts) | The message and cooldown are shown without revealing whether an account exists. | T | PP |

### 13.10 Overlays, focus and stacking

| ID | Condition | Required behaviour | Ver. | Basis |
|---|---|---|---|---|
| UX-EDG-180 | A dialog would open from inside a dialog | Not allowed; content is replaced or a step is used. Only a confirmation alertdialog may sit above one dialog. | T | PP |
| UX-EDG-181 | Dialog content is taller than the viewport | The body scrolls while header and actions stay reachable; the page behind does not scroll. | T | WCAG 1.4.10 |
| UX-EDG-182 | A popover or menu is near a viewport edge or inside a scroll container | It repositions (flip, shift), is never clipped, and follows or closes with its trigger on scroll and resize. | T | PP |
| UX-EDG-183 | A toast would appear while a dialog is open | It appears above the scrim without trapping focus, or is queued until the dialog closes; critical information goes into the dialog. | T | PP |
| UX-EDG-184 | A consent banner or cookie dialog appears on load | It causes no layout shift, does not block the skip link or keyboard access, and is modal only if it must block content. | T | CWV |
| UX-EDG-185 | Elements are rendered in portals, iframes or shadow trees | Tab order stays logical and overlays keep their position in the focus order. | T | WCAG 2.4.3 |
| UX-EDG-186 | Escape is pressed with nested widgets (combobox inside a dialog) | The innermost widget closes first. | T | APG |
| UX-EDG-187 | The scrim is clicked | Only non-critical dialogs close; a dialog with unsaved data asks for confirmation. | T | H3 |
| UX-EDG-188 | Several tooltips or popovers could be shown | At most one is visible; opening one closes the others. | T | PP |
| UX-EDG-189 | An overlay locks page scroll | No layout shift occurs from scrollbar removal and the scroll position is restored on close. | T | CWV |
| UX-EDG-190 | The browser Back button is pressed with an overlay open | A deep-linkable overlay closes first; otherwise Back never leaves the page unexpectedly. | T | H3 |

### 13.11 Modes and system preferences

| ID | Condition | Required behaviour | Ver. | Basis |
|---|---|---|---|---|
| UX-EDG-200 | Dark mode is enabled | Elevation is expressed by surface lightness; images with white backgrounds are handled; large pure-black surfaces are avoided; status contrast is re-verified. | T | WCAG 1.4.3 |
| UX-EDG-201 | The system theme changes or the page loads | The theme updates without reload and without a flash of the wrong theme (preference applied before first paint). | T | PP |
| UX-EDG-202 | The system sets reduced motion, reduced transparency, more contrast, forced colours or a colour scheme | Each preference is honoured, or the unsupported ones are listed in the project parameters. | T | WCAG 2.3.3 |
| UX-EDG-203 | The user sets browser zoom, OS text scale or a minimum font size | The layout stays intact and no text container has a fixed pixel size. | T | WCAG 1.4.4 |
| UX-EDG-204 | A browser translation tool is used | Logic never depends on visible text; brand names and code use `translate="no"`; layout tolerates the changed text. | I | PP |
| UX-EDG-205 | Cookies or local storage are blocked | Non-essential preferences degrade gracefully; a message appears where storage is required for function. | T | PP |
| UX-EDG-206 | A theme and a mode combine | They remap tokens only; status, focus and destructive meaning stay unchanged (UX-TOK-008). | T | PP |

## 14. Restrictions

### 14.1 Prohibited patterns

| ID | Prohibited | Use instead | Ver. | Basis |
|---|---|---|---|---|
| UX-RES-001 | A disabled primary submit button without explanation | Keep it enabled and validate on submit, or state the reason. | I | H5 |
| UX-RES-002 | Placeholder text as the only label | A persistent visible label. | I | WCAG 3.3.2 |
| UX-RES-003 | Colour as the only carrier of meaning | Icon, text or shape in addition. | T | WCAG 1.4.1 |
| UX-RES-004 | Hover-only menus or actions | Click, tap and keyboard activation. | T | WCAG 1.4.13 |
| UX-RES-005 | Clickable `div`/`span`, nested interactive elements | Native button or link. | T | WCAG 4.1.2 |
| UX-RES-006 | Disabling browser zoom or text scaling | Allow zoom to at least 200 %. | T | WCAG 1.4.4 |
| UX-RES-007 | Fixed viewport-height containers that clip content | Minimum heights and flexible layout. | T | WCAG 1.4.10 |
| UX-RES-008 | Autoplay with sound; auto-rotating carousels by default; moving content without a pause control | Static content or user-started media. | I | WCAG 1.4.2, 2.2.2 |
| UX-RES-009 | Scroll-jacking, custom scroll physics, content that must be scrolled to be readable (parallax) | Native scrolling. | T | WCAG 2.3.3 |
| UX-RES-010 | History or Back-button hijacking; unexpected new windows without notice | Normal navigation and a stated new-window indicator. | T | H3 |
| UX-RES-011 | A modal on page load (except legally required consent, legal or age gates); more than the defined modal depth | Inline notices, banners, or sequential steps. | I | H3 |
| UX-RES-012 | Auto-focus that steals focus on load | Auto-focus only the first field of a single-purpose page or a dialog. | T | WCAG 3.2.1 |
| UX-RES-013 | A change of context on focus or input (auto-submit on select change, auto-advance) without warning | An explicit submit control or a stated behaviour. | T | WCAG 3.2.1, 3.2.2 |
| UX-RES-014 | Silent truncation of user input or of critical displayed data (amounts, warnings, legal text) | Show in full or provide accessible expansion. | T | H1 |
| UX-RES-015 | CAPTCHA that requires a cognitive test | A non-cognitive method or an alternative. | T | WCAG 3.3.8 |
| UX-RES-016 | Infinite scroll as the only path, or a footer that cannot be reached | Pagination or explicit "load more". | T | PP |
| UX-RES-017 | Text embedded in images without real text | Real text. | T | WCAG 1.4.5 |
| UX-RES-018 | Justified body text | Start-aligned text. | I | PP |
| UX-RES-019 | Essential information in disabled-style or low-contrast text | Meet UX-COL-001. | T | WCAG 1.4.3 |
| UX-RES-020 | Icon-only controls with unfamiliar icons and no visible label | Visible label or tooltip plus accessible name. | I | WCAG 2.5.3 |
| UX-RES-021 | Hiding primary navigation behind a menu button at widths where it fits | Show the navigation inline from the width where it fits (project parameter P11). | I | H6 |
| UX-RES-022 | Full-page takeovers, interstitials or entry pop-ups without immediate dismiss | Inline, dismissible, non-blocking messages. | I | H3 |
| UX-RES-023 | Custom controls that replace native ones without equivalent semantics and keyboard support (selects, checkboxes, scrollbars) | Native controls or APG-conformant widgets. | T | APG |
| UX-RES-024 | Emoji or Unicode symbols as icons without text alternative; icon fonts | SVG icons with accessible names. | I | WCAG 1.1.1 |
| UX-RES-025 | Unsanitised user HTML, inline event handlers, user-supplied CSS | Structured, sanitised content. | T | PP |
| UX-RES-026 | Time-limited actions without warning or extension | Warning and extension (UX-EDG-080). | T | WCAG 2.2.1 |
| UX-RES-027 | Generic link text ("click here", "read more" alone) | Descriptive text or visually hidden context. | I | WCAG 2.4.4 |
| UX-RES-028 | Pre-selected consent or pre-checked marketing opt-ins | Unchecked by default. | T | PP |
| UX-RES-029 | The `title` attribute as the only tooltip or accessible name | Visible text or an accessible tooltip component. | T | WCAG 4.1.2 |
| UX-RES-030 | Removing the focus outline without replacement | Focus ring specification (UX-A11Y-003). | T | WCAG 2.4.7 |
| UX-RES-031 | Visual reordering that breaks DOM order | Layout that keeps DOM order (UX-LAY-004). | T | WCAG 1.3.2 |
| UX-RES-032 | Fixed pixel font sizes, absolute positioning for layout, `!important` or inline style overrides in components | Tokens and the layout system. | I | PP |
| UX-RES-033 | z-index values outside the scale | Scale tokens (UX-SHP-001). | T | PP |
| UX-RES-034 | Hiding required content on small screens to make it fit | Adapt the layout (UX-DSA-008). | T | PP |
| UX-RES-035 | Placeholder content (lorem ipsum, "TBD", sample data) in released UI | Real or fixture-free content. | I | PP |
| UX-RES-036 | A component used with other semantics than designed (a button styled as a link for navigation) | The correct element (UX-SEM-001). | T | WCAG 4.1.2 |
| UX-RES-037 | Raw technical identifiers, stack traces or unexplained error codes shown to users | Plain-language message with an optional reference code. | T | H9 |
| UX-RES-038 | Meaning or state conveyed only by sound, vibration or animation | Visible, textual equivalents. | T | WCAG 1.3.3 |
| UX-RES-039 | Auto-translating or auto-redirecting by locale without a way to change it | Suggest, don't force, and keep a visible language switcher. | I | H3 |
| UX-RES-040 | Dark patterns | See UX-CNT-008. | I | PP |

### 14.2 Default limits (non-normative, overridable; project parameter P33)

| Item | Default limit | Related rule |
|---|---|---|
| Top-level navigation items | 7 | UX-IA-013 |
| Navigation depth | 3 levels | UX-IA-001 |
| Open modals at once | 1 (plus one confirmation alertdialog) | UX-EDG-180 |
| Stacked toasts | 3 | UX-C12 |
| Primary buttons per view region | 1 | UX-C01 |
| Steps in a multi-step flow | 7 | UX-WIZ-001 |
| Tabs per tab list | 7 | UX-C16 |
| Breadcrumb items before collapse | 4 | UX-C19 |
| Items in an action menu before grouping or search | 12 | UX-C15 |
| Page sizes offered | 10, 25, 50, 100 (maximum 100) | UX-DAT-002 |
| Filter chips shown before overflow | 5 | UX-DAT-004 |
| Visible lines of a card summary before clamp | 3 | UX-C22 |
| Search input debounce | 300 ms | UX-C31 |
| Loading indicator delay / minimum display | 200 ms / 400 ms | UX-EDG-063 |
| Toast duration | 8 s | UX-C12 |
| Idle-timeout warning before expiry | 60 s | UX-EDG-080 |
| Automatic retries | 3 | UX-EDG-068 |
| Sticky elements, share of viewport height | 20 % | UX-EDG-022 |
| Password maximum length accepted | ≥ 64 characters | UX-EDG-054 |

## 15. Quality, verification and acceptance

### 15.1 Review gate

A UI change MUST be rejected if any of the following is true.

| ID | Rejection criterion | Ver. |
|---|---|---|
| UX-QLT-001 | A visible control has no working destination or action (UX-IA-003). | T |
| UX-QLT-002 | A component lacks an applicable state (UX-CMP-004). | T |
| UX-QLT-003 | Raw colour, spacing or type values appear outside token files (UX-TOK-002). | T |
| UX-QLT-004 | Text or controls overlap, clip or become unreachable at 320 px, at 200 % zoom or with long labels (UX-LAY-002, UX-CNT-004). | T |
| UX-QLT-005 | Keyboard users cannot reach, operate or see focus on every control (UX-A11Y-002, UX-A11Y-003). | T |
| UX-QLT-006 | Status, error or selection is conveyed by colour alone (UX-COL-005). | T |
| UX-QLT-007 | A form error lacks its inline message, its summary entry or the preserved input (UX-FRM-006, UX-FRM-007, UX-FRM-009). | T |
| UX-QLT-008 | Reading order differs from visual order (UX-LAY-003). | T |
| UX-QLT-009 | A user-facing string is hard-coded in a component (UX-CNT-002). | T |
| UX-QLT-010 | A pattern forbidden by the project parameters appears, or a mandatory module is missing. | I |
| UX-QLT-011 | A component does not follow its catalogue entry (section 6.2). | T |
| UX-QLT-012 | A visual-regression baseline was updated without a recorded reviewer decision. | I |
| UX-QLT-013 | A finding of the highest severity from a design review is unresolved. | I |
| UX-QLT-015 | An applicable edge case in section 13 is unhandled, or a prohibited pattern in section 14 is present. | T |

| ID | Statement | Ver. |
|---|---|---|
| UX-QLT-014 | Visual-regression baselines SHOULD exist for key templates at the verification widths and in each enabled mode. | I |

### 15.2 Definition of done

A UI slice is done only when it:

| ID | Criterion | Ver. |
|---|---|---|
| UX-DOD-001 | follows the approved template, hierarchy and tokens. | I |
| UX-DOD-002 | reuses design-system components or documents why a new one is needed (UX-DSA-016). | I |
| UX-DOD-003 | implements loading, empty, error, long-content and missing-media states. | T |
| UX-DOD-004 | passes the accessibility requirements of section 10. | T |
| UX-DOD-005 | works at all verification widths, in each enabled mode, density, locale and direction. | T |
| UX-DOD-006 | meets the performance budget. | T |
| UX-DOD-007 | takes all text from the localisation layer with locale-aware formatting. | T |
| UX-DOD-008 | exposes stable test identifiers (UX-SEM-010) and has automated tests. | T |
| UX-DOD-009 | has been checked with realistic content, not placeholder text only. | D |
| UX-DOD-010 | has no unresolved review findings. | I |
| UX-DOD-011 | has been checked against the applicable edge cases in section 13 and the prohibitions in section 14. | T |

### 15.3 Agent workflow for a UI slice

| ID | Statement | Ver. |
|---|---|---|
| UX-AGT-001 | Before implementing, an agent MUST read the relevant sections of this contract and the filled project parameters. | I |
| UX-AGT-002 | An agent MUST search existing components, tokens, patterns, fixtures and route helpers before creating new ones. | I |
| UX-AGT-003 | An agent MUST classify each element (link, action, field, display, composition) and name its owning component. | I |
| UX-AGT-004 | An agent MUST define inputs, events and states before writing markup. | I |
| UX-AGT-005 | An agent MUST add or update tests and fixtures with the slice. | I |
| UX-AGT-006 | An agent MUST run the checks in 15.5 and report the results with evidence. | I |
| UX-AGT-007 | An agent MUST ask the user when this contract and the project parameters do not answer a question. | I |

### 15.4 Acceptance criteria

```gherkin
Feature: Navigation integrity                                   # UX-IA-003
  Scenario: Every visible destination resolves
    Given any released page
    When each visible link, button and menu item is activated
    Then it reaches a working destination or performs its action

Feature: Tokens only                                            # UX-TOK-002, UX-TOK-004
  Scenario: No raw design values in components
    Given the component source
    When it is scanned for raw colours, spacing and shadows
    Then none are found outside the token files

Feature: Form error handling                                    # UX-FRM-006, UX-FRM-007, UX-FRM-009
  Scenario: Failed submit
    Given a form with required fields left empty
    When the user submits
    Then an error summary with one linked entry per error is shown above the page heading
    And keyboard focus is in the error summary
    And each invalid field shows an inline message with the same wording
    And all entered values are preserved

Feature: Keyboard operation                                     # UX-A11Y-002, UX-A11Y-003, UX-LAY-006
  Scenario: Complete a key flow without a pointer
    Given a key flow
    When a user completes it using the keyboard only
    Then every control is reachable and operable
    And the focused element is always visible and never fully covered

Feature: Overlay focus management                               # UX-CMP-011, UX-C13
  Scenario: Modal dialog
    Given a focused trigger button
    When the user opens a modal dialog and later presses Escape
    Then focus moves into the dialog and stays inside while it is open
    And focus returns to the trigger when it closes

Feature: Reflow and zoom                                        # UX-LAY-002, UX-TYP-006
  Scenario: Narrow viewport
    Given a viewport of 320 CSS px at 200 % zoom
    When any page is displayed
    Then no content is clipped or overlapping
    And no horizontal scrolling is required, except in data tables and maps

Feature: System states                                          # UX-CMP-005, UX-STA-004..006
  Scenario: Remote data component
    Given a component that renders remote data
    When the request is pending, returns no items, or fails
    Then a skeleton, an explanatory empty state, or an error message with retry is shown respectively

Feature: Reduced motion                                         # UX-MOT-002
  Scenario: User prefers reduced motion
    Given the user has set reduced motion
    When any non-essential animation would run
    Then it is removed or reduced

Feature: Contrast                                               # UX-COL-001..003
  Scenario: Token pairings
    Given every text, border and icon token with the surface it is used on
    When contrast is calculated in each enabled mode
    Then text meets 4.5:1 (3:1 for large text) and control boundaries and icons meet 3:1

Feature: Consent                                                # UX-PRV-001, UX-PRV-002
  Scenario: First visit
    Given a visitor who has not given consent
    When any page loads
    Then no optional cookie, analytics or third-party embed is active
    And rejecting optional consent takes no more steps than accepting it

Feature: Bulk actions                                           # UX-DAT-007
  Scenario: One selected record has changed state
    Given several selected records and one has changed state since selection
    When the user runs a bulk action
    Then the current state of every record is re-validated
    And the failure of that record is reported individually
    And no record is skipped silently

Feature: Concurrent edit                                        # UX-EDT-003
  Scenario: Stale save
    Given two users editing the same record
    When the second user saves after the first
    Then the save is rejected with a conflict message offering reload, compare or copy
    And the first user's changes are not overwritten

Feature: Out-of-order responses                                 # UX-EDG-061
  Scenario: Slow first request
    Given a search that sends two requests in quick succession
    When the first response arrives after the second
    Then only the result of the second request is shown

Feature: Focus after removal                                    # UX-EDG-074
  Scenario: Delete a list row
    Given the keyboard focus is on a delete control in a row
    When the row is deleted
    Then focus moves to the next row, or to the list heading if none remains

Feature: Offline                                                # UX-EDG-160
  Scenario: Connection lost while editing
    Given a user editing a form
    When the connection is lost
    Then the UI says it is offline, keeps the entered data and blocks or queues the save
    And it recovers automatically with a notice when the connection returns

Feature: Precedence of defaults                                 # UX-PRE-002, UX-PRE-003
  Scenario: Brand colour fails contrast
    Given a project brand colour that fails 4.5:1 as text on the page surface
    When the colour is applied to text
    Then a compliant derived variant is used instead
```

### 15.5 Verification methods

| Concern | Method |
|---|---|
| Token compliance (UX-TOK, UX-QLT-003) | Static scan for raw values; token build check; contrast calculation for every token pairing in each mode |
| Component behaviour (UX-CMP, section 6.2) | Component tests for keyboard, semantics, states, variants; gallery render of every state |
| Accessibility, automated (UX-A11Y-013) | Rule-based checks in component and end-to-end tests |
| Accessibility, manual (UX-A11Y-012) | Keyboard and screen-reader script for key flows |
| Responsive (UX-LAY) | Screenshots at verification widths, 200 % zoom, landscape |
| Performance (UX-PRF) | Lab budget on every change; field data where available |
| Content resilience (UX-CNT) | Fixtures with long, short, missing and multi-locale content |
| Edge cases (section 13) | Scenario tests per applicable row (network conditions, concurrency, focus, locale, viewport), plus gallery fixtures |
| Restrictions (section 14) | Static analysis and review checklist; automated checks where the pattern is detectable |
| Heuristic review (UX-QLT) | The checklist in 15.6, per major flow |

### 15.6 Review heuristics

| # | Check |
|---|---|
| 1 | Is the system status visible at every moment? |
| 2 | Does the wording match the user's vocabulary? |
| 3 | Can the user back out of every action? |
| 4 | Is everything consistent with itself and with conventions? |
| 5 | Are errors prevented before they happen? |
| 6 | Is information visible rather than recalled? |
| 7 | Does the UI serve both novices and experts? |
| 8 | Is everything shown necessary? |
| 9 | Are errors explained in plain language with a fix? |
| 10 | Is help short, searchable and task-focused? |

## 16. References

- WCAG 2.2, new success criteria (W3C WAI): https://www.w3.org/WAI/standards-guidelines/wcag/new-in-22/
- WCAG 2.2, Understanding 2.4.13 Focus Appearance: https://www.w3.org/WAI/WCAG22/Understanding/focus-appearance.html
- WAI-ARIA Authoring Practices Guide, patterns: https://www.w3.org/WAI/ARIA/apg/patterns/
- Design Tokens Format Module 2025.10 (W3C Design Tokens Community Group): https://w3c.github.io/cg-reports/design-tokens/CG-FINAL-format-20251028/
- 10 Usability Heuristics (Nielsen Norman Group): https://www.nngroup.com/articles/ten-usability-heuristics/
- GOV.UK Design System, Error summary: https://design-system.service.gov.uk/components/error-summary/
- Core Web Vitals (web.dev): https://web.dev/articles/vitals
- ISO/IEC/IEEE 29148:2018, Requirements engineering
- ISO/IEC 25010:2023, Product quality model

## 17. Project parameters (to be completed by the agent)

Procedure: (1) read the business-requirements document and fill every parameter it answers, set Status to *from requirements* and name the source; (2) ask the user each remaining question, one at a time, using the question text below; (3) if the user accepts the default, copy it and set Status to *default*; (4) record the answer in **Value** and set Status to *confirmed*. A filled value overrides the default in this document. Parameters that stay *open* keep the default.

### 17.1 Parameters

| # | Parameter | Question the agent asks | Check first in the business requirements | Default if unanswered | Value | Status |
|---|---|---|---|---|---|---|
| P01 | Product type and audience | What are you building (public site, web application, admin tool, mixed) and who uses it? | Goals, user groups, scope | Mixed: public + application + admin | | open |
| P02 | Key journeys and success criteria | Which three to five tasks must users complete without help? | Use cases, user stories | None; derive from requirements | | open |
| P03 | Brand identity | What are the brand name, logo assets and usage rules? Are approved assets available? | Brand, stakeholder notes | Neutral wordmark in the functional typeface | | open |
| P04 | Personality and tone | Which five words describe the look and feel, and what must it never feel like? | Brand, positioning | Calm, clear, trustworthy; never dense or decorative | | open |
| P05 | Colour | What are the brand primary, secondary and accent colours, or should neutral defaults with the placeholder brand scale be used? | Brand | Default neutral scale and placeholder brand scale (4.2) | | open |
| P06 | Modes | Are dark mode and high-contrast mode required? | Non-functional requirements | Light only (UX-CON-006) | | open |
| P07 | Typefaces | Which typefaces (functional, reading, brand), licences and weights? | Brand | System sans-serif stack, no reading or brand face | | open |
| P08 | Languages and direction | Which languages and locales, which is primary, and is right-to-left needed? | Localisation, markets | One language, left-to-right (UX-CON-007) | | open |
| P09 | Devices and browsers | Which devices, browsers and minimum viewport must be supported? Touch, keyboard, mouse? | Technical constraints | Current evergreen browsers; 320 px minimum; touch, keyboard and mouse | | open |
| P10 | Density | Is a compact density needed for data-heavy users? | User groups, admin features | Comfortable only; compact for admin tables | | open |
| P11 | Navigation model | What are the main destinations, their labels and the footer and utility links? | Information architecture, sitemap | Derive from requirements; max 7 top-level items (UX-IA-013) | | open |
| P12 | Template inventory and modules | Which templates from section 8 are needed, and which page modules are mandatory or forbidden? | Page list, functional requirements | T1 to T8 as needed; none forbidden | | open |
| P13 | Routes and slugs | What are the canonical routes and the slug rules per content type? | Content model, SEO | Content-driven slugs, redirect on change (UX-IA-004) | | open |
| P14 | Content types | Which content types are displayed (articles, products, cases, documents …) and with which fields? | Content or data model | Derive from requirements | | open |
| P15 | Roles and personas | Which roles exist and which templates and navigation does each see? | Roles, permissions | Guest, user, editor, administrator | | open |
| P16 | Imagery | What image style, aspect ratios, credit and rights display and focal-point needs apply? | Brand, content | Aspect ratios in UX-IMG-005; credit shown beneath the image | | open |
| P17 | Icons | Is there an approved icon set or style? | Brand | One open, consistently styled 24 px set | | open |
| P18 | Motion personality | Should motion be calm, lively or minimal? Any restrictions? | Brand, accessibility | Calm, default tokens (4.7) | | open |
| P19 | Sensitive content | Which topics need limitation statements or special handling (health, legal, finance …) and with what wording? | Legal, risk | Neutral limitation statement; no data in URLs (UX-PRV-003) | | open |
| P20 | Consent and analytics | Which cookies, analytics and third-party embeds are allowed, and what is the consent policy? | Privacy requirements | None active before consent (UX-PRV-001) | | open |
| P21 | Accessibility target | Is WCAG 2.2 AA a baseline or a formal acceptance gate? Is an accessibility statement required? | Non-functional and legal requirements | AA baseline; statement where law requires | | open |
| P22 | Performance budgets | What load and weight budgets apply per template? | Non-functional requirements | UX-PRF-001 to UX-PRF-007 defaults | | open |
| P23 | Data visualisation | Are charts, maps or dashboards needed, and with which chart types and palette? | Reporting requirements | Component UX-C33 with default data palette | | open |
| P24 | Authentication and accounts | Which sign-in, registration, multi-factor and account-management screens are needed? | Security and account requirements | Email and password with UX-AUT rules | | open |
| P25 | Notifications | Which in-app messages, emails or other notifications exist and how are they shown? | Functional requirements | Patterns in 7.3 | | open |
| P26 | Search and filters | What can users search and filter, and how are results ranked and shown? | Functional requirements | Template T2, UX-DAT rules | | open |
| P27 | Print, export, offline | Are print styles, exports or offline use needed? | Functional requirements | None | | open |
| P28 | Legal and footer content | Which legal pages and footer links are mandatory, with which labels? | Legal requirements | Imprint, privacy, terms, cookie settings where applicable | | open |
| P29 | Third-party content | Which embeds, social links, video or maps are used, and are the accounts confirmed? | Content, marketing | None (UX-ICO-007) | | open |
| P30 | Component scope | Which catalogue components (6.2) are needed in the first release, and which are later? | Scope, release plan | All except UX-C30 (carousel) and UX-C33 (chart) | | open |
| P31 | Documentation tooling | Where is the component gallery hosted and who maintains it? | Team, tooling | In the repository, built with the project | | open |
| P32 | Reference examples | Are there wireframes, designs or reference sites to follow? Please share links or files. | Attachments | None | | open |
| P33 | Behaviour thresholds and limits | Do any of the default limits and timings (14.2, 9.2) need to change, for example toast duration, retries, idle warning, navigation size, page sizes? | Non-functional requirements, user research | Defaults in 14.2 and 9.2 | | open |

### 17.2 Brand primitives (replace the placeholder scale in 4.2)

| Role | Token | Value | Contrast checked | Source |
|---|---|---|---|---|
| Brand primary | `color.brand.primary` | | | |
| Brand secondary | `color.brand.secondary` | | | |
| Brand accent | `color.brand.accent` | | | |
| Neutral overrides (if any) | `color.neutral.*` | | | |
| Data palette (≥ 6 categorical) | `color.data.1…n` | | | |

### 17.3 Project-specific rules

| ID | Type (mandatory module · forbidden pattern · rejection criterion · template rule) | Statement | Ver. | Source |
|---|---|---|---|---|
| UX-PRJ-001 | | | | |
| UX-PRJ-002 | | | | |
| UX-PRJ-003 | | | | |

Rules added here are numbered `UX-PRJ-nnn`, follow the same statement form as section 5 to 15, and extend the review gate (15.1).

### 17.4 Open questions log

| # | Question | Raised by | Answer | Date | Status |
|---|---|---|---|---|---|
| | | | | | open |
