---
name: code-change-verification
description: Verify an approved FinFlow feature or fix after implementation, or when asked to check changes; report targeted compilation, tests and diff results without requiring a command.
---

# Code change verification

Apply after approved implementation or a natural request to verify changes; $code-change-verification is optional. Verification does not authorize additional significant repairs or configuration/dependency changes. Keep one logical task focused; summarize actual results in simple English.

1. Read root AGENTS.md and relevant service status/nested instructions. Review Git status, scoped staged/unstaged diffs and relevant untracked files to identify affected services and API/event consumers. Preserve unrelated changes.
2. Inspect only changed code and relevant tests/build dependencies; follow CodeGraph first when tracing implementation. Select Java 21 per process (system Java may be 8), preserving global settings.
3. Run targeted compilation/tests first from affected service directories: mvn -B -DskipTests compile for main compilation, and mvn -B -Dtest=RelevantTest test for relevant tests. Choose actual existing test names; report zero tests/skips as such. There is no root reactor; -o requires cached dependencies.
4. For cross-service contracts verify directly affected producers/consumers and document untested integrations. Broaden to the affected service suite or mvn -B verify only when change scope, failures or unresolved concerns justify it. Existing H2/simulator/mock-HTTP checks do not prove MySQL concurrency, security filters or live Kafka/wallet operation.
5. Review final scoped diff and Git status for unintended changes. Report exact commands, exit/results, service names, skipped/not-run checks and environmental blockers; compilation alone is not test success.
6. Record meaningful task verification in PROJECT_STATUS.md overall/affected-service sections under the root progress rule, or supply this evidence to the ongoing task/save-progress checkpoint without duplicate updates.

Do not disable tests or alter application/build settings just to pass. No container lifecycle/data reset or dependency installation as an incidental check. Use required approvals for sandbox failures. Stop at unresolved failures with evidence; never claim success without verification.
