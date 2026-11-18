# SpeakCraft — Automatic Git-Push OTA Update Pipeline

## Why `v1.0.0` Showed "No Newer Published Releases"
An already-installed **`v1.0.0`** APK checks updates by querying the Cloud Firestore collection `applicationReleases` (`enabled == true`) in:
- **Firebase Project:** `gen-lang-client-0651259232`
- **Firestore Database:** `ai-studio-android-speakcra-e1a16d03-38bd-4af0-9917-ae6b64cadc11`

Because AI Studio builds compile locally and do not automatically upload signed APKs to an HTTPS host or insert documents into `applicationReleases`, `v1.0.0` saw zero remote releases until a release is published to Firestore.

---

## How Automatic Updates Work When You Push to Git

The included workflow (`.github/workflows/release_on_push.yml`) and publisher script (`scripts/publish_firestore_release.mjs`) automate the entire release pipeline on every `git push`:

1. **Push to `main`, `master`, or `beta`** -> builds & publishes to the **`beta`** channel (`1.1.0-beta.<run_number>`, `versionCode = 1 + run_number`).
2. **Push a Git tag `vX.Y.Z`** -> builds & publishes to the **`stable`** channel (`X.Y.Z`).
3. **Keystore Alignment:**
   - If you configure GitHub Secrets `ANDROID_KEYSTORE_BASE64`, `STORE_PASSWORD`, and `KEY_PASSWORD`, the workflow signs a release APK (`assembleRelease`).
   - Otherwise, it decodes `debug.keystore.base64` from the repository and signs with `debugConfig` (`assembleDebug`), matching the certificate already installed by AI Studio APKs so Android permits in-place updates and `ApkIntegrityVerifier` passes signature verification.
4. **HTTPS Hosting + Checksum:**
   - Uploads `speakcraft_<versionName>.apk` and `.sha256` to **GitHub Releases** over `https://`.
5. **Firestore Publication (Delivers Updates to Existing `v1.0.0` APKs):**
   - Runs `scripts/publish_firestore_release.mjs` via `firebase-admin` to write the validated release document into `applicationReleases`.
   - When a user running **`v1.0.0`** selects **Beta** in **App Updates & Backups -> Release Channel & Check for Updates** and taps **Check for Updates Now**, `v1.0.0` detects the new beta release, downloads the HTTPS APK from GitHub Releases, verifies SHA-256 + signing certificate, snapshots preferences, and launches Android's installer.
6. **Direct GitHub Releases Check (`v1.1.0-beta.1`+):**
   - Builds produced by CI automatically embed `BuildConfig.GITHUB_RELEASE_REPO`, and users can also enter any `owner/repo` directly in **App Updates & Backups** so newer builds check **both** Firestore `applicationReleases` and GitHub Releases directly.

---

## One-Time Setup Checklist on GitHub
1. Push this repository to GitHub.
2. In your GitHub repository go to **Settings -> Secrets and variables -> Actions** and add:
   - **`FIREBASE_SERVICE_ACCOUNT_JSON`**: JSON key for a Google Cloud / Firebase service account on project `gen-lang-client-0651259232` with Firestore write permission (`Cloud Datastore User` / `Firebase Admin`). *(Required so already-installed `v1.0.0` APKs discover new pushes automatically.)*
   - *(Optional for release-key signing)* `ANDROID_KEYSTORE_BASE64`, `STORE_PASSWORD`, `KEY_PASSWORD`. Only switch to a custom upload keystore if the previously installed APK was also signed with that same keystore; otherwise leave unset to keep matching `debug.keystore.base64`.
3. Push any commit to `main` (or trigger **Actions -> Build & Publish SpeakCraft OTA Release -> Run workflow**).
4. On your phone with `v1.0.0` installed: open **App Updates & Backups**, tap **Beta**, and tap **Check for Updates Now**.
