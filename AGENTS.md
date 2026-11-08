# FinFlow repository instructions

## Start and finish each development session

1. Read this AGENTS.md and [PROJECT_STATUS.md](PROJECT_STATUS.md) before meaningful development. Reuse instructions/context already loaded in this session unless there is evidence they changed. Use the latest checkpoint and feature/issue status to resume; do not repeat completed work or the initial repository assessment.
2. Read applicable nested instructions and preserve unrelated changes. Use Git only when it answers a concrete change-review/checkpoint question; it is not a prerequisite for starting or answering a request. Read only task-relevant classes, dependencies and documentation: [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) for affected workflows, [docs/DECISIONS.md](docs/DECISIONS.md) for relevant decisions, and [DATABASE_DESIGN.md](DATABASE_DESIGN.md) for persistence changes. Avoid repository-wide scans unless a concrete unresolved question requires one.
3. When `.codegraph/` exists, use CodeGraph **before** searching or reading implementation code. Use `codegraph_explore` with this repository's absolute `projectPath`, or `codegraph explore "<symbols or question>"`. Trace controller -> service -> repository/entity and integrations; class existence and comments do not prove implementation.
4. Treat code and executed checks as evidence. Existing `docs/02-hld`, `docs/03-lld`, and `docs/SystemDesign` include target designs and educational material, not a completion checklist.
5. Natural language is the default; apply the relevant .agents/skills/ workflow without requiring a command. `$start-project`, `$save-progress` and the other skill names remain optional shortcuts.

## Conversational development

- Treat "Let's start working" or "Continue where we stopped" as a read-only startup: use the latest documented status/checkpoint, identify the last active services, and briefly explain the stopping point and next task. Focus on a named service; otherwise use documented context to identify a clear target and ask which service when unclear. Recommend work without automatically implementing it.
- Interpret informal feature/bug requests as business requirements. Identify affected services, inspect relevant existing behaviour before proposing changes, and check dependencies, API/event contracts and database impact. Do not repeat implemented work (e.g. transfers already exist); identify the requested gap. Never invent missing business rules; ask one useful clarification question at a time only when needed.
- Work on one logical task at a time. For significant features/fixes, investigate first, then briefly explain the proposed solution, important files, tests and database/API/cross-service impact. Pause for approval of the concrete scope/proposal before significant implementation unless already approved. Significant includes money/security behaviour, schema/contract changes and substantial cross-service work. Small, clear, low-risk changes need no extra approval ritual. Existing approval persists; do not ask again for the same scope.
- Implement the approved task, review the exact changed files and relevant dependencies, propose targeted tests that compile the affected code and run them only after user confirmation, check for obvious regressions, and automatically update progress under the rules below. Avoid unnecessary abstractions, dependencies and incidental refactoring.
- Treat "Let's switch to notification-service" (or another service) as a context switch: preserve previous service work/evidence, load the new service's relevant instructions/status, and inspect dependencies only when required. A switch alone does not authorize implementation; track all affected services for joint work.
- For "Explain this code", "Why did we use this approach?" or how/why questions, teach in simple English using actual project examples: what it does, why it helps and what could happen without it. Distinguish established reasons from inference/unknown intent. Answer without incidental code edits or unnecessary theory.
- "What should we implement next?" requests a recommendation, not implementation. "Save today's work", "Let's stop here", "That's enough for today" and "Finish for today" request a save-progress handoff: stop starting new work, inspect actual changes, record completed/unfinished work, services, tests, blockers and next task, then give a short summary. Saving means documentation, not staging/committing; preserve unfinished edits.
- Keep responses conversational and concise. Reuse documented facts and earlier investigation; do not repeat long explanations or broad scans. For these rules, a clear request to implement a task carries that task into investigation/proposal, while generic start/switch/explain/save requests retain their narrower scope.

## Multi-service task scope

- Identify the service(s) for each task before exploring code; do not assume all five are in scope. Use [docs/SERVICE_CATALOG.md](docs/SERVICE_CATALOG.md) for ownership and [docs/SERVICE_DEPENDENCIES.md](docs/SERVICE_DEPENDENCIES.md) for relevant callers/providers.
- Read applicable nested AGENTS.md files before service work. Root rules currently cover all services, including gateway-specific reactive requirements; create nested instructions only for genuinely distinct requirements, never identical copies.
- When switching services, keep completed work and verification evidence, load only the new service's instructions/relevant dependencies, and maintain the session's affected-service list. Cross-service API/event changes require checking the affected producer/consumer contract, not every module.
- Canonical skills are in .agents/skills/ for Codex/Cursor. See [docs/DEVELOPMENT_WORKFLOW.md](docs/DEVELOPMENT_WORKFLOW.md) for daily usage and Claude Code compatibility; do not add duplicate skill trees or a competing CLAUDE.md.

