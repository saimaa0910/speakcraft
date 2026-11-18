import fs from 'node:fs';
import path from 'node:path';
import { initializeApp, cert } from 'firebase-admin/app';
import { getFirestore, FieldValue } from 'firebase-admin/firestore';

function readProjectDefaults() {
  const googleServicesPath = path.resolve('app/google-services.json');
  const firebaseXmlPath = path.resolve('app/src/main/res/values/firebase_applet_config.xml');

  let projectId = process.env.FIREBASE_PROJECT_ID || '';
  if (!projectId && fs.existsSync(googleServicesPath)) {
    const gs = JSON.parse(fs.readFileSync(googleServicesPath, 'utf8'));
    projectId = gs?.project_info?.project_id || '';
  }

  let databaseId = process.env.FIRESTORE_DATABASE_ID || '';
  if (!databaseId && fs.existsSync(firebaseXmlPath)) {
    const xml = fs.readFileSync(firebaseXmlPath, 'utf8');
    const match = xml.match(/name="firestore_database_id"[^>]*>([^<]+)</);
    if (match) databaseId = match[1].trim();
  }

  return { projectId, databaseId: databaseId || '(default)' };
}

function requiredEnv(name) {
  const value = (process.env[name] || '').trim();
  if (!value) {
    throw new Error(`Missing required environment variable: ${name}`);
  }
  return value;
}

async function main() {
  const rawCreds = requiredEnv('FIREBASE_SERVICE_ACCOUNT_JSON');
  const serviceAccount = JSON.parse(rawCreds);
  const { projectId, databaseId } = readProjectDefaults();
  if (!projectId) {
    throw new Error('Could not resolve Firebase project_id from env or app/google-services.json.');
  }

  const versionName = requiredEnv('RELEASE_VERSION_NAME');
  const versionCode = Number.parseInt(requiredEnv('RELEASE_VERSION_CODE'), 10);
  const channel = process.env.RELEASE_CHANNEL === 'stable' ? 'stable' : 'beta';
  const downloadUrl = requiredEnv('RELEASE_DOWNLOAD_URL');
  const installerFileName = (process.env.RELEASE_INSTALLER_FILENAME || `speakcraft_${versionName}.apk`).trim();
  const fileSizeBytes = Number.parseInt(process.env.RELEASE_FILE_SIZE_BYTES || '0', 10);
  const sha256Checksum = requiredEnv('RELEASE_SHA256').toLowerCase();
  const signingCertSha256 = (process.env.RELEASE_SIGNING_CERT_SHA256 || '').trim().toLowerCase();
  const minSupportedVersion = (process.env.RELEASE_MIN_SUPPORTED_VERSION || '1.0.0').trim();
  const mandatory = String(process.env.RELEASE_MANDATORY || 'false').toLowerCase() === 'true';
  const releaseNotes = (process.env.RELEASE_NOTES || `SpeakCraft v${versionName} (${channel})`).slice(0, 4000);
  const releaseDate = new Date().toISOString().slice(0, 10);

  if (!/^[0-9]+\.[0-9]+\.[0-9]+(-[a-zA-Z0-9.]+)?$/.test(versionName)) {
    throw new Error(`Invalid SemVer versionName: ${versionName}`);
  }
  if (!Number.isInteger(versionCode) || versionCode < 1) {
    throw new Error(`Invalid versionCode: ${versionCode}`);
  }
  if (!downloadUrl.startsWith('https://')) {
    throw new Error(`downloadUrl must start with https://: ${downloadUrl}`);
  }
  if (!/^[a-f0-9]{64}$/.test(sha256Checksum)) {
    throw new Error(`Invalid 64-char sha256Checksum: ${sha256Checksum}`);
  }
  if (!/^[0-9]+\.[0-9]+\.[0-9]+$/.test(minSupportedVersion)) {
    throw new Error(`minSupportedVersion must match X.Y.Z: ${minSupportedVersion}`);
  }

  const app = initializeApp({
    credential: cert(serviceAccount),
    projectId
  });
  const db = getFirestore(app, databaseId);

  const safeDocId = `rel_${versionName.replace(/[^a-zA-Z0-9_-]/g, '_')}_${versionCode}`.slice(0, 120);
  const docRef = db.collection('applicationReleases').doc(safeDocId);
  const existing = await docRef.get();

  const payload = {
    versionName,
    versionCode,
    channel,
    releaseDate,
    releaseNotes,
    features: [
      'Premium native Jetpack Compose launch animation',
      'Automated Git-push release build & SHA-256 verification',
      'Beta & Stable channel OTA update support'
    ],
    bugFixes: [],
    securityImprovements: [
      '64-char SHA-256 APK digest and signing certificate verification'
    ],
    downloadUrl,
    installerFileName,
    fileSizeBytes: Number.isFinite(fileSizeBytes) && fileSizeBytes >= 0 ? fileSizeBytes : 0,
    sha256Checksum,
    signingCertSha256,
    minSupportedVersion,
    minSdk: 24,
    mandatory,
    enabled: true,
    supportedOs: ['Android 7.0+'],
    updatedAt: FieldValue.serverTimestamp()
  };

  if (!existing.exists) {
    payload.createdAt = FieldValue.serverTimestamp();
    payload.downloadCount = 0;
    await docRef.set(payload);
  } else {
    await docRef.update(payload);
  }

  console.log(`Published ${safeDocId} (${versionName}, build ${versionCode}, channel=${channel}) to ${projectId}/${databaseId}`);
}

main().catch((err) => {
  console.error(err);
  process.exit(1);
});
