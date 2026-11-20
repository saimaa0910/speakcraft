# 09 — User Journey Map

| Field | Value |
|---|---|
| Document ID | RD-09 |
| Version | 0.1 — Draft for review |
| Date | 2026-11-20 |
| Status | Draft — awaiting Product Owner approval |
| Owner (role) | UX Strategist |
| Approval | Pending — Product Owner |
| Related | RD-07, RD-08, DES-01, DES-04 (target flows), RD-03 |

---

## 1. Method

Current journeys are reconstructed from code (RD-03; UI working paper §3) for persona **P2 Meera** (primary segment) unless noted. "Current implementation" is VERIFIED from code; "Problems/friction" combine VERIFIED defects and INFERRED experience effects; "Opportunity" is RECOMMENDATION and links to the target flow in DES-04.

## 2. Journey map

| # | Stage | User intention | User action | Expected result | Current implementation | Problems | Friction | Opportunity (target) |
|---|---|---|---|---|---|---|---|---|
| 1 | Discover | Find a way to practise interview English privately | Searches the store / hears about it | A clear promise relevant to them | No store listing; GitHub APK; `metadata.json` describes "built from @askvinh YouTube lessons" | No discoverability; creator-centred promise | Sideloading requires "unknown sources" | Store listing (ADR-004) with privacy-first promise (RD-13) |
| 2 | Install | Install quickly on a mid-range phone | Downloads APK | Small, safe install | Debug-signed builds possible; large APK (MediaPipe, TFLite, CameraX) | Security warnings; size | Unknown-source install flow | Release-signed, ≤ 25 MB (NFR-PERF-010) |
| 3 | Open | See value immediately | Launches app | Fast start | 2.9 s non-skippable overlay; splash→Compose colour jump | Delay; visual glitch | Waiting | ≤ 1 s brand moment (DES-06 §2.3) |
| 4 | Registration | Try before committing | Faces sign-in | Option to explore | Google sign-in mandatory; raw error text on failure | No value before account | Account wall | Account optional (FS-01, D-05) |
| 5 | Onboarding | Tell the app their goal | 3-step wizard: name/level, goal text + duplicate minute controls, 7 self-rating sliders | Short, relevant set-up | Self-ratings later shown as "Objective Speech Skills"; errors invisible; no speaking | Long, abstract; misleading reuse of self-ratings | Sliders feel like a test | 5 short choices + first attempt (FS-02, FLOW-01) |
| 6 | Goal selection | Pick "interviews" | Edits free text or chip | Plan reflects goal | Goal never reaches dashboard plan (FND-DOM-03) | Personalisation broken | — | Track-based goal driving Today (FS-03) |
| 7 | Level assessment | Be placed appropriately | Picks one of 4 levels | Fair placement, changeable | 4/5/3-level mismatch; no assessment; presets overwrite sliders | Inconsistent; arbitrary | — | 5 levels with "can do" text; baseline from first attempt (D-17) |
| 8 | Dashboard | Know what to do now | Scrolls Home | One clear next step | ~21 widgets, ~70 targets, 22-tile directory; fake score 62; "1 Day Streak" on day 0; "Welcome back" on first visit | Overload; fabricated data | Decision paralysis | Today with one session card and one Start (FS-03) |
| 9 | Daily practice | Do today's practice | Picks "Daily" tab or dashboard plan or Activity plan | One routine | Three plans; ticking a box counts as done; minutes fabricated | Confusing; untrustworthy | Which plan? | One deterministic session; completion from valid attempts (SRS-BR-009) |
| 10 | Speaking activity | Rehearse an interview answer | Opens one of 10 recorders | Consistent recording experience | 12 start labels; permission denial silent; recording leaks across screens | Inconsistent, fragile | Learning each screen | One session player (FS-04) |
| 11 | Evaluation | Know how it went | Taps "Analyze…" | Transcript + accurate analysis | No transcript by default; acoustic-only; activity score = length formula | Feedback not grounded in what they said | Waiting for meaningless numbers | Tiered transcription; metric catalogue (ARC-05) |
| 12 | Feedback | Learn what to change | Reads report card | 1–2 actionable points | Many generic template sections; mislabelled "Pronunciation"; jargon ("INSUFFICIENT_EVIDENCE") | Overwhelming; distrust | Reading walls of text | One strength + one focus; Measured/Estimated labels (FS-05) |
| 13 | Progress tracking | See improvement | Opens Analytics | Evidence-based trends | Self-ratings shown as objective; sparkline invents 60→62; four streaks | Misleading | — | Trends from valid attempts only, with noise bands (FS-08) |
| 14 | Vocabulary | Learn useful interview words | Opens Vocabulary | Words to use in speech | 20-word bank; "words learned" = words seen; 5-stage flow unreachable | Thin; inflated stats | — | 5-stage lifecycle, ≥ 300 words (FS-07) |
| 15 | Returning user | Pick up where they left off | Opens app next day | Today's next step | Same greeting as day 1; hero pager mostly static | Nothing new | Re-navigating | Greeting with one relevant fact; session ready (SRS-FR-TODAY-010) |
| 16 | Notification engagement | Be reminded at a good time | — | A gentle reminder | No OS notifications exist; in-app alerts duplicated | No re-engagement | — | One daily reminder at chosen time, suppressed if practised (FS-09) |
| 17 | Long-term learning | Keep improving for months | Uses programs/paths | Coherent progression | 6 of 7 programs uncompletable; paths not enforced; content thin | Dead ends | — | Tracks, check-ins (R2), word reviews (FS-06/07/08) |

## 3. Emotional curve (INFERRED)

```
Delight  ┤
         │                                  
Neutral  ┤──●(discover)──●(open)                     
         │                 ╲      ●(onboarding)        
Frustr.  ┤                  ●(sign-in)   ╲  ●(dashboard overload)──●(feedback distrust)──●(no reminders → churn)
```

Target curve: rises at stage 5 (first attempt in onboarding gives a real transcript and one useful tip) and stays positive through short, finishable sessions and visible evidence of progress.

## 4. Top journey fixes (priority)

| # | Fix | Stages | Target flow |
|---|---|---|---|
| 1 | Real transcript + honest feedback | 10–12 | DES-04 FLOW-05 |
| 2 | First attempt inside onboarding; account optional | 4–7 | FLOW-01 |
| 3 | One Today session with one Start | 8–9, 15 | FLOW-02/03 |
| 4 | Evidence-only progress, one streak | 13 | FLOW-08 |
| 5 | Reminders | 16 | FLOW-11 |
| 6 | Vocabulary lifecycle | 14 | FLOW-06/07 |
