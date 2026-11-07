# Daily multi-service workflow

Canonical instructions: [AGENTS.md](../AGENTS.md). Current progress: [PROJECT_STATUS.md](../PROJECT_STATUS.md). Skills live only in .agents/skills/; they contain concise instructions, with documentation loaded only as needed.

## Daily use

1. Say "Let's start working", "Continue where we stopped" or "Today let's work on wallet-service". Read the latest checkpoint, identify the last active services and summarize the stopping point/next task. Focus on a named service; ask for service scope only when unclear. Startup recommends work without implementing it.
2. Work on that scope. For a switch, name the next service/task; read its applicable instructions and relevant dependencies, preserving earlier work. Joint work is allowed; record all affected services and API/event contract impact.
3. After meaningful completed tasks, update overall/affected-service status automatically as part of development, without another documentation permission request. Tiny intermediate edits do not need checkpoints.
4. Say "Save today's work", "Let's stop here", "That's enough for today" or "Finish for today" for end-of-day reconciliation across all services touched, including unfinished work. It preserves unaffected progress and records only observed tests/results/blockers. Saving does not stage or commit. An already documented dirty tree is not new progress.
5. Resume next day from the latest checkpoint and each selected service's section. No full repository assessment is needed.

Describe features/fixes in normal English. Investigate relevant existing behaviour, then propose one logical task with important files and database/API/cross-service impact. Significant implementation waits for approval of that proposal; small clear low-risk changes proceed without repeated approval. Already approved work does not need approval again. After implementation, run relevant tests, review changes and update progress. Ask one necessary question at a time; do not invent business rules.

For explanations, use actual project examples and explain purpose and consequences in simple English. "What should we implement next?" asks for advice, not implementation.

## Skills and optional shortcuts

| Invocation | Purpose |
|---|---|
| `$start-project` | Read-only state handoff, service selection and next-task recommendation |
| `$save-progress` | Overall/per-service checkpoint, evidence, blockers and next task |
| `$targeted-code-investigation` | Scope service, explore CodeGraph call paths, report source evidence before changes |
| `$code-change-verification` | Targeted compilation/tests for affected services, contract impact and unintended-diff review |
| `$database-migration-review` | Read-only MySQL/JPA/schema/migration safety review; no destructive execution |

Natural language selects the appropriate workflow; commands are optional. Examples: "Let's switch to notification-service", "Implement wallet transfer" (first check the existing transfer flow and clarify the missing behaviour), "Fix this issue", "Explain this code" or "Why did we use optimistic locking?".

## Agent compatibility

- **Codex:** repository .agents/skills/ is a supported discovery location, including from service subdirectories. Speak naturally or optionally use the dollar-prefixed shortcuts above; the skill name/description is discoverable before its full instructions load. Root AGENTS.md defines the conversational defaults. See [official OpenAI skill documentation](https://learn.chatgpt.com/docs/build-skills).
- **Cursor:** open this repository as the project; Cursor discovers .agents/skills/ on startup. Type `/` in Agent chat and select the skill name. No .cursor/skills/ copies are needed. See [Cursor Agent Skills](https://prod.cursor.com/docs/skills).
- **Claude Code (future):** native project skills use .claude/skills/<name>/SKILL.md and slash invocation. Before adopting it, arrange compatible copies or reviewed one-way synchronization from .agents/skills/, preserving this canonical source, and explicitly load common AGENTS.md instructions. No .claude directory, synchronization automation or competing CLAUDE.md is created now. See [Claude Code skills](https://code.claude.com/docs/en/skills).

Discovery/invocation support can vary by installed agent version or policy. This setup has not been exercised in a new Codex session, Cursor or Claude Code. If a skill is missing from the selector, start a fresh session in this repository and inspect agent diagnostics before creating duplicate copies.

## Documentation boundaries

Use SERVICE_CATALOG.md for ownership/entry points, SERVICE_DEPENDENCIES.md for communications, ARCHITECTURE.md for material workflows, DECISIONS.md for significant established decisions, DATABASE_DESIGN.md for persistence. Maintain small relevant updates; do not turn design proposals into completed implementation. Skills grant no extra permissions, never stage/commit automatically, and require no additional scripts/dependencies.
