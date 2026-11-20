# Non-Functional Requirements (NFR) — SpeakCraft

| Field | Value |
|---|---|
| Document ID | NFR |
| Version | 0.1 — Draft for review |
| Date | 2026-11-20 |
| Status | Draft — **all targets Proposed — require Product Owner / engineering approval** |
| Owner (role) | Solution Architect / QA Architect |
| Approval | Pending — Product Owner (gate G1) and Android Tech Lead review |
| Related | PRD §13, SRS §7, ARC-01, ARC-05, SEC-01, DES-07, TST-01 |

> **No performance guarantees are made.** No measurements of the current or future app exist (nothing could be built or run in Stage A). Every target below is a **proposal** with a rationale and a verification method, to be confirmed or adjusted after the device spike (SPK-01) and the first baseline measurements (T-05.4 after SPK-01 and T-18.2 in PLN-02).

## 1. Reference devices and conditions

| Class | Profile (proposed) | Purpose |
|---|---|---|
| RD-LOW | Android 8–10, 2–3 GB RAM, entry-level SoC (e.g. Android Go-class or 2019-era budget device) | Floor: Tier T3a/T3b behaviour, memory and latency limits |
| RD-MID | Android 12, 4 GB RAM, mid-range SoC | Tier T2 typical device for the primary segment |
| RD-HIGH | Android 14–16, 6–8 GB RAM, recent mid/high SoC with on-device recognition | Tier T1 |
| Emulator | API 24, 31, 33, 36 images | Compatibility smoke and Robolectric parity |

Measurements: release build, cold state unless stated, median of 10 runs, P90 also recorded. Exact models are chosen in TST-01 §5 from devices available to the team (ASM-07).

## 2. Performance (NFR-PERF)

| ID | Requirement | Proposed target | Rationale | Verification |
|---|---|---|---|---|
| NFR-PERF-001 | Cold start to interactive Today screen | ≤ 2.0 s RD-MID; ≤ 3.5 s RD-LOW (P90) | Android vitals treat ≥ 5 s cold start as excessive; Today must feel immediate | Macrobenchmark-style manual timing / `adb shell am start -W` |
| NFR-PERF-002 | Brand launch moment | ≤ 1.0 s, skipped when reduced motion is on; never delays interactivity beyond NFR-PERF-001 | Current overlay is 2.1–2.9 s (RD-03 FND-UI) | UI test + timing |
| NFR-PERF-003 | Navigation between primary destinations | ≤ 300 ms to first frame; no dropped-frame jank > 5 % frames | Perceived responsiveness | Frame metrics via JankStats-style manual check / profiler |
| NFR-PERF-004 | Recording start latency (tap → capture running) | ≤ 300 ms (T1/T3b); ≤ 800 ms (T2/T3a, recogniser bind) | Users start speaking immediately; avoid clipped first words | Instrumented timing |
| NFR-PERF-005 | Evaluation latency (Stop → result visible) for a 60 s attempt | ≤ 2.5 s RD-MID; ≤ 5 s RD-LOW (P90), excluding recogniser finalisation | Feedback must feel like a continuation of speaking | Instrumented timing with recorded fixtures |
| NFR-PERF-006 | Recogniser finalisation after Stop | ≤ 1.5 s typical (platform-dependent; monitored, not guaranteed) | Platform behaviour; SPK-01 will measure | SPK-01 report |
| NFR-PERF-007 | Plan generation | ≤ 150 ms on RD-LOW | Runs when Today opens | Unit benchmark |
| NFR-PERF-008 | Local queries for Today/Progress screens | ≤ 50 ms each on RD-LOW with 2 years of daily use data (≈ 3,000 attempts) | Offline-first must stay fast as history grows | Room benchmark with seeded DB |
| NFR-PERF-009 | Peak memory during a 120 s attempt evaluation | ≤ 120 MB Java heap increase; no OOM on RD-LOW | Current boxed-array design risks OOM (FND-AI-13) | Profiler, stress test |
| NFR-PERF-010 | Install size (universal APK / AAB download) | ≤ 25 MB † | Low-end/low-bandwidth markets (RD-11-I8); removal of MediaPipe/TFLite/CameraX | CI size report |
| NFR-PERF-011 | Battery: a 10-minute session | ≤ 2 % battery on RD-MID | Daily use must not feel costly | Battery historian/manual |
| NFR-PERF-012 | No main-thread disk or network I/O | 0 StrictMode violations in debug builds on core flows | Current main-thread JSON writes (FND-DATA-08) | StrictMode in debug + CI test |

