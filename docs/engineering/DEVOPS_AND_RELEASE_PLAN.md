# DevOps and Release Plan

| Field | Value |
|---|---|
| Document ID | ENG-01 |
| Version | 0.2 — Draft for review |
| Date | 2026-11-21 |
| Status | Draft — awaiting Product Owner approval |
| Owner (role) | DevOps / Release Engineer |
| Approval | Pending — Product Owner |
| Related | SEC-01 Security Design (SEC-REQ-*, TC-SEC-*), TST-01 Test Strategy, ADR-001, ADR-002, ADR-004, ADR-005, ADR-006, ADR-007, ADR-008, ADR-012, RD-02 Tech Stack Audit, RD-03 (FND-OPS-*, FND-SEC-02/11), RD-16 §3 (R0 scope), PLN-01 Roadmap, PLN-02 WBS (EP-01), PLN-04 RAID, NFR (NFR-REL-*) |

> **Evidence rule.** Labels follow GOV-02 DC-3: **VERIFIED** (seen at `path:line`), **INFERRED**, **ASSUMPTION**, **RECOMMENDATION**. **Nothing in this plan was built, run or deployed.** No GitHub, Play Console or Firebase setting was inspected online; their current state is **UNKNOWN** unless it is visible in the repository. Proposed thresholds and estimates are **Proposed — requires approval** (DC-5). New tools or third-party actions are flagged **NEW DEPENDENCY — requires approval** (DC-6).
>
> **Division of authority.** Security requirements, threats and security tests are defined in SEC-01. This document defines how the pipeline implements them. The secrets **policy** is SEC-01 §8; the operational **inventory** is §6 here.

---

## 1. Current state (VERIFIED unless labelled)

### 1.1 Workflow `release_on_push.yml` walk-through

There is a single workflow (512 lines) with one job, `build-and-publish-update`, on `ubuntu-latest` (`.github/workflows/release_on_push.yml:47-49`).

| # | Step / setting | What it does | Evidence | Assessment |
|---|---|---|---|---|
| T | Triggers | Push to `main`, `master` or `beta`; tags `v*.*.*`; `workflow_dispatch` with inputs channel, version_name, mandatory, signing_mode (`auto`/`release`/`debug`) | `:3-38` | **Every push to main publishes a release.** No PR workflow exists |
| P | Permissions | `contents: write` for the whole workflow | `:40-41` | Over-privileged for build steps |
| C | Concurrency | `ota-release-${ref}`, no cancel | `:43-45` | OK for releases |
| 1 | Checkout | `actions/checkout@v4`, depth 2 | `:54-57` | Mutable tag |
| 2 | JDK | Temurin 21 via `actions/setup-java@v4` | `:59-63` | OK; mutable tag |
| 3 | Android SDK | `android-actions/setup-android@v3`, platform 36, build-tools 36.0.0 | `:65-68` | Third-party action, mutable tag |
| 4 | Firebase config | Writes `app/google-services.json` from secret `GOOGLE_SERVICES_JSON` (or the legacy alias `GOOGLESERVICES`), `chmod 600`. A Python check requires an Android client for `com.aistudio.speakcraft.qxmvkp` and a web OAuth client (type 3) | `:70-104` (package hard-coded `:91,94`) | Good sanity check. The package name must change if D-15 changes the applicationId |
| 5 | Gradle | `gradle/actions/setup-gradle@v4` with **Gradle 9.3.1 installed by the action**, because no wrapper is committed | `:106-109`; `gradle/wrapper/` holds only `gradle-wrapper.properties` | FND-OPS-02 |
| 6 | Node 20 | `actions/setup-node@v4` | `:111-114` | Used only by the publish script |
| 7 | Versioning | `VERSION_CODE = 2 + GITHUB_RUN_NUMBER`. A branch push gives `1.1.0-beta.(1+run)`; a tag gives `X.Y.Z` (stable) or beta if it has a suffix; dispatch uses the input or a default. SemVer regex check; stable may not carry a pre-release suffix; fails if the GitHub release tag already exists; commit message sanitised to 180 characters | `:116-228` (`:137` formula) | INFERRED: `run_number` restarts if the workflow file is renamed or recreated, so the versionCode would regress. The documentation states `1 + run_number` (`docs/AUTO_UPDATE_GITHUB_CI_SETUP.md:16`), which is drift (FND-OPS-07) |
| 8 | Signing | `stable` or `release` mode requires `ANDROID_KEYSTORE_BASE64`, `STORE_PASSWORD` and `KEY_PASSWORD`; decodes `my-upload-key.jks`, validates alias `upload` with keytool, captures the certificate SHA-256, and builds `assembleRelease`. **Otherwise** (beta with `auto` and no keystore, or `debug` mode) it decodes `DEBUG_KEYSTORE_BASE64`, validates `androiddebugkey`/`android`, and builds `assembleDebug`. A debug-signed stable build is refused | `:230-329` (fallback `:257-262,297-328`) | **Critical (FND-SEC-02):** debuggable, debug-signed builds can reach testers |
| 9 | Build | `gradle <task> -PversionCodeOverride … -PbuildChannelOverride … -PgithubReleaseRepoOverride … --no-daemon` | `:331-353` | Only `assemble*`. No tests, no lint |
| 10 | Verify | Copies the APK, computes SHA-256 and a `.sha256` file, runs `apksigner verify --print-certs`, and asserts the certificate equals the keystore certificate. Writes a step summary | `:355-437` | Good integrity check (keep the pattern) |
| 11 | Release notes | Heredoc containing the commit subject plus machine-readable fields (`VersionCode`, `SHA256`, `SigningCertSHA256`, hard-coded `MinSupportedVersion: 1.0.0`) for the in-app parser | `:439-466` (`:462`) | Not user-facing release notes |
| 12 | GitHub Release | `softprops/action-gh-release@v2`; prerelease for beta; uploads the APK and `.sha256` | `:468-478` | Third-party action, mutable tag |
| 13 | Firestore publish | If the service-account secret is set: `node --test` of the publish script, then `npm install --no-package-lock firebase-admin@^13.0.0` (**unpinned**, with `FIREBASE_SERVICE_ACCOUNT_JSON` in the step environment), then writes `applicationReleases` | `:480-501` (`:483,500`) | FND-OPS-03. Retired by ADR-004 |
| 14 | Warn | Warns if the service account is missing | `:503-506` | — |
| 15 | Cleanup | `always()`: removes the keystores and `google-services.json` | `:508-512` | Good (keep) |

### 1.2 Gradle signing and versioning

