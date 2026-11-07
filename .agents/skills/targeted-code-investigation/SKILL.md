---
name: targeted-code-investigation
description: Investigate natural FinFlow requests such as fix this issue, implement a feature, explain this code or why this approach; trace relevant existing behaviour before advice or changes.
---

# Targeted code investigation

Apply to natural feature, bug and explanation requests; $targeted-code-investigation is optional. Understand the business requirement, not just the wording. Investigation alone does not authorize significant implementation.

1. Read root AGENTS.md and relevant PROJECT_STATUS.md service/issue entries. Identify affected services and whether the requested behaviour already exists; use docs/SERVICE_CATALOG.md if ownership is unclear. Ask one necessary clarification question at a time when business rules/scope cannot be resolved; never invent missing requirements.
2. Read applicable nested instructions. When .codegraph/ exists, use codegraph_explore with this repository's projectPath, or codegraph explore, before grep/find or source searches. Query named symbols/files or a narrow flow. Do not rebuild the index; if unavailable/incomplete, state the limit and use targeted rg/read fallbacks.
3. Trace relevant controller/DTO -> service -> entity/repository and needed security/config/test paths. Follow directly affected HTTP/event providers/consumers using docs/SERVICE_DEPENDENCIES.md; inspect only source required to answer the question.
4. Establish actual branches, authorization, validation and transaction/error behaviour; class names/comments or target docs are not proof. Reuse prior evidence; do not repeatedly reopen already supplied source.
5. Report concise findings with file/line references, verification limits and recommended next step. For meaningful features/fixes, briefly propose one logical task, important files, tests and database/API/cross-service impact; wait for approval before significant implementation unless already approved. Small clear low-risk changes do not need repeated approval. For how/why questions, explain actual project examples, purpose and what could happen without the approach; distinguish established reasons from inference and avoid unnecessary theory or code edits.

No repository-wide scan, unrelated investigation or application edits. Expand scope only when a concrete unresolved dependency requires it. Do not invent implemented features or passed tests.
