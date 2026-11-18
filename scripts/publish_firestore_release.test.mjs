import test from 'node:test';
import assert from 'node:assert/strict';
import {
  readProjectDefaults,
  parseAndValidateServiceAccount,
  buildReleaseDocument
} from './publish_firestore_release.mjs';

test('readProjectDefaults resolves project_id and named database_id from app config', () => {
  const defaults = readProjectDefaults(process.cwd());
  assert.equal(defaults.projectId, 'gen-lang-client-0651259232');
  assert.equal(defaults.googleServicesProjectId, 'gen-lang-client-0651259232');
  assert.equal(
    defaults.databaseId,
    'ai-studio-android-speakcra-e1a16d03-38bd-4af0-9917-ae6b64cadc11'
  );
});

test('parseAndValidateServiceAccount validates fields and project match without exposing secret', () => {
  assert.throws(
    () => parseAndValidateServiceAccount('{not-valid-json', 'gen-lang-client-0651259232'),
    /not valid JSON/
  );
  assert.throws(
    () =>
      parseAndValidateServiceAccount(
        JSON.stringify({
          project_id: 'wrong-project',
          client_email: 'sa@wrong-project.iam.gserviceaccount.com',
          private_key: 'fake-key-material'
        }),
        'gen-lang-client-0651259232'
      ),
    /does not match expected Firebase project_id/
  );

  const parsed = parseAndValidateServiceAccount(
    JSON.stringify({
      project_id: 'gen-lang-client-0651259232',
      client_email: 'sa@gen-lang-client-0651259232.iam.gserviceaccount.com',
      private_key: 'fake-key-material'
    }),
    'gen-lang-client-0651259232'
  );
  assert.equal(parsed.project_id, 'gen-lang-client-0651259232');
});

test('buildReleaseDocument builds firestore.rules and ApplicationRelease compatible payload', () => {
  const { safeDocId, payload } = buildReleaseDocument(
    {
      RELEASE_VERSION_NAME: '1.1.0-beta.2',
      RELEASE_VERSION_CODE: '3',
      RELEASE_CHANNEL: 'beta',
      RELEASE_DOWNLOAD_URL:
        'https://github.com/octocat/speakcraft/releases/download/v1.1.0-beta.2-build.3/speakcraft_1.1.0-beta.2.apk',
      RELEASE_INSTALLER_FILENAME: 'speakcraft_1.1.0-beta.2.apk',
      RELEASE_FILE_SIZE_BYTES: '24567890',
      RELEASE_SHA256: 'A'.repeat(64),
      RELEASE_SIGNING_CERT_SHA256: 'BB:'.repeat(31) + 'BB',
      RELEASE_MIN_SUPPORTED_VERSION: '1.0.0',
      RELEASE_MANDATORY: 'false',
      RELEASE_NOTES: 'SpeakCraft v1.1.0-beta.2 (beta) — Upgraded UI/UX'
    },
    new Date('2026-11-18T10:00:00Z')
  );

  assert.equal(safeDocId, 'rel_1_1_0-beta_2_3');
  assert.match(safeDocId, /^[a-zA-Z0-9_-]+$/);
  assert.equal(payload.versionName, '1.1.0-beta.2');
  assert.equal(payload.versionCode, 3);
  assert.equal(payload.channel, 'beta');
  assert.equal(payload.releaseDate, '2026-11-18');
  assert.equal(payload.fileSizeBytes, 24567890);
  assert.equal(payload.sha256Checksum, 'a'.repeat(64));
  assert.equal(payload.signingCertSha256, 'b'.repeat(64));
  assert.equal(payload.minSupportedVersion, '1.0.0');
  assert.equal(payload.minSdk, 24);
  assert.equal(payload.mandatory, false);
  assert.equal(payload.enabled, true);

  assert.throws(
    () =>
      buildReleaseDocument({
        RELEASE_VERSION_NAME: '1.1.0',
        RELEASE_VERSION_CODE: '3',
        RELEASE_CHANNEL: 'nightly',
        RELEASE_DOWNLOAD_URL: 'https://github.com/octocat/speakcraft/releases/download/v1.1.0/app.apk',
        RELEASE_FILE_SIZE_BYTES: '1024',
        RELEASE_SHA256: 'a'.repeat(64)
      }),
    /Invalid RELEASE_CHANNEL/
  );
});
