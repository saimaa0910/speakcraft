const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";
const ADMIN_UID = "admin_789";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

test("Unauthenticated user cannot read or write any collections", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  const now = new Date(Date.now() - 1000);

  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Alice",
      communicationLevel: "Intermediate",
      onboardingCompleted: true,
      createdAt: now,
      updatedAt: now,
    })
  );
  await assertFails(unauthDb.collection("applicationReleases").where("enabled", "==", true).get());
  await assertFails(unauthDb.collection("extractedTechniques").where("moderationStatus", "==", "PUBLISHED").get());
});

test("User profile, speechFeedback, learningProgress, and cross-user isolation", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const past = new Date(Date.now() - 5000);

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Alice",
      communicationLevel: "Intermediate",
      primaryGoal: "Executive storytelling",
      onboardingCompleted: true,
      dailyGoalMinutes: 15,
      streakDays: 1,
      lastPracticeDate: "2026-11-17",
      totalPracticeMinutes: 15,
      wordsLearnedCount: 3,
      recordingsCount: 0,
      completedExercisesCount: 1,
      fluencyScore: 68,
      pronunciationScore: 72,
      vocabularyScore: 70,
      grammarScore: 75,
      articulationScore: 65,
      listeningScore: 74,
      confidenceSelfRating: 60,
      createdAt: past,
      updatedAt: past,
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("learningProgress").doc("lesson_3vs").set({
      userId: ALICE_UID,
      lessonId: "lesson_3vs",
      moduleSlug: "public_speaking",
      completed: true,
      attemptsCount: 2,
      lastScore: 84,
      notes: "Focused on pause after hook.",
      createdAt: past,
      updatedAt: past,
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("speechFeedback").doc("fb_01").set({
      userId: ALICE_UID,
      sessionId: "sess_01",
      transcript: "Today I practiced pausing.",
      strengths: ["Steady cadence"],
      mistakes: ["Used 'um' once"],
      recordingExamples: ["'So um the plan is'"],
      betterAlternatives: ["Pause silently before 'the plan is'"],
      fillerWords: ["um (1)"],
      repeatedExpressions: [],
      specificExercises: ["Zero-Filler 60s Sprint"],
      pronunciationAssessable: true,
      pronunciationNote: "Final consonants audible.",
      articulationClarity: "Clear consonant release.",
      thoughtOrganization: "PREP ordering clear.",
      nextRecommendedActivity: "Articulation Lab",
      createdAt: past,
      updatedAt: past,
    })
  );

  // Reject Bob reading Alice's progress or feedback
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("learningProgress").get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("speechFeedback").get());
  // Reject Ghost field
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      isAdmin: true,
      updatedAt: new Date(Date.now() - 1000),
    })
  );
});

