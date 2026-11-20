# 08 — User Personas

| Field | Value |
|---|---|
| Document ID | RD-08 |
| Version | 0.1 — Draft for review |
| Date | 2026-11-20 |
| Status | Draft — awaiting Product Owner approval |
| Owner (role) | UX Researcher |
| Approval | Pending — Product Owner (decision D-01) |
| Related | RD-07, RD-09, RD-11, RD-14 §2, PRD §6–8 |

---

## 1. Evidence base and limitations

**No user research, interviews, surveys, analytics or support data exist for SpeakCraft** (RD-00 §4). The personas below are therefore **evidence-informed hypotheses** built from:

| Source | What it tells us |
|---|---|
| Product Owner requests (PO-REQ-1119 goal list, levels Beginner→Advanced, 10/15-minute plans) | Intended goals, levels, session length |
| Existing content (RD-03 FND-DOM-13): workplace, interviews, presentations, storytelling, everyday conversation modules | Intended use contexts |
| Code: recognition locales en-US, en-GB, en-IN, en-AU; onboarding goal chips; 10–45 min commitments | Intended markets and commitments |
| Competitor research (RD-11): segments served by ELSA, Stimuler, Speak, Praktika, Yoodli, Orai; markets (India, Indonesia, LatAm) where low-price speaking apps have traction; common complaints (score distrust, cloud privacy, billing) | Market needs and gaps |

Every persona attribute below is labelled with its basis; **all are ASSUMPTIONS until validated** in the R1 beta (ASM-01…ASM-06, PLN-04). Names are illustrative; pronouns are not assumed.

## 2. Segment overview

| Persona | Segment | Priority | CEFR (approx.) | Design status |
|---|---|---|---|---|
| P1 Aarav | Working professional | **Primary** | B2 | Designed for |
| P2 Meera | Job seeker / career returner | **Primary** | B1+ | Designed for |
| P3 Daniel | University student / graduate | Secondary | B1 | Designed for |
| P4 Sofia | Advanced professional | Served | C1 | Supported |
| P5 Hoang | Elementary everyday speaker | Served (guided templates) | A2+ | Supported |
| — | Complete beginner; native public speaker; exam candidate | Not targeted | — | Non-goal (RD-16) |

## 3. Personas

### P1 — Aarav, software engineer (Primary)

| Attribute | Detail | Basis |
|---|---|---|
| Context | 28, works for a services company with international clients; daily stand-ups and weekly client calls in English | Workplace/meeting content focus; en-IN locale |
| Goals | Give concise updates; explain technical issues clearly; stop rambling; sound confident with clients | PO-REQ-1119 goals "workplace communication", "presentation skills" |
| Pain points | Freezes when asked a question unexpectedly; uses fillers ("basically", "actually"); long answers without structure; no one gives honest feedback | Filler/structure features in current code; RD-11 delivery-coach gap |
| Current habits | Watches YouTube communication videos; occasionally rehearses in the shower; no structured practice | INFERRED from creator-content origin of the app |
| Motivation | Promotion; client-facing role | ASSUMPTION |
| Barriers | Time (commute, evenings); embarrassment practising aloud near others; distrust of apps that "grade accent" | RD-11 score-distrust evidence |
| Preferred duration | 10 min/day on weekdays | PO-REQ-1119 plan examples |
| Required capabilities | Meeting-update and explanation prompts; structure feedback; pace and fillers; private practice | CAP-03/04/05/06 |
| Success criteria | Fewer fillers per minute; answers within time target; uses a clear structure (point → reason → example) | M-FILL, M-LEN, M-STRUCT |

### P2 — Meera, job seeker returning to work (Primary)

| Attribute | Detail | Basis |
|---|---|---|
| Context | 31, returning to the workforce after a career break; preparing for interviews | Interview content; PO-REQ-1119 "speak confidently in interviews" |
| Goals | Answer "Tell me about yourself" and behavioural questions confidently; use stronger vocabulary | Interview module; vocabulary lifecycle request |
| Pain points | Anxiety; answers too short; struggles to recall the right word; worried about being judged | ASSUMPTION; RD-11 anxiety/privacy theme |
| Current habits | Practises answers by writing them down, rarely aloud | ASSUMPTION |
| Motivation | Getting a job within months | ASSUMPTION |
| Barriers | Privacy (does not want recordings uploaded); low confidence; irregular schedule | RD-11 privacy gap |
| Preferred duration | 10–15 min, more before an interview | ASSUMPTION |
| Required capabilities | STAR-structured answers; retry and compare; vocabulary in use; offline and private | CAP-04/05/06/07, F-1 |
| Success criteria | Complete STAR answers; answer length within target; learned words used in answers | M-STRUCT, M-LEN, M-TARGET |

