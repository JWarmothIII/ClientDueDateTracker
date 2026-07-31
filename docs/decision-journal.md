# Decision Journal

This journal records important product and engineering decisions for Client Due Date Tracker. It
captures why a direction was chosen, what it affects, and what still needs to be resolved.

Add new entries at the top of the journal so the latest decision is easy to find. Do not rewrite an
older decision when the direction changes. Add a new entry and mark the older decision as
`Superseded`.

## Entry format

```markdown
## YYYY-MM-DD - Decision title

**Status:** Proposed | Accepted | Superseded

**Decision:** A short statement of the direction being taken.

**Context:** The problem, constraints, and alternatives that shaped the decision.

**Rationale:** Why this direction was chosen.

**Consequences:** The expected benefits, costs, and tradeoffs.

**Follow-ups:** Work or decisions that still need to happen.
```

---

## 2026-07-30 - Finalize V1 operational behavior and first usable workflow

**Status:** Accepted

**Decision:** The domain foundation ships with a usable dashboard, minimal atomic onboarding,
rolling generation, quick completion/reopening, and one privacy-safe daily notification summary.

**Context:** The earlier entries established ownership and persistence direction but left several
product interpretations open. Implementing the first real schema and workflow required one
consistent definition of correction, replacement, recurrence, display, and permission behavior.

**Rationale:** Completing the operational loop now proves the architecture against real writes,
queries, background work, and UI states instead of leaving a package-only scaffold.

**Consequences:**

- Requirements may be reopened; reopening clears `completedAt`. Generated and ad hoc requirements
  may be permanently deleted after confirmation. A deleted generated occurrence writes its opaque
  key to a suppression ledger.
- Exited clients remain as history. Confirming exit records actual date and planned/unplanned kind.
  Correcting an exit reactivates the contract, clears actual-exit data, and retains planned exit.
  Permanent client deletion remains separate.
- Used contract types and templates are versioned through replacement. Each replacement selects
  future clients only or future and active clients. Existing occurrences never migrate. Explicit
  deactivation stops future generation but preserves occurrence history.
- Generation maintains a rolling 30-day horizon. Source-date and trigger-event corrections change
  only pending, non-customized generated work. Completed or manually customized work is preserved.
- Shared trigger/deadline instructions form one requirement. CTS second billing cycle is the end of
  the month following intake. USPO assessment is scheduled assessment plus 10 days and its original
  plan is due at the end of the actual assessment-completion month. State / RSUD has separate Day 5
  treatment-plan and Day 8 first-PRT requirements.
- Ninety-day reviews anchor to the last plan completion. A plan-change event creates an immediate
  review and restarts the cycle. Version-1 business-day adjustment excludes weekends only.
- The dashboard upcoming window is seven days. It shows every overdue item, five recently
  completed items, and the three most recently updated pinned notes. Requirement rows can complete
  and reopen; pinned notes are display-only in this change.
- Notification lead defaults to three days. One lead reminder is delivered, then incomplete
  overdue work is eligible once per day. One unique worker targets approximately 10:00 AM local
  time and posts a single generic summary.
- First launch explains notifications before Android 13+ permission is requested. Denial is
  respected and the dashboard links to Android application settings. Notification content never
  includes initials, titles, dates, notes, or client-specific details.
- Client initials are trimmed and uppercased while punctuation is retained. Scheduled assessment
  is required; actual completion is optional. Assessment and exit dates cannot precede intake, and
  actual completion cannot be future-dated.
- Notes allow multiple pins, require nonblank content and a client, may reference a requirement
  owned by that client, support confirmed permanent deletion, and have no V1 export.

**Follow-ups:**

- Run the device acceptance walkthrough for notification grant and denial on an Android 13+
  emulator.
- Add holiday calendars only if clinic workflow requires more than weekend adjustment.

## 2026-07-25 - Treat the requirement matrix as customizable configuration

**Status:** Accepted

**Decision:** CTS, USPO, and State/RSUD provide the initial contract types and requirement
templates, but the matrix is seed data rather than hard-coded Kotlin behavior. Amanda can customize
templates and individual generated requirements.

**Context:** Contract requirements differ substantially and may change. They include fixed
deadlines, recurring work, calendar-boundary rules, event-triggered work, and procedural
instructions. The initial matrix is:

