# 11 — Competitor Analysis

| Field | Value |
|---|---|
| Document ID | RD-11 |
| Version | 0.1 — Draft for review |
| Date | 2026-11-20 |
| Status | Draft — awaiting product owner approval |
| Owner (role) | Product Analyst |
| Approval | Pending — Product Owner |
| Related | RD-07 Product Audit, RD-12 Product Opportunity Analysis, RD-13 Product Vision |

---

## 1. Purpose, method and evidence

**Purpose.** This document describes the 2025–2026 market for spoken-English and communication coaching apps, compares SpeakCraft's current state with category norms, and sets out evidence-based implications (RD-11-I1…I10) for RD-12 and RD-13. SpeakCraft's intended distinctive constraint is privacy-first, offline-capable, on-device evaluation with no cloud AI by default.

**Method.** Desk research on 2026-11-20 covered:
- official sites, US App Store listings, privacy policies and help-centre articles;
- Duolingo's Q3 2026 SEC shareholder letter;
- selected independent reviews, press articles and a technical vendor document.

Pages were read with an automated fetch tool that summarises content, so short quotes are as extracted. Search queries named only competitors and general market topics, never project or owner details. SpeakCraft facts in §5 come from the internal code audit and are cited as **RD-03**.

**Evidence labels** (used in every table, with source numbers from §9):

| Label | Meaning |
|---|---|
| **VO** — VERIFIED-official | First-party page opened (company site, store listing, policy, help centre, filing). It verifies the *claim*, not actual performance. |
| **V3** — VERIFIED-third-party | Independent page opened (review, press, journal, policy tracker). Bias is flagged where relevant. |
| **SO** — SEARCH-ONLY | Seen in search results only. The page was not opened, or it failed or returned 404. Lower confidence. |
| **INF** — INFERENCE | Analyst interpretation, not stated by a source. |
| **REC** — RECOMMENDATION | Proposed direction for the redesign. Inspiration only; no interface is to be copied. |

**Access date.** All sources were accessed on 2026-11-20 unless a publication date is given.

**Limitations.**
1. Store data is from the **US App Store only**. Google Play pages could not be fetched.
2. Prices are **approximate**. IAP lists are often unlabelled, and prices vary by region, platform and promotion.
3. App Store privacy labels are developer-reported and not verified by Apple.
4. LanguaTalk, SpeakShark, Stimuler's blog and FinalRound promote their own products.
5. Nothing was tested hands-on.
6. Unverified items are listed in §9.2.

---

## 2. Competitor profiles (condensed)

### 2.1 ELSA Speak: AI pronunciation, broadening to communication

| # | Dimension | Finding | Label |
|---|---|---|---|
| 1 | Positioning / user | "#1 AI English Speaking App for Fluency and Confidence". Professionals, job seekers, students, expats; B2B Business and Schools tiers. 4.7★, ~115K ratings | VO [1][2] |
| 2 | Workflow / session | "Coach" hub: AI Conversation Coach with learning path, Pronunciation Coach, Daily Practice, Skills, Dictionary. 5 lessons/day picked (2024) | VO [6]; V3 [9] |
| 3 | Assessment / feedback | Real-time pronunciation and word stress; Speech Analyzer for free speech; insights on pace, hesitation, grammar. Phoneme-level claims with colour-coded sounds. EPS "mapped to CEFR and IELTS", "93.88% agreement … within ±1 CEFR band", no methodology or sample size. "Strict": some natives score ~80% (2024) | VO [2][3]; SO [11]; V3 [9] |
| 4 | Vocabulary | Study sets and pronunciation dictionary, rated weaker than other apps' flashcards; no spaced repetition found | VO [2]; V3 [9] |
| 5 | Personalisation / placement | Path adapts to level, goals, accent, industry. Speaking test of <10 min before lessons. Bilingual tutor for beginners. US/UK/AU accents | VO [1][2]; V3 [9] |
| 6 | Progress | Skill dashboards; test-score predictions (IELTS, TOEFL, TOEIC, CEFR); Progress tab | VO [1][2][6] |
| 7 | Gamification | Achievements, leaderboard, avatars, games (e.g., Shadowing) | VO [2][6] |
| 8 | Onboarding | Speaking assessment, then suggested lessons; step count unverified | V3 [9] |
| 9 | Navigation | **4 tabs**: Coach, AI Chats, Discover, Progress | VO [6] |
| 10 | Pricing | US IAP $19.99/mo to $129.99/yr, 7-day trial. JP: Pro ¥8,699/yr, Premium ¥27,800/yr, lifetime SKUs; free tier ~25 lessons with ads. "ELSA Professional" (interview, presentation, meeting coaches) pre-sold for a tentative March 2026 launch | VO [2][7]; V3 [8] |
| 11 | Offline / privacy | "Needs a continuous internet connection"; ~25 Mbps recommended. Audio collected; retention "as long as … reasonably necessary"; AWS in 4 regions; silent on training. Audio linked to identity on store label | VO [4][5][2]; SO [12][13] |
| 12 | Complaints | Auto-renewal and trial billing, cancellation friction, login bugs; overwhelming content; IPA lesson titles; strict scoring | SO [10]; V3 [9] |

### 2.2 Stimuler: confidence and IELTS fluency for price-sensitive markets

