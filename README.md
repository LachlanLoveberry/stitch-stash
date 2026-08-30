# Stitch Stash

![Kotlin](https://img.shields.io/badge/Kotlin-100%25-7F52FF?logo=kotlin&logoColor=white)
![Jetpack Compose](https://img.shields.io/badge/UI-Jetpack%20Compose-4285F4?logo=jetpackcompose&logoColor=white)
![Min SDK](https://img.shields.io/badge/Min%20SDK-26-3DDC84?logo=android&logoColor=white)
![Room](https://img.shields.io/badge/Persistence-Room-orange)
![Build](https://github.com/LachlanLoveberry/stitch-stash/actions/workflows/test.yml/badge.svg)
![Release](https://github.com/LachlanLoveberry/stitch-stash/actions/workflows/release.yml/badge.svg)
![License](https://img.shields.io/badge/license-personal--project-lightgrey)

A local-first Android app that tracks crochet progress toward market deadlines — built for one specific person, not the App Store. No streaks, no guilt mechanics, no ads, no account wall. It projects reality honestly and celebrates effort instead of punishing gaps.

- **Package**: `com.lachlan.stitchstash`
- **Min SDK**: 26 (Android 8.0) · **Target/Compile SDK**: 36 · **JDK**: 17
- **Stack**: Kotlin, Jetpack Compose, Material 3, Room, WorkManager, Coil, Jsoup, OkHttp, Google Drive REST API

---

## Engineering notes

The Android-specific parts matter less here than the habits behind them — most of this carries over to any stack:

- **Domain logic has no framework imports.** [`ForecastEngine`/`ScenarioSolver`](app/src/main/java/com/lachlan/stitchstash/domain/forecast/) and [`StickerEarner`](app/src/main/java/com/lachlan/stitchstash/domain/stickers/StickerEarner.kt) are plain Kotlin — the deadline math and the reward rules run and test without touching Android at all.
- **Dependencies are chosen, not defaulted.** No Hilt/Koin — [`AppViewModelFactory`](app/src/main/java/com/lachlan/stitchstash/ui/AppViewModelFactory.kt) wires everything by hand, because a DI framework is overhead this app's size doesn't need. Documented as a tradeoff in [CLAUDE.md](CLAUDE.md), not left for someone to wonder about.
- **Imported data is copied, not linked.** [`ImageStorage`](app/src/main/java/com/lachlan/stitchstash/data/storage/ImageStorage.kt) copies external photos into app storage on import, so a scraped image doesn't break later when the source disappears.
- **Backups are versioned for migration, not just restore.** [`BackupSerializer`](app/src/main/java/com/lachlan/stitchstash/data/backup/) schema-versions every entity — an export that can't survive the next schema change isn't a real backup.
- **Recurring bugs get turned into standing rules, not one-off fixes.** [CLAUDE.md](CLAUDE.md) documents two bug classes this codebase kept hitting — UI built around optional data that isn't there, and form fields that capture data with no screen ever showing it back — plus two shared components (`LabeledSliderField`, `DialogActionRow`) built so the next screen doesn't reintroduce them.
- **CI gates real things.** [`test.yml`](.github/workflows/test.yml) runs unit and instrumented tests on push; [`release.yml`](.github/workflows/release.yml) builds and signs a release APK from a `v*` tag.

**UI/UX.** The interface is designed around the one thing it's for: a calm, honest read on progress, not a dashboard to admire. Optional-data screens (finish cards, reflections) only ever appear when there's something real to show — an empty state that still renders is worse than not offering the feature. Numeric inputs (hours/week, target pieces) are tap-to-type as well as drag, because sliders alone are a bad way to enter an exact number. Every reward — stickers, confetti, milestone call-outs — is tied to something the user actually did, never to time-based pressure to open the app.

## Features

- Onboarding, pattern library, and progress tracking against a market deadline
- Ribblr URL import (scrapes cover image/title/designer, falls back to manual entry)
- Stickers and a celebration flow tied to real milestones
- Finish-card generator (4 border styles) with Android share sheet export
- Forecast playground: lock any two of market date / target pieces / weekly hours, solve the third
- Multiple markets, each trackable independently
- Optional Google Drive backup (debounced auto-backup + manual + restore) and local export
- In-app update checks against GitHub Releases

## Architecture

**Single-Activity Compose app.** [MainActivity.kt](app/src/main/java/com/lachlan/stitchstash/MainActivity.kt) hosts one `NavHost` ([ui/navigation/AppNavigation.kt](app/src/main/java/com/lachlan/stitchstash/ui/navigation/AppNavigation.kt)). Onboarding completion gates which start destination it picks.

**Manual DI via the `Application` class.** [StitchStashApp.kt](app/src/main/java/com/lachlan/stitchstash/StitchStashApp.kt) builds the Room database and a single `StitchRepository` on `onCreate()`. ViewModels are constructed by [AppViewModelFactory.kt](app/src/main/java/com/lachlan/stitchstash/ui/AppViewModelFactory.kt), pulling the repository off the `Application` via `APPLICATION_KEY`.

**Data layer.** Room is the single source of truth: 8 entities (`Pattern`, `Colourway`, `Completion`, `Market`, `AppSettings`, `Sticker`, `Scenario`, `FinishCard`) registered in [StitchStashDatabase.kt](app/src/main/java/com/lachlan/stitchstash/data/db/StitchStashDatabase.kt). All reads/writes flow through [`StitchRepository`](app/src/main/java/com/lachlan/stitchstash/data/repository/StitchRepository.kt) as `Flow`-based queries.

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
    ├── components/                # ProgressRing, Confetti, UpdateBanner, SoftScaffold,
    │                               #   LabeledSliderField, DialogActionRow
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

## Dev-machine setup

### 1. Prerequisites
- **Android Studio** (Meerkat 2024.3+ or newer; latest stable preferred)
- **JDK 17** (bundled with recent Android Studio)
- **Android SDK API 36** + emulator (Tools → SDK Manager)
- **gcloud CLI** (only for Drive setup): `brew install --cask google-cloud-sdk`

### 2. Open the project
1. Move `~/stitch-stash` to the dev machine.
2. Copy `local.properties.template` → `local.properties`; set `sdk.dir` to your Android SDK path.
3. Open the repo in Android Studio. Accept the prompt to **regenerate the Gradle wrapper jar** (or run `gradle wrapper --gradle-version 8.14.3` if you have a global Gradle).
4. Wait for Gradle sync. First run downloads ~700 MB of dependencies.
5. Plug in her phone or pick an emulator, hit Run (▶).

### 3. Google Cloud setup (Drive backup)

Optional — skip it and everything else still works.

The GCP project (`stitch-stash-app`) and Drive API are already provisioned. See **[CLOUD_SETUP.md](./CLOUD_SETUP.md)** for the remaining Console-UI steps (~5 min) — OAuth consent screen, Android OAuth client, Drive folder.

### 4. Release signing

Full instructions in [CLOUD_SETUP.md → Release signing](./CLOUD_SETUP.md#release-signing).

TL;DR: generate `release.keystore` once, register its SHA-1 as a second SHA on the Android OAuth client, add 4 secrets to the GitHub repo, push a `v*` tag → automatic signed-APK release.

### 5. Running tests

```bash
gradle :app:testDebugUnitTest        # JVM unit tests (domain layer, validation)
gradle :app:connectedDebugAndroidTest # instrumented UI tests (needs device/emulator)
gradle build                          # full build incl. lint + tests
```

CI runs both suites on every push via [`test.yml`](.github/workflows/test.yml).

## Update flow

Tag a release in git:
```bash
git tag v0.1.0
git push --tags
```

GH Actions builds + publishes the APK. The in-app `UpdateChecker` polls the GitHub API on Home launch; if a newer tag exists, an "Update available" banner offers a deep link to the Releases page.

Edit `OWNER` and `REPO` constants in `data/update/UpdateChecker.kt` if your GitHub repo isn't `LachlanLoveberry/stitch-stash`.

## Attributions

<a href="https://www.flaticon.com/free-icons/crochet" title="crochet icons">Crochet icons created by Eucalyp - Flaticon</a>
</content>
