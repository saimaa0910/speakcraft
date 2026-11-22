# Contributing to SpeakCraft

This guide covers local development setup, secret-free builds and tests, Firestore rules verification, and release hygiene for SpeakCraft (Android / Kotlin / Jetpack Compose).

---

## 1. Prerequisites

| Tool | Required Version | Notes |
|---|---|---|
| **JDK** | **21** (Temurin 21) | Used to run Gradle (matches CI). Java bytecode target is Java 11. |
| **Android SDK** | Platform **36**, Build-Tools **36.0.0** | `compileSdk = 36`, `targetSdk = 36`, `minSdk = 26` (`D-14`). |
| **Gradle** | **9.3.1** (committed wrapper) | Official wrapper (`gradlew`, `gradlew.bat`, `gradle-wrapper.jar`) with pinned `distributionSha256Sum` in `gradle/wrapper/gradle-wrapper.properties`. |
| **Android Studio** | Latest stable supporting **AGP 9.1.1** | Kotlin 2.2.10, Compose BOM `2024.09.00`. |
| **Node.js** | **20 LTS** *(optional)* | Required only when running Firestore security rules tests locally. |

> **Important:** Never create or commit a `local.properties` file containing API keys or secrets. Environment variables and secrets are injected via `.env` (gitignored, modeled after `.env.example`) or CI environment secrets.

---

## 2. Quick Start (Secret-Free Local Build & Tests)

SpeakCraft supports building and running the complete unit, Robolectric, and Android Lint suites on a clean clone **without** production Firebase credentials (`ENG-01 §3.2 Option C`):

- `gradle.properties` sets `googleServices.missing.passthrough=true`.
- `AuthHelper.kt` resolves `default_web_client_id` at runtime via `resources.getIdentifier(...)` instead of a compile-time `R.string.default_web_client_id` reference.
- Robolectric tests use `TestFirebaseInit.kt` (`ensureTestFirebaseApp`) to fall back to synthetic `FirebaseOptions` when `app/google-services.json` is absent.

### Step 1: Prepare a Debug Keystore (Clean Clone Only)

`debug.keystore` is gitignored. Before assembling a debug APK on a fresh clone, restore the shared dev debug keystore from `debug.keystore.base64` (or generate a local throwaway keystore):

```bash
if [ ! -f debug.keystore ]; then
  if [ -f debug.keystore.base64 ]; then
    base64 -d debug.keystore.base64 > debug.keystore
  else
    keytool -genkeypair -v \
      -keystore debug.keystore \
      -storepass android \
      -alias androiddebugkey \
      -keypass android \
      -keyalg RSA \
      -keysize 2048 \
      -validity 10000 \
      -dname "CN=Android Debug,O=Android,C=US"
  fi
fi
```

### Step 2: Build, Test, and Lint

Run the exact quality-gate command executed by `.github/workflows/ci.yml` using the committed Gradle Wrapper:

```bash
./gradlew :app:assembleDebug :app:compileReleaseKotlin :app:testDebugUnitTest :app:lintDebug
```

*(Note: Cloud container environments that disallow `./gradlew` run `gradle :app:assembleDebug :app:compileReleaseKotlin :app:testDebugUnitTest :app:lintDebug` directly against Gradle 9.3.1.)*

---

## 3. Optional Firebase Dev Setup (Sign-In & Firestore Sync)

You only need Firebase configuration if you are actively testing Google Sign-In or live Firestore sync on a device/emulator:

1. Obtain `google-services.json` for the **dev** Firebase project (`D-27`: isolated from production) and place it at `app/google-services.json`. Do **not** commit production credentials.
2. Register your debug keystore's SHA-1 and SHA-256 fingerprints in the Firebase Console (required for Credential Manager Google Sign-In).
3. *(Optional)* If App Check is enabled on the dev project, register the App Check debug token logged to Logcat in the Firebase Console.

---

## 4. Running Firestore Security Rules Tests

With Node.js 20 LTS and the Firebase CLI installed:

```bash
npx firebase emulators:exec --only firestore --project demo-speakcraft "node --test firestore.test.js"
```

---

## 5. CI Quality Gates & Architecture Guardrails

All pull requests and pushes to `main` run `.github/workflows/ci.yml` with **zero repository secrets**:

1. **Validate Gradle Wrapper (`wrapper-validation`)**: Runs `gradle/actions/wrapper-validation@v4` to verify `gradle/wrapper/gradle-wrapper.jar` against official Gradle checksums (`T-01.1.1`).
2. **Build & Test (`build-test`)**: Runs `./gradlew :app:assembleDebug :app:compileReleaseKotlin :app:testDebugUnitTest :app:lintDebug` and uploads JUnit XML/HTML, Lint reports, and a 7-day reviewer debug APK (`T-01.2.1`).
3. **Static Security & Hygiene (`security-static`)**:
   - Verifies legacy `release_on_push.yml` stays deleted (`T-01.3.1`).
   - Verifies `.github/workflows/release.yml` contains zero debug-signing fallbacks (`DEBUG_KEYSTORE_BASE64`, `assembleDebug`, `debug.keystore`).
   - Verifies `.github/workflows/ci.yml` never uses `pull_request_target` or repository secrets.
   - Scans `app/src` for private key material.
   - Enforces **ADR-003 (`T-04.1.2`)**: `firebase-ai` cloud AI SDK must stay absent from `app/build.gradle.kts`, `gradle/libs.versions.toml`, and `app/src/main`.
   - Enforces **ADR-011 (`T-04.1.1`)**: YouTube Intelligence symbols and creator branding must stay absent from `app/src/main`.

### Release Pipeline Policy (`T-01.3.1`)

- Pushes to `main` **never** publish a release.
- Releases are built exclusively from annotated SemVer tags (`vX.Y.Z` or `vX.Y.Z-beta.N`) via `.github/workflows/release.yml`, signed with release signing credentials, verified non-debuggable with `apksigner`/`aapt2`, and published as draft GitHub Releases.

### Privacy Policy Guardrails (`T-04.2.1` / `ADR-003`)

- **On-Device Intelligence Only:** SpeakCraft contains no Cloud AI SDK (`firebase-ai`). Audio DSP, speech recognition, and evaluation run strictly on-device.
- **No Transcript or Media-Path Uploads:** Audio recordings, video files, local device file paths, and speech transcripts stay on the device and are excluded from Firestore speaking-session, speech-feedback, and conversation-session cloud payloads.