| # | Dimension | Finding | Label |
|---|---|---|---|
| 1 | Positioning / user | "Speak English confidently in moments that matter". Interviews, meetings, IELTS/TOEFL. Models "built for non-native speakers", aiming to be understood rather than to sound native. Strong in India, Indonesia, Mexico, Colombia. $3.75M pre-Series A (Apr 2025), 4M+ installs claimed. 4.8★, 8.8K ratings | VO [14][15]; SO [18] |
| 2 | Workflow / session | AI tutor "Sarah" scenarios; day-wise roadmap; 100+ topics; ~10 min to unlimited | VO [14][17] |
| 3 | Assessment / feedback | Each attempt scored on fluency, grammar, vocabulary, pronunciation. Shows what you said, an improved version, and why. Filler words, pace, pauses. **IELTS band from a 60-s speech**; 15+ mock tests; AI video interviewer | VO [14][15] |
| 4 | Vocabulary | Scored as a speaking dimension only; no SRS found | VO [14] |
| 5 | Personalisation / placement | Adaptive weak-spot practice; "pre-test" mentioned by users | VO [14]; SO [20] |
| 6 | Progress | Scores over time; detailed reports (paid) | VO [14] |
| 7 | Gamification | **Buddy Streaks** (shared streaks with friends); WhatsApp group with instructor | VO [15] |
| 8 | Onboarding | 60-s speaking challenge as the early hook; full flow unverified | VO [15] |
| 9 | Navigation | Not determinable | — |
| 10 | Pricing | "Less than $5/month". US IAP $1.99/wk to $59.99/yr. Refund only within 24 h and <30 min of use | VO [15]; SO [19] |
| 11 | Offline / privacy | Call audio **streamed to Hume AI**; **transcripts to OpenAI**; explicit consent required; audio kept "for a limited period" (unspecified). No offline mode found | VO [16] |
| 12 | Complaints | "5.5 band for nonsense speech"; "rated every response positively"; recognition errors; call lag and crashes; refunds | VO [15]; SO [20] |

### 2.3 Duolingo: mass-market gamified courses; AI Video Call moving into Super

| # | Dimension | Finding | Label |
|---|---|---|---|
| 1 | Positioning / user | Mass market. Q3 2026: 64.9M DAU, 153.1M MAU, 13.1M paid subscribers | VO [21] |
| 2 | Workflow / session | Path lessons. Video Call with Lily runs ~1 min early in a course, up to ~3 min later. Speaking-answerable exercises "doubled" in 2026 | VO [21][22] |
| 3 | Assessment / feedback | Video Call "won't correct your grammar", "won't be penalized". DET research model scores **intelligibility, not native-likeness** (Spearman 0.82 vs experts; beat GOP, Whisper confidence, Microsoft), with device and L1 bias noted; consumer use unverified. Speaking checks seen as minimal or lenient | VO [22][25]; SO [32] |
| 4 | Vocabulary | Course-embedded; current SRS use unverified | — |
| 5 | Personalisation / placement | Adaptive placement test | SO [30] |
| 6 | Progress | **Duolingo Score** 0–160, CEFR-mapped, but "rises as you complete lessons", so it reflects course position, not measured ability | VO [23]; INF |
| 7 | Gamification | Streaks, leagues, XP. **Energy** replaced Hearts (2025), drew backlash and petitions | SO [26] |
| 8 | Onboarding | Lesson before sign-up raised DAU ~20% (historic A/B test); current flow unverified | SO [29] |
| 9 | Navigation | Not verified | — |
| 10 | Pricing | **Max sunset** Nov 2026; unlimited Video Call goes to all Super users by end of 2026. Super ~$84–96/yr in the US (third-party, uncertain), PPP-adjusted | VO [21]; V3 [24]; SO [31] |
| 11 | Offline / privacy | Stores audio and transcripts, "including training … Duolingo's own artificial intelligence models". Video Call AI cost fell from ~30¢ to "well under one cent" per call via open-weight models. Offline support unclear | V3 [27][24] |
| 12 | Complaints | Energy system; "AI-first" contractor memo backlash (2025); shallow speaking; synthetic voices | SO [26][28][32] |

### 2.4 Speak: conversation-first curriculum (OpenAI-backed)

| # | Dimension | Finding | Label |
|---|---|---|---|
| 1 | Positioning / user | "Not a chatbot", a structured curriculum. Korea-born; $78M at a $1B valuation (Dec 2024). 4.8★, 67K ratings | VO [33][34]; SO [39] |
| 2 | Workflow / session | **Learn → Practice → Apply** with the Speak Tutor. Roleplay = 3 tasks. Audio-driven roleplays and voice-first lessons (Dec 2025) | VO [33][37]; V3 [36] |
| 3 | Assessment / feedback | Pronunciation and "how natural your phrasing is". Review found feedback brief and drills **lenient** (perfect scores with errors). Live Roleplays on OpenAI Realtime | VO [33]; V3 [36]; SO [39] |
| 4 | Vocabulary | Phrasebook plus Smart Review mastery scores; phrases not reused in chats; unit-tied "vocab side quests" | V3 [36]; VO [37] |
| 5 | Personalisation / placement | Mistake-driven review; Premium Plus "Made for You" lessons. Self-rated level; long onboarding quiz; mic permission requested late | VO [33]; SO [40] |
| 6 | Progress | **Speak Level** ability measure (Dec 2025; KR/JP/TW first, full rollout 2026); no cumulative error tracking in Premium | VO [37]; V3 [36] |
| 7 | Gamification | Streaks with save, freeze (hold 2), challenge and auto-repair; Leagues | VO [38] |
| 8 | Onboarding | Multi-question profile quiz before first lesson | SO [40] |
| 9 | Navigation | Home plus a Free Talk tab confirmed; full count unverified | VO [37] |
| 10 | Pricing | 7-day trial, Premium or Premium Plus; US IAP up to **$164.99/yr** and $39.99/mo | VO [33][34] |
| 11 | Offline / privacy | Audio used to "improve … automatic speech recognition" under legitimate interests; **OpenAI and Microsoft** receive audio; no audio retention period or training opt-out. Audio linked to identity | VO [35][34] |
| 12 | Complaints | Lenient recognition; shallow feedback; repetitive at higher levels; few voices; unclear tier limits | V3 [36] |

### 2.5 Praktika: AI avatar tutors