| Item | Current | Evidence | Assessment |
|---|---|---|---|
| Release signing | `KEYSTORE_PATH` from env or `rootProject/my-upload-key.jks`; alias `upload`; passwords from env **defaulting to `""`** | `app/build.gradle.kts:49-55` | Should fail fast instead (SEC-01 §8.1) |
| Debug signing | `rootProject/debug.keystore` with hard-coded `android`/`android`, used by the `debug` build type | `app/build.gradle.kts:56-61,70-72` | Replace with the AGP default per-developer debug keystore |
| Minification | `isMinifyEnabled = false`; empty ProGuard rules | `app/build.gradle.kts:66`; `app/proguard-rules.pro` | FND-SEC-11 |
| App Check debug library | `implementation` (ships in release) | `app/build.gradle.kts:139` | → `debugImplementation` (ADR-008) |
| Version defaults | versionCode 2, versionName `1.1.0-beta.1`, channel `beta`, overridable through Gradle properties | `app/build.gradle.kts:10-18,31-32` | Single source of truth needed (§7) |
| BuildConfig | `BUILD_CHANNEL`, `GITHUB_RELEASE_REPO` | `app/build.gradle.kts:36-45` | `GITHUB_RELEASE_REPO` is removed with the updater |
| Test asserting version | One test hard-asserts `VERSION_CODE == 2` and `VERSION_NAME == "1.1.0-beta.1"` | audit §9 (`SemVerAndUpdateTest.kt:146-148`) | Breaks under CI overrides (FND-OPS-07) |
| Firebase config | `googleServices.missing.passthrough=true`, yet `AuthHelper.kt:53,93` references `R.string.default_web_client_id`, which only the JSON generates | `gradle.properties:28` | INFERRED: local builds and unit tests fail without the JSON |
| Toolchain | AGP 9.1.1, Gradle 9.3.1, Kotlin 2.2.10; Java 11 source/target; foojay resolver applied with no toolchain requested | `gradle/libs.versions.toml:2,11`; `gradle/wrapper/gradle-wrapper.properties:3`; `app/build.gradle.kts:75-78`; `settings.gradle.kts:15` | — |
| Repository hygiene | Google repository content-filtered in `pluginManagement`; `FAIL_ON_PROJECT_REPOS` | `settings.gradle.kts:3-9,18` | Good (keep) |

### 1.3 Secrets in use (names VERIFIED from the workflow; values and existence UNKNOWN)

`GOOGLE_SERVICES_JSON` / `GOOGLESERVICES`, `ANDROID_KEYSTORE_BASE64`, `STORE_PASSWORD`, `KEY_PASSWORD`, `DEBUG_KEYSTORE_BASE64`, `FIREBASE_SERVICE_ACCOUNT_JSON`, `github.token` (`release_on_push.yml:73,237-240,483,126`). The target for each is in §6.

### 1.4 Quality gates

| Gate | Present? | Evidence |
|---|---|---|
| google-services sanity check | **Yes** | `:82-104` |
| SemVer and channel validation; duplicate-tag guard | **Yes** | `:174-209` |
| Keystore and alias validation; `apksigner` certificate match; SHA-256 | **Yes** | `:274-289,395-406,380-386` |
| Env-passed inputs (injection hygiene); credential cleanup | **Yes** | `:119-127,508-512` |
| Unit / Robolectric tests (161 `@Test` exist) | **No** | absent |
| Android lint | **No** | absent |
| Firestore rules emulator tests (`firestore.test.js` not runnable as committed) | **No** | absent |
| Rules deployment (repo vs deployed rules UNKNOWN) | **No** | absent |
| PR workflow / branch protection | **No** workflow; protection UNKNOWN | — |
| Gradle wrapper validation, dependency locking | **No** | — |
| Secret or dependency scanning, SBOM | **No** (GitHub-side settings UNKNOWN) | — |
| AAB build; R8 | **No** | `:292-293,324-325`; `app/build.gradle.kts:66` |
| Staged rollout or kill switch | Only the Firestore `enabled` flag | audit §8 |

### 1.5 Current-state summary

| Finding | Severity | Addressed in |
|---|---|---|
| FND-SEC-02: debug builds distributed on push | Critical | §4.4, §5; immediate containment ENG-T-26 |
| FND-OPS-01: no CI gates | High | §4.2–4.3 |
| FND-OPS-04: self-updater; publish script | High | §4.4 (retired with ADR-004) |
| FND-OPS-02: no wrapper; local build needs the JSON | Medium | §3 |
| FND-OPS-03: supply chain | Medium | §11 |
| FND-OPS-05: no crash visibility | Medium | §9 (ADR-007) |
| FND-OPS-07: version drift and test assertion | Low | §7 |

---

## 2. Target build variants and environment configuration

### 2.1 Firebase project topology (RECOMMENDATION; decision ED-01)

| Option | Description | Pros | Cons |
|---|---|---|---|
| A. Single project | Debug and release apps registered in the existing project and named database | Simplest; no new project | Developer and test data mixes with real user data; rules experiments risk production; shared quota; debug certificates authorised against production |
| **B. Two projects (recommended)** | **prod** = the existing project and named database (`firebase.json:4`), retained for legacy migration (D-19, SEC-01 A-3). **dev** = a new project used by debug builds, manual integration testing and test accounts | Isolation; debug certificates and App Check debug tokens never touch prod; safe rules trials | One more project to configure; two `google-services.json` files |
| C. Three projects (dev / staging / prod) | Adds a pre-production project | Mirrors production for QA | Overhead not justified for the team size (ASSUMPTION) |

CI automated tests use the **Firestore emulator with a `demo-` project id**, not the dev project (§4.2).

### 2.2 Build types (RECOMMENDATION)

| Attribute | `debug` | `staging` (optional; ED-02) | `release` |
|---|---|---|---|
| applicationId | `<id per D-15>` + `.debug` | `<id>` + `.staging` | `<id per D-15>` |
| Debuggable | yes | **no** | **no** |
| R8 minify + `shrinkResources` | no | **yes** (same rules as release) | **yes** |
| Signing | Per-developer AGP default debug keystore | Upload or app-signing key (CI only) | App-signing key (pre-Play) or upload key (Play) (§5) |
| Firebase project | dev | dev | prod |
| `google-services.json` source | `app/src/debug/google-services.json` (local, uncommitted) | `app/src/staging/…` (CI secret) | `app/src/release/…` (CI `release` environment secret) |
| Firestore database id | Resource overlay `src/debug/res/values/firebase_config.xml` | overlay | `src/release/res/…` (the existing named database) |
| App Check provider | Debug provider (`debugImplementation`) | Debug provider or none (dev project not enforced) | Play Integrity once Play-linked (ADR-008) |
| Logging | AppLogger verbose | Release behaviour | WARN/ERROR redacted only (SEC-01 §10) |
| Distributed to testers | **Never** | QA only (internal), never public | GitHub Release (interim) or Play tracks |

The `google-services` plugin supports per-variant JSON placement under `src/<variant>/` (INFERRED, plugin behaviour; verify with AGP 9.1.1 in Stage B).

### 2.3 Namespace and applicationId (D-15)

| Item | Current (VERIFIED) | Change impact (INFERRED) |
|---|---|---|
| `namespace` | `com.example` (`app/build.gradle.kts:24`) | A code-only change (package of `R`/`BuildConfig`; source package moves under ADR-012). No user impact |
| `applicationId` | `com.aistudio.speakcraft.qxmvkp` (`app/build.gradle.kts:28`) | **A new applicationId is a new app.** Existing installs cannot update in place. Requires a new Firebase Android app registration and OAuth client, new SHA registrations, and updates to the CI package check (`release_on_push.yml:91,94`). Local legacy data in the old sandbox is unreachable, so only cloud data (signed-in users) can be migrated (SEC-01 §5.6). Low cost if done **before** any Play listing |
| Recommendation | — | Decide D-15 **before** the first Play upload and before R1 beta distribution, so the identity changes at most once |

### 2.4 Feature flags (no Remote Config in the stack)

