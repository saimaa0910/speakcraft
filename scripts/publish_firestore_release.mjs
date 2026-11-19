import fs from 'node:fs';
import path from 'node:path';
import { pathToFileURL } from 'node:url';

export function readProjectDefaults(rootDir = process.cwd()) {
  const googleServicesPath = path.resolve(rootDir, 'app/google-services.json');
  const firebaseXmlPath = path.resolve(rootDir, 'app/src/main/res/values/firebase_applet_config.xml');

  let googleServicesProjectId = '';
  if (fs.existsSync(googleServicesPath)) {
    const gs = JSON.parse(fs.readFileSync(googleServicesPath, 'utf8'));
    googleServicesProjectId = String(gs?.project_info?.project_id || '').trim();
  }
  const projectId = (process.env.FIREBASE_PROJECT_ID || googleServicesProjectId || '').trim();

  let databaseId = (process.env.FIRESTORE_DATABASE_ID || '').trim();
  if (!databaseId && fs.existsSync(firebaseXmlPath)) {
    const xml = fs.readFileSync(firebaseXmlPath, 'utf8');
    const match = xml.match(/name="firestore_database_id"[^>]*>([^<]+)</);
    if (match) databaseId = match[1].trim();
  }

  return {
    projectId,
    googleServicesProjectId,
    databaseId: databaseId || '(default)'
  };
}

function requiredEnv(env, name) {
  const value = String(env[name] || '').trim();
  if (!value) {
    throw new Error(`Missing required environment variable: ${name}`);
  }
  return value;
}

export function parseAndValidateServiceAccount(rawCreds, expectedProjectId) {
  const trimmed = String(rawCreds || '').trim();
  let jsonText = trimmed;
  if (trimmed && !trimmed.startsWith('{') && /^[A-Za-z0-9+/=\s]+$/.test(trimmed)) {
    const decoded = Buffer.from(trimmed.replace(/\s+/g, ''), 'base64').toString('utf8').trim();
    if (decoded.startsWith('{')) {
      jsonText = decoded;
    }
  }
  let serviceAccount;
  try {
    serviceAccount = JSON.parse(jsonText);
  } catch {
    throw new Error('FIREBASE_SERVICE_ACCOUNT_JSON is not valid JSON.');
  }

  if (!serviceAccount || typeof serviceAccount !== 'object') {
    throw new Error('FIREBASE_SERVICE_ACCOUNT_JSON must be a JSON object.');
  }

  for (const field of ['project_id', 'client_email', 'private_key']) {
    if (typeof serviceAccount[field] !== 'string' || !serviceAccount[field].trim()) {
      throw new Error(`FIREBASE_SERVICE_ACCOUNT_JSON is missing required field: ${field}`);
    }
  }

  if (expectedProjectId && serviceAccount.project_id.trim() !== expectedProjectId) {
    throw new Error(
      `Service account project_id (${serviceAccount.project_id.trim()}) does not match expected Firebase project_id (${expectedProjectId}).`
    );
  }

  return serviceAccount;
}