| # | Dimension | Finding | Label |
|---|---|---|---|
| 1 | Positioning / user | AI tutors at "~$8/month" vs "~$400/month" for a human tutor. 4.7★, ~169K ratings | VO [41][42] |
| 2 | Workflow / session | Animated avatar chats that "remember your context"; 1,000+ lessons; lessons built from uploaded PDFs, photos, videos | VO [41][42] |
| 3 | Assessment / feedback | Pronunciation, grammar and word-choice feedback; user-set **Soft / Balanced / Strict** correction. Per-line button only; no lesson summary or error list | VO [41]; V3 [43] |
| 4 | Vocabulary | Personal dictionary; **no SRS**, words never resurface; flashcards recently added | V3 [43]; VO [42] |
| 5 | Personalisation / placement | Goals, interests, **self-selected CEFR**, no placement test | V3 [43] |
| 6 | Progress | Speaking metrics; changing goals resets progress | VO [42]; V3 [43] |
| 7 | Gamification | Streaks, challenges | VO [42] |
| 8 | Onboarding | ~5 setup screens plus avatar choice before first chat | V3 [43] |
| 9 | Navigation | Practice tab (~140 topics) confirmed; rest unverified | V3 [43] |
| 10 | Pricing | US IAP $9.99/mo; $59.99–$139.99/yr | VO [42] |
| 11 | Offline / privacy | "Offline mode isn't available yet". Recordings used to "fine tune our AI models" (legitimate interests); no opt-out or retention period | VO [45][44] |
| 12 | Complaints | Recognition errors; Stripe/Apple billing mismatches; "creepy" avatars; no memory; rigid paths | SO [46]; V3 [43] |

### 2.6 Grammarly: communication-quality reference (writing only)

| # | Dimension | Finding | Label |
|---|---|---|---|
| 1 | Positioning | AI writing assistant; parent company rebranded **Superhuman** (Oct 2025) | SO [50] |
| 2–3 | Feedback model | Free: grammar, **tone detection**. Paid: tone adjustment, rewrites, "fluency for English". Inline, one-tap suggestions | VO [47] |
| 4–9 | Other dimensions | Not applicable to speaking; not researched | — |
| 10 | Pricing | India-only Plus ₹392/member/mo annual (₹1,000 monthly); US Pro ~$12/mo annual | VO [47]; SO [51] |
| 11 | Offline / privacy | "An online application", no offline use. "We do not sell or monetize user … content". **User toggle** to stop training on content | VO [49][48] |
| 12 | Complaints | Not researched | — |

### 2.7 BoldVoice: accent training with coach videos

| # | Dimension | Finding | Label |
|---|---|---|---|
| 1 | Positioning / user | "People hear you, not your accent". Non-native professionals and interviewees. 2M+ users claimed; 4.8★, 54K ratings | VO [52][53] |
| 2 | Workflow / session | Record → analysis → fix → drills → scores. Hollywood dialect-coach videos. **5–10 min daily** | VO [52][53] |
| 3 | Assessment / feedback | "Sound-level analysis"; stress feedback; native comparison. Published accent-strength regression research | VO [52]; SO [55] |
| 4 | Vocabulary | Not a focus | — |
| 5 | Personalisation / placement | 14-sentence assessment giving % and level; personalised plan | SO [56]; VO [52] |
| 6 | Progress | Scores and "comprehensive analytics" | VO [53] |
| 7 | Gamification | None described | — |
| 8 | Onboarding | Assessment first | SO [56] |
| 9 | Navigation | Not determinable | — |
| 10 | Pricing | 7-day trial; **$149.99/yr**; other IAP $14.99–$199.99 | VO [52][53] |
| 11 | Offline / privacy | Stores voice samples and uses them to improve ML models; "may be retained"; opt-out only by disabling voice features. Store label omits Audio Data, which is inconsistent with the policy | VO [54][53]; INF |
| 12 | Complaints | Price, billing and refunds (small samples); "robotic American" voices | SO [57]; VO [53] |

### 2.8 Yoodli: AI roleplay for communication skills (enterprise-led)

| # | Dimension | Finding | Label |
|---|---|---|---|
| 1 | Positioning / user | "The AI Platform for Human Skills". Primarily enterprise (sales, onboarding, managers); individuals secondary. $40M Series B (Dec 2025) | VO [58]; SO [60] |
| 2 | Workflow / session | Roleplays, personas, learning journeys, in-flow coaching, measurement dashboards; Zoom/Teams/CRM integrations | VO [58] |
| 3 | Assessment / feedback | Scenario **rubrics** (clarity, empathy, objection handling), framed as one strength plus one improvement. Historically filler/pacing analytics. Competitor claims no STAR content coaching | VO [58]; SO [61][62] |
| 4–5 | Vocab / personalisation | Custom rubrics (Business tier) | VO [59] |
| 6–7 | Progress / gamification | Measure dashboards; certifications | VO [58][59] |
| 8–9 | Onboarding / nav | Not verified | — |
| 10 | Pricing | Free = **5 lifetime sessions**; Pro $8/mo (annual); Advanced $20/mo; Business by quote; no refunds | VO [59] |
| 11 | Offline / privacy | Cloud only. **Excluding data from AI training requires Advanced+** (privacy sold as an upsell); SOC 2 Type 2 | VO [59] |
| 12 | Complaints | "Stingy" free tier; few public reviews | SO [62] |

### 2.9 Orai: presentation delivery coach

| # | Dimension | Finding | Label |
|---|---|---|---|
| 1 | Positioning / user | AI "pocket coach" for presenters (PMs, founders, sales); "a few minutes a day". 4.6★, 3.8K ratings | VO [63][64] |
| 2 | Workflow / session | Pick scenario (pitch, toast, tough question) → speak → **scorecard plus one concrete tip** | VO [63] |
| 3 | Assessment / feedback | Filler words, pace (WPM), clarity %, energy, confidence, conciseness; transcript and playback | VO [63][64] |
| 4–5 | Vocab / personalisation | Not verified | — |
| 6–7 | Progress / gamification | Weekly trends ("+18% clarity"); streaks | VO [63] |
| 8–9 | Onboarding / nav | Not verified | — |
| 10 | Pricing | $12.99/mo; $49.99/yr; **$99.99 lifetime** | VO [64] |
| 11 | Offline / privacy | Audio linked to identity; usage data used for tracking; no offline claim | VO [64] |
| 12 | Complaints | Bugs; inaccurate pause detection; development cadence unclear | SO [65] |