| Requirement category | CTS | USPO | State / RSUD |
| --- | --- | --- | --- |
| Intake Paperwork | Complete within 10 days of arrival or referral receipt; three SOW-specific forms | For in-house clients, complete before assessment | Complete before assessment |
| Assessment | Complete within 14 calendar days of intake; write and submit for approval within 7 days | Complete within 10 days of assessment | ASAM and BIO, with signatures, due by the end of Day 2 in the facility |
| Original Treatment Plan | Complete by the second billing cycle | Due by month-end and submitted with billing | Due within 5 days of entry and brought to PRT |
| Treatment Plan Reviews | Every 90 days and whenever the plan changes | Every 90 days and submitted with billing | Weekly and brought to PRT |
| Monthly Progress Reports | Due at month-end or the next business day; billing submitted to Megan | Due by end of day on the first of the month or the next business day | Not applicable |
| Termination / Discharge Summary | Due 14 days before a planned exit or within 14 days after an unplanned exit | Triggered by receipt of termination Prob 45; send to agent within one week and submit with billing | Due within 3 calendar days after exit; email to probation officer and include the success plan |
| Case Notes | Complete within 7 days of seeing the client | Complete in InSync within 7 days | Complete in InSync within 7 days |

**Rationale:** Configurable definitions let the app follow real clinic workflows without requiring a
code release when paperwork rules change.

**Consequences:**

- A matrix row is a category and may produce multiple templates when actions have distinct
  deadlines or completion events.
- Procedural instructions remain together unless Amanda needs to complete them independently.
- Used templates are immutable. Editing one deactivates it and creates a replacement for future
  occurrences.
- A generated requirement snapshots its title, instructions, due date, and notification lead.
- Amanda may customize those snapshot fields for one occurrence without affecting its template or
  recurrence series.
- Ad hoc requirements are allowed, so a requirement's template reference is optional.
- The rule engine must support configurable offsets, recurrences, calendar boundaries, manual
  event anchors, and manual due dates. Contract-specific rules must not be hard-coded.

**Follow-ups:**

- Finalize the serializable deadline-policy shape and stable rule codes.
- Decide how far ahead recurring occurrences are materialized.
- Decide the seeded template split for matrix cells with multiple deadlines.
- Clarify billing-cycle inputs and the exact meaning of ambiguous source wording.

## 2026-07-25 - Model requirements as independent occurrences

**Status:** Accepted

**Decision:** Each scheduled or event-triggered instance of paperwork is a separate Requirement
occurrence with its own due date, state, completion time, and notification history.

**Context:** Weekly, monthly, and 90-day requirements must preserve prior completed and overdue
instances. Reusing one row would erase that history.

**Rationale:** Occurrence rows make dashboard queries, completion history, overdue behavior, and
notification idempotency explicit.

**Consequences:**

- Requirement status stores only `PENDING` or `COMPLETED`.
- Due-soon and overdue are calculated from a pending occurrence's due date and the current date;
  they are not persisted statuses.
- An occurrence edit affects only that occurrence.
- Amanda records manual trigger events such as a client interaction, treatment-plan change,
  termination notice, or billing event; the owning use case generates the resulting occurrences.
- "Next business day" treats Monday through Friday as business days in version 1. Holidays are not
  automatically excluded.
- Intake-relative rules use `Contract.startDate`.
- Automatic lock-screen notifications use generic wording and do not reveal initials, requirement
  names, dates, or note content.

**Follow-ups:**

- Define the occurrence key used to prevent duplicate generation.
- Define the dashboard upcoming window separately from per-occurrence notification lead time.
- Define recurrence generation and retry behavior for WorkManager.

## 2026-07-25 - Make notes a client-owned feature

**Status:** Accepted

**Decision:** Notes are a first-class write-owning feature for free-form therapist entries about
client interactions, meetings, paperwork, or other client context. Every note belongs to a client
and may optionally reference a requirement.

**Context:** Notes are not limited to requirement-template guidance. Template instructions remain
on RequirementTemplate, while Amanda's entries have an independent lifecycle and may be pinned.

**Rationale:** Client ownership matches the general use of notes better than requiring every entry
to be about a contract or a piece of paperwork.

**Consequences:**

