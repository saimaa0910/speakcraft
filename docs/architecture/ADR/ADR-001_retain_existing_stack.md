# ADR-001 — Retain the existing technology stack as the baseline

| Field | Value |
|---|---|
| Status | **Proposed — requires approval** |
| Date | 2026-11-20 |
| Owner (role) | Solution Architect |
| Related | RD-02, ARC-01, PO-REQ-1120 §2 |

## Context
SpeakCraft is a single-module Android app in Kotlin 2.2.10 with Jetpack Compose (Material 3), Navigation Compose, a manual DI container, DataStore, Firebase Auth (Google via Credential Manager), Cloud Firestore and App Check, built with AGP 9.1.1 / Gradle 9.3.1 and released through GitHub Actions (RD-02). The Product Owner requires that the redevelopment retain the existing stack unless a change is essential and approved.

## Problem
The redesign touches almost every screen and the whole learning/evaluation domain. It must be clear which technologies are fixed so that design work does not drift into migrations.

## Options
1. **Retain the stack** and change only through targeted ADRs (ADR-002…ADR-015).
2. Adopt a DI framework (Hilt) and Gradle multi-module architecture.
3. Re-platform (Flutter/KMP/React Native).

## Decision (proposed)
Option 1. The following are **fixed** for Stage B: Kotlin; Jetpack Compose + Material 3; Navigation Compose; coroutines/Flow; kotlinx-serialization (single JSON stack — `org.json` usage to be removed); manual DI via an `AppContainer` owned by an `Application` subclass; DataStore for preferences; Firebase Auth (Google) and Cloud Firestore; App Check; Android platform `SpeechRecognizer`, `TextToSpeech`, `AudioRecord`; GitHub Actions; JUnit4 + Robolectric + Compose UI test.

## Consequences
- No Hilt migration: the container is small enough to stay manual, but it must become process-scoped (fixes FND-ARCH-02).
- No multi-module split now (see ADR-012).
- Every other change (Room, WorkManager, removals, upgrades) is decided individually.

## Risks
Manual DI requires discipline as the codebase grows. Mitigation: constructor injection everywhere; no service-locator lookups inside composables; ViewModel factories built only in the navigation layer.

## Approval status
Pending — Product Owner.
