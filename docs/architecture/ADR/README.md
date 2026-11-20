# Architecture Decision Records — Index

| Field | Value |
|---|---|
| Document ID | ADR-000 |
| Version | 0.1 — Draft for review |
| Date | 2026-11-20 |
| Status | Draft |
| Owner (role) | Solution Architect |
| Approval | Each ADR individually — Product Owner (final approver) |
| Related | RD-02 Tech Stack Audit, ARC-01 Solution Architecture, PLN-05 Governance |

All ADRs below are **Proposed — requires explicit approval**. None has been implemented. Per PO-REQ-1120 §2, any technology change (new dependency, removal, replacement) must be approved before implementation.

Format for each ADR: Context · Problem · Options · Decision (proposed) · Consequences · Risks · Approval status.

| ADR | Title | Kind | Linked decision | Status |
|---|---|---|---|---|
| [ADR-001](ADR-001_retain_existing_stack.md) | Retain the existing technology stack as the baseline | Confirmation | — | Proposed |
| [ADR-002](ADR-002_room_local_source_of_truth.md) | Room as the local source of truth for learning data | **New dependency** (catalog-declared, unused) | D-11 | Proposed — requires approval |
| [ADR-003](ADR-003_remove_cloud_ai.md) | Remove Firebase AI Logic (cloud AI) from the product | Removal | D-04 | Proposed — requires approval |
| [ADR-004](ADR-004_distribution_and_updater.md) | Distribution channel and the in-app APK updater | Platform/product | D-03 | Proposed — requires approval |
| [ADR-005](ADR-005_workmanager_reminders_sync.md) | WorkManager for reminders and background sync | **New dependency** | D-12 | Proposed — requires approval |
| [ADR-006](ADR-006_dependency_currency.md) | Dependency currency upgrade and clean-up | Version upgrade / removal | — | Proposed — requires approval |
| [ADR-007](ADR-007_crash_reporting.md) | Crash and stability reporting | Possible new dependency | D-13 | Proposed — requires approval |
| [ADR-008](ADR-008_app_check_provider.md) | App Check provider and debug-provider hardening | Provider change | — | Proposed — requires approval |
| [ADR-009](ADR-009_speech_recognition_strategy.md) | Speech capture and recognition strategy | Architecture (platform APIs) | D-02, D-14 | Proposed — requires approval |
| [ADR-010](ADR-010_defer_video_analysis.md) | Defer video capture and automated body-language analysis | Scope / removal | — | Proposed — requires approval |
| [ADR-011](ADR-011_remove_youtube_intelligence.md) | Remove YouTube Intelligence and creator-derived content pipeline | Removal | D-08 | Proposed — requires approval |
| [ADR-012](ADR-012_package_by_feature.md) | Package-by-feature structure inside the single `:app` module | Structure | — | Proposed — requires approval |
| [ADR-013](ADR-013_product_analytics.md) | Product success measurement and analytics | Possible new dependency | D-09 | Proposed — requires approval |
| [ADR-014](ADR-014_sync_and_conflict_model.md) | Sync and conflict-resolution model | Architecture | — | Proposed — requires approval |
| [ADR-015](ADR-015_recording_storage_retention.md) | Recording storage format and retention | Architecture / privacy | D-10 | Proposed — requires approval |