- `NOTES.client_id` is required and `requirement_id` is optional.
- The Notes domain verifies that a referenced requirement belongs to the same client's contract.
- Initials remove a direct name field, but unrestricted free text may still contain identifying or
  sensitive information; the architecture must not claim that all stored notes are de-identified.
- Version 1 relies on the device lock, Android application sandbox, and platform storage
  protection. It does not add an app-specific lock or database encryption.
- Android's default backup and device-transfer behavior remains enabled. In this project,
  "local-only" means no application server, external API, analytics upload, or app-controlled sync;
  it does not guarantee that data never participates in Android backup.

**References:**

- [HHS guidance on de-identification of health information](https://www.hhs.gov/hipaa/for-professionals/special-topics/de-identification/index.html)
- [Android Auto Backup guidance](https://developer.android.com/identity/data/autobackup)

**Follow-ups:**

- Decide whether pinned notes appear on the dashboard.
- Define note retention, deletion, and any export behavior before those capabilities are added.
- Ensure public product wording describes the chosen privacy boundary accurately.

## 2026-07-25 - Use four write-owning domains and two consuming features

**Status:** Accepted

**Decision:** Use these business-feature boundaries:

```text
domain/client/                owns clients
domain/contractdefinition/    owns contract types and requirement templates
domain/contracttracking/      owns contracts and requirement occurrences
domain/notes/                 owns therapist notes
domain/dashboard/             consumes read APIs and owns no tables
domain/notification/          consumes APIs and owns no business tables
```

**Context:** The ERD contains reusable definitions, live operational records, and notes with a
separate purpose. Dashboard and notification behavior combine information without owning the
source records.

**Rationale:** Ownership follows business capabilities rather than screens, individual tables, or
global technical layers.

**Consequences:**

- All domains continue to use one physical Room database.
- Cross-domain access is allowed only through the owning domain's explicit `api` package.
- Stable IDs, read models, and narrow query or command interfaces may cross a boundary.
- Room entities, DAOs, mappers, repository implementations, and UI types may not cross a boundary.
- A workflow spanning domains lives in the feature that owns the user outcome and uses a `usecase`
  package only when the coordination is real.
- Writes that establish one invariant succeed or fail through one Room transaction. Retryable
  side effects such as notification delivery stay outside the transaction.

**Follow-ups:**

- Define the exact owner APIs and read models.
- Define the database transaction-runner interface.
- Add automated checks for package dependencies and cycles.

## 2026-07-25 - Keep domain slices lean inside one application module

**Status:** Accepted

**Decision:** Keep the existing `app` Gradle module and organize each domain with only the role
packages it needs:

```text
api/       stable cross-domain surface, only when needed
model/     framework-free business types and rules
data/      Room types, DAOs, mappings, and repository implementations
ui/        ViewModels, UI state, and Compose UI
usecase/   multi-step or cross-domain workflows, only when justified
```

Business-neutral code uses focused top-level packages such as `app`, `database`, `designsystem`,
and `platform`. Do not create generic `shared` or `core` dumping grounds.

**Context:** Separate Gradle modules would provide compiler-enforced boundaries but add
disproportionate build, Hilt, and navigation complexity for the current application.

**Rationale:** Feature-first packages provide clear ownership while keeping the project lightweight.

**Consequences:**

- Package rules are enforced with automated architecture tests as well as documentation.
- No domain may import another domain's `data` or `ui` package.
- The exact architecture-test library remains an implementation choice; ArchUnit's JUnit 4
  integration is the current candidate.

**Follow-ups:**

- Add the chosen architecture-test dependency and boundary tests.
- Rewrite the architecture and screen-convention docs around these package semantics.

## 2026-07-25 - Separate business models from Room models

**Status:** Accepted

**Decision:** Each owning domain has framework-free business models and separate Room entities,
with explicit mappings in that domain's `data` package.

**Context:** Deadline rules, statuses, identifiers, recurrence, and lifecycle invariants are richer
than the storage primitives in the ERD.

**Rationale:** Separating representations keeps Room schema decisions out of business rules without
reintroducing global DDD-lite layers.

**Consequences:**

- Business models and APIs use typed IDs such as `ClientId`, `ContractId`, and `RequirementId`.
  Room primary and foreign keys remain `Long`.
- Calendar fields use `LocalDate`.
- Audit, completion, and notification moments use UTC `Instant`.
- Room stores dates and timestamps as sortable numeric epoch values.
- Room entities never appear in UI state or owner APIs.
- Mappers and their failure behavior require tests.

**Follow-ups:**

- Define validation and mapping behavior for corrupt or unknown stored rule values.
- Define create-command types so unsaved business inputs do not require fake IDs.

## 2026-07-25 - Establish the client and contract lifecycle

**Status:** Accepted

**Decision:** A client has exactly one contract, and onboarding creates the client, contract, and
initial requirement occurrences atomically.

**Context:** One contract per client is the actual clinic workflow. A partial client record is not
useful to the application.

**Rationale:** The model should express the real workflow rather than support speculative contract
history.

**Consequences:**

- `CONTRACTS.client_id` remains unique.
- `CLIENTS.assessment_date` is required; the client cannot be created or persisted without it.
- `Contract.startDate` is the intake date used by intake-relative rules.
- A future exit date may be recorded while the contract remains active.
- A contract becomes inactive only when Amanda explicitly confirms the exit; background work does
  not deactivate it automatically.
- The schema still needs separate planned-versus-actual exit semantics to support CTS rules for
  planned and unplanned exits.

**Follow-ups:**

- Finalize the contract exit fields and confirmation command.
- Define whether and how an incorrectly confirmed exit can be corrected.

## 2026-07-25 - Replace the disposable Room schema with the real foundation

**Status:** Accepted

**Decision:** Remove the persistence-test table and its stale schema snapshots. Export the revised
business schema as the canonical Room version 1 instead of carrying development-only migrations.

**Context:** The existing database contains no production information and the exported version 2
snapshot is an orphan from an earlier spelling/package cleanup.

**Rationale:** The first real application schema should start with an accurate baseline rather than
preserve disposable proof-of-persistence history.

**Consequences:**

- Existing development installations must be cleared or reinstalled.
- No destructive-migration fallback ships in version 1.
- Ownership cascades delete a client's contract graph and a contract's requirement occurrences.
- Referenced contract definitions are restricted from deletion; used templates are replaced rather
  than mutated.
- Deleting a requirement preserves a client note and clears its optional requirement reference.
- The original ERD must be revised for required assessment dates, client-owned notes, optional
  template references, customizable occurrence snapshots, recurrence anchors, and planned/actual
  exit behavior.

**Follow-ups:**

- Produce the revised ERD before implementing Room entities.
- Finalize foreign-key indices, uniqueness rules, and event/occurrence columns.

## 2026-07-25 - Build the full domain foundation in the first architecture change

**Status:** Accepted

**Decision:** The first implementation includes the architecture reorganization, revised Room
schema, framework-free models, owner APIs, mappings, repository implementations, architecture
tests, and public documentation updates.

**Context:** A package-only scaffold would leave the most important data and ownership decisions
untested, while complete UI workflows would be too broad before their screens are planned.

**Rationale:** Implement enough behavior to prove the domain boundaries and persistence model, then
build screens and detailed workflows in focused feature stories.

**Consequences:** Existing dashboard code must continue to compile, but new production UI is not
part of this foundation unless required for integration.

**Follow-ups:**

- Decide the initial repository operation surface.
- Define unit, Room integration, migration-baseline, and architecture test scenarios.
- Update `README.md`, `docs/architecture.md`, `docs/screen-conventions.md`, and
  `docs/developer-reference.md` together.

## 2026-07-25 - Organize the application by business domain

**Status:** Accepted

**Decision:** Use a domain-oriented architecture in which each business domain is treated as a
feature boundary. Do not organize the application around a layered DDD-lite architecture.

**Context:** The project needs an architecture that keeps the code for a business capability
together. A previous architecture change moved toward a layered DDD-lite structure; that commit
was removed before it was shared.

**Rationale:** Domain-oriented feature boundaries should make related UI, state, business rules,
and data access easier to find and evolve together.

**Consequences:** The package structure, dependency rules, shared-code policy, architecture
documentation, and screen conventions must be reviewed against this direction before the
architecture is implemented.

**Follow-ups:**

- Define the initial business domains and their responsibilities.
- Decide the internal structure used within each domain.
- Define how domains communicate without creating circular dependencies.
- Decide what belongs outside a domain and when code may be shared.
- Plan the migration from the current package structure.
