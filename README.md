# Stitch Stash

A calm, reward-focused crochet progress companion for Android. Local-first, no nagging, no streaks — celebrates effort and projects reality honestly.

- **Package**: `com.lachlan.stitchstash`
- **Min SDK**: 26 (Android 8.0)
- **Stack**: Kotlin, Jetpack Compose, Material 3, Room, WorkManager, Coil, Jsoup, OkHttp, Google Drive REST API

## What's built (all phases)

### Phase 1 — Local MVP
- 3-screen soft onboarding (market date / weekly hours / target pieces), each "Skip for now"
- Home: progress ring, recent wins, calm tracking sentence, market countdown
- Pattern library + manual add (name + photo + multiple colourways)
- "I finished one" with backlog dates, photo, energy tag, notes
- Live forecast projection at the active market

### Phase 2 — Ribblr import
- Paste-URL dialog in the Add Pattern flow
- Jsoup + OkHttp scrape pulls cover image, title, designer (Open Graph + Twitter card meta)
- Cover image copied to internal storage so it survives Ribblr CDN changes
- Manual fallback on failure — never blocks adding a pattern

### Phase 3 — Rewards & polish
- Sticker catalog (emoji-based, single-file swap to custom art later)
- Earning rules: first piece, first of pattern, pattern complete, milestones (5/10/25), first Ribblr import, welcome back after 7+ days
- Celebration dialog with confetti animation when logging a piece
- Programmatic finish card renderer: 4 border styles (floral / scallop / granny squares / simple), photo crop, watermark
- Finish card gallery + Android share intent via FileProvider
- Sticker book grid (earned + locked, with "?" placeholders)

### Phase 4 — Forecast playground
- Three lockable rows: market date / target pieces / weekly hours
- Pick any two → third solves live (`ScenarioSolver`)
- Save multiple named scenarios; one is active and drives home framing
- Per-pattern estimate methods: rough bucket (quick/evening/project/big), similar-to another pattern, or direct hours

### Phase 5 — Multi-market
- Markets entity + management screen in Settings
- Add / rename / reschedule / skip without deletion
- Home auto-pivots to next non-skipped upcoming market

### Phase 6 — Google Drive backup
- Google sign-in via the classic `GoogleSignIn` API with `drive.file` scope (narrowest)
- Versioned JSON backup file (schema v1) covering all entities + settings
- `BackupWorker` (WorkManager): debounced 15-min uploads + manual "Back up now"
- 30-file retention with automatic pruning
- Restore from any listed backup
- Independent "Export to device" for an offline copy

### Phase 7 — Distribution
- `UpdateChecker` queries GitHub Releases for newer tag; banner in Home
- GH Actions workflow (`.github/workflows/release.yml`) builds + publishes signed APKs on `v*` tags
- Splash background theme using cream brand colour to bridge cold launch into Compose

## Dev-machine setup

### 1. Prerequisites
- **Android Studio** (Meerkat 2024.3+ or newer; latest stable preferred)
- **JDK 17** (bundled with recent Android Studio)
- **Android SDK API 36** + emulator (Tools → SDK Manager)
- **gcloud CLI** (only for Drive setup): `brew install --cask google-cloud-sdk`

### 2. Open the project
1. Move `~/stitch-stash` to the dev machine.
2. Copy `local.properties.template` → `local.properties`; set `sdk.dir` to your Android SDK path.
3. Open the repo in Android Studio. Accept the prompt to **regenerate the Gradle wrapper jar** (or run `gradle wrapper --gradle-version 8.9` if you have a global Gradle).
4. Wait for Gradle sync. First run downloads ~700 MB of dependencies.
5. Plug in her phone or pick an emulator, hit Run (▶).

### 3. Google Cloud setup (Phase 6 backup)

Skip if you want to defer the Drive integration; Phases 1–5 + 7 all work without it.

The GCP project (`stitch-stash-app`) and Drive API are already provisioned. See **[CLOUD_SETUP.md](./CLOUD_SETUP.md)** for the remaining Console-UI steps (~5 min) — OAuth consent screen, Android OAuth client, Drive folder.

### 4. Release signing (Phase 7)

