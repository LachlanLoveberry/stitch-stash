# Google Cloud setup for Drive backup

One-time setup so Stitch Stash can write JSON backups to a shared Google Drive folder. Skip entirely if you're not using Phase 6 — the rest of the app works without it.

**Already done via gcloud CLI** (state captured here for reference / for re-doing in a new GCP account):

```bash
gcloud auth login
gcloud projects create stitch-stash-app --name="Stitch Stash"
gcloud config set project stitch-stash-app
gcloud services enable drive.googleapis.com
```

Current state:
- Project ID: `stitch-stash-app`
- Owner account: `lachlanloveberry@gmail.com`
- APIs enabled: Google Drive API
- Direct console link: https://console.cloud.google.com/home/dashboard?project=stitch-stash-app

---

## What still needs the Cloud Console UI (~5 min)

Google deliberately doesn't expose Android OAuth client creation or consent-screen config via `gcloud`. Three quick clicks:

### Step 1 — OAuth consent screen

Open: <https://console.cloud.google.com/apis/credentials/consent?project=stitch-stash-app>

| Field | Value |
|---|---|
| User Type | **External** |
| App name | `Stitch Stash` |
| User support email | `lachlanloveberry@gmail.com` |
| Developer contact email | `lachlanloveberry@gmail.com` |
| Authorized domains | *(leave blank)* |

Save and continue → **Scopes** page → "Add or Remove Scopes" → search `drive.file` → tick `.../auth/drive.file` → Update → Save and continue.

**Test users** page → Add:
- `lachlanloveberry@gmail.com`
- *(your wife's Google email)*

Save → publishing status stays as **Testing** (no Google verification required while you have ≤100 testers, and we'll never approach that).

### Step 2 — Generate the Android signing-key SHA-1

This has to run on the **dev machine** where you'll build the APK, because debug keystores are per-machine.

**Debug builds**:
```bash
keytool -keystore ~/.android/debug.keystore -storepass android \
  -alias androiddebugkey -list | grep SHA1
```

**Release builds** (once you've created a release keystore — see [Release signing](#release-signing) below):
```bash
keytool -keystore release.keystore -alias stitch -list | grep SHA1
```

You can register both SHAs on the same OAuth client, so debug + release builds both work.

### Step 3 — Create the Android OAuth Client ID

Open: <https://console.cloud.google.com/apis/credentials?project=stitch-stash-app>

Create Credentials → **OAuth client ID**:

| Field | Value |
|---|---|
| Application type | **Android** |
| Name | `Stitch Stash Android` |
| Package name | `com.lachlan.stitchstash` |
| SHA-1 certificate fingerprint | *(paste from Step 2)* |

Create → done. No client ID needs to be embedded in the app — the legacy `GoogleSignIn` API matches `package + SHA-1` at runtime.

---

## Inside the app

1. Build + install the app.
2. Settings → **Sign in with Google** → pick the account that's in the test users list.
3. **Set folder** → paste the Drive folder ID. To get the ID:
   - Open the shared "Stitch Stash Backups" folder in Drive on web
   - The URL is `https://drive.google.com/drive/folders/<FOLDER_ID>` — copy the part after `/folders/`.
4. **Back up now** → check Drive on another account → JSON file should appear within a minute.

## Backup folder ownership

Recommended: **you** create a dedicated folder named `Stitch Stash Backups` in your own Drive, share it with edit access to your wife's account. The wife signs in inside the app with her account; her uploads land in the shared folder, owned by her but visible to you. Cleaner than dropping JSON into the family share root.

## Release signing

Once you're ready to publish APKs to GitHub Releases:

```bash
# On the dev machine — keep this file safe forever:
keytool -genkey -v -keystore release.keystore \
  -keyalg RSA -keysize 2048 -validity 25000 -alias stitch
keytool -keystore release.keystore -alias stitch -list | grep SHA1
```

Add the release SHA-1 as a **second** SHA on the OAuth client (same one as Step 3 — don't make a new client).

For the GitHub Actions release workflow (`.github/workflows/release.yml`), add these repo secrets:

| Secret | Value |
|---|---|
| `KEYSTORE_BASE64` | `base64 -i release.keystore \| pbcopy` then paste |
| `KEYSTORE_PASSWORD` | the password you set during keystore creation |
| `KEY_ALIAS` | `stitch` |
| `KEY_PASSWORD` | the key password (often same as keystore password) |

Push a tag like `v0.1.0` → workflow builds, signs, and publishes the APK to GitHub Releases automatically.

---

## Troubleshooting

**"Error 10" or "API_UNAVAILABLE" on Google sign-in**
SHA-1 mismatch — your installed APK was signed with a key whose SHA-1 isn't registered on the OAuth client. Re-run Step 2 with the right keystore, add the SHA-1, wait 5 min for propagation.

**"This app isn't verified" warning**
Expected while in Testing mode. Tap "Advanced" → "Go to Stitch Stash (unsafe)". Only test users will see this prompt; non-test users would be blocked.

**Backups never appear in Drive**
Check Settings → "Last backup at" — if it never updates, the WorkManager job is failing. Usually:
- Drive folder ID is wrong (the share link gives you a different ID; need the *folder* page URL)
- Wife's account isn't a test user on the consent screen
- The account she's signed into the app with isn't the same one with access to the folder

**OAuth client ID disappeared**
GCP occasionally requires re-confirmation of consent screen settings if left untouched for ~6 months. Just re-save the consent screen page and the client comes back.