export function buildReleaseDocument(env, now = new Date()) {
  const versionName = requiredEnv(env, 'RELEASE_VERSION_NAME');
  const versionCode = Number.parseInt(requiredEnv(env, 'RELEASE_VERSION_CODE'), 10);
  const rawChannel = requiredEnv(env, 'RELEASE_CHANNEL').toLowerCase();
  if (rawChannel !== 'stable' && rawChannel !== 'beta') {
    throw new Error(`Invalid RELEASE_CHANNEL: ${rawChannel}. Expected "stable" or "beta".`);
  }
  const channel = rawChannel;

  const downloadUrl = requiredEnv(env, 'RELEASE_DOWNLOAD_URL');
  const installerFileName = String(env.RELEASE_INSTALLER_FILENAME || `speakcraft_${versionName}.apk`).trim();
  const fileSizeBytes = Number.parseInt(String(env.RELEASE_FILE_SIZE_BYTES || '0'), 10);
  const sha256Checksum = requiredEnv(env, 'RELEASE_SHA256').toLowerCase();
  const signingCertSha256 = String(env.RELEASE_SIGNING_CERT_SHA256 || '')
    .trim()
    .replace(/:/g, '')
    .toLowerCase();
  const minSupportedVersion = String(env.RELEASE_MIN_SUPPORTED_VERSION || '1.0.0').trim();
  const mandatory = String(env.RELEASE_MANDATORY || 'false').trim().toLowerCase() === 'true';
  const releaseNotes = String(env.RELEASE_NOTES || `SpeakCraft v${versionName} (${channel})`)
    .trim()
    .slice(0, 4000) || `SpeakCraft v${versionName} (${channel})`;
  const releaseDate = now.toISOString().slice(0, 10);

  if (!/^[0-9]+\.[0-9]+\.[0-9]+(-[a-zA-Z0-9.]+)?$/.test(versionName)) {
    throw new Error(`Invalid SemVer versionName: ${versionName}`);
  }
  if (!Number.isInteger(versionCode) || versionCode < 1) {
    throw new Error(`Invalid versionCode: ${versionCode}`);
  }
  if (!downloadUrl.startsWith('https://') || downloadUrl.length > 600) {
    throw new Error(`downloadUrl must start with https:// and be <= 600 chars: ${downloadUrl}`);
  }
  if (!installerFileName || installerFileName.length > 200) {
    throw new Error(`Invalid installerFileName: ${installerFileName}`);
  }
  if (!Number.isInteger(fileSizeBytes) || fileSizeBytes <= 0) {
    throw new Error(`Invalid RELEASE_FILE_SIZE_BYTES (must be > 0): ${env.RELEASE_FILE_SIZE_BYTES}`);
  }
  if (!/^[a-f0-9]{64}$/.test(sha256Checksum)) {
    throw new Error(`Invalid 64-char sha256Checksum: ${sha256Checksum}`);
  }
  if (signingCertSha256 && !/^[a-f0-9]{64}$/.test(signingCertSha256)) {
    throw new Error(`Invalid 64-char signingCertSha256: ${signingCertSha256}`);
  }
  if (!/^[0-9]+\.[0-9]+\.[0-9]+$/.test(minSupportedVersion)) {
    throw new Error(`minSupportedVersion must match X.Y.Z: ${minSupportedVersion}`);
  }

  const safeDocId = `rel_${versionName.replace(/[^a-zA-Z0-9_-]/g, '_')}_${versionCode}`.slice(0, 120);

  const payload = {
    versionName,
    versionCode,
    channel,
    releaseDate,
    releaseNotes,
    features: [
      'Premium native Jetpack Compose experience & Editorial Studio UI',
      'Automated Git-push release build & SHA-256 verification',
      'Beta & Stable channel OTA update support'
    ],
    bugFixes: [],
    securityImprovements: [
      '64-char SHA-256 APK digest and signing certificate verification'
    ],
    downloadUrl,
    installerFileName,
    fileSizeBytes,
    sha256Checksum,
    signingCertSha256,
    minSupportedVersion,
    minSdk: 24,
    mandatory,
    enabled: true,
    supportedOs: ['Android 7.0+']
  };

  return { safeDocId, payload };
}

export async function main() {
  const rawCreds = requiredEnv(process.env, 'FIREBASE_SERVICE_ACCOUNT_JSON');
  const { projectId, googleServicesProjectId, databaseId } = readProjectDefaults();
  if (!projectId) {
    throw new Error('Could not resolve Firebase project_id from env or app/google-services.json.');
  }

  const serviceAccount = parseAndValidateServiceAccount(
    rawCreds,
    googleServicesProjectId || projectId
  );

  const { safeDocId, payload } = buildReleaseDocument(process.env);

  const { initializeApp, cert } = await import('firebase-admin/app');
  const { getFirestore, FieldValue } = await import('firebase-admin/firestore');

  const app = initializeApp({
    credential: cert(serviceAccount),
    projectId
  });
  const db = getFirestore(app, databaseId);

  const docRef = db.collection('applicationReleases').doc(safeDocId);
  const existing = await docRef.get();

  const writePayload = {
    ...payload,
    updatedAt: FieldValue.serverTimestamp()
  };

  if (!existing.exists) {
    writePayload.createdAt = FieldValue.serverTimestamp();
    writePayload.downloadCount = 0;
    await docRef.set(writePayload);
  } else {
    await docRef.update(writePayload);
  }

  console.log(
    `Published ${safeDocId} (${payload.versionName}, build ${payload.versionCode}, channel=${payload.channel}) to ${projectId}/${databaseId}`
  );
}

const isCliEntry = process.argv[1] && import.meta.url === pathToFileURL(path.resolve(process.argv[1])).href;
if (isCliEntry) {
  main().catch((err) => {
    console.error(err?.message || 'Failed to publish release metadata to Firestore.');
    process.exit(1);
  });
}