test("Admin RBAC: ordinary users cannot self-promote, publish releases, or edit global catalog", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const adminDb = testEnv.authenticatedContext(ADMIN_UID).firestore();
  const past = new Date(Date.now() - 3000);

  // Seed admin user with rules disabled (represents server/console provisioning)
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    await ctx.firestore().collection("adminUsers").doc(ADMIN_UID).set({
      userId: ADMIN_UID,
      email: "admin@speakcraft.app",
      role: "ADMIN",
      active: true,
      createdAt: past,
      updatedAt: past,
    });
  });

  // Ordinary user cannot create adminUsers document
  await assertFails(
    aliceDb.collection("adminUsers").doc(ALICE_UID).set({
      userId: ALICE_UID,
      email: "alice@example.com",
      role: "ADMIN",
      active: true,
      createdAt: past,
      updatedAt: past,
    })
  );

  // Retired in-app updater collections (ADR-004 / T-04.1.3) must be denied for BOTH ordinary users and admins
  const retiredRelease = {
    versionName: "1.1.0",
    versionCode: 2,
    channel: "stable",
    releaseDate: "2026-11-17",
    releaseNotes: "Retired collection test.",
    downloadUrl: "https://releases.speakcraft.app/android/stable/speakcraft-1.1.0.apk",
    sha256Checksum: "a".repeat(64),
    minSupportedVersion: "1.0.0",
    mandatory: false,
    enabled: true,
    createdAt: past,
    updatedAt: past,
  };
  await assertFails(aliceDb.collection("applicationReleases").doc("rel_1_1_0").set(retiredRelease));
  await assertFails(adminDb.collection("applicationReleases").doc("rel_1_1_0").set(retiredRelease));
  await assertFails(aliceDb.collection("applicationReleases").where("enabled", "==", true).get());
  await assertFails(adminDb.collection("applicationReleases").get());

  // Admin can create moderated technique; regular user sees only PUBLISHED
  await assertSucceeds(
    adminDb.collection("extractedTechniques").doc("tech_pending").set({
      title: "High Low Buffalo",
      category: "Storytelling",
      difficulty: "Intermediate",
      creatorAdvice: "Pending transcript audit.",
      practicalExercise: "Pending review.",
      sourceVideoTitle: "Pending Video",
      sourceVideoUrl: "https://example.com/pending",
      moderationStatus: "PENDING_REVIEW",
      isCreatorDerived: true,
      createdAt: past,
      updatedAt: past,
    })
  );
  await assertSucceeds(
    adminDb.collection("extractedTechniques").doc("tech_pub").set({
      title: "3Vs of Communication",
      category: "Public Speaking",
      difficulty: "Beginner",
      creatorAdvice: "Align visual, vocal, and verbal channels.",
      practicalExercise: "Audit a 60s recording across all 3 passes.",
      sourceVideoTitle: "Communication Fundamentals",
      sourceVideoUrl: "https://example.com/fundamentals",
      moderationStatus: "PUBLISHED",
      isCreatorDerived: true,
      createdAt: past,
      updatedAt: past,
    })
  );

  await assertFails(aliceDb.collection("extractedTechniques").doc("tech_pending").get());
  await assertSucceeds(aliceDb.collection("extractedTechniques").doc("tech_pub").get());
  await assertSucceeds(
    aliceDb.collection("extractedTechniques").where("moderationStatus", "==", "PUBLISHED").get()
  );

  // Retired applicationUpdateEvents collection must also be default-denied for all callers
  await assertFails(
    aliceDb.collection("applicationUpdateEvents").doc("evt_1").set({
      userId: ALICE_UID,
      fromVersion: "1.0.0",
      toVersion: "1.1.0",
      channel: "stable",
      status: "CHECKED",
      detail: "Retired telemetry test.",
      createdAt: past,
    })
  );
  await assertFails(adminDb.collection("applicationUpdateEvents").get());
});