### 2.10 Others (brief)

| Product | Key facts | Label |
|---|---|---|
| **Speeko** | Public speaking. Pace, fillers, **intonation**, sentiment, talk time, **hedging/redundant wording**; ~2 min/day; AI roleplay "Convos"; voice-coach content. Claims "no one has access to your audio data except you". $24.99/mo, $79.99–$99.99/yr | VO [66][67] |
| **Poised** | Desktop real-time meeting coach (speakshare, fillers, hedging words). **"Shutting down on 10/8/2026"** | VO [68] |
| **Cambly** | Human tutors: Small Groups $12, Private+ $28, Pro $40/mo (promo). Recordings, transcripts, automated post-lesson feedback; AI practice between lessons (2024) | VO [69]; SO [70] |
| **TalkPal** | Multi-language AI chat (roleplay, debate, call modes); free tier ~10 min/day; prices vary ($6.99–$19.99/mo); billing complaints; "word-level scoring only, no phoneme feedback" | VO [71]; SO [72] |
| **Indie offline apps** | Small iOS apps claim on-device or offline pronunciation scoring (e.g., "Articulate", "AnyAccent", "MiiTalk"); quality unknown | SO [73] |

---

## 3. Cross-product comparison

| Dimension | ELSA | Stimuler | Duolingo | Speak | Praktika | BoldVoice | Yoodli | Orai |
|---|---|---|---|---|---|---|---|---|
| Workflow / session length | Coach hub, 5 lessons/day | Scored tasks, ~10 min+ | Path lessons; calls 1–3 min | Learn→Practice→Apply | Avatar chats | Drills, 5–10 min | Rubric roleplays | Scenario, then scorecard; minutes |
| Speaking assessment | Phoneme claim, EPS | 4 dimensions + fillers/pace; IELTS band | Minimal correction | Pronunciation and phrasing; lenient | Per-line; strictness setting | Sound-level, accent % | Rubric | WPM, fillers, clarity, energy |
| Vocabulary | Study sets | In-speech score | In course | Phrasebook + review | Dictionary, no SRS | — | — | — |
| Personalisation / placement | <10-min speaking test | Pre-test, roadmap | Placement test | Self-rated | Self-selected CEFR | 14-sentence test | Custom rubrics | — |
| Progress measure | EPS (CEFR/IELTS) | Band and scores | Score = course position | Speak Level (new) | Metrics; resets | % score | Dashboards | Weekly trends |
| Gamification | Leaderboard, games | Buddy Streaks | Streaks, leagues, Energy | Streaks + freeze, leagues | Streaks | — | Certifications | Streaks |
| Onboarding to first speech | Assessment first | 60-s challenge | Lesson before sign-up (historic) | Long quiz | ~5 screens + avatar | Assessment first | ? | Immediate scenario |
| Primary tabs | 4 | ? | ? | Home, Free Talk + ? | Practice + ? | ? | ? | ? |
| Pricing (US, approx.) | $19.99/mo–$129.99/yr | <$5/mo | Super ~$84–96/yr | ≤$164.99/yr | $9.99/mo | $149.99/yr | Free (5), $8–20/mo | $49.99/yr; $99.99 lifetime |
| Offline | **No** | Not found | Unclear | Not found | **No** | Not found | No | Not found |
| Voice-data privacy | Cloud (AWS); training silent | Hume AI + OpenAI | Trains own AI | OpenAI + Microsoft; trains ASR | Fine-tunes models | Trains ML | Training unless Advanced | Audio linked |
| Top complaints | Billing, strictness | Inflated scores, crashes | Energy, shallow speaking | Lenient, shallow | Recognition, billing | Price, billing | Stingy free tier | Bugs |

Sources: see §2.1–2.9. "?" means not verified.

---

## 4. Category patterns and gaps

### 4.1 Table stakes (each seen in 3 or more products)

1. A speak-first loop with **instant feedback after every attempt** (ELSA, Stimuler, Speak, Praktika, BoldVoice, Orai, Speeko). VO
2. **Scenario roleplay that includes job interviews** (ELSA, Stimuler, Speak, Praktika, Speeko, Yoodli; Duolingo open calls). VO
3. **Multi-dimension scores.** ESL apps score pronunciation, fluency, grammar and vocabulary; delivery apps score WPM, fillers, pauses, clarity and energy. VO
4. **Mapping to external scales** (CEFR, IELTS, TOEFL, TOEIC) by ELSA, Stimuler and Duolingo; Speak Level is new. VO
5. **Streaks with protection, and leaderboards or leagues** (Duolingo, Speak, ELSA, Stimuler, Orai). VO
6. A **personalised path** built from goals, level and interests. **Short sessions** of 2–10 minutes. VO
7. **Freemium with tight caps**, a 7–14-day trial, an annual-plan push and regional pricing. VO
8. **Cloud voice processing**, audio linked to identity, unspecified retention, and model training by default under "legitimate interests". VO

### 4.2 Gaps and underserved needs