## 3. Reliability (NFR-REL)

| ID | Requirement | Proposed target | Verification |
|---|---|---|---|
| NFR-REL-001 | Crash-free users | ≥ 99 % (beta), ≥ 99.5 % (production) | Play vitals / diagnostic logs (ADR-007) |
| NFR-REL-002 | ANR rate | ≤ 0.47 % user-perceived (Play bad-behaviour threshold) | Play vitals |
| NFR-REL-003 | Recording interruption handling | 100 % of interruption scenarios (call, background, focus loss, permission revoke, storage full) leave no partial attempt and no corrupt file | TC-SESS-0xx instrumented + manual |
| NFR-REL-004 | Data durability | 0 lost valid attempts across process death, force-stop and reboot after the result screen is shown | Kill-process tests |
| NFR-REL-005 | Idempotency | Replaying any event or sync batch twice yields identical state | Unit/property tests |
| NFR-REL-006 | Offline behaviour | 100 % of core-loop flows pass in airplane mode | Offline test suite |
| NFR-REL-007 | Sync recovery | After 7 days offline with 50 attempts, sync completes within 2 min of connectivity on RD-MID and the local/cloud states match | Integration test with emulator |
| NFR-REL-008 | Recogniser failure recovery | Recogniser errors never crash; attempt falls back to Inconclusive or T3b within 2 s | Fault-injection tests |
| NFR-REL-009 | Deletion completeness | Account deletion leaves 0 documents under `users/{uid}` and 0 local files for the profile, verified after resume-from-interruption | Emulator + device test |

## 4. Security (NFR-SEC)

| ID | Requirement | Verification |
|---|---|---|
| NFR-SEC-001 | Firestore rules deny all access except the owner's `users/{uid}` subtree; fact documents create-only with schema validation | Rules emulator tests in CI (100 % of collections) |
| NFR-SEC-002 | No PII (uid, email, name, transcript) in release logs | Log-redaction unit tests; lint rule/grep in CI |
| NFR-SEC-003 | Distributed builds are release-signed, non-debuggable, R8-minified | CI release job assertions |
| NFR-SEC-004 | Only `MainActivity` exported; no intent extras alter security state | Manifest test; code review |
| NFR-SEC-005 | Dependencies have no known critical vulnerabilities at release | Dependency review in release checklist (ENG-01) |
| NFR-SEC-006 | No cloud-AI SDK on the runtime classpath | CI dependency assertion (TC-SEC-010) |

Detailed, testable security requirements SEC-REQ-01…40 are defined authoritatively in SEC-01 §12 and are binding; each links to the NFR it supports.

## 5. Privacy (NFR-PRIV)

| ID | Requirement | Verification |
|---|---|---|
| NFR-PRIV-001 | Recording consent: microphone used only after explicit user action per attempt; recording state always visible | UI tests |
| NFR-PRIV-002 | Local processing: in tiers T1/T2/T3b no audio or transcript leaves the device (0 bytes to non-Firebase hosts; Firebase payloads contain no audio/transcript fields) | Network-capture test on device + payload schema tests |
| NFR-PRIV-003 | Cloud sync contains only the fields listed in ARC-03 §8 | Payload schema tests |
| NFR-PRIV-004 | Export completeness: export contains 100 % of personal data categories in SRS §9 | Export test comparing DB tables to file |
| NFR-PRIV-005 | Deletion: see NFR-REL-009; "Clear history" options delete exactly the stated categories | Tests per option |
| NFR-PRIV-006 | Backup: recordings, exports and diagnostics excluded from cloud backup and device transfer | Backup-rules test + manual `bmgr` check |
| NFR-PRIV-007 | Privacy text matches behaviour (generated from policy constants) | Snapshot test of privacy screen vs constants |

## 6. Accessibility (NFR-A11Y)