### P3 — Daniel, final-year student (Secondary)

| Attribute | Detail | Basis |
|---|---|---|
| Context | 21, preparing for campus placements, group discussions and class presentations | Presentation/storytelling content |
| Goals | Introduce self well; present a project in 2 minutes; speak at a steady pace | PO-REQ-1119 goals |
| Pain points | Speaks too fast when nervous; limited vocabulary; budget-sensitive; patchy connectivity | RD-11 low-bandwidth/price evidence |
| Habits | Mobile-first; responds to streaks | RD-11 gamification norms |
| Barriers | Data costs; older mid-range phone | NFR-COMP |
| Preferred duration | 5–10 min | ASSUMPTION |
| Required capabilities | Presentation and story prompts; pace feedback; works offline on a mid-range phone | CAP-06, F-2 |
| Success criteria | Pace within comfortable band; fewer long pauses | M-WPM, M-PAUSE |

### P4 — Sofia, senior analyst (Served)

| Attribute | Detail | Basis |
|---|---|---|
| Context | 38, C1 speaker presenting to leadership | Advanced level in PO-REQ-1119 |
| Goals | Polish delivery: fewer hedges, deliberate pauses, crisp structure | Delivery metrics |
| Pain points | Generic apps are too basic; wants precise, honest metrics | RD-11 |
| Required capabilities | Structured answers, story prompts, hedging and filler metrics, progress trends | M-HEDGE, M-FILL, CAP-08 |
| Success criteria | Measurable reduction in hedges/fillers over 4 weeks | KPI-07 |

### P5 — Hoang, shop supervisor (Served with guided templates)

| Attribute | Detail | Basis |
|---|---|---|
| Context | 34, A2+, uses English with customers | Beginner/Elementary levels in PO-REQ-1119 |
| Goals | Say common phrases clearly; learn practical words | Read-aloud/repeat content; vocabulary |
| Pain points | Free speaking is hard; recognition may fail on accented speech | ASM-05 risk |
| Required capabilities | Read aloud and Listen & repeat with slow reference audio; word lifecycle; gentle feedback | TPL-READ/REPEAT; CAP-07 |
| Success criteria | "Recognised as intended" rising on everyday phrases | M-MATCH |

## 4. Needs matrix

| Need | P1 | P2 | P3 | P4 | P5 |
|---|---|---|---|---|---|
| Short daily session | ●● | ●● | ●● | ● | ●● |
| Realistic scenario rehearsal | ●● | ●● | ●● | ●● | ● |
| Transcript + specific feedback | ●● | ●● | ●● | ●● | ● |
| Structure feedback (STAR/PREP) | ●● | ●● | ● | ●● | — |
| Pace/fillers/pauses | ●● | ● | ●● | ●● | ● |
| Vocabulary in use | ● | ●● | ●● | ● | ●● |
| Privacy/offline | ● | ●● | ●● | ● | ● |
| Guided read/repeat | — | ● | ● | — | ●● |

●● critical · ● useful · — low

## 5. Anti-personas (not designed for)

| Anti-persona | Why not |
|---|---|
| Absolute beginner learning the alphabet and basic grammar | Requires a full curriculum (NG-05) |
| IELTS candidate wanting a band prediction | Cannot be offered honestly (NG-04) |
| User wanting an AI chat companion | NG-01/NG-02 |
| Content creator / administrator | No admin in R1–R2 (NG-10) |

## 6. Validation plan

| Assumption | Method | When |
|---|---|---|
| ASM-01 segment fit | Beta screener (role, level, goal); 5–8 interviews | R1 beta |
| ASM-02 privacy matters | Interview question; online-consent opt-in rate | R1 beta |
| ASM-03 10-minute sessions | Duration choice distribution; completion | R1 beta |
| ASM-05 recognition adequacy | SPK-01 test set incl. Indian, SE Asian, Latin American accents (with consent) | R0 |