| Gap | Evidence | Strength |
|---|---|---|
| **G1 Trust in scores.** Calibration fails both ways; no confidence shown; no abstaining on poor audio | ELSA strict [9]; Speak lenient [36]; Stimuler "5.5 for nonsense" [15]; unvalidated ELSA claim [3]; Microsoft lists noise, distance, speed and multi-speaker limits [75] | Strong |
| **G2 Privacy and offline.** No scaled player works offline; voice goes to third-party AI; training is opt-out at best; privacy sold for a fee | [5][45][49][35][16][44][54][27]; Yoodli [59] | Strong |
| **G3 Professional communication for non-native speakers.** Delivery coaches are not ESL-aware; ESL apps only just adding coaches; individual tools retreating | ELSA Professional tentative [7]; Yoodli enterprise [58]; Poised closing [68]; Orai/Speeko delivery-only [63][66] | Moderate |
| **G4 Honest progress.** Scores track lessons or habit; bands come from 60-s samples; ±1-band agreement | [23][15][3][43] | Strong |
| **G5 Low bandwidth and low price.** Heavy media and connectivity; demand at <$5/mo; affordability is the main access barrier | [5][12][15][18][47][74] | Moderate |
| **G6 Vocabulary that transfers to speech.** SRS missing, or phrases not reused | [43][36][9] | Moderate |
| **G7 Billing trust.** Trial traps, refund friction, dual billing | [10][46][57][72][19] | Strong |
| **G8 Actionable feedback.** Feedback buried or brief; "one tip" is the exception | [43][36] vs Orai [63] | Moderate |

INF: Cloud AI has a per-session cost and needs connectivity. That is why free tiers are capped hard (Yoodli 5 lifetime sessions, ELSA 3 AI chats [8], TalkPal ~10 min/day). On-device evaluation has near-zero marginal cost, which makes a generous free tier sustainable.

---

## 5. SpeakCraft current state vs category

**Baseline (VERIFIED, RD-03).**
- **Platform and data:**
  - Native Android (Kotlin/Compose). Google Sign-In is **mandatory before any use**.
  - Progress syncs to Firestore. Session transcripts (≤15k characters) are **saved to Firestore although the Settings privacy text says transcripts stay on-device**.
  - `android:allowBackup=true` with empty rules.
- **Speech pipeline:**
  - Live transcription (SpeechRecognizer) is **off by default** and captures only the first utterance.
  - File transcription is stubbed and always returns empty. The bundled Whisper/TFLite path is fake.
  - So, by default, feedback is **acoustic-only** (loudness, pauses, pitch spread). Grammar and vocabulary scores are null.
- **Scoring:**
  - The freeform "pronunciation" score is actually loudness + pitch variation + voicing ratio.
  - Grammar uses ~40 regex rules with notable false positives.
  - A **good evidence gate refuses to score silent or very short audio**. A prior bug that gave ~78/100 for empty recordings is fixed.
- **Breadth:**
  - ~23 navigation routes. 15 curriculum modules derived from a single YouTube creator (`@askvinh`, also a bottom-tab label). 150 activities in 10 modules with 25 templates.
  - ~10 overlapping practice "labs".
  - Template-based conversation simulator and "AI mentor" (no LLM; simulated streaming).
  - YouTube page scraping. Video body-language analysis whose MediaPipe models are not bundled.
- **Engagement:** streaks and a daily 9-step checklist; **no reminders or notifications**.
- **Distribution:** GitHub APK sideloading with an in-app self-updater; no Play Store presence; no pricing.
- **Offline:** local rule engines work offline, but sign-in and many flows assume connectivity.

| Area | Category norm | SpeakCraft today (RD-03) | Opportunity |
|---|---|---|---|
| Time to first speech | Assessment or lesson within minutes; Duolingo historically ran a lesson before sign-up [29] | Blocked by mandatory Google Sign-In | REC: speak in the first minute, no account needed |
| Transcript-based feedback | Universal (all competitors give word or phrase feedback) | Off by default; first utterance only; file path stubbed, so no grammar or vocab by default | REC: real on-device transcription on by default (engine via ADR) |
| Score honesty | Opaque scores; calibration complaints (G1) | "Pronunciation" is really loudness and pitch; regex grammar false positives; **evidence gate is a strength** | REC: rename to what is measured; keep and extend the gate; show confidence |
| Privacy | Cloud and training by default (G2) | Transcripts to Firestore **contradict** in-app privacy text; allowBackup open | REC: make claims true first, then market verifiable privacy |
| Offline | None offline (G2) | Rule engines offline, but sign-in and flows need network | REC: full core loop in airplane mode |
| Information architecture | ELSA 4 tabs [6]; focused hubs | ~23 routes, ~10 overlapping labs, creator-named tab | REC: consolidate into a few purpose-led areas |
| Content provenance | Expert or proprietary curricula | Single creator; page scraping | INF: legal and fragility risk; REC: own or licensed content |
| "AI" conversation | Cloud LLM roleplay | Templates presented with simulated streaming | REC: honest scripted roleplay; never imply an LLM |
| Professional comms | Emerging coaches (G3) | Breadth exists, but feedback is acoustic-only | REC: structured interview and presentation packs with transparent rubrics |
| Progress | Course-position or band scores (G4) | Streaks + 9-step checklist | REC: repeatable benchmark; habit kept separate from ability |
| Retention | Streaks + protection + reminders | Streaks; **no reminders** | REC: opt-in, respectful reminders; lighter daily goal |
| Distribution and pricing | Store presence; freemium | Sideloaded APK, self-updater, no pricing | REC: decide store and monetisation; transparent terms (G7) |
| Low-end devices | Heavy media, 250 MB apps [15] | Unbundled video features; scraping needs network | REC: small core install; optional packs |

---

## 6. Differentiation opportunities