| ID | Requirement | Target | Verification |
|---|---|---|---|
| NFR-A11Y-001 | Screen readers | All core-loop screens fully operable with TalkBack; meaningful labels; headings on section titles; recording state announced | Manual TalkBack script (DES-07 TC-A11Y) + semantics tests |
| NFR-A11Y-002 | Font scaling | No clipped or overlapping essential text at 200 % font scale on compact width | Robolectric screenshot/semantic tests at fontScale 2.0 + manual |
| NFR-A11Y-003 | Contrast | Text ≥ 4.5:1; large text and UI components ≥ 3:1, light and dark themes | Token contrast table (DES-05) + Accessibility Scanner |
| NFR-A11Y-004 | Touch targets | ≥ 48 × 48 dp for all interactive elements | Semantics tests + Scanner |
| NFR-A11Y-005 | Reduced motion | When system animator scale is 0 or in-app Reduce motion is on, no non-essential animation; reactive to changes | UI tests |
| NFR-A11Y-006 | Colour independence | No information conveyed by colour alone (Measured/Estimated/Inconclusive have text) | Design review + tests |

## 7. Compatibility (NFR-COMP)

| ID | Requirement | Target |
|---|---|---|
| NFR-COMP-001 | Minimum Android version | API 24 (Android 7.0), subject to D-14 (option: raise to API 26) |
| NFR-COMP-002 | Tier behaviour | T1 on API ≥ 33 with on-device support; T2 on API 31–32 (or T1 fallback); T3a/T3b below 31 or without on-device support — each verified on ≥ 1 reference device |
| NFR-COMP-003 | RAM | Fully functional on 2 GB devices |
| NFR-COMP-004 | Storage | ≤ 60 MB app data after 30 days of daily use with default retention (WAV transcoded to AAC) † |
| NFR-COMP-005 | Screens | Compact, medium and expanded window classes; portrait required, landscape supported without state loss |
| NFR-COMP-006 | Recogniser variants | Works with Google's recognition service and at least one OEM service where available; otherwise degrades to T3b |
| NFR-COMP-007 | Locales | Recognition locales en-US, en-GB, en-IN, en-AU where supported |
| NFR-COMP-008 | 16 KB page size | App runs on 16 KB page-size devices/emulators (Play requirement for apps targeting Android 15+) |

## 8. Maintainability (NFR-MAINT)

| ID | Requirement | Target |
|---|---|---|
| NFR-MAINT-001 | Module boundaries | Package-by-feature per ADR-012; features do not import other features; composables do not reference repositories (enforced by review + a simple CI import check) |
| NFR-MAINT-002 | File size | No Kotlin file > 600 lines without a recorded justification |
| NFR-MAINT-003 | Test coverage | ≥ 70 % line coverage for `speech/`, `data/`, and engines; every SRS-BR has boundary tests |
| NFR-MAINT-004 | Static analysis | Android Lint with zero errors; warnings baseline not growing |
| NFR-MAINT-005 | Single engine per concern | One plan engine, one streak computation, one evaluation pipeline, one recognition session, one capability service (RD-03 §11) |
| NFR-MAINT-006 | Documentation | Docs updated in the same PR as behaviour changes; no "verified/passed" claim without CI link |
| NFR-MAINT-007 | Dependencies | Only ADR-approved dependencies; versions in the catalog; no unused entries |
| NFR-MAINT-008 | Strings | 100 % of user-visible strings in resources |
| NFR-MAINT-009 | Content | Content pack schema-validated in CI; every item has provenance metadata |

## 9. Evaluation quality and honesty (NFR-QUAL)

| ID | Requirement | Target | Verification |
|---|---|---|---|
| NFR-QUAL-001 | No fabricated outputs | 0 code paths that emit default/baseline scores, constant confidences or simulated analysis | Code review checklist + unit tests asserting null on missing evidence |
| NFR-QUAL-002 | Evidence gate correctness | 100 % of fixtures in the gate test set classified correctly (silence, noise-only, < 1.5 s, valid speech) | Unit tests with audio fixtures |
| NFR-QUAL-003 | Grammar-rule precision | Each shipped rule ≥ 0.90 precision on the labelled corpus (≥ 30 positive and ≥ 30 negative examples per rule, incl. unpunctuated ASR-style text) | SPK-02 corpus tests in CI |
| NFR-QUAL-004 | Metric accuracy | Pace (WPM) within ±10 % and long-pause count within ±1 of manual annotation on the fixture set | Fixture tests |
| NFR-QUAL-005 | Inconclusive rate | ≤ 15 % on T1/T2 for valid-speech fixtures and in beta (KPI-06) | Fixtures + beta data |
| NFR-QUAL-006 | Determinism | Same inputs → identical metrics and feedback (versioned rules) | Unit tests |

## 10. Approval

Targets are reviewed at gate G1 (product) and G3 (architecture) and re-baselined after SPK-01 and the first R0 measurement pass.