test("Activity Engine: activityProgress, activityAttempts, savedActivities, and dailyPracticePlans security & schema bounds", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  const past = new Date(Date.now() - 5000);
  const now = new Date(Date.now() - 1000);

  // 1. activityProgress create & partial update
  const progressRef = aliceDb
    .collection("users")
    .doc(ALICE_UID)
    .collection("activityProgress")
    .doc("m01_daily_english_speaking");

  await assertSucceeds(
    progressRef.set({
      userId: ALICE_UID,
      activityId: "m01_daily_english_speaking",
      moduleId: "mod_01_english_fluency",
      skillCategory: "ENGLISH_FLUENCY",
      completed: true,
      attemptsCount: 1,
      bestObjectiveScore: 82,
      latestObjectiveScore: 82,
      latestConfidenceSelfRating: 70,
      totalPracticeSeconds: 65,
      lastPracticedDateKey: "2026-11-18",
      createdAt: past,
      updatedAt: past,
    })
  );

  await assertSucceeds(
    progressRef.update({
      skillCategory: "ENGLISH_FLUENCY",
      completed: true,
      attemptsCount: 2,
      bestObjectiveScore: 88,
      latestObjectiveScore: 88,
      latestConfidenceSelfRating: 78,
      totalPracticeSeconds: 130,
      lastPracticedDateKey: "2026-11-18",
      updatedAt: now,
    })
  );

  // Reject createdAt tampering & ghost fields
  await assertFails(
    progressRef.update({
      createdAt: now,
      updatedAt: now,
    })
  );
  await assertFails(
    progressRef.update({
      ghostField: "hack",
      updatedAt: now,
    })
  );

  // 2. activityAttempts create & immutability
  const attemptRef = aliceDb
    .collection("users")
    .doc(ALICE_UID)
    .collection("activityAttempts")
    .doc("att_01");

  await assertSucceeds(
    attemptRef.set({
      userId: ALICE_UID,
      activityId: "m01_daily_english_speaking",
      moduleId: "mod_01_english_fluency",
      skillCategory: "ENGLISH_FLUENCY",
      exerciseType: "SPEAKING_EXERCISE",
      evaluatorName: "SpeakingEvaluator",
      dateKey: "2026-11-18",
      durationSeconds: 65,
      objectiveScore: 82,
      confidenceSelfRating: 70,
      passed: true,
      aiEvaluated: true,
      responseSummary: "Today I practiced clear speaking without rushing.",
      whatWentWell: ["Steady flow"],
      specificMistakes: ["One 'um' filler"],
      improvementSuggestions: ["Pause cleanly before examples"],
      nextPracticeTask: "Try Impromptu Speaking next.",
      recordedMediaPath: "",
      wordsPerMinute: 128,
      fillerCount: 1,
      createdAt: past,
      updatedAt: past,
    })
  );

  // Attempts are immutable audit logs
  await assertFails(
    attemptRef.update({
      objectiveScore: 100,
      updatedAt: now,
    })
  );

  // Reject out-of-range score > 100
  await assertFails(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("activityAttempts")
      .doc("att_bad_score")
      .set({
        userId: ALICE_UID,
        activityId: "m01_daily_english_speaking",
        moduleId: "mod_01_english_fluency",
        skillCategory: "ENGLISH_FLUENCY",
        exerciseType: "SPEAKING_EXERCISE",
        evaluatorName: "SpeakingEvaluator",
        dateKey: "2026-11-18",
        durationSeconds: 65,
        objectiveScore: 140,
        confidenceSelfRating: 70,
        passed: true,
        aiEvaluated: true,
        createdAt: past,
        updatedAt: past,
      })
  );

  // 3. savedActivities create, list, delete
  const savedRef = aliceDb
    .collection("users")
    .doc(ALICE_UID)
    .collection("savedActivities")
    .doc("m02_tongue_twisters");

  await assertSucceeds(
    savedRef.set({
      userId: ALICE_UID,
      activityId: "m02_tongue_twisters",
      moduleId: "mod_02_pronunciation_articulation",
      title: "Tongue Twisters",
      note: "Practice before morning meetings",
      createdAt: past,
      updatedAt: past,
    })
  );
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).collection("savedActivities").get());

  // 4. dailyPracticePlans create & update
  const planRef = aliceDb
    .collection("users")
    .doc(ALICE_UID)
    .collection("dailyPracticePlans")
    .doc("2026-11-18");

  await assertSucceeds(
    planRef.set({
      userId: ALICE_UID,
      dateKey: "2026-11-18",
      targetMinutes: 30,
      plannedActivityIds: ["m02_pronunciation_drills", "m03_pause_practice", "m01_daily_english_speaking"],
      completedActivityIds: ["m02_pronunciation_drills"],
      pinnedActivityIds: [],
      personalizationSummary: "30-min balanced plan",
      createdAt: past,
      updatedAt: past,
    })
  );

  await assertSucceeds(
    planRef.update({
      targetMinutes: 45,
      plannedActivityIds: ["m02_pronunciation_drills", "m03_pause_practice", "m01_daily_english_speaking", "m06_active_listening"],
      completedActivityIds: ["m02_pronunciation_drills"],
      pinnedActivityIds: ["m06_active_listening"],
      personalizationSummary: "45-min customized plan",
      updatedAt: now,
    })
  );

  // 5. Cross-user isolation & unauthenticated rejection
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("activityProgress").get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("activityAttempts").get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("savedActivities").get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("dailyPracticePlans").get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("activityProgress").get());

  // 6. Owner queries match Kotlin repository queries
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).collection("activityProgress").get());
  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("activityAttempts")
      .orderBy("createdAt", "desc")
      .limit(200)
      .get()
  );
  await assertSucceeds(planRef.get());
  await assertSucceeds(savedRef.delete());
});