| # | RECOMMENDATION | Rationale | Evidence |
|---|---|---|---|
| D1 | **Verifiable privacy.** Core loop runs offline without an account. Audio is ephemeral by default. A "what left this device" ledger. Cloud features are opt-in only, with the vendor named | Every scaled competitor is cloud-based and trains on voice; privacy is sold as an upsell | G2; [35][16][44][54][59] |
| D2 | **Honest scoring.** Abstain on weak signal. Show confidence or ranges. Label each metric as *measured* (WPM, pauses) or *estimated*. No IELTS/CEFR equivalence without validation | Trust failures are the most consistent complaint; SpeakCraft's evidence gate already points this way | G1; [9][36][15][3][75]; RD-03 |
| D3 | **Intelligibility over native-likeness.** Feedback asks "would a listener understand this word?" | Research direction and competitor positioning agree, and it is more achievable on-device | [25][14] |
| D4 | **On-device-robust communication metrics.** Rate, pause distribution, fillers, fluent-run length, restarts, lexical variety, hedges and apologies, signposting, key-point coverage | Delivery apps prove users value these; no one combines them with ESL guidance | G3; [63][66] |
| D5 | **Professional packs with transparent rubrics** (STAR interview, status update, presentation open and close, polite disagreement), with one prioritised tip per attempt | The professional gap plus the actionability gap; deterministic checks can be explained | G3, G8; [7][58][63] |
| D6 | **Repeatable benchmark check-ins** every 2–4 weeks, showing a trend with a noise band | No competitor measures ability honestly over time | G4; [23][37] |
| D7 | **Low-end and low-bandwidth first.** Small base install, optional packs, fully offline after install, generous free tier | Underserved markets; near-zero marginal cost | G5; [5][15][74] |
| D8 | **Vocabulary for production.** "Review" means saying the word in a spoken prompt, confirmed by the on-device recogniser | Competitors' vocabulary features don't transfer to speech | G6; [43][36] |
| D9 | **Transparent monetisation** (clear trial or none, easy cancel, possibly a one-time unlock) | Billing distrust is category-wide; lifetime SKUs exist | G7; [64][8] |

### 6.1 What SpeakCraft should not try to compete on

| Do not compete on | Why (evidence / inference) |
|---|---|
| **Phoneme-level mispronunciation diagnosis** | ELSA claims models trained on "millions of non-native recordings" [3]; BoldVoice runs dedicated accent research [55]. INF: generic on-device ASR is built to be accent-robust, so it hides errors, and GOP or ASR-confidence methods underperform purpose-built models [25]. Offer word-level intelligibility instead. |
| **Open-ended LLM conversation** | Cloud leaders run large models (Speak [39], Stimuler [16], Duolingo [21]), and the cost is collapsing toward commodity [24]. On-device LLMs (Gemini Nano Prompt API) are limited to recent flagships and run foreground-only [76], which does not fit low-end targets. Use honest scripted or branching roleplay. |
| **Avatars and video coaching** | Bandwidth- and storage-heavy (Praktika [41], BoldVoice [52]); contradicts D7. |
| **Validated exam-score prediction** | Needs large human-rated datasets; competitor claims are unvalidated [3] or doubted by users [15]. |
| **Human tutors** | Cambly and Preply's territory [69][70]. INF: at most, let users export a recording locally to share with a teacher. |
| **Content volume** | ELSA cites 8,000+ lessons [2]. Win on depth in fewer professional scenarios. |

---

## 7. Patterns worth adopting vs patterns to avoid

Inspiration only. **No competitor interface, layout, visual design, copy or character should be copied.**

| Adopt (principle) | Seen at | Avoid (anti-pattern) | Seen at |
|---|---|---|---|
| One concrete, prioritised tip per attempt | Orai [63] | Opaque single scores with no explanation or uncertainty | ELSA, Stimuler [3][15] |
| User-selected correction strictness | Praktika [41] | Inflated or lenient scoring that flatters | Speak, Stimuler [36][15] |
| Streak protection (freeze or repair) to forgive slips | Speak [38] | Energy or hearts friction; league pressure | Duolingo [26] |
| Intelligibility framing ("be understood") | Stimuler, DET research [14][25] | Exam-band claims from very short samples | Stimuler [15] |
| Training opt-out as a simple user toggle | Grammarly [48] | Privacy only on higher tiers | Yoodli [59] |
| Shared or social accountability (opt-in) | Stimuler Buddy Streaks [15] | Store privacy labels that don't match the policy | BoldVoice [53][54] |
| Strength + improvement feedback framing | Yoodli [58] | Trial traps, 24-hour refund windows, dual billing | [10][19][46] |
| Lesson or practice before account creation | Duolingo (historic) [29] | Progress resets when goals change | Praktika [43] |
| Lifetime or one-time purchase option | Orai, ELSA [64][8] | Simulated "AI" that implies capabilities it lacks | INF (applies to SpeakCraft, RD-03) |

---

## 8. Implications for the redesign

- **RD-11-I1 — Make privacy claims true before marketing them.**
  - Stop saving transcripts to Firestore by default, or correct the in-app text.
  - Lock down backup rules.
  - Offer an account-free core mode.

  *Basis:* RD-03 contradiction; G2; D1.
- **RD-11-I2 — Real on-device transcription, on by default, is the precondition for credible feedback.** Without it, grammar, vocabulary and fluency feedback cannot exist, and every competitor provides transcript-based feedback. Engine choice goes through an ADR on the existing stack. *Basis:* RD-03; §3.
- **RD-11-I3 — Honest naming and confidence.**
  - Rename acoustic scores to what they measure (e.g., "voice energy and variation", not "pronunciation").
  - Keep and extend the evidence gate.
  - Show confidence.
  - Never present templates as an LLM.

  *Basis:* G1; D2; RD-03.
- **RD-11-I4 — Positioning: ESL-aware professional communication with intelligibility feedback.** Not accent elimination, not open chat. *Basis:* G3; D3–D5; §6.1.
- **RD-11-I5 — Consolidate the information architecture.** Move from ~23 routes and ~10 labs to a small number of purpose-led areas. Remove creator-specific branding from navigation. *Basis:* ELSA's 4 tabs [6]; RD-03.
- **RD-11-I6 — Separate habit metrics from ability metrics.** Introduce a repeatable benchmark task. *Basis:* G4; D6.
- **RD-11-I7 — Retention without manipulation.**
  - Add opt-in reminders (currently none).
  - Shorten the 9-step daily checklist toward the 2–10 minute category norm.
  - Forgive missed days.

  *Basis:* §4.1 item 6; [38][53][67]; RD-03.
