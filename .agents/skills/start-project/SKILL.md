---
name: start-project
description: Resume FinFlow when the user says let's start working, continue where we stopped or names today's service; summarize status and recommend the next task.
---

# Start project

Apply for natural startup/resume messages; $start-project remains an optional shortcut. Startup recommends work and does not itself authorize implementation.

1. Locate the repository root; read AGENTS.md and PROJECT_STATUS.md, including the latest checkpoint, last active services and per-service sections. Check git status --short, staged/unstaged summaries and recent relevant history (start with git log -5 --oneline).
2. Focus on explicitly named services. Otherwise infer a clear target from the latest checkpoint/context; if unclear, ask which service(s) they want before service exploration. Briefly state where work stopped and recommend one next task. Shared setup is a valid scope; never assume all services or invent a last active service when undocumented.
3. For selected services, read applicable nested AGENTS.md instructions. Consult docs/SERVICE_CATALOG.md and docs/SERVICE_DEPENDENCIES.md only as needed. Inspect only relevant diffs/dependencies to resolve handoff discrepancies; follow the CodeGraph rule before code tracing.
4. Summarize selected-service completed/partial/pending work, historical verification and blockers concisely. For generic startup, recommend without implementing and ask one useful task-selection question only when needed. If implementation was explicitly requested, move into scoped investigation/proposal under AGENTS.md; significant implementation still needs approval unless its scope/proposal was already approved.
5. Recognize natural switches such as let's switch to notification-service. Preserve previous work/evidence, load only the new service's relevant context/dependencies and keep all affected services tracked. Switching alone does not authorize code changes.

Startup itself is read-only: no application/configuration/documentation edits or builds/tests. Do not repeat the full repository analysis. Compare dirty/untracked files to the latest checkpoint; do not mistake already documented work for new progress. Agent invocation syntax can differ; see docs/DEVELOPMENT_WORKFLOW.md.
