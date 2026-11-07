---
name: database-migration-review
description: Review FinFlow database changes when a feature affects schema or the user asks about a migration; check MySQL/JPA integrity and safety without applying changes.
---

# Database migration review

Apply when relevant to a natural feature/schema request or migration question; $database-migration-review is optional. Explain database impact in simple English before significant implementation approval. Read-only review; no migration execution or application edits.

1. Read root AGENTS.md, relevant service status/nested instructions, DATABASE_DESIGN.md and pertinent decisions. Establish owning schema and current tooling from relevant changed files. Current baseline is MySQL 8.4/JPA Hibernate update, H2 tests, no versioned migration framework; do not assume Flyway/Liquibase or invent a baseline.
2. Review scoped Git changes to SQL, entities/repositories and configuration. Use CodeGraph before locating/tracing code. Compare mappings and any supplied schema/migration evidence; live schema/data drift is unknown unless actually inspected.
3. Check PK/FK relationships versus scalar logical IDs, nullability/defaults, unique/idempotency scope, indexes for affected queries, money precision/rounding and existing-data compatibility. Preserve service database ownership; no cross-service FK/transaction assumptions.
4. Review backfill ordering, duplicate/null data, concurrent writers/optimistic versions, deployment compatibility, locks/long-running DDL and MySQL implicit-commit/rollback implications. State what restore/forward-fix evidence is missing; a down script does not prove safe rollback.
5. Report severity, file/line evidence, data-loss/integrity risk, unresolved questions and targeted validation needed. H2 success alone does not establish MySQL migration safety.

Never execute destructive SQL, migration apply/rollback, data reset, volume deletion or schema changes without explicit approval for that action/environment. This review grants none. Do not introduce migration dependencies. Update database/status docs only if the authorized task establishes new facts; preserve unrelated changes.
