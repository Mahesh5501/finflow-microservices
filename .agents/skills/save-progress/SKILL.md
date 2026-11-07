---
name: save-progress
description: Save FinFlow progress when the user says save today's work, let's stop here, that's enough for today or finish for today; record all affected services and next steps.
---

# Save progress

Apply for natural save/end-of-day messages; $save-progress is an optional shortcut. Stop starting new implementation, preserve unfinished edits and update documentation only. Saving does not mean staging or committing.

1. Read root AGENTS.md and PROJECT_STATUS.md, especially the last checkpoint. Inspect Git status, staged/unstaged summaries and relevant history/diffs/untracked files since that checkpoint. HEAD alone does not identify uncommitted progress; flag uncertain attribution.
2. Identify every service affected during the session, including completed and unfinished work and directly affected API/event callers/consumers. Read applicable nested instructions. Do not assume only the currently selected service changed; do not attribute unrelated user edits to this session.
3. Update overall status and the separate sections for affected services with actual completed/partial/pending work, important files and responsibilities, changed contracts, blockers and next tasks. Preserve unaffected service sections and historical evidence.
4. Record tests/checks actually executed, commands, results, service scope and limits from session output or reliable dated evidence. Otherwise record not run/unknown; never promote historical results to current verification.
5. Add a concise dated checkpoint with current HEAD, last active service(s), all services affected during the session and relevant staged/unstaged/untracked work. Distinguish last active from merely affected services; if unknown, record unknown. Update docs/ARCHITECTURE.md only for material architecture/workflow changes; docs/SERVICE_DEPENDENCIES.md only for communication/dependency changes; docs/DECISIONS.md only for significant decisions. Update catalog/database docs only when their facts change.
6. Review documentation scope/accuracy and give a short end-of-day summary of affected services, progress, tests, blockers and recommended next task.

No full analysis, automatic full-suite rerun, application edits, staging, commits or resets. Follow CodeGraph before any necessary code investigation. Never discard unrelated work, invent decisions or add duplicate checkpoints when nothing material changed. Routine progress documentation needs no extra permission.
