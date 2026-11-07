---
name: start-project
description: Resume FinFlow when the user says let's start working, continue where we stopped or names today's service; summarize status and recommend the next task.
---

# Start project

Apply for natural startup/resume messages; $start-project remains an optional shortcut. Startup recommends work and does not itself authorize implementation.

1. Locate the repository root; use AGENTS.md and the latest PROJECT_STATUS.md checkpoint/service entries to identify where work stopped. Reuse already loaded context unless it may have changed. Normal startup needs no git status, git diff or history commands.
2. Focus on explicitly named services. Otherwise infer a clear target from the latest checkpoint/context; if unclear, ask which service(s) they want before service exploration. Briefly state where work stopped and recommend one next task. Shared setup is a valid scope; never assume all services or invent a last active service when undocumented.
3. For selected services, read applicable nested AGENTS.md instructions. Consult docs/SERVICE_CATALOG.md and docs/SERVICE_DEPENDENCIES.md only as needed. Inspect actual relevant files/dependencies only to resolve a concrete handoff discrepancy; use Git only if unexpected edits or a change-review question genuinely require it. Follow CodeGraph before code tracing; Git is not source evidence.
4. Summarize selected-service completed/partial/pending work, historical verification and blockers concisely. For generic startup, recommend without implementing and ask one useful task-selection question only when needed. If implementation was explicitly requested, move into scoped investigation/proposal under AGENTS.md; significant implementation still needs approval unless its scope/proposal was already approved.
5. Recognize natural switches such as let's switch to notification-service. Preserve previous work/evidence, load only the new service's relevant context/dependencies and keep all affected services tracked. Switching alone does not authorize code changes.

Startup itself is read-only: no application/configuration/documentation edits or builds/tests. Do not repeat the full repository analysis. Do not perform routine repository scans or Git checks; preserve documented unfinished work. Do not mistake a historical test result for fresh verification. Agent invocation syntax can differ; see docs/DEVELOPMENT_WORKFLOW.md.
