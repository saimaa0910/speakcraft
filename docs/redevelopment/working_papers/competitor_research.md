# Competitor Research: Spoken-English and Communication Coaching Apps (2025–2026)

**Purpose:** Inform the redesign of a privacy-first, offline-capable Android app for spoken English: pronunciation, fluency, vocabulary, and professional, interview and presentation communication. By default it evaluates speech on the device, with no cloud AI.
**Research date / access date for every source below:** 2026-11-20 (unless a source's own publication date is given).
**Method:** Web search plus page fetches of official sites, App Store listings (US storefront), privacy policies, help-centre articles, a regulatory filing, and selected third-party reviews. Google Play listings could not be fetched (pages render client-side), so store data comes from the iOS App Store.

## Evidence labels used throughout

| Label | Meaning |
|---|---|
| **[V-O]** | Verified: the page was opened and is first-party (company site, store listing written by the developer, privacy policy, help centre, SEC filing). Note that first-party claims are verified as *claims*, not as true performance. |
| **[V-3]** | Verified: the page was opened and is third-party (independent review, press, journal, policy tracker). |
| **[S]** | Search-level only. Reported in search results, but the page was not opened or could not be opened (404 or blocked). Lower confidence. |
| **[I]** | Inference: my interpretation, not directly stated by a source. |
| **[R]** | Recommendation or inspiration for the redesign. |

Source IDs such as `[E2]` refer to the **Sources** list at the end.

**Caveats that apply everywhere**
- Page contents were extracted by an automated fetch tool that summarises pages. Short quotes are as returned by that tool.
- App Store in-app purchase (IAP) lists show prices but often do not say which tier each price belongs to. Prices vary by region, platform (web vs store) and promotion. Treat every price as approximate.
- App Store privacy labels are self-reported by developers and not verified by Apple.
- Several review sites (LanguaTalk, Headway, SpeakShark, Stimuler's blog) promote their own or a competing product. This is flagged where it matters.

---

## 0. Executive summary (decision-relevant)

1. **No major competitor works offline, and all of them process voice in the cloud.** ELSA says it "needs a continuous internet connection to function" [E5]. Praktika says offline mode "isn't available yet" [P5]. Grammarly is "an online application" [G3]. Speak sends audio to OpenAI and Microsoft [Sp3], and Stimuler streams call audio to Hume AI and sends transcripts to OpenAI [St3]. **[V-O]**
2. **Voice data is used for model improvement by default, and retention periods are not specified.** Speak (ASR improvement, under "legitimate interests") [Sp3], Praktika (to "fine tune our AI models") [P4], BoldVoice (to "improve … machine learning models") [B3], Duolingo ("training and running Duolingo's own artificial intelligence models") [D7]. None of the policies opened states a concrete audio retention period. **[V-O / V-3]**
3. **Privacy is already sold as a premium feature.** Yoodli's $8/month Pro tier lets session data be used to improve the platform. Excluding roleplay data from AI training requires the $20/month Advanced tier [Y2]. **[V-O]**
4. **Users do not reliably trust the scores, and calibration fails in both directions.** ELSA's scoring was reported as strict, with some native speakers scoring around 80% or less (2024) [E9]. A 2026 review found Speak's drills "lenient", giving perfect scores to mispronounced words [Sp4]. A Stimuler user reports a 5.5 IELTS-style band for nonsense speech [St2]. ELSA's headline accuracy claim ("93.88% agreement with expert human raters within ±1 CEFR band") comes with no published methodology [E3]. **[V-O / V-3]**
5. **"Progress" scores mostly measure course completion or habit, not ability.** The Duolingo Score "rises as you complete lessons and move through units" [D3]. Streaks, leagues and leaderboards are everywhere (Duolingo, Speak, ELSA, Stimuler, Orai). Speak only launched a "Speak Level" ability measure in Dec 2025, starting with Korea, Japan and Taiwan [Sp5]. **[V-O]**
6. **AI open conversation is becoming commoditised in the cloud.** Duolingo is retiring its Max tier and putting unlimited Video Call into Super [D1]. Per-call AI cost fell from about 30 cents to "well under one cent", mainly through open-weight models [D4]. **[V-O / V-3]** An offline-first app should not try to compete on open-ended LLM conversation. **[I]**
7. **Professional communication is fragmenting.** ESL apps are adding interview, presentation and meeting coaches (ELSA Professional, tentatively March 2026 [E7]). Yoodli has moved toward enterprise roleplay ($40M Series B, Dec 2025 [Y3]). Poised, a desktop meeting coach, is "shutting down on 10/8/2026" [Po1]. Orai and Speeko coach delivery metrics but are not ESL-aware. **[V-O / S]** No product clearly combines ESL-aware feedback with professional structure and delivery coaching on mobile. **[I]**
8. **Low-bandwidth and price-sensitive markets are underserved.** ELSA requires "a significant amount of data" [E5]. Avatars, video calls and a 250 MB iOS app (Stimuler [St2]) assume good connectivity. Stimuler's traction in India, Indonesia and LatAm at "less than $5/month" [St2][St5] shows demand. The GSMA puts the mobile internet "usage gap" at 38% of the world's population, with affordability as the main barrier [M1]. **[V-O / S]**
9. **Offline pronunciation apps exist only as small indie iOS apps** (e.g., "Articulate: AI Pronunciation", "AnyAccent", "MiiTalk") that claim on-device scoring [M4]. **[S]** "Offline" alone is not a moat. Credibility, honest measurement and professional-communication depth have to carry the differentiation. **[I]**

---

## 1. ELSA Speak

**1. Positioning / target user.** Bills itself as the "#1 AI English Speaking App for Fluency and Confidence". It targets professionals, job seekers, students, travellers and expats, and self-learners, and also sells ELSA Business (50+ learners) and ELSA Schools (21+ learners) [E1]. **[V-O]** Developer: Elsa Corp [E2]. About 115K ratings at 4.7★ on the US App Store [E2]. **[V-O]**

**2. Core workflow / session.** The app was redesigned (blog dated 4 Jan 2026) with a **Coach** hub: AI Conversation Coach with a personalised learning path, Pronunciation Coach, Daily Practice, Skills, and Dictionary [E6]. **[V-O]** A 2024 review found the app selected **five lessons per day** [E9]. **[V-3]** Exact session length was not stated by any source.

**3. Speaking assessment and feedback.**
- Real-time pronunciation and word-stress feedback. A "Speech Analyzer" gives "instant detailed feedback" on free speech. AI conversations cover open dialogues, role-plays, job interviews and presentations. Insights cover "pace, hesitation, and grammar score" [E2]. **[V-O]**
- An ELSA representative is quoted as saying the AI pinpoints mistakes "at a phoneme level", with colour-coded text (green = correct), mouth and tongue diagrams, and comparison against American English targets [E12]. **[S]**
- **ELSA Proficiency Score (EPS)** is "mapped to globally recognized CEFR and IELTS benchmarks". The headline claim is "93.88% agreement with expert human raters within ±1 CEFR band". Models are "trained on millions of non-native recordings" [E3]. **[V-O]** The efficacy page cites only internal studies, with **no sample sizes, methodology or independent validation** [E3]. **[V-O]** (**[I]** Agreement "within ±1 CEFR band" is a loose criterion: a true B1 rated anywhere from A2 to B2 counts as agreement.)
- Release notes say speaking scores map to IELTS, TOEFL, CEFR and TOEIC [E2]. **[V-O]**
- A 2024 review called the algorithm "strict", with some native speakers scoring around 80% or less [E9]. **[V-3]**

**4. Vocabulary.** Personalised study sets and a pronunciation dictionary [E2][E9]. The 2024 reviewer found the study sets less useful than other apps' flashcards [E9]. **[V-3]** No evidence of spaced repetition was found. **Not verified.**

**5. Personalisation / placement.** The learning path adapts to "level, goals, progress, preferred accent, and industry focus" [E1]. **[V-O]** Onboarding includes a speaking test of **under ten minutes** that identifies problem sounds and intonation, then suggests lessons [E9]. **[V-3]** The app offers a bilingual AI tutor so beginners can start in their native language [E1]. **[V-O]** Accent options now include American, British, Australian and others [E2]. **[V-O]** (The 2024 review said American only [E9], so this changed.)

**6. Progress tracking.** Visual dashboards across skill areas [E1]. A **Progress** tab with Profile, Achievements and Leaderboard [E6]. Test-score predictions sit in the Speech Analyzer [E1]. **[V-O]**

**7. Gamification / retention.** Achievements, leaderboard, avatars and "interactive AI-powered games", including a Shadowing game [E2][E6]. **[V-O]** No source described streak mechanics. **Not verified.**

**8. Onboarding.** The speaking assessment comes before the first lessons [E9]. **[V-3]** The exact number of steps before the first speaking attempt was **not verified**.

**9. Navigation.** **Four bottom tabs: Coach, AI Chats, Discover, Progress** [E6]. **[V-O]** AI Chats holds the AI Tutor and a Role-Play Lab, with suggested, random or custom scenarios. Discover holds Courses, Topics and Study Sets [E6]. **[V-O]**

**10. Pricing.**
- US App Store IAP list (tiers mostly unlabelled): **$19.99 monthly up to $129.99 per year**, with entries such as $99.99 (1-year Premium), $71.00 (1 year), and $39.99–$58.00 (3 months). 7-day free trial [E2]. **[V-O]**
- Japan (article dated May 2026): Pro ¥8,699/yr or ¥61,999 lifetime; Premium ¥3,380/mo or ¥27,800/yr. Pro caps ELSA AI conversation (wording conflicts between "3 total" and "3/day") and allows only a 30-minute Speech Analyzer trial. Premium is unlimited. The Free tier has about 25 lessons, ads, and no personalised daily plan [E8]. **[V-3]**
- **ELSA Professional** (business coaches "Claire" for interviews, "Liam" for presentations, "Elle" for meetings) was pre-sold with a tentative launch of March 2026. No price was stated [E7]. **[V-O]** Whether it shipped was **not verified**.

**11. Offline / privacy.**
- "ELSA requires a significant amount of data, so it needs a continuous internet connection to function" [E5]. **[V-O]** An ELSA help article reportedly recommends at least 25 Mbps and around 40 Mbps for video lessons, and states that ELSA cannot be used offline because processing happens on ELSA's servers [E13]. **[S]** (The page returned 404 when fetched.)
- Privacy notice (ELSA Corp, last updated March 2025): it collects "audio and video recordings" from users. Retention is "as long as we deem it as reasonably necessary". Data is processed in the US, Europe, Japan, India, Vietnam and Singapore, on AWS. Unnamed "content transcription and analysis services" act as processors. Deletion requests "may" be denied. The notice is **silent on whether audio trains models** [E4]. **[V-O]**
- An ELSA School FAQ says speech is processed on ELSA's own servers with proprietary models, with EU servers for GDPR [E10]. **[S]**
- The App Store label lists Audio Data as linked to the user, and Identifiers as used for tracking [E2]. **[V-O]**

**12. Complaints / weaknesses.** Billing is the most frequent theme: auto-renewal, being charged before the trial ended, hard cancellation, and price mismatches between web and app [E11]. **[S]** Login problems; one 2026 hands-on review gave 2.3/5 for value and reliability [E14]. **[S]** From 2024: content feels overwhelming, IPA-symbol lesson titles, rigid intonation drills, a strict algorithm, and relatively high price [E9]. **[V-3]** Directory listings note occasional recognition errors and feedback that lacks explanation [E11]. **[S]**

---

## 2. Stimuler

**1. Positioning / target user.** "Speak English confidently in moments that matter." Aimed at non-native speakers facing interviews, promotions, meetings, and IELTS/TOEFL [St1]. It states its speech models are "built for non-native speakers" and focus on **being understood rather than sounding native** [St1]. Communities are concentrated in Indonesia, India, Mexico, Colombia and the US [St1]. **[V-O]** Bengaluru company that raised a $3.75M pre-Series A in April 2025 (Lightspeed). It reports 4M+ installs and 45K+ paying users [St5]. **[S]** About 8.8K ratings at 4.8★ on the US App Store [St2]. **[V-O]**

**2. Core workflow / session.** Practice with "Sarah, your AI tutor" in scenarios such as a job interview. Each attempt is scored, and practice adapts to weak spots [St1]. Day-wise roadmaps and 100+ topics; sessions run "from about 10 minutes to unlimited" [St4]. **[V-O]** A "60-second challenge" gives a band estimate [St2]. **[V-O]**

**3. Speaking assessment and feedback.** Every attempt is scored on **fluency, grammar, vocabulary and pronunciation**. Feedback shows what you said, a suggested improved version, and explanations, such as tense or preposition fixes [St1]. **[V-O]** Speech insights cover **filler words, pace, tone and pauses**. A 60-second speech yields an **IELTS band score**. There are 15+ full-length IELTS Speaking mocks and live video mock tests with an AI interviewer [St2]. **[V-O]** It uses a "proprietary voice AI engine", and reports are returned within about 20–30 s [St4]. **[V-O / S]** The company does not explain how the band mapping is calibrated. **Not verified.**

**4. Vocabulary.** Vocabulary is scored as a speaking dimension [St1]. No standalone vocabulary or spaced-repetition system was found. **Not verified.**

**5. Personalisation / placement.** Adaptive exercises target weak spots [St1]. A Google Play reviewer mentions a "pre-test" [St7]. **[S]** Paid tiers add a structured "practice roadmap" [St2]. **[V-O]**

**6. Progress tracking.** Scores are tracked over time. Paid plans include "detailed progress reports" [St1]. **[V-O]**

**7. Gamification / retention.** **Buddy Streaks** (invite friends to keep shared streaks) were added in v2.4.39 [St2]. One reviewer mentions a WhatsApp group with a human instructor [St2]. **[V-O]**

**8. Onboarding.** The 60-second speaking challenge appears to be an early hook [St2]. **[V-O]** The full flow was **not verified**.

**9. Navigation.** **Not determinable** from sources.

**10. Pricing.** Free daily practice, plus a subscription for unlimited practice [St1]. US IAP list: weekly $1.99; monthly $4.99, $9.99 or $12.99; annual $18.99–$59.99. The listing describes premium as "less than $5/month" [St2]. **[V-O]** Regional examples: DKK 49/month or DKK 199/year; Nigeria ₦3,500–₦14,900/month [S]. Refunds: within 24 h and under 30 min of use for direct purchases; same calendar day for trial conversions [St6]. **[S]**

**11. Offline / privacy.** The privacy policy says audio of speeches is collected for analysis. "Real-time voice audio during AI calling sessions" is **streamed to Hume AI's servers**. **Speech transcripts go to OpenAI**. Explicit in-app consent is requested, and AI features are unavailable if you decline. Audio is kept "for a limited period of time", with no number given [St3]. **[V-O]** The App Store label lists audio data as linked to the user [St2]. **[V-O]** No offline mode was found. **[I]** It is effectively cloud-dependent.

**12. Complaints / weaknesses.** App Store reviewers report that nonsense speech received a 5.5 band and that every answer was rated positively [St2]. **[V-O]** Others report inaccurate recognition, call lag and crashes, slow support, and refund difficulty [St7]. **[S]** The AI feels basic to advanced learners [St2]. **[V-O]**

---

## 3. Duolingo (English course, speaking features, Max / Video Call)

**1. Positioning / target user.** The mass-market gamified language app. Q3 2026: 64.9M DAU (+29% YoY), 153.1M MAU, 13.1M paid subscribers [D1]. **[V-O]**

**2. Core workflow / session.** Path-based bite-size lessons (well known; not re-verified here). **Video Call with Lily** sits on certain path nodes and in the Practice Hub. Calls last **about 1 minute early in a course, up to about 3 minutes later**. Courses: English, Spanish, French, German, Italian, Portuguese, plus Japanese and Korean on iOS (Sep 2024 post) [D2]. **[V-O]** In 2026, "the proportion of exercises that learners can answer by speaking has doubled" since the start of the year [D1]. **[V-O]**

**3. Speaking assessment and feedback.** Lily "won't correct your grammar during the call", and learners "won't be penalized for any mistakes" [D2]. **[V-O]** Duolingo leadership reportedly chose deliberately minimal correction because learners "really, really hate making mistakes" (Duocon 2024) [D13]. **[S]** Transcripts can reportedly be reviewed after calls [D13]. **[S]** On the research side, the Duolingo English Test team published an **intelligibility-based (not native-likeness) pronunciation scoring model**. It reached Spearman 0.82 against expert raters, "nearly matching" human–human agreement, and outperformed GOP, Whisper-confidence and a Microsoft commercial tool. The paper notes bias from device audio quality and language family (Jul 2025) [D5]. **[V-3/O]** Using that model inside the consumer app was **not verified**. Earlier research and reviews found the consumer app's speaking checks lenient on segmental errors, and G2 reviewers call speaking exercises "relatively minimal" [D14]. **[S]**

**4. Vocabulary.** Integrated into course content. Duolingo published a spaced-repetition model ("half-life regression", ACL 2016) [D11]. **[S]** Whether it is still used in production in 2026 was **not verified**.

**5. Personalisation / placement.** Has an adaptive placement test for choosing a starting point [D10]. **[S]**

**6. Progress tracking.** The **Duolingo Score** runs 0–160 on the same scale as the Duolingo English Test and maps to CEFR bands (e.g., 30–59 = A2, 100–114 = early B2). It is unlocked after Unit 1 and "rises as you complete lessons and move through units". The most advanced courses reach 129, the end of B2 (Oct 2024) [D3]. **[V-O]** **[I]** This is a curriculum-position score dressed in CEFR language, not an independent measurement of speaking ability.

**7. Gamification / retention.** Streaks, leagues, XP, gems (well known). In 2025, **Energy** (25 units, drains on every exercise, unlimited for paid tiers) replaced Hearts on mobile. It drew public backlash, including petitions and users quitting [D6]. **[S]**

**8. Onboarding.** Duolingo's growth team reported that moving sign-up behind a first lesson raised DAU by about 20%, and later soft/hard-wall optimisation added another 8.2% [D9]. **[S]** The current flow was **not verified**.

**9. Navigation.** **Not verified** in this research.

**10. Pricing.** **Max is being sunset.** New Max sales stopped in Nov 2026. Individual Max subscribers move to Super Family at the same price. Unlimited Video Call is rolling into **Super** for all subscribers by end of 2026 [D1][D4]. **[V-O / V-3]** Super prices are not published by Duolingo. Third-party US figures are roughly $84–96/yr and $12.99–13.99/mo, with Family around $120/yr. Prices are PPP-adjusted by region [D12]. **[S, uncertain]**

**11. Offline / privacy.** The policy (version dated May 26, 2026, via a policy tracker) says Duolingo "may generate, record, and store audio recordings or transcripts" and use them "for product improvement and personalization purposes, including training and running Duolingo's own artificial intelligence models" [D7]. **[V-3]** An opt-out in Settings is reported by the tracker but **not verified** in the policy text. Offline support: third-party sources conflict on whether lesson downloads still exist, and nothing confirms that speaking exercises work offline [S]. **Not verified.** Video Call AI cost fell to "well under one cent" per call via open-weight models [D4]. **[V-3]**

**12. Complaints / weaknesses.** The Energy system backlash [D6]. The April 2025 "AI-first" memo about phasing out contractors drew heavy backlash and a later CEO clarification [D8]. **[S]** Speaking practice is perceived as shallow, and synthetic voices are criticised [D14]. **[S]**

---

## 4. Speak (speak.com)

**1. Positioning / target user.** "Speak a new language with confidence, one real conversation at a time." It calls itself spoken-fluency-first and "not a chatbot" but a structured curriculum [Sp1]. **[V-O]** It started in South Korea, where it reportedly is the #1 English app. It raised $78M at a $1B valuation (Dec 2024; OpenAI Startup Fund participated) [Sp7]. **[S]** About 67K ratings at 4.8★ on the US App Store [Sp2]. **[V-O]**

**2. Core workflow / session.** The "Speak Method" is **Learn → Practice → Apply**, with Apply being a conversation with the Speak Tutor [Sp1]. Activity types include Roleplay, Free Talk, Speaking Drill, Vocab Builder, Tutor Lesson, Tutor Q&A and Speak Tutor [Sp4]. **[V-O / V-3]** In Roleplay you complete three tasks, and the session ends when they are done [Sp4]. **[V-3]** The winter 2025 release made roleplays audio-driven two-way conversations with hints and difficulty adjustment, and added voice-first lessons [Sp5]. **[V-O]**

**3. Speaking assessment and feedback.** Real-time feedback on pronunciation and phrasing, judging "how natural your phrasing is" [Sp1]. The listing cites analysis of pronunciation, intonation and fluency [Sp2]. **[V-O]** The independent review found feedback **brief** (a short error summary after roleplay) and **speaking drills lenient**, with perfect scores despite mispronounced or reordered words [Sp4]. **[V-3]** Live Roleplays reportedly run on OpenAI's Realtime API [Sp7]. **[S]**

**4. Vocabulary.** Phrases (not single words) are saved to a **Phrasebook**. **Smart Review** resurfaces due phrases with a mastery score. Saved phrases are not reused in chats or new lessons [Sp4]. **[V-3]** "Vocab side quests" are tied to the current unit [Sp5]. **[V-O]**

**5. Personalisation / placement.** Mistakes and history drive custom review. Premium Plus adds "Made for You" lessons and a Tutor lesson creator [Sp1]. **[V-O]** Onboarding reportedly asks for a self-rated level, native language, interests and goals, and one teardown says the quiz is long and asks for mic permission late [Sp8]. **[S]**

**6. Progress tracking.** **Speak Level** "measures speaking ability over time". It first launched for English learners in Korea, Japan and Taiwan, with full rollout planned for 2026 [Sp5]. **[V-O]** In standard Premium, the review found no cumulative tracking of recurring mistakes [Sp4]. **[V-3]**

**7. Gamification / retention.** Streaks with a Classic Streak Save, Streak Freeze (hold up to 2), Save Streak Challenge and Auto Streak Repair, plus Leagues [Sp6]. Milestone celebrations [Sp5]. **[V-O]**

**8. Onboarding.** See item 5. **[S]**

**9. Navigation.** A redesigned Home screen and a **Free Talk tab** where roleplay lessons live are confirmed [Sp5]. **[V-O]** Full tab count **not verified**.

**10. Pricing.** 7-day free trial, then **Premium** or **Premium Plus**. Prices vary by region and promotion [Sp1]. **[V-O]** US IAP list tops out at **$164.99/yr (Premium Plus annual)** and **$39.99/mo (Premium Plus monthly)** [Sp2]. **[V-O]** Premium has been reported at about $17.99/mo or $83.99/yr (US, Aug 2026) [S].

**11. Offline / privacy.** The privacy notice (last updated 06/05/2026) says audio is used to "develop, create, improve, optimize, maintain, and otherwise enhance its automatic speech recognition" under *legitimate interests*, not consent. **OpenAI and Microsoft** receive audio to provide the service, and OpenAI also receives it "for improving the Services". There is no audio-specific retention period and no training opt-out [Sp3]. **[V-O]** The App Store label lists audio as linked to identity, and Contact Info, Identifiers and Usage Data as used for tracking [Sp2]. **[V-O]** No offline mode was found. **[I]** It is cloud-dependent by design.

**12. Complaints / weaknesses.** Lenient recognition gives a false sense of mastery. Feedback is shallow. Variety drops at higher levels and Tutor Q&A feels repetitive. Conversation partners always end with a question. Voice and accent choice is limited. Tier limits are unclear and expensive [Sp4]. **[V-3]** (The reviewer's site promotes its own app.)

---

## 5. Praktika

**1. Positioning / target user.** "Learn and speak new languages with AI tutors", priced against human tutoring ("~$8/month" vs "~$400/month") [P1]. **[V-O]** Raised a $35.5M Series A (May 2024). In 2024 it claimed 1.2M MAU and about $20M in trailing revenue [P7]. **[S]** About 169K ratings at 4.7★ on the US App Store [P2]. **[V-O]**

**2. Core workflow / session.** Guided chats with **animated AI avatar tutors** that "remember your context". Free conversation on any topic, plus grammar, vocabulary, listening and reading exercises [P1]. Over 1,000 lessons, including IELTS/TOEFL prep and role-plays. Lessons can be built from uploaded photos, PDFs, videos or voice notes [P2]. **[V-O]** Session length **not stated**.

**3. Speaking assessment and feedback.** Instant feedback on pronunciation, grammar and word choice, with selectable **correction style: Soft, Balanced or Strict**. Tagline: "No judgement, ever" [P1]. **[V-O]** Pronunciation and fluency feedback after each conversation [P2]. **[V-O]** The independent review found a per-line "Feedback" button that appears even with no mistake, **no end-of-lesson summary, no categorised error list, and no follow-up exercises** [P3]. **[V-3]**

**4. Vocabulary.** Tap a word to define it and save it to a personal dictionary. **No spaced repetition.** Saved words never reappear in lessons [P3]. **[V-3]** A flashcard section was added in a recent update [P2]. **[V-O]**

**5. Personalisation / placement.** Setup covers daily study time, goal, skills, **self-selected CEFR level (A1–C1)**, interests and avatar choice. **No placement test** [P3]. **[V-3]** The privacy policy says recordings are analysed "to determine your current language proficiency level" at the start [P4]. **[V-O]**

**6. Progress tracking.** Speaking metrics [P2]. **[V-O]** Changing goals **resets progress**, and errors don't influence future lessons [P3]. **[V-3]**

**7. Gamification / retention.** Streaks and challenges [P2]. **[V-O]**

**8. Onboarding.** About five configuration screens plus avatar selection before the first chat [P3]. **[V-3]**

**9. Navigation.** A Practice tab with about 140 conversation topics is confirmed [P3]. Others **not verified**.

**10. Pricing.** US IAP: $9.99/mo; weekly $6.99 (period inferred); 3 months $19.99 (promo) to $49.99; annual $59.99, $119.99 or $139.99 [P2]. **[V-O]** The reviewer saw only 3-month and annual options, with a country-varying price [P3]. **[V-3]**

**11. Offline / privacy.** "Real-time AI feedback and conversation features require an internet connection". Offline mode "isn't available yet", though the company is "exploring" limited offline access [P5]. **[V-O]** The policy (last updated Aug 10, 2026) collects "recordings and transcripts of your conversations with the AI tutor" and analyses them to "fine tune our AI models" under legitimate interests. No opt-out, no named AI processors, and no retention period [P4]. **[V-O]** Its App Store label lists audio as **not linked** to identity, while Identifiers and Usage Data are used for tracking [P2]. **[V-O]**

**12. Complaints / weaknesses.** Voice recognition errors (including switching languages). Billing problems (Stripe vs Apple mismatches, refunds unanswered). Avatars described as "creepy". The app "doesn't remember what you did 10 minutes before". The phone heats up [P6]. **[S]** Rigid paths, easy-to-miss feedback, no audio-only option, and vague lesson titles [P3]. **[V-3]** Trustpilot (IE) showed 3.7 from about 1,136 reviews [P6]. **[S]**

---

## 6. Grammarly (communication-quality reference, writing only)

**1. Positioning.** An AI writing assistant. The parent company rebranded as **Superhuman** in Oct 2025, while the writing product keeps the Grammarly name [G4]. **[S]**

**2–3. Workflow and feedback model (reference value).** Inline suggestions while you write. Free tier: spelling and grammar, **tone detection** ("See your writing tone"), and 100 AI prompts a month. The paid tier adds **tone adjustment**, full-sentence rewrites, **"fluency for English"**, inclusive language, plagiarism and AI-text detection, and 2,000 prompts a month [G1]. **[V-O]** **[I]** The reference value lies in how the model separates feedback into categories, makes each suggestion one-tap actionable, and frames tone without judgement. None of this involves speech.

**4–5. Vocabulary / personalisation.** "Unlimited personalized suggestions" on the paid tier [G1]. **[V-O]**

**6–9.** Not applicable or not researched (no speaking product). Navigation not researched.

**10. Pricing.** The fetch returned India pricing: **Plus ₹392/member/month billed annually, or ₹1,000 monthly**, "exclusive to India", via local payment methods. Enterprise is quote-based [G1]. **[V-O]** US Pro is reported at about $12/member/month billed annually [G5]. **[S]** **[I]** A dedicated, cheaper India tier shows that large incumbents price specifically for India.

**11. Offline / privacy.** "Grammarly is an online application, which means your computer must be connected to the internet" [G3]. **[V-O]** "We do not sell or monetize user and customer content." Users can **turn off "Product Improvement and Training"** to stop their content training models. Saved documents are kept until deleted [G2]. **[V-O]** SOC 2 Type 2; Enterprise adds BYOK encryption and data-loss prevention [G1]. **[V-O]** **[I]** This is the clearest example in the set of a **user-facing training opt-out toggle**. Most speaking apps lack one.

**12. Complaints.** Not researched in depth (out of scope for speaking).

---

## 7. BoldVoice

**1. Positioning / target user.** "Expert accent lessons and precise AI feedback, so people hear **you**, not your accent." Aimed at non-native professionals (logos from Google, Microsoft and Meta) and interview candidates [B1]. **[V-O]** Claims 2M+ users. About 54K ratings at 4.8★ on the US App Store [B2]. **[V-O]**

**2. Core workflow / session.** The cycle is record → analysis of pronunciation, stress and clarity → "what to fix and how" → targeted drills → track scores [B1]. Video lessons from "Hollywood dialect coaches" and interactive mouth diagrams. **Daily tasks take 5–10 minutes**, and the app says "10 minutes a day is enough" [B1][B2]. **[V-O]**

**3. Speaking assessment and feedback.** "Sound-level analysis of mistakes with personalized tips", stress feedback, and comparison to native speakers [B1][B2]. **[V-O]** BoldVoice publishes research on an **accent-strength** metric: a regression over speech-embedding dimensions predicting human-rated accent strength, checked for robustness to recording quality [B4]. **[S]** A 2025 *Language Learning & Technology* review describes an assessment in which learners record **14 sentences** and get an overall percentage with a proficiency level, plus highlighting of mispronounced sounds and native-model comparison [B5]. **[S]** (The review's verdict could not be retrieved because the fetch failed.)

**4. Vocabulary.** Not a focus. **Not verified.**

**5. Personalisation / placement.** "Personalized learning paths" and "a personalized speech assessment and a custom practice plan" [B1]. **[V-O]**

**6. Progress tracking.** Scores and confidence over time, with "comprehensive analytics" [B1][B2]. **[V-O]**

**7. Gamification.** **Not verified** (none described on official pages).

**8. Onboarding.** Assessment first (see item 3) [B5]. **[S]**

**9. Navigation.** **Not determinable.**

**10. Pricing.** 7-day free trial [B1]. US IAP: **12-month Premium $149.99**. Other entries range from $14.99 to $199.99, unlabelled [B2]. **[V-O]**

**11. Offline / privacy.** The privacy policy (effective Oct 12, 2026) says BoldVoice "collects and stores voice samples to provide pronunciation feedback" and uses them **to improve its machine learning models**. Samples "may be retained as part of our records" with no fixed period. Deletion requests are handled within 28 business days. Users can opt out of future voice collection only by disabling voice features. BoldVoice also runs **ChatGPT apps**, whose recordings are kept until deleted [B3]. **[V-O]** **[I]** The App Store label we saw does **not list Audio Data**, although the policy describes storing voice samples. This mismatch is worth noting as a category-wide trust issue. No offline mode was found.

**12. Complaints / weaknesses.** Price and billing or refund issues, including reported double charging via Apple and Stripe. Samples are small and some sources unverified [B6]. **[S]** "Robotic American" voices; American-accent focus only [B2][S]. Little evidence on scoring accuracy either way [B6]. **[S]**

---

## 8. Yoodli (public speaking / roleplay; now enterprise-led)

**1. Positioning / target user.** "The AI Platform for Human Skills". Primarily **enterprise**: sales, onboarding, managers, healthcare. Individuals are a smaller segment (public speaking, engineering). It claims "1M+" people trained [Y1]. **[V-O]** $40M Series B (Dec 2025), reported valuation $300M+, customers include Google, Snowflake, Databricks [Y3]. **[S]**

**2. Core workflow.** Roleplays (AI tutors, scenarios, certifications), Personas, Create (learning journeys from a prompt), Coach (in the flow of work), and Measure (dashboards). Integrations with Zoom, Meet, Teams, Salesforce and others [Y1]. **[V-O]**

**3. Assessment / feedback.** **Rubric-based** and scenario-specific (discovery questions, objection handling, clarity, confidence, empathy, plain language). Feedback is framed as a strength plus an area to improve [Y1]. **[V-O]** Historically it offered delivery analytics (filler words, pacing, transcript) for individuals [Y4]. **[S]** A competitor claims Yoodli coaches delivery but not answer structure such as STAR [Y5]. **[S, competitor claim]**

**4–5. Vocabulary / personalisation.** Not a vocabulary product. Custom rubrics in Business [Y2]. **[V-O]**

**6. Progress.** "Measure" dashboards and exports (Business) [Y2]. **[V-O]**

**7. Gamification.** Certifications [Y1]. **[V-O]**

**8–9. Onboarding / navigation.** **Not verified.**

**10. Pricing.** Starter free: **5 lifetime sessions** (any session over 30 s counts). **Pro $8/mo** billed annually: up to 10 roleplays a week; data "may be used to improve the platform". **Advanced $20/mo** billed annually: unlimited, and **roleplay data excluded from AI training**. Business and Enterprise are quote-based. No refunds on cancellation [Y2]. **[V-O]**

**11. Offline / privacy.** SOC 2 Type 2 and GDPR. Custom data retention only on Business and Enterprise [Y2]. **[V-O]** Cloud-only. **[I]**

**12. Complaints.** The free tier is called "stingy" [S]. Few public reviews; Capterra shows 0 [S]. Enterprise integration gaps are mentioned on G2 [S].

---

## 9. Orai (public speaking)

**1. Positioning / target user.** An AI "pocket coach" for presentations, for "clear, confident speaking" in "a few minutes a day". Aimed at professionals such as PMs, founders and sales teams [O1]. **[V-O]** About 3.8K ratings at 4.6★ [O2]. Claims 500k+ downloads [O1]. **[V-O]**

**2. Core workflow.** Choose a scenario (pitch, toast, tough question), speak, and receive a **scorecard plus one concrete tip** for next time (e.g., "pause after the problem statement") [O1]. Prepared-script and freestyle modes. Short lessons [O2]. **[V-O]**

**3. Assessment / feedback.** Scores **filler words, pace (WPM, e.g., "147 wpm, a touch fast"), clarity (%), energy, confidence, conciseness**. Recording, playback and transcript [O1][O2]. **[V-O]**

**4–5. Vocabulary / personalisation.** **Not verified.**

**6. Progress.** Weekly trends (e.g., "+18% clarity this week") and session history [O1][O2]. **[V-O]**

**7. Gamification.** Streaks (e.g., a 7-day streak) [O1]. **[V-O]**

**8–9. Onboarding / navigation.** **Not verified.**

**10. Pricing.** US: **$12.99/mo, $49.99/yr, $99.99 lifetime** [O2]. **[V-O]**

**11. Offline / privacy.** The App Store label lists Audio Data as linked to the user, and Usage Data as used for tracking [O2]. **[V-O]** No offline claim. **[I]**

**12. Complaints.** Bugs and inaccurate pause detection [O3]. **[S]** The Android changelog found ends Feb 2025 [O3] (**[S]**), but the iOS app shows v3.25.28 updated "Oct 16" (year not shown) [O2]. Activity level is therefore unclear.

---

## 10. Other relevant products (brief)

### Speeko (public speaking)
- AI speech coach for presentations, meetings and interviews. iOS, macOS, Android, web. Claims "500,000 professionals" [Sk1]. **[V-O]**
- Metrics: **pace (WPM), filler words ("eloquence"), intonation (pitch variety), sentiment, talk time, and word choice (hedging, qualifying, redundancy)**. About **2 minutes a day** of personalised exercises. Vocal warm-ups, notecards, content from voice coach Roger Love, optional 1:1 coaching [Sk1]. AI roleplays ("Convos") for interviews and difficult conversations [Sk2]. **[V-O]**
- Privacy claim: "no one has access to your audio data except you" [Sk1]. **[V-O]** Its App Store label did not list Audio Data [Sk2]. **[V-O]**
- US IAP: $24.99/mo; $79.99 or $99.99/yr [Sk2]. **[V-O]**

### Poised (desktop meeting coach), shutting down
- Real-time feedback during calls on Mac and Windows: speakshare, clarity and filler words, confidence and **hedging words**. Invisible to other participants, and does not bot-join meetings [Po1]. **[V-O]**
- Banner: **"Poised is shutting down on 10/8/2026"** [Po1]. **[V-O]** **[I]** This signals that standalone real-time meeting-coaching tools for individuals struggle as a business.

### Cambly (human tutors + AI between lessons)
- Human-tutor conversation platform. Small Groups $12, Private+ $28, Pro $40 per month (promotional, 50% off Black Friday). Private+ includes lesson recordings, transcripts, and "automated feedback after every lesson" [C1]. **[V-O]**
- October 2024 AI features: lesson summaries, AI practice between lessons, pronunciation, vocabulary, speaking level check [C2]. **[S]** **[I]** The human-plus-AI hybrid trend (Cambly, Preply) is aimed at the "trust" gap that pure-AI apps have.

### TalkPal (multi-language AI chat)
- Claims 130+ languages and 10M+ users. Modes: Roleplays, Characters, Debates, Call Mode, Sentence, Word, Dialogue and Photo Mode. Free tier plus Premium; 14-day trial [T1]. **[V-O]**
- Prices vary widely by source ($6.99–$19.99/mo; MXN 199/mo). The free tier is about 10 minutes a day. Billing complaints dominate one-star reviews. An English-focused review says "word-level scoring only, no phoneme feedback" [T2]. **[S]**

### Indie offline pronunciation apps (iOS)
- Several small apps advertise **on-device or offline** pronunciation scoring, e.g., "Articulate: AI Pronunciation" ("100% Offline AI"), "AnyAccent" ("100% offline"), "MiiTalk" ("scoring works all offline"), and apps using Apple's on-device speech recognition [M4]. **[S, developer claims, unverified]** **[I]** Offline pronunciation is not novel at the indie level. No credible, scaled, Android-first, privacy-first *communication* coach was found.

---

## 11. Cross-product comparison (snapshot)

| Product | Primary focus | Speaking assessment | Scores mapped to exams/CEFR | AI open conversation | Offline | Voice to cloud / third parties | US price signal (approx.) |
|---|---|---|---|---|---|---|---|
| ELSA | Pronunciation → broad speaking | Phoneme-level claimed [S]; Speech Analyzer | Yes: EPS (CEFR/IELTS), IELTS/TOEFL/TOEIC [E2][E3] | Yes (roleplay, avatars) | **No** [E5] | Yes, own servers on AWS [E4] | $19.99/mo; up to $129.99/yr [E2] |
| Stimuler | Fluency, IELTS, confidence | 4 dimensions + filler/pace/pauses | IELTS band from 60 s [St2] | Yes (Hume AI, OpenAI) | Not found | **Hume AI, OpenAI** [St3] | <$5/mo; $1.99/wk–$59.99/yr [St2] |
| Duolingo | Gamified courses | Minimal correction in Video Call [D2] | Duolingo Score 0–160 (course position) [D3] | Yes; Video Call moving into Super [D1] | Unclear | Yes; trains own AI [D7] | Super ~$84–96/yr [S] |
| Speak | Conversation-first curriculum | Pronunciation/phrasing; lenient per review [Sp4] | Speak Level (rolling out) [Sp5] | Yes (OpenAI Realtime [S]) | Not found | **OpenAI, Microsoft** [Sp3] | Up to $164.99/yr [Sp2] |
| Praktika | Avatar tutors | Per-line feedback; Soft/Balanced/Strict [P1] | Self-selected CEFR [P3] | Yes (avatars) | **No** [P5] | Yes; fine-tunes models [P4] | $9.99/mo; $59.99–$139.99/yr [P2] |
| BoldVoice | Accent / pronunciation | Sound-level; accent-strength research | % + proficiency level [S] | No (drills, video) | Not found | Yes; trains ML [B3] | $149.99/yr [B2] |
| Yoodli | Enterprise roleplay | Rubric-based | No | Yes (roleplay) | No | Yes; training unless Advanced+ [Y2] | Free 5 sessions; $8 / $20 per mo [Y2] |
| Orai | Presentation delivery | WPM, fillers, clarity, energy, conciseness | No | No | Not found | Audio linked to user [O2] | $12.99/mo; $49.99/yr; $99.99 lifetime [O2] |
| Speeko | Presentation delivery | WPM, fillers, pitch, sentiment, hedging | No | Roleplay "Convos" | Not found | "No one has access … except you" [Sk1] | $24.99/mo; $79.99–$99.99/yr [Sk2] |
| Grammarly | Writing quality | n/a (text) | n/a | n/a | **No** [G3] | Yes; opt-out toggle [G2] | India ₹392/mo annual [G1] |

---

## 12. Synthesis

### 12.1 Table-stakes patterns (common across the category)

Verified patterns (each confirmed in 3 or more products above):
1. **Speak-first loop with instant feedback after every utterance**: ELSA, Stimuler, Speak, Praktika, BoldVoice, Orai, Speeko. **[V]**
2. **Scenario roleplay including job interviews**: ELSA (Interview Coach, Role-Play Lab), Stimuler (job interview with "Sarah"), Speak (Roleplay, Free Talk), Praktika, Speeko (Convos), Yoodli, and Duolingo Video Call (open chat). **[V]**
3. **Multi-dimension scoring.** ESL apps score pronunciation, fluency, grammar and vocabulary (Stimuler, ELSA). Delivery apps score WPM, filler words, pauses, clarity and energy (Orai, Speeko, Stimuler, earlier Yoodli). **[V]**
4. **Mapping scores to recognised scales**: CEFR, IELTS, TOEFL or TOEIC (ELSA EPS, Stimuler bands, Duolingo Score). Speak added "Speak Level" in late 2025. **[V]**
5. **Streak-centred retention with streak protection**: Speak (freeze, repair, save), Duolingo, Stimuler (Buddy Streaks), Orai. Leaderboards and leagues: Duolingo, Speak, ELSA. **[V]**
6. **Personalised daily plan or path** from onboarding goals, level and interests: ELSA, Speak, Praktika, BoldVoice, Stimuler roadmap. **[V]**
7. **Short sessions**: BoldVoice 5–10 min, Speeko about 2 min, Duolingo calls 1–3 min, Orai "a few minutes", ELSA 5 lessons a day. **[V]**
8. **Freemium with tight caps, a 7–14-day trial, and an annual-plan push**, with regional pricing: all consumer apps. **[V]**
9. **Cloud processing of voice, audio linked to identity, vague retention, and model improvement under "legitimate interests"**: ELSA, Speak, Stimuler, Praktika, BoldVoice, Duolingo. **[V]**
10. **Expansion from pronunciation toward "communication"**: interview, presentation and meeting coaches (ELSA Professional, Stimuler, Speeko, Yoodli). **[V]**

### 12.2 Gaps and underserved needs

**A. Trust in feedback accuracy (strong evidence)**
- **[V]** Calibration complaints run in both directions: too strict (ELSA, natives around 80% [E9]), too lenient (Speak perfect scores on errors [Sp4]; Stimuler "5.5 for gibberish" and "rated every response positively" [St2]), plus recognition errors (Praktika [P6], ELSA [E11]).
- **[V]** Efficacy and accuracy claims are not independently validated (ELSA [E3]). Microsoft documents that pronunciation scoring degrades with noise, distance from the mic, multiple speakers, mixed languages and abnormal speaking speed [M2]. Duolingo's research shows bias by device audio quality and language family [D5].
- **[I]** No competitor shows **confidence or uncertainty**, abstains when audio is poor, or explains *what was measured and how reliably*. Users get a precise-looking number with no error bars. This is the clearest trust opening.

**B. Privacy and offline (strong evidence)**
- **[V]** None of the scaled players works offline (ELSA [E5], Praktika [P5], Grammarly [G3]; no offline mode found for the others). Several send voice to third-party AI vendors (Speak → OpenAI/Microsoft [Sp3]; Stimuler → Hume AI/OpenAI [St3]). Model training on audio is common and not opt-in (Speak, Praktika, BoldVoice, Duolingo).
- **[V]** Privacy is monetised as an upsell (Yoodli [Y2]) or offered as a buried toggle (Grammarly [G2]). App Store labels are inconsistent with policies (BoldVoice [B2] vs [B3]).
- **[I]** Users in regulated or sensitive contexts (job seekers rehearsing real interview answers, employees rehearsing confidential presentations, minors, people in surveillance-heavy environments) have no trustworthy option. Rehearsal content is often personal: salary negotiations, real project details, career history.

**C. Professional communication beyond pronunciation (moderate evidence)**
- **[V]** ESL apps are just starting professional coaches (ELSA Professional, tentative March 2026 [E7]). Delivery coaches (Orai, Speeko) measure pace and fillers but are not ESL-aware. Yoodli is enterprise-led [Y1]. Poised is shutting down [Po1].
- **[I]** Underserved: **ESL-aware professional skills on mobile**. Examples: answer structure (STAR/PREP, signposting), concision, hedging and over-apologising, register and politeness, turn-taking phrases for meetings, handling questions, and number and date fluency. Also the *combination* of language accuracy with delivery and structure in one feedback view.

**D. Honest progress measurement (strong evidence)**
- **[V]** The Duolingo Score rises with lesson completion [D3]. Streaks and leagues measure habit. Stimuler offers band estimates from 60-second samples [St2]. ELSA's agreement criterion is ±1 CEFR band [E3]. Praktika resets progress when goals change [P3].
- **[I]** No product offers a **repeatable, standardised benchmark task** (same prompt type, same conditions, every few weeks) with transparent metrics and a measurement-noise band. Learners cannot tell real improvement from score drift.

**E. Low-bandwidth and price-sensitive markets (moderate evidence)**
- **[V]** ELSA requires continuous connectivity and "a significant amount of data" [E5]. 25+ Mbps is reportedly recommended [E13] (**[S]**). Avatars (Praktika), video calls (Duolingo) and live video mocks (Stimuler) are bandwidth-heavy. Stimuler's iOS binary is 250 MB [St2].
- **[V/S]** There is demand at low price points (Stimuler "<$5/month", growth in India, Indonesia and LatAm [St2][St5]; Grammarly's India-only tier [G1]). Affordability is the main barrier in GSMA data [M1].
- **[I]** Cloud AI has a per-session cost (even Duolingo's "well under one cent" call [D4] also needs a connection), so free tiers are capped hard: Yoodli 5 lifetime sessions, ELSA AI 3 sessions, TalkPal about 10 min a day. On-device evaluation has near-zero marginal cost and could make a **generous free tier** economically sustainable.

**F. Vocabulary that transfers into speech (moderate evidence)**
- **[V]** Praktika has no spaced repetition, and saved words never return [P3]. Speak's Smart Review exists, but phrases are not reused in conversations [Sp4]. ELSA's study sets are rated weak [E9].
- **[I]** Gap: vocabulary scheduled for **productive use**, where target words reappear as required elements in later speaking prompts, and success is judged by whether the learner actually *said* the word correctly in context.

**G. Billing trust (strong evidence)**
- **[V/S]** Trial-to-paid surprises, refund friction and dual-billing problems are reported for ELSA, Praktika, BoldVoice, TalkPal and Stimuler (24-hour refund window) [E11][P6][B6][T2][St6].
- **[I]** Transparent, low-pressure monetisation is itself a differentiator in this category.

**H. Feedback actionability**
- **[V]** Feedback is buried or brief (Praktika: no lesson summary [P3]; Speak: short error summary [Sp4]). In contrast, Orai gives **"one concrete tip"** per attempt [O1] and Praktika lets users choose correction strictness [P1]. Both are useful patterns.

### 12.3 Differentiation opportunities for a privacy-first, on-device, offline-capable coach

All items in this section are **[R]** recommendations or inspiration. None asks for copying any competitor's interface.

1. **Make privacy verifiable, not just claimed.**
   - The core practice loop works fully in airplane mode, without an account.
   - Audio is ephemeral by default: processed in memory, discarded after scoring unless the user chooses to keep a recording.
   - Show a plain-language "what left this device" ledger, which by default says nothing did.
   - Any cloud feature is a separate, explicit, per-feature opt-in with the vendor named.
   - Make the store privacy label honestly minimal, and use it in positioning. This directly answers gap B and contrasts with "privacy as a $12/month upsell".
2. **Honest scoring as a brand promise (gap A).**
   - Report a score only when signal quality is adequate. Otherwise say "too noisy to judge — try again closer to the mic" (Microsoft's own guidance supports retry thresholds [M2]).
   - Show ranges or confidence levels rather than false precision, and label each metric as *measured* (e.g., WPM, pauses) vs *estimated* (e.g., intelligibility).
   - Avoid claiming IELTS or CEFR equivalence unless validated. Use "practice indicators" and explain why.
   - Publish a short methodology note and a small human-rated validation set.
3. **Intelligibility over native-likeness.** Frame pronunciation feedback around "would a listener understand this word?" rather than accent elimination. This matches the research direction (Duolingo's intelligibility model [D5]) and Stimuler's positioning [St1], and is more achievable on-device (see 12.4).
4. **Lean on metrics that are robust on-device.** These can be computed reliably from on-device ASR transcripts and timestamps:
   - speaking rate
   - pause distribution and long-pause count
   - filler words
   - mean length of fluent run
   - self-repetitions and restarts
   - lexical variety
   - hedging and over-apology markers
   - discourse signposting
   - answer length versus target
   - coverage of required key points

   Delivery coaches (Orai, Speeko) show that users value these metrics. Combining them with ESL guidance is the unclaimed space (gap C).
5. **Professional communication packs with structure rubrics.** Interview (STAR), status update, presentation opening and closing, disagreeing politely, and asking clarifying questions. Use deterministic, explainable checks (e.g., "you stated the Result: yes/no"; "you used 2 signposts") rather than opaque LLM grading. Deliver **one prioritised tip per attempt** (gap H).
6. **Repeatable benchmark for honest progress (gap D).** Offer a fixed-format "check-in" task every 2–4 weeks under the same conditions. Show a trend with a noise band, and keep habit metrics (streaks) visibly separate from ability metrics.
7. **Built for low-end devices and low bandwidth (gap E).**
   - A small base install, with optional downloadable model or content packs and stated sizes.
   - Works on mid- and low-RAM Android devices.
   - Content updates as small deltas, and nothing requires streaming video.
   - Pair with a generous free tier and local-currency pricing, which the near-zero marginal cost of on-device evaluation makes possible.
8. **Production-oriented vocabulary scheduling (gap F).** A spaced-repetition queue where "review" means *using* the word in a spoken prompt, and success is confirmed by the on-device recogniser hearing it in context.
9. **Gentle retention without manipulation.** Streak protection is now normal (Speak's freezes [Sp6]). Pair it with weekly reflection and evidence of improvement rather than leagues or energy-style friction, which drew backlash at Duolingo [D6]. Offer user-selectable correction strictness, which Praktika has validated as a concept [P1].
10. **Transparent monetisation (gap G).** Clear trial terms, easy cancellation, possibly a one-time or lifetime unlock (Orai and ELSA show lifetime SKUs exist [O2][E8]). No dual-billing paths.
11. **Optional, explicit cloud "boost".** If ever offered, keep it clearly separate (e.g., an opt-in open-conversation session with a named vendor). Never require it for core value.

### 12.4 What an on-device, offline app realistically cannot match (be honest in positioning)

- **[I]** **Phoneme-level mispronunciation diagnosis at ELSA or BoldVoice quality.** ELSA claims models "trained on millions of non-native recordings" [E3]. BoldVoice has dedicated accent-strength research [B4]. General-purpose on-device ASR is trained to be *robust* to accents, which means it tends to *hide* mispronunciations by recognising the intended word anyway. Simple approaches such as GOP and ASR-confidence underperform purpose-built models in Duolingo's published comparison [D5]. A small on-device phoneme model is feasible, but its accuracy will likely be lower and must be validated before shipping phoneme-level claims. A safer offline proposition is word-level intelligibility ("the recogniser heard *X* when you intended *Y*") with honest framing.
- **[I]** **LLM-quality open-ended conversation.** Speak (OpenAI Realtime [Sp7]), Stimuler (Hume AI/OpenAI [St3]) and Duolingo Video Call [D1] run large cloud models. On-device LLM access on Android (Gemini Nano through ML Kit's Prompt API) is limited to recent flagship devices such as Pixel 10 at the time of writing, and runs only in the foreground [M3]. **[S]** Neither suits low-end target devices. Realistic offline substitutes are scripted or branching dialogues, templated roleplays with slot-filling, and deterministic rubric checks.
- **[I]** **Natural-sounding TTS voices, animated avatars, and large video libraries** (BoldVoice's coach videos [B1], Praktika's avatars [P1]) all cost bandwidth and storage.
- **[I]** **Validated exam-score prediction** (IELTS or TOEFL bands) needs large human-rated datasets. Even competitors that claim it either do not publish validation (ELSA [E3]) or draw user doubt (Stimuler [St2]).
- **[I]** **Human judgement and cultural nuance** (Cambly, Preply). This can be partly offset by an optional "export a practice recording to share with a teacher" feature, kept local and user-initiated.
- **[I]** **Content breadth at scale** (ELSA cites 8,000+ lessons [E2]). Focus on depth in fewer professional scenarios instead.

---

## Sources (all accessed 2026-11-20 unless noted)

**ELSA Speak**
- [E1] ELSA homepage — https://elsaspeak.com/en/ [V-O]
- [E2] ELSA Speak, US App Store — https://apps.apple.com/us/app/elsa-learn-and-speak-english/id1083804886 [V-O]
- [E3] ELSA efficacy page — https://elsaspeak.com/en/efficacy [V-O]
- [E4] ELSA Corp privacy notice (last updated March 2025) — https://elsaspeak.com/en/privacy/ (also hosted at https://elsanow.io/Privacy) [V-O]
- [E5] ELSA FAQ "ELSA isn't hearing me speak?" — https://elsaspeak.com/en/faqs/elsa-isnt-hearing-me-speak [V-O]
- [E6] "Discover the New ELSA Speak Experience" (4 Jan 2026) — https://blog.elsaspeak.com/en/discover-the-new-elsa-speak-experience/ [V-O]
- [E7] "What is the ELSA Professional plan" (24 Nov 2025) — https://blog.elsaspeak.com/en/what-is-the-elsa-professional-plan [V-O]
- [E8] Weblio ELSA pricing guide (JP, May 2026) — https://ninja.weblio.jp/elsaspeak-pricing [V-3]
- [E9] FluentU ELSA review (2024) — https://fluentu.com/blog/reviews/elsa-speak [V-3]
- [E10] ELSA School speech-tech FAQ — https://elsaschool-support.freshdesk.com/en/support/solutions/articles/31000177613-faq-about-elsa-s-speech-analysis-technology [S; 404 on fetch]
- [E11] Trustpilot / Capterra ELSA reviews — https://uk.trustpilot.com/review/elsaspeak.com ; https://www.capterra.com/p/240698/ELSA-Speak/reviews/ [S]
- [E12] Phoneme-level / colour feedback — https://blog.elsaspeak.com/en/advantage-of-elsa-feedback ; https://www.dell.com/nl-be/perspectives/ai-app-helps-non-native-english-speakers-ace-pronunciation/ [S]
- [E13] ELSA help "ideal conditions" (bandwidth, no offline) — https://elsanow.freshdesk.com/support/solutions/articles/31000154780-what-are-the-ideal-conditions-to-practice-with-elsa- [S; 404 on fetch]
- [E14] Nubia ELSA review 2026 — https://nubiapage.com/elsa-speak-review-2026/ [S]

**Stimuler**
- [St1] Stimuler homepage — https://stimuler.tech/ [V-O]
- [St2] Stimuler, US App Store — https://apps.apple.com/us/app/id1627501632 [V-O]
- [St3] Stimuler privacy policy — https://stimuler.tech/privacy-policy [V-O]
- [St4] Stimuler blog "Cambly vs Stimuler" (Feb 2026; vendor-authored) — https://stimuler.tech/blog/cambly-vs-stimuler-which-app-actually-helps-you-speak-better [V-O]
- [St5] Funding (Apr 2025) — https://india.entrepreneur.com/news-and-trends/stimuler-raises-usd-375-mn-funding-to-scale-voice-first/490839 [S]
- [St6] Refund policy — https://stimuler.tech/refund-cancellation-policy [S]
- [St7] Google Play reviews (aggregated) — https://grand-screen.com/apps/stimuler-ai-english-fluency/reviews/ [S]

**Duolingo**
- [D1] Duolingo Q3 2026 shareholder letter (3 Nov 2026), SEC — https://www.sec.gov/Archives/edgar/data/1562088/000162828026069474/q3fy26duolingo9-30x26share.htm [V-O]
- [D2] "Video Call with Lily" (24 Sep 2024) — https://blog.duolingo.com/video-call/ [V-O]
- [D3] "Duolingo Score" (23 Oct 2024) — https://blog.duolingo.com/duolingo-score/ [V-O]
- [D4] Subscription Insider, Max dropped (5 Nov 2026) — https://www.subscriptioninsider.com/blog/duolingo-drops-premium-tier-after-ai-feature-costs-plunge [V-3]
- [D5] DET blog, intelligibility-based pronunciation model (30 Jul 2025) — https://blog.englishtest.duolingo.com/new-research-in-language-learning-a-pronunciation-scoring-model-built-around-intelligibility-not-imitation [V-O]
- [D6] Energy system — https://blog.duolingo.com/duolingo-energy/ ; https://www.androidauthority.com/quitting-duolingo-energy-system-3599842/ [S]
- [D7] ConductAtlas excerpt of Duolingo privacy policy (version dated 26 May 2026) — https://conductatlas.com/platform/duolingo/duolingo-privacy-policy/ai-audio-recordings-stored-for-product-improvement/ [V-3]
- [D8] AI-first memo backlash — https://www.theregister.com/2025/04/29/duolingo_ceo_ai_first_shift ; https://fortune.com/2025/08/18/duolingo-ceo-admits-controversial-ai-memo-did-not-give-enough-context-insists-company-never-laid-off-full-time-employees/ [S]
- [D9] Delayed sign-up A/B tests — https://review.firstround.com/the-tenets-of-a-b-testing-from-duolingos-master-growth-hacker [S]
- [D10] Placement test — https://blog.duolingo.com/partial-credit-improvements-to-duolingos-placement-test/ [S]
- [D11] Settles & Meeder, "A Trainable Spaced Repetition Model for Language Learning", ACL 2016 — https://aclanthology.org/P16-1174/ [S; not opened]
- [D12] Third-party Duolingo price guides — https://languageappguide.com/pricing/duolingo-cost/ ; https://duolingoguides.com/how-much-is-super-duolingo/ [S]
- [D13] Video Call feedback design / transcripts — https://www.nasdaq.com/articles/duolingo-expands-video-call-feature-android-adding-new-language-support-and-enhancements [S]
- [D14] Speaking-depth criticism — https://learn.g2.com/does-duolingo-work ; https://aftermath.site/duolingo-ai.md [S]

**Speak**
- [Sp1] Speak homepage — https://www.speak.com/ [V-O]
- [Sp2] Speak, US App Store — https://apps.apple.com/us/app/speak-learn-english/id1286609883 [V-O]
- [Sp3] Speak privacy notice (last updated 06/05/2026) — https://www.speak.com/privacy [V-O]
- [Sp4] LanguaTalk Speak review (updated 2 Nov 2026; site promotes own app) — https://languatalk.com/blog/speak-app-review/ [V-3]
- [Sp5] Speak winter 2025 release (10 Dec 2025) — https://www.speak.com/blog/winter-2025 [V-O]
- [Sp6] Speak help: Streak 101 — https://help.speak.com/en/articles/11645358-streak-101-how-streaks-work-how-to-keep-them [V-O]
- [Sp7] TechCrunch, Series C (Dec 2024) — https://techcrunch.com/2024/12/10/openai-backed-speak-raises-78m-at-1b-valuation-to-help-users-learn-languages-by-talking-out-loud ; https://latent.space/wiki/speak-ba0259c2/ [S]
- [Sp8] Onboarding descriptions — https://lingtuitive.com/blog/speak-review ; https://screensdesign.com/articles/language-learning-app-onboarding/ [S]

**Praktika**
- [P1] Praktika homepage — https://praktika.ai/ [V-O]
- [P2] Praktika, US App Store — https://apps.apple.com/us/app/praktika-ai-language-tutor/id1624701477 [V-O]
- [P3] LanguaTalk Praktika review (updated 23 Jun 2026; site promotes own app) — https://languatalk.com/blog/praktika-review/ [V-3]
- [P4] Praktika privacy policy (last updated 10 Aug 2026) — https://praktika.ai/privacy [V-O]
- [P5] Praktika help: offline use — https://intercom.help/praktika-ai/en/articles/12009296-does-praktika-support-offline-use [V-O]
- [P6] Trustpilot Praktika reviews — https://www.trustpilot.com/review/praktika.ai ; https://ie.trustpilot.com/review/praktika.ai?page=9 [S]
- [P7] TechCrunch, Series A (May 2024) — https://techcrunch.com/2024/05/22/praktika-raises-35-5m-to-use-ai-avatars-to-make-learning-languages-feel-more-natural [S]

**Grammarly**
- [G1] Grammarly plans (rendered with India pricing) — https://www.grammarly.com/plans [V-O]
- [G2] Grammarly Trust Center — https://www.grammarly.com/trust [V-O]
- [G3] "Can I use Grammarly offline?" — https://support.grammarly.com/hc/en-us/articles/115000091011-Can-I-use-Grammarly-offline [V-O]
- [G4] TechCrunch, rebrand to Superhuman (29 Oct 2025) — https://techcrunch.com/2025/10/29/grammarly-rebrands-to-superhuman-launches-a-new-ai-assistant/ [S]
- [G5] US Pro pricing (third-party) — https://www.usecarly.com/blog/grammarly-pricing/ [S]

**BoldVoice**
- [B1] BoldVoice homepage — https://www.boldvoice.com/ [V-O]
- [B2] BoldVoice, US App Store — https://apps.apple.com/us/app/boldvoice-accent-training/id1567841142 [V-O]
- [B3] BoldVoice privacy policy (effective 12 Oct 2026) — https://www.boldvoice.com/privacy [V-O]
- [B4] BoldVoice accent-strength research — https://accent-strength.boldvoice.com/ [S]
- [B5] Almahdi & Smith, "Review of advanced L2 pronunciation tool: BoldVoice", LLT 29(1), 2025 — https://www.lltjournal.org/item/1202/ [S; fetch failed]
- [B6] Complaint aggregators — https://www.complaintsboard.com/boldvoice-b148659 ; https://justuseapp.com/en/app/1567841142/boldvoice-accent-training/reviews [S]

**Yoodli**
- [Y1] Yoodli homepage — https://yoodli.ai/ [V-O]
- [Y2] Yoodli pricing — https://yoodli.ai/pricing [V-O]
- [Y3] TechCrunch, Series B (5 Dec 2025) — https://techcrunch.com/2025/12/05/ex-googlers-yoodli-triples-valuation-to-300m-with-ai-built-to-assist-not-replace-people [S]
- [Y4] Yoodli blog, filler words — https://yoodli.ai/blog/how-to-stop-using-filler-words [S]
- [Y5] FinalRound AI (competitor) Yoodli review — https://www.finalroundai.com/blog/yoodli-review-pros-cons [S]

**Orai**
- [O1] Orai homepage — https://orai.com/ [V-O]
- [O2] Orai, US App Store — https://apps.apple.com/us/app/orai-improve-public-speaking/id1203178170 [V-O]
- [O3] Reviews / changelog — https://justuseapp.com/en/app/1203178170/orai-improve-public-speaking/reviews ; https://apptopia.com/google-play/app/com.orai/about [S]

**Others**
- [Sk1] Speeko homepage — https://www.speeko.co/ [V-O]
- [Sk2] Speeko, US App Store — https://apps.apple.com/us/app/speeko-public-speaking-coach/id1071468459 [V-O]
- [Po1] Poised homepage (shutdown banner) — https://www.poised.com/ [V-O]
- [C1] Cambly English page — https://www.cambly.com/english?lang=en [V-O]
- [C2] Cambly AI features — https://support.cambly.com/hc/en-us/articles/25429180057229-AI-at-Cambly ; https://prnewswire.com/news-releases/with-millions-of-data-points-about-how-people-learn-english-cambly-reimagines-the-learning-experience-using-world-class-tutors-ai-powered-features-302266081.html [S]
- [T1] TalkPal homepage — https://talkpal.ai/ [V-O]
- [T2] TalkPal pricing and reviews — https://languatalk.com/blog/talkpal-review/ ; https://speakshark.com/blog/talkpal-review-2026 [S]

**Market and technical context**
- [M1] GSMA, State of Mobile Internet Connectivity 2025 — https://www.gsma.com/somic/the-state-of-mobile-internet-connectivity-2025/ [S]
- [M2] Microsoft, "Characteristics and limitations of Pronunciation Assessment" (updated 2026) — https://learn.microsoft.com/en-us/azure/foundry/responsible-ai/speech-service/pronunciation-assessment/characteristics-and-limitations-pronunciation-assessment [V-3]
- [M3] Google ML Kit GenAI APIs overview — https://developers.google.com/ml-kit/genai ; https://android-developers.googleblog.com/2025/05/on-device-gen-ai-apis-ml-kit-gemini-nano.html [S]
- [M4] Indie offline pronunciation apps (developer claims) — https://apps.apple.com/us/app/-/id6753924420 ; https://apps.apple.com/us/app/id6758423887 ; https://apps.apple.com/app/id6520394022 [S]

---

## Items that could not be verified (do not rely on them)
- Navigation and tab structure for Duolingo, Stimuler, BoldVoice, Yoodli and Orai. Full tab count for Speak and Praktika.
- Whether ELSA Professional launched (tentatively March 2026), and its price.
- Duolingo's current offline-lesson support and whether speaking exercises run offline. Current Super prices.
- How Stimuler's IELTS band estimate is calibrated. BoldVoice LLT review verdict (fetch failed).
- Any independent, peer-reviewed accuracy figures for ELSA, Stimuler, Speak or Praktika scoring.
- Whether any competitor stores audio on-device only. None claims this except Speeko's general statement [Sk1] and the indie apps [M4].