- **RD-11-I8 — Offline and low-end first.**
  - The whole core loop works in airplane mode.
  - The core does not depend on YouTube scraping or unbundled ML models.
  - Small install, with optional packs.

  *Basis:* G5; D7; RD-03.
- **RD-11-I9 — Content provenance.** Replace single-creator, scraped content with owned or licensed material that is versioned for offline use. *Basis:* RD-03; INF on legal and fragility risk.
- **RD-11-I10 — Distribution and monetisation decision.** Decide on store presence vs sideloading, and on transparent pricing (or none) with honest trial terms. *Basis:* G7; D9; RD-03.

---

## 9. Sources and unverified items

### 9.1 Sources (all accessed 2026-11-20)

1. ELSA homepage — https://elsaspeak.com/en/ (VO)
2. ELSA Speak, US App Store — https://apps.apple.com/us/app/elsa-learn-and-speak-english/id1083804886 (VO)
3. ELSA efficacy — https://elsaspeak.com/en/efficacy (VO)
4. ELSA Corp privacy notice (Mar 2025) — https://elsaspeak.com/en/privacy/ (VO)
5. ELSA FAQ, connectivity — https://elsaspeak.com/en/faqs/elsa-isnt-hearing-me-speak (VO)
6. "Discover the New ELSA Speak Experience" (4 Jan 2026) — https://blog.elsaspeak.com/en/discover-the-new-elsa-speak-experience/ (VO)
7. "What is the ELSA Professional plan" (24 Nov 2025) — https://blog.elsaspeak.com/en/what-is-the-elsa-professional-plan (VO)
8. Weblio ELSA pricing (JP, May 2026) — https://ninja.weblio.jp/elsaspeak-pricing (V3)
9. FluentU ELSA review (2024) — https://fluentu.com/blog/reviews/elsa-speak (V3)
10. ELSA reviews — https://uk.trustpilot.com/review/elsaspeak.com ; https://www.capterra.com/p/240698/ELSA-Speak/reviews/ (SO)
11. ELSA phoneme feedback — https://blog.elsaspeak.com/en/advantage-of-elsa-feedback ; https://www.dell.com/nl-be/perspectives/ai-app-helps-non-native-english-speakers-ace-pronunciation/ (SO)
12. ELSA help, ideal conditions — https://elsanow.freshdesk.com/support/solutions/articles/31000154780-what-are-the-ideal-conditions-to-practice-with-elsa- (SO; 404)
13. ELSA School speech FAQ — https://elsaschool-support.freshdesk.com/en/support/solutions/articles/31000177613-faq-about-elsa-s-speech-analysis-technology (SO; 404)
14. Stimuler homepage — https://stimuler.tech/ (VO)
15. Stimuler, US App Store — https://apps.apple.com/us/app/id1627501632 (VO)
16. Stimuler privacy policy — https://stimuler.tech/privacy-policy (VO)
17. Stimuler blog, "Cambly vs Stimuler" (Feb 2026; vendor) — https://stimuler.tech/blog/cambly-vs-stimuler-which-app-actually-helps-you-speak-better (VO)
18. Stimuler funding (Apr 2025) — https://india.entrepreneur.com/news-and-trends/stimuler-raises-usd-375-mn-funding-to-scale-voice-first/490839 (SO)
19. Stimuler refund policy — https://stimuler.tech/refund-cancellation-policy (SO)
20. Stimuler Google Play reviews — https://grand-screen.com/apps/stimuler-ai-english-fluency/reviews/ (SO)
21. Duolingo Q3 2026 shareholder letter (3 Nov 2026) — https://www.sec.gov/Archives/edgar/data/1562088/000162828026069474/q3fy26duolingo9-30x26share.htm (VO)
22. Duolingo, Video Call (24 Sep 2024) — https://blog.duolingo.com/video-call/ (VO)
23. Duolingo Score (23 Oct 2024) — https://blog.duolingo.com/duolingo-score/ (VO)
24. Subscription Insider, Max dropped (5 Nov 2026) — https://www.subscriptioninsider.com/blog/duolingo-drops-premium-tier-after-ai-feature-costs-plunge (V3)
25. DET intelligibility pronunciation model (30 Jul 2025) — https://blog.englishtest.duolingo.com/new-research-in-language-learning-a-pronunciation-scoring-model-built-around-intelligibility-not-imitation (VO)
26. Duolingo Energy — https://blog.duolingo.com/duolingo-energy/ ; https://www.androidauthority.com/quitting-duolingo-energy-system-3599842/ (SO)
27. ConductAtlas excerpt, Duolingo privacy (26 May 2026) — https://conductatlas.com/platform/duolingo/duolingo-privacy-policy/ai-audio-recordings-stored-for-product-improvement/ (V3)
28. Duolingo AI-first backlash — https://www.theregister.com/2025/04/29/duolingo_ceo_ai_first_shift ; https://fortune.com/2025/08/18/duolingo-ceo-admits-controversial-ai-memo-did-not-give-enough-context-insists-company-never-laid-off-full-time-employees/ (SO)
29. Duolingo delayed sign-up tests — https://review.firstround.com/the-tenets-of-a-b-testing-from-duolingos-master-growth-hacker (SO)
30. Duolingo placement test — https://blog.duolingo.com/partial-credit-improvements-to-duolingos-placement-test/ (SO)
31. Duolingo price guides — https://languageappguide.com/pricing/duolingo-cost/ ; https://duolingoguides.com/how-much-is-super-duolingo/ (SO)
32. Duolingo speaking-depth criticism — https://learn.g2.com/does-duolingo-work ; https://aftermath.site/duolingo-ai.md (SO)
33. Speak homepage — https://www.speak.com/ (VO)
34. Speak, US App Store — https://apps.apple.com/us/app/speak-learn-english/id1286609883 (VO)
35. Speak privacy notice (06/05/2026) — https://www.speak.com/privacy (VO)
36. LanguaTalk Speak review (2 Nov 2026; vendor-affiliated) — https://languatalk.com/blog/speak-app-review/ (V3)
37. Speak winter 2025 release (10 Dec 2025) — https://www.speak.com/blog/winter-2025 (VO)
38. Speak help, Streak 101 — https://help.speak.com/en/articles/11645358-streak-101-how-streaks-work-how-to-keep-them (VO)
39. Speak funding / Realtime API — https://techcrunch.com/2024/12/10/openai-backed-speak-raises-78m-at-1b-valuation-to-help-users-learn-languages-by-talking-out-loud ; https://latent.space/wiki/speak-ba0259c2/ (SO)
40. Speak onboarding descriptions — https://lingtuitive.com/blog/speak-review ; https://screensdesign.com/articles/language-learning-app-onboarding/ (SO)
41. Praktika homepage — https://praktika.ai/ (VO)
42. Praktika, US App Store — https://apps.apple.com/us/app/praktika-ai-language-tutor/id1624701477 (VO)
43. LanguaTalk Praktika review (23 Jun 2026; vendor-affiliated) — https://languatalk.com/blog/praktika-review/ (V3)
44. Praktika privacy policy (10 Aug 2026) — https://praktika.ai/privacy (VO)
45. Praktika help, offline — https://intercom.help/praktika-ai/en/articles/12009296-does-praktika-support-offline-use (VO)
46. Praktika Trustpilot — https://www.trustpilot.com/review/praktika.ai (SO)
47. Grammarly plans (India pricing rendered) — https://www.grammarly.com/plans (VO)
48. Grammarly Trust Center — https://www.grammarly.com/trust (VO)
49. Grammarly offline FAQ — https://support.grammarly.com/hc/en-us/articles/115000091011-Can-I-use-Grammarly-offline (VO)
50. Grammarly → Superhuman rebrand (29 Oct 2025) — https://techcrunch.com/2025/10/29/grammarly-rebrands-to-superhuman-launches-a-new-ai-assistant/ (SO)
51. Grammarly US pricing (third-party) — https://www.usecarly.com/blog/grammarly-pricing/ (SO)
52. BoldVoice homepage — https://www.boldvoice.com/ (VO)
53. BoldVoice, US App Store — https://apps.apple.com/us/app/boldvoice-accent-training/id1567841142 (VO)
54. BoldVoice privacy policy (12 Oct 2026) — https://www.boldvoice.com/privacy (VO)
55. BoldVoice accent-strength research — https://accent-strength.boldvoice.com/ (SO)
56. Almahdi & Smith, BoldVoice review, *LLT* 29(1), 2025 — https://www.lltjournal.org/item/1202/ (SO; fetch failed)
57. BoldVoice complaints — https://www.complaintsboard.com/boldvoice-b148659 ; https://justuseapp.com/en/app/1567841142/boldvoice-accent-training/reviews (SO)
58. Yoodli homepage — https://yoodli.ai/ (VO)
59. Yoodli pricing — https://yoodli.ai/pricing (VO)
60. Yoodli Series B (5 Dec 2025) — https://techcrunch.com/2025/12/05/ex-googlers-yoodli-triples-valuation-to-300m-with-ai-built-to-assist-not-replace-people (SO)
61. Yoodli blog, filler words — https://yoodli.ai/blog/how-to-stop-using-filler-words (SO)
62. FinalRound AI Yoodli review (competitor) — https://www.finalroundai.com/blog/yoodli-review-pros-cons (SO)
63. Orai homepage — https://orai.com/ (VO)
64. Orai, US App Store — https://apps.apple.com/us/app/orai-improve-public-speaking/id1203178170 (VO)
65. Orai reviews / changelog — https://justuseapp.com/en/app/1203178170/orai-improve-public-speaking/reviews ; https://apptopia.com/google-play/app/com.orai/about (SO)
66. Speeko homepage — https://www.speeko.co/ (VO)
67. Speeko, US App Store — https://apps.apple.com/us/app/speeko-public-speaking-coach/id1071468459 (VO)
68. Poised homepage (shutdown notice) — https://www.poised.com/ (VO)
69. Cambly English page — https://www.cambly.com/english?lang=en (VO)
70. Cambly AI features — https://support.cambly.com/hc/en-us/articles/25429180057229-AI-at-Cambly (SO)
71. TalkPal homepage — https://talkpal.ai/ (VO)
72. TalkPal pricing and reviews — https://languatalk.com/blog/talkpal-review/ ; https://speakshark.com/blog/talkpal-review-2026 (SO)
73. Indie offline pronunciation apps — https://apps.apple.com/us/app/-/id6753924420 ; https://apps.apple.com/us/app/id6758423887 ; https://apps.apple.com/app/id6520394022 (SO)
74. GSMA, State of Mobile Internet Connectivity 2025 — https://www.gsma.com/somic/the-state-of-mobile-internet-connectivity-2025/ (SO)
75. Microsoft, Pronunciation Assessment characteristics and limitations — https://learn.microsoft.com/en-us/azure/foundry/responsible-ai/speech-service/pronunciation-assessment/characteristics-and-limitations-pronunciation-assessment (V3)
76. Google ML Kit GenAI APIs — https://developers.google.com/ml-kit/genai (SO)

Internal: **RD-03** — SpeakCraft code audit (all §5 baseline facts).

### 9.2 Unverified items (do not rely on)

- Primary tab structures for Duolingo, Stimuler, BoldVoice, Yoodli and Orai; full tab counts for Speak and Praktika.
- Whether ELSA Professional launched, and at what price.
- Duolingo's current offline support and Super prices; whether the DET intelligibility model is used in the consumer app.
- How Stimuler calibrates IELTS bands; the *LLT* verdict on BoldVoice.
- Any independent, peer-reviewed accuracy figures for ELSA, Stimuler, Speak or Praktika scoring.
- Offline quality of the indie apps [73]; Speeko's audio-handling details beyond its homepage claim [66].