## Progress documentation

- After every meaningful completed development task, update PROJECT_STATUS.md overall and the affected service sections with actual completed/partial/pending progress, service names, important changed files/components and responsibilities, tests executed with exact commands/results (or not run and why), blockers and the next logical task. Preserve unaffected service progress; repository-only setup can be recorded as shared work.
- Make a small update to affected status entries and a concise dated checkpoint. Record important files/components, executed checks/results, remaining work and the next task. Include Git baseline/uncommitted details when relevant and already known or needed for a save/review; do not run Git solely to fill a routine progress entry. Preserve earlier verification evidence and unresolved issues; do not present historical tests as newly executed.
- Update docs/ARCHITECTURE.md only when architecture or component workflows materially change; docs/SERVICE_DEPENDENCIES.md only when communication/dependencies change; docs/SERVICE_CATALOG.md only when service ownership/technology/entry points change; docs/DECISIONS.md only for a significant technical decision; DATABASE_DESIGN.md only for relevant persistence changes.
- After a meaningful task is completed and verified, record progress immediately rather than waiting for end of day. If verification is blocked, record partial work and the actual blocker. Do not rewrite documentation unnecessarily or update it after each tiny intermediate edit. Routine progress updates are part of the task: perform them without interrupting implementation for permission. Do not invent progress, results or decisions.

- Skills describe reusable procedures; PROJECT_STATUS.md records task progress. Automatically refine an existing skill only when a concrete workflow improvement is established, not after every implementation task. Preserve the five existing skills and natural-language invocation.

## Efficient source inspection and verification

- Do not run git status, git diff or Git history for routine questions, explanations, greetings or normal session startup. Use Git when genuinely needed to review changes, save a checkpoint or investigate unexpected edits; reuse useful results instead of repeating them. Git output never substitutes for reading actual source.
- For implementation, identify the affected service/files and read the actual relevant classes, methods, configuration, tests and dependencies before edits. Trace execution and directly affected contracts where needed; class existence does not establish working behavior. CodeGraph's current verbatim source counts as source inspection; do not immediately re-read the same code without a reason.
- Review the exact changed implementation after a meaningful task automatically; execute targeted tests only after explicit user confirmation. A Maven test invocation already compiles main/test code; do not prepend a separate compile unless it resolves a specific concern. Broaden to a service suite/verify only for scope, failures or unresolved integration/packaging concerns. Do not repeat passed checks without new changes, failures or unresolved evidence.
- Reuse established project/session facts and keep each task focused. Avoid unrelated-service/repository scans and unnecessary subagents; use them only when explicitly requested or required by applicable instructions. Report actual commands/results and verification limits concisely.

## Actual technology baseline

- Five independent Maven projects; no root aggregator or shared application module.
- Java 21; Spring Boot parent 3.5.6 in every service.
- Gateway: Spring Cloud BOM 2025.0.3, Gateway WebFlux, reactive Redis, OAuth2 resource server, Resilience4j, Actuator/Prometheus/Brave, optional Lombok (narrow use).
- Other services: Spring MVC, Security, Data JPA, Jakarta Validation, MySQL runtime, H2 tests; JJWT 0.12.6. Identity uses Redis; commerce/notification use optional Spring Kafka.
- Compose pins MySQL 8.4, Redis 7.4-alpine, Kafka 3.8.1. Do not infer resolved library versions from prose; inspect the POM/dependency tree when needed.
- Verify Java selection: this workstation defaults to Java 8. `finflow-env.sh` shows the installed Java 21 location. Do not change global Java settings.

## Boundaries and coding conventions