- Compile-time `BuildConfig` booleans, declared once per build type, for example `FEATURE_WORDS_NOTEBOOK`, `FEATURE_TRANSCRIPT_SYNC`, `FEATURE_ROLE_PLAY`. Default `false` in `release` until approved.
- Flags gate code paths, not data shape. Firestore schema compatibility follows §8.4 regardless of flags.
- Limitation: turning off a shipped feature requires a new release. A remote kill switch would need Remote Config, which would be a new dependency and an ADR (§8.4).

---

## 3. Local development setup (RECOMMENDATION)

### 3.1 Toolchain

| Item | Standard |
|---|---|
| Gradle | Committed wrapper (`gradlew`, `gradlew.bat`, `gradle/wrapper/gradle-wrapper.jar`) for **9.3.1**, generated by the official `wrapper` task, with `distributionSha256Sum` added to `gradle-wrapper.properties` (ADR-006 §4). CI uses `./gradlew` only |
| JDK to run Gradle | **21** (matches CI Temurin 21, `release_on_push.yml:59-63`). Optionally pin it with Gradle's daemon JVM criteria file. Bytecode target stays Java 11 unless ADR-006 changes it |
| Android SDK | Platform 36, build-tools 36.0.0 (as in CI) |
| IDE | Current stable Android Studio that supports AGP 9.1.1 (version confirmed in Stage B) |
| Node.js | 20 LTS, only for the Firestore rules tests |
| Firebase CLI | Installed from the committed `package.json`/lockfile (`npx firebase …`); never a global, unpinned install |

### 3.2 `google-services.json` for development (decision ED-03)

| Option | Description | Assessment |
|---|---|---|
| A. Commit the **dev** project's JSON | Builds work out of the box | Simple. Exposes dev project identifiers publicly; still needs SHA registration for sign-in; blurs the "no Firebase config in git" rule |
| B. Uncommitted; developers download the dev JSON from the Firebase console (access granted per developer) | Normal Firebase practice | A local build still **fails without the file** today (FND-OPS-02) |
| **C. B + build succeeds without it (recommended)** | The web client id is resolved at runtime (`resources.getIdentifier("default_web_client_id", …)`) instead of `R.string.default_web_client_id`. A missing value maps to "Sign-in isn't available in this version" (SEC-01 §4.6). Unit tests use repository fakes, never `FirebaseApp` | Contributors without Firebase access can still build and run the **full no-account core loop** (D-05). CI PR builds need no secrets, which also protects against fork exfiltration (THR-11) |

Supporting changes:

- Add `google-services.json` to `.gitignore` explicitly.
- Keep `googleServices.missing.passthrough=true` (`gradle.properties:28`) for local and PR builds.
- Release builds **fail** if the release JSON is missing (a CI assertion, so prod is never built without configuration).

### 3.3 Developer onboarding steps (documented in `CONTRIBUTING.md` during Stage B)

1. Install JDK 21, Android Studio and SDK 36.
2. `./gradlew assembleDebug testDebugUnitTest lintDebug`. This works with no Firebase file.
3. *(Optional, for sign-in and sync)* Request dev-project access, place the JSON in `app/src/debug/`, and register your own debug keystore SHA-1/SHA-256 in the **dev** project.
4. *(Optional)* Register the App Check debug token printed in Logcat in the dev project.
5. Rules tests: `npm ci && npx firebase emulators:exec --only firestore --project demo-speakcraft "npm test"`.

---

## 4. CI/CD pipeline design (RECOMMENDATION)

Action references below are written `@<sha> # vN`. Every action is pinned to a full commit SHA (§11). YAML is **illustrative, not tested**.

### 4.1 Workflow set (target)

| Workflow | Trigger | Purpose | Secrets |
|---|---|---|---|
| `ci.yml` | `pull_request` → `main`; `push` → `main` | Quality gates; required checks | **None** |
| `release.yml` | `push` tags `v*.*.*` (plus manual re-run of an existing tag) | Build, sign, verify and publish release artefacts | `release` environment only |
| `deploy-rules.yml` *(only if ED-06 = CI deploy)* | `workflow_dispatch` | Deploy Firestore rules to prod | `firebase-prod` environment |
| `nightly.yml` *(R1, optional (FS-00 §4))* | `schedule` once per night on `main`, plus `workflow_dispatch` | Instrumented tests on Gradle Managed Devices and emulator-backed integration suites (TST-01 §14.2); until it exists, instrumented tests run in the release-candidate pipeline (consistency review, CL-21) | **None** |
| *(retired)* `release_on_push.yml` | — | Replaced after ADR-004 approval (ENG-T-12). Containment first (ENG-T-26) | — |

### 4.2 PR workflow `ci.yml`

| Job | Steps | Output / artefact | Required check |
|---|---|---|---|
| `wrapper-validation` | Checkout; `gradle/actions/wrapper-validation@<sha>` | — | **Yes** |
| `build-test` | Checkout; `setup-java` 21; `setup-gradle` (cache on); `./gradlew assembleDebug assembleRelease testDebugUnitTest lintDebug` (`assembleRelease` **unsigned**, to catch R8 and keep-rule regressions); Room schema check (exported schema unchanged or migration test present, if ADR-002) | Unit and Robolectric reports (JUnit XML/HTML), lint HTML + SARIF, R8 mapping (7 days), **debug APK for reviewers only** (retention 7 days, never attached to a release) | **Yes** |
| `security-static` | TC-SEC-010 (cloud-AI and appcheck-debug classpath check over `releaseRuntimeClasspath`, plus a DEX string scan of the unsigned release APK); TC-SEC-011 (merged manifest); TC-SEC-012 (backup rules); TC-SEC-041 (no raw `Log`); TC-SEC-045 (workflow pins and permissions); TC-SEC-043 (secret pattern scan) | Check logs | **Yes** |
| `rules-test` | `setup-node` 20; `setup-java` (emulator); `npm ci` (lockfile); `npx firebase emulators:exec --only firestore --project demo-speakcraft "npm test"` (TC-SEC-020…038) | Rules coverage report (HTML) | **Yes** |
| `dependency-review` *(optional)* | `actions/dependency-review-action@<sha>` on PRs | — | Advisory. Availability depends on repository visibility and plan (verify) |

Common settings:

- `permissions: contents: read` at workflow level.
- `concurrency: ci-${{ github.ref }}` with `cancel-in-progress: true`.
- Runner pinned to an explicit image label (for example `ubuntu-24.04`) instead of `ubuntu-latest`.
- Job timeout 30 minutes (**Proposed**).

```yaml
# ci.yml — ILLUSTRATIVE (not executed)
name: CI
on:
  pull_request: { branches: [main] }
  push: { branches: [main] }
permissions: { contents: read }
concurrency: { group: ci-${{ github.ref }}, cancel-in-progress: true }
jobs:
  build-test:
    runs-on: ubuntu-24.04
    timeout-minutes: 30
    steps:
      - uses: actions/checkout@<sha> # v4
      - uses: actions/setup-java@<sha> # v4
        with: { distribution: temurin, java-version: "21" }
      - uses: gradle/actions/setup-gradle@<sha> # v4
      - run: ./gradlew assembleDebug assembleRelease testDebugUnitTest lintDebug --stacktrace
      - uses: actions/upload-artifact@<sha> # v4
        if: always()
        with: { name: reports, path: "app/build/reports/**", retention-days: 7 }
  rules-test:
    runs-on: ubuntu-24.04
    steps:
      - uses: actions/checkout@<sha> # v4
      - uses: actions/setup-node@<sha> # v4
        with: { node-version: "20", cache: npm }
      - uses: actions/setup-java@<sha> # v4
        with: { distribution: temurin, java-version: "21" }
      - run: npm ci
      - run: npx firebase emulators:exec --only firestore --project demo-speakcraft "npm test"
```