Full instructions in [CLOUD_SETUP.md → Release signing](./CLOUD_SETUP.md#release-signing).

TL;DR: generate `release.keystore` once, register its SHA-1 as a second SHA on the Android OAuth client, add 4 secrets to the GitHub repo, push a `v*` tag → automatic signed-APK release.

### 5. Project layout

```
app/src/main/java/com/lachlan/stitchstash/
├── MainActivity.kt
├── StitchStashApp.kt              # Application class — owns DB + repository
├── data/
│   ├── db/                        # Room: entities, DAOs, database (v1)
│   ├── repository/                # StitchRepository — Flow facade
│   ├── ribblr/                    # RibblrScraper (Jsoup + OkHttp)
│   ├── drive/                     # DriveBackupService, GoogleSignInHelper
│   ├── backup/                    # BackupModel + BackupSerializer (JSON v1)
│   ├── storage/                   # ImageStorage (copy Uris to internal)
│   └── update/                    # UpdateChecker (GitHub Releases)
├── domain/
│   ├── forecast/                  # ForecastEngine + ScenarioSolver (pure math)
│   ├── stickers/                  # StickerCatalog + StickerEarner (pure rules)
│   └── model/                     # PatternWithProgress, CompletionWithContext
├── work/                          # BackupWorker (WorkManager)
└── ui/
    ├── theme/                     # Warm cream/rose/sage palette
    ├── components/                # ProgressRing, Confetti, UpdateBanner, SoftScaffold
    ├── navigation/                # NavHost with all routes
    ├── onboarding/                # 3-step soft setup
    ├── home/
    ├── patterns/                  # list + add + estimate
    ├── log/                       # completion + celebration dialog
    ├── stickers/                  # sticker book
    ├── finishcard/                # programmatic renderer + preview + gallery
    ├── plan/                      # lock-and-solve playground + scenarios
    ├── markets/                   # multi-market management
    └── settings/                  # everything else, incl. Drive backup
```

## Manual test plan

1. **First launch** → 3 soft screens; skip the middle two to verify defaults seed correctly.
2. **Patterns**: add 3 patterns. Try the Ribblr import button with a real pattern URL — cover should populate.
3. **Tap a pattern** → estimate screen. Try the rough bucket, then a similar-to choice, then direct hours. Each persists.
4. **Log a finish** → photo + energy tag + today's date → celebration dialog with confetti, sticker drop, "Make card" button.
5. **Finish card** → cycle through all 4 border styles. Save to gallery, then tap Share — Android system share sheet appears.
6. **Log another** with date set to last week — accepts without complaint, percentage updates.
7. **Plan** → tap any padlock to make that field the *result* and the other two locked. Outputs animate live as you change the slider/stepper.
8. **Save 2-3 scenarios** with different names. Tap one to activate; verify home screen tracking sentence reframes.
9. **Settings → Markets**: add a second market further out. Skip the first; Home now points at the second.
10. **Settings → Sign in with Google**: complete OAuth, paste folder ID, tap "Back up now". The JSON should appear in Drive within ~15 minutes (or immediately if you trigger the `_now` variant).
11. **Restart app**: all data persists; if signed in, "Available backups" populates from Drive on next visit to Settings.

## What still needs you (genuinely can't test alone)

- Her reaction to first-run tone
- Whether sticker frequency feels rewarding vs trivial (tune thresholds in `StickerEarner.kt`)
- Phone-specific things: actual Camera vs gallery preference (currently gallery-only via PickVisualMedia — easy to add camera in Phase 8 if she'd prefer)

## Update flow

Tag a release in git:
```bash
git tag v0.1.0
git push --tags
```

GH Actions builds + publishes the APK. The in-app `UpdateChecker` polls the GitHub API on Home launch; if a newer tag exists, an "Update available" banner offers a deep link to the Releases page.

Edit `OWNER` and `REPO` constants in `data/update/UpdateChecker.kt` if your GitHub repo isn't `lachlanloveberry/stitch-stash`.

## Attributions

<a href="https://www.flaticon.com/free-icons/crochet" title="crochet icons">Crochet icons created by Eucalyp - Flaticon</a>