- Preserve `com.finflow.apigateway`, `com.finflow.identity`, `com.finflow.wallet`, `com.finflow.commerce`, and `com.finflow.notification` package roots.
- Follow current four-space Java formatting, explicit imports, constructor injection, PascalCase types, camelCase members, uppercase enum/constants, and records for request/response DTOs.
- api-gateway may use Lombok narrowly (`@Getter`/`@Setter` on configuration properties, `@RequiredArgsConstructor` for single-dependency beans, `@Slf4j` for loggers). Do not use `@Data`.
- identity-service may use Lombok for `@Getter` on entities/`ApiException` and `@Slf4j` for loggers only. Keep **explicit constructors** for Spring injection (no `@RequiredArgsConstructor`) so dependencies stay visible while learning. Do not use `@Data`. Other services stay Lombok-free unless explicitly approved.
- Controllers handle HTTP, validation, caller extraction and DTO mapping. Services orchestrate use cases and local transaction boundaries. Repositories remain Spring Data interfaces. Keep entity invariants near domain data.
- Identity owns identities/memberships/tokens/audit; wallet owns balance/history/transfers; commerce owns products/carts/orders/returns/payment work; notification owns inbox records. Do not access another service's database.
- Preserve `PaymentPort` and its conditional adapters and `CommerceEventPublisher` implementations. Do not add a second competing payment path.
- Keep wallet HTTP outside the commerce placement transaction. Review cancellation/settlement/expiry races before changing workflow boundaries.
- Gateway is reactive: avoid blocking work there. Do not impose gateway Redis abstractions on MVC services.
- Do not delete legacy placeholder types or rename packages as incidental cleanup.

## APIs, security and data

- Preserve `/api/v1` paths, DTO field names, `Idempotency-Key`, and current error codes unless a contract change is explicitly in scope.
- Obtain caller identity from authenticated principals. Enforce permissions **and** ownership/organisation access. Never trust client owner IDs.
- Current JWT permissions are a union across memberships; wallets are user-owned, not organisation-owned. These are documented limitations, not a basis to invent tenant guarantees.
- Use Jakarta request validation and domain checks; follow existing `ApiException` and controller-advice patterns. Security-filter errors are separate from MVC advice.
- Use `BigDecimal` for money. Wallet uses scale 2, commerce scale 4; explicitly address conversion and rounding on integration changes. Never introduce float/double money.
- Preserve existing table/column names, unique constraints and optimistic versions. Verify actual JPA associations; scalar ID fields do not create foreign keys.
- Current schema management is Hibernate `ddl-auto=update`; no Flyway/Liquibase baseline exists. Do not silently introduce migrations, destructive DDL, production schema changes, or test database configuration changes.
- Never log passwords, bearer/refresh tokens, or raw verification/reset/invite tokens in new production paths. Existing mail stubs log raw tokens and remain an open issue.
- Redis keys must use owner prefixes (`identity:`, `gateway:`; `wallet:` is reserved, not implemented).

## Test execution requires user confirmation

- The user has explicitly requested that tests run only after confirmation. This overrides automatic test-execution instructions in skills or earlier checkpoints. Automatically review source and propose the relevant test scope, then wait for explicit authorization before executing unit/integration tests, Postman test runs, or Maven phases that execute tests. Approval to implement a change alone does not authorize tests.
- Once a test scope is explicitly approved, execute within that scope without asking again unnecessarily. Until then, record tests as not run / awaiting confirmation; do not describe source review as test verification or promote historical passes to current results.

## Testing and scope control

- After implementation, propose relevant targeted tests under Java 21 and execute them only after user confirmation, e.g. `mvn -B -Dtest=RelevantTest test` in the affected service. Use `mvn -B test` for the affected suite or `mvn -B verify` when scope/packaging warrants it. Report actual results and limits; never claim tests passed without running them. `-o` requires cached dependencies; there is no root Maven build.
- Existing tests use JUnit Jupiter/Spring Boot/H2; wallet-adapter tests use `MockRestServiceServer`. Preserve test isolation and add focused behavioural regressions for changed business behaviour, especially replay, access control and transaction failure paths.
- Inspect what each test actually exercises: service-level tests may bypass HTTP/security filters, while dedicated HTTP tests may include them. Passing a suite proves only its exercised paths, not MySQL concurrency, Redis limits, Kafka delivery or Docker startup. Use appropriate integration checks when those concerns change.
- Do not replace failing assertions with disabled tests or switch real integrations to mocks merely to obtain green results. Distinguish environmental failures from application failures.
- Mockito needs temporary agent files on this Windows environment; sandbox denial was resolved by an approved unsandboxed rerun. Packaging failures are recorded in status; do not alter build configuration as an incidental workaround.
- Keep changes focused; do not modify unrelated files or recreate/refactor services incidentally. Introduce dependencies or architectural changes only with a concrete reason and user approval; existing explicit authorization counts, so do not ask again for an already approved change. Keep configuration changes within the authorized task.
- Do not start/stop containers, wipe data, or run volume cleanup merely to inspect the project. Dockerfiles skip tests; an image build is not test evidence.