### 4.3 Branch protection and repository settings for `main` (RECOMMENDATION; ED-08)

| Setting | Value |
|---|---|
| Require a pull request before merging | Yes. Direct pushes blocked, admins included |
| Required approvals | 1 (ASSUMPTION: at least a two-person team, developer plus Product Owner or reviewer). With a solo developer, keep the PR requirement and required checks, and allow self-merge |
| Required status checks | `wrapper-validation`, `build-test`, `security-static`, `rules-test`; branch must be up to date |
| History | Linear; no force pushes; no deletions |
| CODEOWNERS | `.github/**`, `firestore.rules`, `firebase.json`, `app/build.gradle.kts`, `gradle/**`, `app/src/main/AndroidManifest.xml`, `app/src/main/res/xml/**` → Security Engineer and Release Engineer |
| Tag protection (rulesets) | Tags `v*` can be created only by maintainers |
| Environments | `release` (required reviewer: Product Owner or Release Manager; deployment limited to tags `v*`); `firebase-prod` (if ED-06). Required reviewers on private repositories depend on the GitHub plan; verify (ASSUMPTION) |
| Secret scanning and push protection; Dependabot alerts | Enable where the plan allows (SEC-01 §8.1) |

### 4.4 CD pipeline `release.yml` (tag-driven only)

Principles: releases happen **only** from tags `vX.Y.Z[-beta.N]` on a commit in `main`. **Never on every push. Release-signed only. Debug builds are never distributed** (ADR-004, ADR-008 §4).

