# Business Requirements Document (BRD) — SpeakCraft Redevelopment

| Field | Value |
|---|---|
| Document ID | BRD |
| Version | 0.2 — Draft for review (2026-11-21: Product Owner aim — curriculum based on the @askvinh channel's techniques — and D-08 decision recorded; see §1 and §2) |
| Date | 2026-11-21 |
| Status | Draft — awaiting Product Owner approval |
| Owner (role) | Business Analyst |
| Approval | Pending — Product Owner (final approver) |
| Related | RD-13 Vision, RD-14 Strategy, RD-16 Scope, PRD, SRS, GOV-01 RTM |

> This BRD is the **authoritative source** for business objectives (`BO-nn`), business requirements (`BR-nnn`) and success measures (`KPI-nn`). Sources of business intent are limited to what exists in the repository: the Product Owner's requests **PO-REQ-1119** (19-Nov-2026, DOC-059) and **PO-REQ-1120** (this Stage A brief), the historical documentation (RD-01), and the code (RD-03). Anything beyond these is marked **PROPOSAL**.

---

## 1. Business context

SpeakCraft is an individually owned Android application intended to help people improve spoken English and professional communication. It was built rapidly through successive AI-agent development rounds (26 commits between 18 and 20 November 2026), producing broad but shallow functionality, contradictory documentation and an oscillating AI strategy (RD-00, RD-05). The Product Owner is dissatisfied with the product and has commissioned a foundation-level rediscovery and redesign on the existing technology stack (PO-REQ-1120).

There is **no evidence** of: paying customers, a published store listing, revenue targets, a marketing plan, commercial partners, or a licence for the third-party creator content the current app is built around. The app is currently distributed as GitHub-release APKs.

**Product Owner direction (2026-11-21).** The Product Owner's aim is a communication-improvement app whose curriculum is built from the frameworks, techniques, tasks, exercises, activities and tips taught on the @askvinh YouTube channel, giving feedback (mistakes and coach tips) after every submitted attempt. No licence is sought; the Product Owner decided D-08 "original wording, link out" (BR-008 is met by original re-expression; PRD-FR-056/057, PRD-FR-047).

## 2. Stakeholders

| Stakeholder | Interest | Role in approvals |
|---|---|---|
| **Product Owner** (individual project owner) | A coherent, premium-quality product with a professional engineering foundation | Final approver of every gate (PLN-05) |
| End users — personas P1–P5 (RD-08) | Improve spoken English confidently, privately, in little time | Represented by beta testers in UAT |
| Beta testers (to be recruited) | Early access; give feedback | UAT participants |
| Google Play (if ADR-004 approved) | Policy compliance (data safety, account deletion, no self-updating) | External constraint |
| Google Firebase (provider) | Terms of service; quota | External dependency |
| Third-party content creator whose channel the curriculum is based on | Use of name, frameworks and material | Affected party. **D-08 decided 2026-11-21:** techniques re-expressed in original wording with links to the creator's specific videos; no use of name, likeness or branding (PRD-FR-057) |
| Contributors acting as the product/engineering roles (this documentation) | Clear, traceable plan | Prepare review artefacts |

## 3. Business objectives

| ID | Objective | Why (evidence) | Measured by |
|---|---|---|---|
| **BO-01** | Deliver a **trustworthy core speaking-practice experience**: users speak, see what they said, and receive honest, specific feedback | The core loop does not work by default and several scores are fabricated or mislabelled (RD-03 FND-AI-01, AI-08, DOM-04) | KPI-01, KPI-05, KPI-06 |
| **BO-02** | Build a **sustainable short daily practice habit** | PO-REQ-1119 Phases 2–5 (daily plan, streak, reminders); competitor norm (RD-11) | KPI-02, KPI-03, KPI-04 |
| **BO-03** | Let users **see measurable improvement** based on evidence | PO-REQ-1119 Phase 3 ("avoid false real-time updates"); category gap (RD-11-I6) | KPI-07 |
| **BO-04** | Make **privacy a verifiable advantage** and be **compliant and store-ready** | PO-REQ-1119 constraints 4–7; RD-03 FND-SEC-03, SEC-05, SEC-06 | KPI-09 |
| **BO-05** | **Reduce complexity** to a maintainable, tested foundation on the existing stack | 3 plans, 4 streaks, 26 routes, no CI tests (RD-03 §11, FND-OPS-01); PO-REQ-1120 §2, §18 | KPI-10, KPI-11 |
| **BO-06** | **Remove legal, IP and platform-policy exposure** | Creator-derived content without permission; YouTube scraping; self-updating APKs (FND-DOM-09, AI-11, OPS-04) | KPI-12 |

## 4. Business requirements

| ID | Requirement | Objective | Source | Priority |
|---|---|---|---|---|
| BR-001 | The product shall focus on **spoken communication practice** for intermediate speakers, with professional, interview, presentation and everyday scenarios | BO-01, BO-05 | RD-14 D-01; PO-REQ-1119 goals | P0 |
| BR-002 | The product shall provide a **personalised daily practice session** based on the user's goal, level, available time and measured performance | BO-02 | PO-REQ-1119 Phase 2 | P0 |
| BR-003 | Every speaking attempt shall produce **evidence-based feedback**: what was measured, what was estimated, and what could not be evaluated | BO-01 | PO-REQ-1119 Phase 8C ("do not invent … mistakes"); PP-1 | P0 |
| BR-004 | Core practice and evaluation shall work **without network connectivity** | BO-04 | PO-REQ-1119 constraint 5 | P0 |
| BR-005 | **Recordings and transcripts shall not leave the device** without the user's explicit, informed consent; no cloud AI shall process them | BO-04 | PO-REQ-1119 constraints 3, 4, 7; PO-REQ-1120 §9.5 | P0 |
| BR-006 | The product shall not depend on **large language models or paid cloud AI** | BO-04, BO-05 | PO-REQ-1119 constraints 3, 4 | P0 |
| BR-007 | Users shall be able to **export their data, clear learning history, delete their account and all associated data** | BO-04 | PO-REQ-1119 Phase 6 §3 | P0 |
| BR-008 | All learning **content shall be original or properly licensed** | BO-06 | RD-03 FND-DOM-09; PP-8 | P0 |
| BR-009 | Distributed builds shall be **release-signed** and distributed through a channel whose policies the product complies with | BO-04, BO-06 | FND-SEC-02, OPS-04; ADR-004 | P0 |
| BR-010 | The product shall support a **vocabulary learning lifecycle** that moves words into spoken use, with spaced review | BO-02, BO-03 | PO-REQ-1119 Phases 8–9 | P1 |
| BR-011 | Progress, streaks and statistics shall be **derived only from validated practice events**, never from screen visits or self-ticks | BO-03 | PO-REQ-1119 Phases 3–4 | P0 |
| BR-012 | The product shall send **practice reminders** that respect user preferences and platform permissions, and shall not include software-update notifications | BO-02 | PO-REQ-1119 Phase 5 | P1 |
| BR-013 | The product shall be **accessible** (screen readers, font scaling, contrast, reduced motion) | BO-01 | PO-REQ-1120 §3 rule 20 | P1 |
| BR-014 | The product shall be usable **without creating an account**; signing in enables backup and multi-device sync | BO-02, BO-04 | RD-14 D-05 (PROPOSAL based on RD-11 D1) | P1 |
| BR-015 | Engineering shall use the **existing technology stack**, with automated quality gates (tests, lint, rules tests) before any release | BO-05 | PO-REQ-1120 §2, §11 | P0 |
| BR-016 | Existing users' **essential data** (profile, learning preferences, vocabulary progress) shall be preserved through the transition | BO-05 | PO-REQ-1119 constraint 8 ("preserve … user progress") | P1 |
| BR-017 | Administrative functions shall not be exposed in the client unless backed by **server-side authorisation**; R1–R2 ship no in-app admin | BO-06 | PO-REQ-1119 Phase 7 rules | P1 |
| BR-018 | Product success shall be **measurable** in a way consistent with the privacy position | BO-01…03 | RSK-07; ADR-013 | P1 |

## 5. Expected outcomes

1. A new user reaches a valid first speaking attempt with real feedback in the first session.
2. A returning user starts today's session in ≤ 2 taps and finishes it in ~10 minutes.
3. Users report the feedback as accurate and specific.
4. The product's privacy statements are true and verifiable by tests.
5. The codebase has one engine per concern, CI gates, and documentation that matches the code.
6. No dependency on unlicensed third-party content or scraping.

## 6. Constraints

| ID | Constraint | Source |
|---|---|---|
| BC-01 | Retain the existing stack (Kotlin, Compose, Material 3, Firebase Auth/Firestore); technology changes only by approved ADR | PO-REQ-1120 §2 |
| BC-02 | Android-native only; no web, desktop or cross-platform client | PO-REQ-1119 constraint 1–2 |
| BC-03 | No LLMs; no paid cloud AI; no transcripts/recordings to cloud AI | PO-REQ-1119 constraints 3, 4, 7 |
| BC-04 | Individual project: one Product Owner/approver; limited engineering capacity | PO-REQ-1120 §13.7 |
| BC-05 | No backend services exist (no Cloud Functions); any server-side logic would be new scope | RD-03 §2 |
| BC-06 | Stage A produces documentation only; no production changes before approval | PO-REQ-1120 §17 |

## 7. Business rules (business level — system rules are in SRS §16)

| ID | Rule |
|---|---|
| BRU-01 | No feature may claim to use "AI" unless it uses a learned model; deterministic features are described as what they are |
| BRU-02 | No feature may present a score or status that the evidence does not support |
| BRU-03 | No monetisation, advertising or paywall is in scope until decision D-07 |
| BRU-04 | No user data is used for any purpose other than providing the service to that user |
| BRU-05 | Content attribution must be accurate; nothing is labelled "verified" without a verifiable source |

## 8. Success measures (KPIs)

All targets are **Proposed — requires Product Owner approval**. Measurement method depends on decision **D-09 / ADR-013** (recommended: structured beta programme for R1). "Beta cohort" = 10–30 recruited testers (ASSUMPTION).

| ID | KPI | Definition | Proposed target | Release | Objective |
|---|---|---|---|---|---|
| KPI-01 | Activation | % of new users completing ≥ 1 **valid** (non-inconclusive) speaking attempt in their first session | ≥ 70 % | R1 beta | BO-01 |
| KPI-02 | Time to first attempt | Median time from first app open to first valid attempt | ≤ 3 min | R1 | BO-02 |
| KPI-03 | Practice frequency | Mean practice days per week per active user over their first 4 weeks | ≥ 3 | R1 | BO-02 |
| KPI-04 | Session completion | % of started daily sessions completed | ≥ 70 % | R1 | BO-02 |
| KPI-05 | Feedback accuracy | % of "Was this accurate?" responses that are positive | ≥ 75 % | R1 | BO-01 |
| KPI-06 | Inconclusive rate | % of attempts marked Inconclusive on Tier T1/T2 devices | ≤ 15 % | R1 | BO-01 |
| KPI-07 | Measurable improvement | % of users with ≥ 4 weeks of use who improve ≥ 1 measured metric between first and latest check-in beyond its noise band | ≥ 50 % | R2 | BO-03 |
| KPI-08 | Stability | Crash-free users (Play vitals or diagnostic log) | ≥ 99 % | R1 | BO-01 |
| KPI-09 | Privacy & data rights | Privacy incidents; data-rights test suite pass rate | 0; 100 % | R1 | BO-04 |
| KPI-10 | Engineering quality | P0/P1 open defects at release; CI gates passing; line coverage of `speech/` and `data/` packages | 0 / 0; 100 %; ≥ 70 % | R0/R1 | BO-05 |
| KPI-11 | Complexity | Number of duplicate engines per concern (RD-03 §11); distinct user-facing screen layouts | 1 per concern; ≤ 30 distinct layouts † (DES-02 lists 38 R1 screen IDs, including sub-screens and settings pages that share layouts) | R1 | BO-05 |
| KPI-12 | Legal/policy exposure | Unlicensed third-party content items; policy-violating mechanisms | 0; 0 | R0 | BO-06 |

## 9. Business risks

Business-level risks are tracked in PLN-04 (RAID). Key items: RSK-04 content volume, RSK-05 IP exposure, RSK-06 capacity, RSK-07 measurability, DEP-03 Play developer account.

## 10. Proposals (not requirements)

| ID | Proposal | Status |
|---|---|---|
| PROP-01 | Monetisation options for later consideration (free with optional one-time unlock; low-price tier in price-sensitive markets) — enabled by near-zero marginal cost of on-device processing | **PROPOSAL — not in scope**, decision D-07 |
| PROP-02 | Recruit a beta cohort from the primary segment for R1 validation | PROPOSAL — needed for KPI measurement |
| PROP-03 | Commission an original content set (scenario prompts, 600+ vocabulary entries) | PROPOSAL — sizing in PLN-02 EP-09 |