| Step | Action | Gate / evidence |
|---|---|---|
| 1 | The job runs in environment `release`, which pauses for the required reviewer's approval | Approval record (who, when) |
| 2 | Checkout the tag; assert the tag commit is reachable from `origin/main` | Fail otherwise |
| 3 | Assert that the CI checks for that commit passed (re-run `ci.yml` jobs through `workflow_call`, or query the check-runs API) | Fail on any failure |
| 4 | Assert the tag equals `version.properties` `versionName`; compute versionCode (§7); assert it is greater than the last released code (read from the last GitHub release's notes or Play) | Fail on mismatch |
| 5 | Restore the release `google-services.json` and signing key from **environment** secrets (`chmod 600`, under `$RUNNER_TEMP`) | — |
| 6 | `./gradlew bundleRelease assembleRelease` | — |
| 7 | Verify: `apksigner verify --print-certs` (APK) and `jarsigner -verify` (AAB); certificate equals the expected fingerprint; certificate is **not** the debug certificate; `apkanalyzer manifest debuggable` = false; R8 mapping present (TC-SEC-009); TC-SEC-010 on the signed artefact | Fail on any mismatch |
| 8 | `SHA256SUMS` for the APK and AAB; signing-certificate SHA-256 recorded | Artefacts |
| 9 | Release notes = the `CHANGELOG.md` section for this version (Keep-a-Changelog format), plus SHA-256 values, the certificate fingerprint and install instructions. **No machine-parsed updater fields** | Fail if the section is missing |
| 10 | Create the GitHub Release: **draft** first; the Release Manager reviews and publishes. Beta (`-beta.N`) → prerelease. Assets: APK + `SHA256SUMS`. AAB, mapping file and SBOM (if any) → workflow artefacts (retention 90 days) **and** the offline release archive | Release URL |
| 11 | Play upload *(once ADR-004 B is approved and the account exists)*: **manual upload of the AAB in Play Console (recommended)**. Automated alternatives are **NEW DEPENDENCY — requires approval**: Gradle Play Publisher (Gradle plugin) or a third-party upload action, plus a Play service account | Play Console release record |
| 12 | Cleanup in `always()`: delete key material and the JSON (keep today's pattern, `release_on_push.yml:508-512`) | — |

**Rules deployment** is separate from app releases and runs before the app release when the change is additive (§8.4). ED-06 decides between manual deployment by the Release Manager (`firebase deploy --only firestore:rules` from a clean checkout of the tag, with the CLI output and ruleset id recorded in the release checklist) and a `deploy-rules.yml` workflow in environment `firebase-prod`, authenticated with keyless Workload Identity Federation (`google-github-actions/auth`: **NEW DEPENDENCY — requires approval**) or a narrowly scoped deployer service account.

---

## 5. Signing (RECOMMENDATION; decision ED-04)

### 5.1 Strategy

| Phase | Key used to sign distributed APKs | Notes |
|---|---|---|
| Interim (GitHub Releases, before Play) | **App signing key K_app**, owned by the project | CI signs inside the protected `release` environment |
| Play (ADR-004 B) | Google signs with K_app under **Play App Signing**. CI signs AABs with a separate **upload key K_upload** | At Play onboarding, choose "use an existing app signing key" and upload K_app through Play's encrypted export (PEPK). GitHub-installed builds can then update to Play builds **without reinstalling** (INFERRED, Play App Signing behaviour; verify at onboarding) |
| Alternative | Let Play generate K_app | Users who installed GitHub builds must reinstall once at the Play transition. Acceptable if D-15 changes the applicationId anyway |

- Whether the existing secret `ANDROID_KEYSTORE_BASE64` (alias `upload`) exists and what it signed is **UNKNOWN**. If D-15 keeps the applicationId and that key signed builds testers use, it can become K_app. Otherwise generate a new K_app.
- **Existing debug-signed installs must reinstall** in every option, because the certificate changes (`release_on_push.yml:426` already warns about this).
- Signature schemes: v2 + v3 (minSdk 24 does not need v1; AGP selects schemes). The release build fails if signing variables are missing.

### 5.2 Key custody

| Key | Custodian (role) | Storage | Backup | CI access | Rotation |
|---|---|---|---|---|---|
| K_app (app signing) | Release Manager (primary) + Product Owner (secondary) | Password manager vault (keystore file plus passwords) | Encrypted offline copies in **two** separate locations; restore tested once a year (**Proposed**) | Interim only, through `release` environment secrets; **removed from CI after Play onboarding** | Only on compromise. Play supports signing-key rotation with v3 lineage on Android 9+ (INFERRED); older devices keep the original key |
| K_upload | Release Manager | Vault | Vault backup | `release` environment secrets | On compromise or custodian change, through a Play upload-key reset request |
| Developer debug keys | Each developer | `~/.android/debug.keystore` (AGP default) | none needed | **Never** | Ad hoc. Register in the dev project only |
| Shared CI debug keystore (`DEBUG_KEYSTORE_BASE64`) | — | — | — | **Retire** | Treat as compromised (SEC-01 §8.1; SEC-REQ-39) |

Key parameters: RSA ≥ 2048 (4096 recommended), validity ≥ 30 years (Play requires validity extending beyond 2033; INFERRED). Publish the K_app certificate SHA-256 in `README` and every release note so sideloading users can check it with `apksigner verify --print-certs`.

---

## 6. Secrets inventory and handling (operational; policy in SEC-01 §8)

| Secret | Purpose | Current (VERIFIED name) | Target name and scope | Used by job | Rotation / action | Owner |
|---|---|---|---|---|---|---|
| Firebase config, prod | Release builds | `GOOGLE_SERVICES_JSON` or `GOOGLESERVICES` (repository-wide) | `GOOGLE_SERVICES_JSON_PROD`, **environment `release`** | `release.yml` | Replace when the Android app registration changes (D-15). Delete the legacy alias | Release Engineer |
| Firebase config, dev | Staging builds (ED-02) | — | `GOOGLE_SERVICES_JSON_DEV`, environment `staging` (non-fork only) | staging build | As above | Release Engineer |
| Release keystore | Signing | `ANDROID_KEYSTORE_BASE64`, `STORE_PASSWORD`, `KEY_PASSWORD` | `RELEASE_KEYSTORE_BASE64`, `RELEASE_STORE_PASSWORD`, `RELEASE_KEY_ALIAS`, `RELEASE_KEY_PASSWORD`, **environment `release`** | `release.yml` | §5.2 | Release Manager |
| Debug keystore | Debug signing for beta | `DEBUG_KEYSTORE_BASE64` | **Delete** | — | Remove its fingerprints from the prod Firebase app | Security Engineer |
| Firebase service account | Writes `applicationReleases` | `FIREBASE_SERVICE_ACCOUNT_JSON` | **Delete** after ADR-004; **revoke the key in GCP IAM** | — | One-time | Security Engineer |
| Rules deployer (only if ED-06 = CI) | `firebase deploy --only firestore:rules` | — | Workload Identity Federation (preferred, no stored key) or `FIREBASE_RULES_DEPLOYER_JSON` in environment `firebase-prod`, with a role limited to rules administration | `deploy-rules.yml` | Key rotation every 90 days if a key is used (**Proposed**) | Release Engineer |
| Play publisher (only if automated upload is approved) | Upload the AAB | — | Environment `play`; Play Console permission limited to this app's testing tracks | `release.yml` | 90 days or on change | Release Manager |
| `GITHUB_TOKEN` | Create releases | `contents: write` workflow-wide (`:40-41`) | `contents: read` by default; `contents: write` only on the release job | `release.yml` | n/a | — |
| App Check debug tokens | Developer testing | — | Never stored in the repository or CI; registered in the dev project only | — | Revoke when a developer leaves | Developers |

Handling rules (RECOMMENDATION):

1. Secrets are passed through `env:`, never interpolated into `run:` scripts (current good practice, `:119-127`).
2. Never echo or print secrets. Masking is a backstop, not a control.
3. Decode key material into `$RUNNER_TEMP` with `chmod 600`, and delete it in an `always()` step.
4. **No secrets in `ci.yml`**, and never use `pull_request_target`.
5. Keep a rotation log in the vault: secret name, date, who, reason.
6. When someone leaves the project: rotate everything they could access within 7 days (**Proposed**).

---

## 7. Versioning (RECOMMENDATION; decision ED-07)

### 7.1 Scheme

- **versionName:** SemVer `MAJOR.MINOR.PATCH` with an optional `-beta.N` (N = 1–98).
- **versionCode** is derived deterministically from versionName:

  `versionCode = MAJOR × 1,000,000 + MINOR × 10,000 + PATCH × 100 + B`

  where `B = N` for `-beta.N` and `B = 99` for a final release.

| versionName | versionCode |
|---|---|
| 1.2.0-beta.1 | 1,020,001 |
| 1.2.0-beta.7 | 1,020,007 |
| 1.2.0 | 1,020,099 |
| 1.2.1 | 1,020,199 |
| 2.0.0 | 2,000,099 |

Properties of the scheme:

- A final release is always greater than its betas, and every later version is greater still.
- It is greater than any legacy code (`2 + run_number`), so in-place upgrades from release-signed legacy installs remain possible if D-15 keeps the id.
- Constraints: MINOR ≤ 99, PATCH ≤ 99. The Play maximum of 2,100,000,000 allows MAJOR up to 2099.

### 7.2 Single source of truth

- `version.properties` at the repository root holds **only** `versionName`. Gradle computes versionCode with the formula. Local builds use the file value.
- A release PR bumps `version.properties` **and** adds the `CHANGELOG.md` section. The tag is created on that merge commit, and `release.yml` asserts that tag == file (§4.4 step 4).
- Remove `versionCodeOverride` and `versionNameOverride` (`app/build.gradle.kts:10-14`), `githubReleaseRepoOverride` and `GITHUB_RELEASE_REPO`. Keep `BUILD_CHANNEL` only if the About screen shows "beta".
- Remove the test that asserts fixed version values (FND-OPS-07). Replace it with a unit test of the formula.
- A versionCode is never reused. A rebuilt or fixed release always bumps PATCH or the beta number.

---

## 8. Release channels, rollout, checklist, rollback and hotfix

### 8.1 Channels (ADR-004)

| Channel | Audience | Artefact | Distribution | Availability |
|---|---|---|---|---|
| Developer builds | Developers | `debug` | Local / CI reviewer artefact (7 days) | Always. **Never distributed** |
| QA builds *(if ED-02)* | QA | `staging` (release-like) | CI artefact or internal sharing | R0+ |
| **Interim beta** | Recruited testers (Proposed ≤ 50) | Release-signed APK from a `-beta.N` tag | GitHub Release marked *prerelease*. Testers install manually (enable "install unknown apps" for the browser or file manager) after checking the SHA-256 and certificate. **No in-app updater**; testers are told of new builds through the beta channel (email or issue thread) | R0/R1 until Play exists |
| Play internal testing | ≤ 100 testers | AAB | Play internal track | When the Play account and D-03 allow (DEP: Play account) |
| Play closed testing | Beta cohort | AAB | Closed track | R1/R2 |
| Play production | Public | AAB | Staged rollout (§8.2) | R3 or per D-03 |

### 8.2 Staged rollout (Play production; Proposed — requires approval)

| Stage | Users | Minimum soak | Advance only if (Android vitals / feedback) |
|---|---|---|---|
| 1 | 1% | 48 h | User-perceived crash rate < 1.09% and ANR rate < 0.47% (Play bad-behaviour thresholds; INFERRED); no S1 issue reported |
| 2 | 5% | 48 h | As above, and no regression of more than +0.2 pp against the previous release (Proposed) |
| 3 | 20% | 72 h | As above |
| 4 | 50% | 72 h | As above |
| 5 | 100% | — | — |

NFR-REL-* owns the final thresholds. The interim GitHub channel has no percentage rollout; risk is limited by the small tester cohort and by publishing prerelease first.

### 8.3 Release checklist

A release issue is created from a template. Every item needs its evidence attached before the `release` environment approval.

| # | Item | Owner (role) | Evidence required | Blocking |
|---|---|---|---|---|
| 1 | Scope matches the plan; the `CHANGELOG.md` section is complete and accurate, with no unverified claims (RD-15 EP-5) | Product Manager | CHANGELOG diff in the release PR | Yes |
| 2 | Release PR merged; `version.properties` bumped; computed versionCode greater than the last release | Release Engineer | PR link; CI log of the formula assertion | Yes |
| 3 | All required checks green on the release commit (build, unit, Robolectric, lint, security-static, rules-test) | Android Tech Lead | CI run URL | Yes |
| 4 | Lint: no new errors; warnings triaged | Android Tech Lead | Lint SARIF artefact | Yes |
| 5 | Rules emulator suite passed and the coverage report reviewed | Security Engineer | Coverage artefact | Yes |
| 6 | Room migration tests passed (if the schema changed) | Android Tech Lead | Test report | Yes |
| 7 | Release artefact checks TC-SEC-009 and TC-SEC-010 passed on the signed artefacts | Security Engineer | `release.yml` log | Yes |
| 8 | Manual regression on the TST-01 device matrix, including speech tiers T1, T2 and T3a/T3b, offline mode, sign-in/sign-out and deletion | QA | Test-run record (TST-01 template) | Yes |
| 9 | Privacy review: SEC-01 §3 inventory unchanged or updated; in-app text, privacy policy and data-safety inputs consistent (TC-SEC-044) | Security Engineer + Product Owner | Signed privacy checklist | Yes |
| 10 | Firestore rules: if changed, deployed **before** the app release (additive) and ruleset id recorded; deployed rules equal the repository rules | Release Engineer | Deploy output, ruleset id, commit SHA | Yes (if changed) |
| 11 | Firebase/GCP configuration evidence current: providers, API-key restrictions, App Check status, budget alerts, SHA fingerprints (TC-SEC-049) | Release Engineer | Screenshots or exports | Yes |
| 12 | Signing certificate fingerprint equals the expected K_app/K_upload value | Release Engineer | `apksigner` / `jarsigner` output | Yes |
| 13 | Dependency alerts: no open critical or high without a waiver (TC-SEC-047) | Security Engineer | Dependabot screenshot or waiver | Yes |
| 14 | Operational readiness links valid: privacy policy, account-deletion page, support contact (§9.3) | Product Owner | Link check | Yes (Play) |
| 15 | Rollback plan confirmed: previous artefact archived; affected flags known; schema compatibility (§8.4) confirmed | Release Engineer | Checklist note | Yes |
| 16 | `release` environment approval given | Product Owner / Release Manager | GitHub approval record | Yes |
| 17 | Draft GitHub Release reviewed and published; Play track updated (if applicable) | Release Engineer | Release and Play URLs | Yes |
| 18 | Testers notified with install and verification instructions (interim channel) | Product Manager | Message link | No |
| 19 | 72 h post-release monitoring: vitals or shared diagnostics; feedback triage; go / halt decision recorded | Release Engineer + QA | Monitoring note | No (but it triggers §8.5) |

### 8.4 Rollback strategy

| Layer | Strategy |
|---|---|
| Android app | Android cannot downgrade an install (versionCode). **Play:** *halt rollout* stops new installs of the bad version, then ship a fix-forward hotfix or a **"rollback build"** (the previous good code rebuilt with a higher versionCode). **GitHub interim:** mark the release "Do not install" in its notes, remove the asset or revert it to prerelease, and publish the hotfix |
| Firestore schema compatibility rules | (1) Changes within a schema version are **additive only**. (2) New required fields or changed semantics need a `schemaVersion` bump, with rules accepting **both** versions until the oldest supported app version writes the new one (at least two releases). (3) Never rename or remove fields that supported versions read. (4) **Deploy order:** rules accepting old + new go first, then the app; old acceptance is removed only after adoption. (5) Clients ignore unknown fields. (6) A client that sees data newer than it understands (`meta/sync.maxDataSchemaVersion`) stops uploading and asks the user to update, so it cannot overwrite newer documents under last-writer-wins (SEC-01 §5.4) |
| Firestore rules | Keep the previous ruleset id. Roll back by redeploying the previous tagged `firestore.rules` (or through the console's rules history). A rules rollback must not re-open retired legacy paths |
| Room | Migrations are never destructive. Downgrade is not supported, so rollback means fix forward |
| Feature flags | Compile-time `BuildConfig`. Disabling a shipped feature needs a new release. Mitigation: ship risky features **off** in `release` and on in beta first. A remote kill switch would need Remote Config (new dependency, ADR) and is **not proposed** |

### 8.5 Hotfix process

Defect severity is defined by **TST-01 §11.1, which is authoritative** (S1 — Critical, S2 — High, S3 — Medium, S4 — Low; consistency review, CL-25). The examples below are a summary; this table only sets the release targets per severity.

| Severity | Definition (examples) | Target |
|---|---|---|
| S1 — Critical | Privacy leak, data loss, crash on launch, security vulnerability, sign-in broken for all users | Fix released ≤ 48 h (**Proposed**); halt rollout immediately |
| S2 — High | A core-loop feature broken for a segment (one speech tier, one OEM) | Next patch ≤ 7 days (**Proposed**) |
| S3 — Medium, S4 — Low | Other defects | Normal release train |

Steps:

1. Open an incident issue with the severity.
2. Halt the rollout or mark the GitHub release.
3. Fix on a branch from `main`. If `main` contains unreleased risky work, branch `hotfix/x.y.(z+1)` from the release tag and cherry-pick back to `main` afterwards.
4. Open a PR with **the same required checks** (no bypass).
5. Bump PATCH and update the CHANGELOG.
6. Tag, run `release.yml`, and get environment approval.
7. Write a post-incident note within 5 working days (cause, detection, prevention, test added).

Branching model: trunk-based. Short-lived feature branches; `main` is always releasable; releases come from tags.

---

## 9. Crash monitoring, logging and operational readiness

### 9.1 Crash and stability monitoring (ADR-007; decision D-13)

| Phase | Mechanism | Notes |
|---|---|---|
| R0/R1 interim beta | **Local, PII-free diagnostic log** with an uncaught-exception recorder that chains to the default handler. Settings → About → "Share diagnostics" with a preview (SEC-01 §10.2). Tester feedback channel | No SDK; nothing is sent automatically |
| Play tracks | **Play Console Android vitals** (crash rate, ANR rate, stack traces). R8 mapping files uploaded with each AAB so traces are deobfuscated | No SDK |
| Only if A + C prove insufficient | Firebase Crashlytics: **NEW DEPENDENCY — requires approval** (D-13); opt-in; PII-scrubbed; data-safety update | — |

### 9.2 Logging

All logging goes through `AppLogger` (SEC-01 §10.1). CI enforces it (TC-SEC-041). Release builds emit WARN and ERROR only, redacted.

### 9.3 Operational readiness

| Item | Requirement | Owner | Needed by |
|---|---|---|---|
| Privacy policy URL | Public page with the SEC-01 §9.5 content; versioned; linked in-app (Settings → About) and in the store listing | Product Owner (content), Release Engineer (hosting, ED-09) | Interim beta (recommended); Play (mandatory) |
| Account-deletion web page | Public request page plus the operator runbook (SEC-01 §9.4); SLA ≤ 30 days (Proposed) | Product Owner + Security Engineer | Before any Play submission |
| Support contact | A monitored email address shown in-app and in the listing | Product Owner | Interim beta |
| Data-safety form | Built from SEC-01 §9.6; reviewed for every release that changes data flows | Security Engineer | Play |
| App content declarations | Target audience (adults, A-4), ads: none, content-rating questionnaire, permissions declarations | Product Manager | Play |
| GCP budget and usage alerts | Budget alert at 50/90/100% of a monthly budget (amount **Proposed** by the Product Owner); Firestore read/write usage alerts | Release Engineer | R0 |
| Firebase project hygiene | Only the Google provider enabled; API-key restrictions; unused Android apps and SHA fingerprints removed (including the shared debug certificate); App Check metrics reviewed | Release Engineer + Security Engineer | R0 (prod), repeated each release (checklist #11) |
| Firestore backup / PITR | Decide whether to enable point-in-time recovery or scheduled backups (cost against recovery), and disclose the retention window (ED-10) | Product Owner | R1 |
| Incident contacts | Named responder and backup for S1 (§8.5) | Product Owner | Interim beta |

---

## 10. Environments and test devices (handoff to TST-01)

TST-01 owns the device matrix, test levels and exit criteria. ENG-01 provides the environments and builds.

| Environment | Purpose | Data | Provided by ENG |
|---|---|---|---|
| Local | Development | Firestore emulator or dev project | Wrapper, onboarding guide (§3.3), emulator scripts |
| CI | Automated gates | Firestore emulator, `demo-speakcraft` (offline) | `ci.yml` jobs and artefacts (§4.2) |
| Dev Firebase project | Manual integration: sign-in, sync, multi-device, deletion | Test Google accounts only (no real user data) | Project setup, test-account list, seed and cleanup scripts (Stage B) |
| Prod Firebase project | Real users | Real data | Release-only access; checklist evidence |

Device handoff to TST-01 (INPUT; TST-01 decides the final matrix):

- Coverage of each speech tier: **T3** devices on API 24–30, **T2** on API 31–32, **T1** on API 33+ (ADR-009). At least two OEMs; at least one low-RAM device. The final minSdk depends on D-14. The SPK-01 devices are reused.
- ENG provides, per release candidate:
  - a release-signed RC APK (or a `staging` build if ED-02);
  - install and verification instructions;
  - `adb` scripts for TC-SEC-008, 013 and 050 (intent fuzzing, `bmgr` backup and restore);
  - a log-capture procedure for TC-SEC-048.
- Cloud device farms (for example Firebase Test Lab) are a new service with cost and are **not proposed**; they need an approval if QA asks for them.

---

## 11. Supply-chain hardening (RECOMMENDATION; implements SEC-REQ-32, 33)

| Control | Implementation | Status |
|---|---|---|
| Pin actions by full SHA | Every `uses:` → `@<40-hex> # vX.Y.Z`. Applies to the current `@v4`, `@v3` and `@v2` tags (`release_on_push.yml:55,60,66,107,112,469`). TC-SEC-045 enforces it | RECOMMENDATION |
| Dependabot updates | `.github/dependabot.yml` for `github-actions`, `gradle` (version catalog) and `npm` (rules tests); weekly; grouped minor/patch updates | RECOMMENDATION (GitHub-native) |
| Gradle wrapper integrity | Committed wrapper; `distributionSha256Sum`; wrapper validation in CI (TC-SEC-046) | RECOMMENDATION |
| Gradle dependency pinning | The version catalog already uses explicit versions (`gradle/libs.versions.toml`). Add Gradle **dependency locking** (built-in) for the release runtime classpath. Gradle **dependency verification** metadata (built-in) is optional later, given its maintenance cost | RECOMMENDATION |
| Repository restrictions | Keep `FAIL_ON_PROJECT_REPOS` and the Google-repository content filter (`settings.gradle.kts:3-9,18`) | VERIFIED (keep) |
| npm | `package.json` + `package-lock.json` for the rules tests; `npm ci` only. **No `npm install` of unpinned packages** in workflows (current `:500`) | RECOMMENDATION |
| Minimal token permissions | Workflow default `contents: read`; job-level escalation only in `release.yml` | RECOMMENDATION |
| Protected environments | `release` and `firebase-prod` with required reviewers and tag/branch deployment rules (§4.3) | RECOMMENDATION (plan-dependent) |
| Fork safety | No secrets in PR workflows; no `pull_request_target`; inputs passed through `env:` (keep) | RECOMMENDATION |
| CODEOWNERS | Sensitive paths (§4.3) | RECOMMENDATION |
| Runner image | Explicit label (`ubuntu-24.04`) instead of `ubuntu-latest` | RECOMMENDATION |
| Third-party actions | Minimise. `android-actions/setup-android` and `softprops/action-gh-release` are third-party; prefer the preinstalled runner SDK and `gh release create` (GitHub CLI preinstalled) to remove both. Verify the SDK components on the runner image | RECOMMENDATION |
| Build provenance (optional) | `actions/attest-build-provenance` (GitHub first-party) for release APK/AAB; availability for private repositories depends on the plan | Optional |
| SBOM (optional) | GitHub dependency-graph SBOM export (built-in). A CycloneDX Gradle plugin would be a **NEW DEPENDENCY** and is not proposed for R1 | Optional |
| Release integrity for users | `SHA256SUMS` plus the certificate fingerprint in every release note and in `README` | RECOMMENDATION |

---

## 12. Work items (input to PLN-02 WBS, epic EP-01 Foundation)

Estimates are **relative and preliminary** (S ≈ ≤ 1 day, M ≈ 2–4 days, L ≈ 1–2 weeks of one engineer; **Proposed — requires approval**). The feature grouping F-01.x is a proposal; PLN-02 assigns the final IDs.

| ID | Work item | Proposed group | Est. | Depends on | Verifies / delivers |
|---|---|---|---|---|---|
| ENG-T-26 | **Immediate containment:** make `release_on_push.yml` `workflow_dispatch`-only or require the release keystore; stop debug distribution; freeze admin and shared writes in the deployed rules (SEC-01 §2.3, L0) | F-01.4 Security controls | S | PO approval | THR-04, THR-16, THR-18 |
| ENG-T-01 | Commit the Gradle 9.3.1 wrapper with `distributionSha256Sum`; CI uses `./gradlew` | F-01.1 Build & tooling | S | ADR-006 | FND-OPS-02; TC-SEC-046 |
| ENG-T-02 | Build and unit-test without `google-services.json` (runtime web-client-id lookup; per-variant config directories; `.gitignore` entry; tests free of `FirebaseApp`) | F-01.1 | M | ED-03; FS-01 | §3.2 |
| ENG-T-03 | Create the dev Firebase project; register debug apps; developer SHA and App Check debug-token guide | F-01.1 | S | ED-01 | §2.1 |
| ENG-T-04 | Build types (`debug` suffix, `release` R8 + `shrinkResources`, optional `staging`); keep rules for Firestore mappers and kotlinx-serialization; per-variant resource overlays | F-01.1 | L | ED-02; ADR-006 | SEC-REQ-27 |
| ENG-T-05 | Signing configuration refactor: env-based, fail-fast; remove the hard-coded `debugConfig`; publish fingerprints | F-01.3 Release / CD | S | ED-04 | SEC-REQ-27, 39 |
| ENG-T-06 | Versioning: `version.properties`, versionCode formula, formula unit test; remove override properties and the fixed-version test | F-01.3 | S | ED-07 | FND-OPS-07 |
| ENG-T-07 | `ci.yml`: wrapper validation, build (debug + unsigned release), unit + Robolectric, lint, artefacts, concurrency, minimal permissions | F-01.2 CI | M | ENG-T-01, 02 | FND-OPS-01 |
| ENG-T-08 | Rules test harness: `package.json` + lockfile, emulator job with a `demo-` project, port `firestore.test.js`, coverage artefact | F-01.2 | M | — | SEC-REQ-12 |
| ENG-T-09 | Target rules plus emulator suite TC-SEC-020…038, including the L1 legacy variant (with the Security Engineer; schema from ARC-03) | F-01.4 | L | ARC-03; ENG-T-08 | SEC-REQ-07–13 |
| ENG-T-10 | Security static checks: TC-SEC-010 (classpath + DEX scan), 011 (manifest), 012 (backup rules), 041 (logging), 043 (secret scan), 045 (workflow lint) | F-01.4 | M | ENG-T-07 | SEC-REQ-28–31, 26, 32, 34 |
| ENG-T-11 | `release.yml`: tag-only, environment approval, AAB + APK, signature and debuggable verification, `SHA256SUMS`, notes from CHANGELOG, draft GitHub Release | F-01.3 | M | ENG-T-05, 06, 13 | SEC-REQ-27 |
| ENG-T-12 | Retire `release_on_push.yml`, the publish script and its tests; delete `DEBUG_KEYSTORE_BASE64` and `FIREBASE_SERVICE_ACCOUNT_JSON`; revoke the service-account key | F-01.3 | S | ADR-004 (D-03) | THR-04, THR-10 |
| ENG-T-13 | Pin all actions by SHA; Dependabot configuration; CODEOWNERS; branch protection, tag rules and environments configured (screenshots as evidence) | F-01.2 | S | ED-08 | SEC-REQ-32 |
| ENG-T-14 | Migrate secrets to environments under the target names; start the rotation log | F-01.3 | S | ENG-T-13 | §6 |
| ENG-T-15 | Signing-key strategy execution: K_app decision, vault custody, two offline backups, restore test | F-01.3 | M | ED-04; D-15 | THR-17 |
| ENG-T-16 | `CHANGELOG.md`, release-notes template, release-checklist issue template (§8.3), hotfix template | F-01.3 | S | — | §8 |
| ENG-T-17 | `AppLogger` with redaction; replace the 84 `Log` calls; remove PII from `FirestoreErrorHandler` | F-01.4 | M | — | SEC-REQ-34; TC-SEC-040 |
| ENG-T-18 | Diagnostic log, uncaught-exception recorder and "Share diagnostics" (narrow FileProvider path) | F-01.5 Ops readiness | M | ADR-007 (D-13); ENG-T-17 | SEC-REQ-35 |
| ENG-T-19 | Network security config; explicit no-cleartext; manifest clean-up (permissions allow-list, FileProvider removal, `<queries>`) | F-01.4 | S | ADR-004, ADR-009, ADR-010 | SEC-REQ-29–31 |
| ENG-T-20 | Backup and data-extraction rules (SD-02) plus the `bmgr` test procedure (TC-SEC-013, 050) | F-01.4 | M | ARC-03 file names | SEC-REQ-17, 40 |
| ENG-T-21 | Prod Firebase hygiene: API-key restrictions, providers, SHA clean-up, budget and usage alerts, App Check monitoring | F-01.5 | S | ED-01 | SEC-REQ-03, 37–39 |
| ENG-T-22 | Rules deployment procedure (manual runbook or `deploy-rules.yml`); ruleset id recorded per release | F-01.3 | S | ED-06 | SEC-REQ-12 |
| ENG-T-23 | Operational readiness: hosted privacy policy, account-deletion page and runbook, support contact | F-01.5 | M | ED-09; PO/legal content | SEC-REQ-25 |
| ENG-T-24 | Room schema export and migration tests in CI | F-01.2 | S | ADR-002 (D-11) | §8.4 |
| ENG-T-25 | Play Console onboarding: app creation, Play App Signing enrolment (upload K_app), internal track, data-safety form, Play Integrity linking | F-01.3 | M | D-03; Play account (DEP) | ADR-004, ADR-008 |
| ENG-T-27 | Legacy rules transition L1/L2 deployment and the operator clean-up script with its runbook (SEC-01 §5.6) | F-01.4 | M | D-19; ENG-T-09 | SEC-REQ-13 |
| ENG-T-28 | Device-test handoff: RC distribution procedure, `adb` scripts, test accounts in the dev project | F-01.5 | S | TST-01 | §10 |

---

## 13. Open decisions

### 13.1 Existing master decisions (RD-99 §8) affecting this plan

| Decision | Impact on ENG-01 |
|---|---|
| D-03 distribution / updater | Channels (§8.1); retirement of the publish script (ENG-T-12); Play onboarding (ENG-T-25) |
| D-11 Room | Schema export and migration tests (ENG-T-24) |
| D-12 WorkManager | Manifest permissions allow-list; device tests |
| D-13 crash reporting | §9.1 |
| D-14 minSdk and speech tiers | Device matrix (§10) |
| D-15 applicationId / namespace | Firebase registration, CI package check, signing continuity (§2.3, §5) |
| D-19 legacy migration scope | L1/L2 rules timing (ENG-T-27) |

### 13.2 New engineering decisions (consolidated into RD-99 §8 as D-27…D-31; ED-02, ED-03, ED-05, ED-07, ED-08 and ED-11 are engineering-level choices approved with ENG-01 at G3)

| ID | Decision | Options | Recommendation |
|---|---|---|---|
| ED-01 (→ D-27) | Firebase project topology | Single project / **dev + prod** / dev + staging + prod | **dev + prod.** Prod = the existing project and named database |
| ED-02 | `staging` build type | Add (release-like, dev project) / no | Add when QA starts RC testing in R1. Not needed in R0 |
| ED-03 | Dev `google-services.json` | Commit the dev JSON / uncommitted / **uncommitted + build works without it** | **Uncommitted + build works without it** |
| ED-04 (→ D-28) | App-signing key strategy | **Project-owned K_app uploaded to Play App Signing** / Play-generated key (one reinstall at the transition) | Project-owned K_app, unless D-15 changes the applicationId, in which case either is acceptable |
| ED-05 | Play upload | **Manual upload** / Gradle Play Publisher (NEW DEPENDENCY) / third-party action (NEW) | **Manual** until release frequency justifies automation |
| ED-06 (→ D-29) | Rules deployment | **Manual by the Release Manager with recorded evidence** / CI with Workload Identity Federation (new action) / CI with a scoped service-account key | **Manual** in R0–R1; re-evaluate WIF for R2 |
| ED-07 | Versioning single source of truth | **`version.properties` + formula** / tag-derived only | **`version.properties` + formula**, with a tag-equality assertion |
| ED-08 | Review model under branch protection | 1 required approval / self-merge with required checks (solo) | Depends on team size (ASSUMPTION). Required checks are mandatory in both cases |
| ED-09 (→ D-30) | Hosting for the privacy policy and deletion page | GitHub Pages / other static host / Google Sites | GitHub Pages (no app change; version-controlled), subject to repository visibility |
| ED-10 (→ D-31) | Firestore PITR or scheduled backups in prod | Enable (cost; disclose the retention window) / disable | Product Owner decision. If enabled, disclose the 7-day window (SEC-01 §9.3) |
| ED-11 | Optional supply-chain extras (dependency review, provenance attestations, secret push protection) | Enable where the plan allows / skip | Enable those available on the repository's plan; record which are unavailable |

---

## 14. Change log

| Version | Date | Change | Author (role) |
|---|---|---|---|
| 0.1 | 2026-11-20 | Initial draft: current-state walk-through, build variants, local setup, CI/CD design, signing, secrets inventory, versioning, channels, checklist, rollback and hotfix, operations, environments handoff, supply chain, ENG-T work items, open decisions | DevOps / Release Engineer |
| 0.2 | 2026-11-21 | Consistency review: CL-25 (§8.5 defect severity names aligned to TST-01 §11, which is authoritative), CL-21 (§4.1 optional nightly instrumented-test workflow, R1) | Technical Writer (consistency review) |
