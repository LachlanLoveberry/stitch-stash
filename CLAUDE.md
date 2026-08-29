# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project

Stitch Stash is a single-module Android app (`com.lachlan.stitchstash`) — a local-first crochet progress tracker. Kotlin + Jetpack Compose + Material 3, min SDK 26, compile/target SDK 36, JDK 17.

Persistence is Room (DB name `stitch_stash.db`, current schema v1, schemas exported to [app/schemas/](app/schemas/)). Background work uses WorkManager. Optional Google Drive backup uses the **classic `GoogleSignIn` API** (not Credential Manager) — matching is by Android package + SHA-1 at runtime, so there is no OAuth client ID embedded in code.

## Build & run

There is **no checked-in `gradlew` script** — only [gradle/wrapper/gradle-wrapper.properties](gradle/wrapper/gradle-wrapper.properties). Use a system-installed `gradle` (or regenerate the wrapper with `gradle wrapper --gradle-version <ver>` once). Builds also work through Android Studio's Run button.

**CLI `gradle` on this machine**: there's no Homebrew/JDK `gradle` install; instead a launcher script at `~/.local/bin/gradle` wraps the exact Gradle 8.14.3 distribution Android Studio already downloaded into `~/.gradle/wrapper/dists/gradle-8.14.3-bin/`, run with Android Studio's bundled JBR (`/Applications/Android Studio.app/Contents/jbr/Contents/Home`) as `JAVA_HOME` (that JBR is JDK 21, newer than the JDK 17 the project targets, but works fine as the Gradle *launcher* JVM — it does not affect the JDK 17 language target used to compile the app). `~/.local/bin` is on `PATH` via `~/.zshrc`. If that cached distribution is ever cleared, recreate the script:
```bash
mkdir -p ~/.local/bin
GRADLE_HOME=~/.gradle/wrapper/dists/gradle-8.14.3-bin/<hash>/gradle-8.14.3   # find <hash> under that dir
cat > ~/.local/bin/gradle <<'EOF'
#!/bin/sh
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
exec "$GRADLE_HOME/bin/gradle" "$@"
EOF
chmod +x ~/.local/bin/gradle
```
(substituting the real `$GRADLE_HOME` path into the heredoc). Use `gradle :app:compileDebugKotlin` for a fast compile-only check while iterating.

```bash
gradle :app:assembleDebug        # build debug APK
gradle :app:installDebug         # install on connected device/emulator
gradle :app:assembleRelease      # signed release build (needs env vars below)
gradle :app:lint                 # Android lint
gradle build                     # full build (no JVM/instrumented tests exist yet)
```

There are no JVM unit tests or instrumented tests in the repo (`app/src/test` and `app/src/androidTest` do not exist).

Release signing reads four env vars at configure time: `STITCH_KEYSTORE_PATH`, `STITCH_KEYSTORE_PASSWORD`, `STITCH_KEY_ALIAS`, `STITCH_KEY_PASSWORD`. If `STITCH_KEYSTORE_PATH` is unset, the release build is unsigned. See [app/build.gradle.kts:23](app/build.gradle.kts:23).

`local.properties` is required for `sdk.dir` — copy from [local.properties.template](local.properties.template).

## Release flow

Push a `v*` tag → [.github/workflows/release.yml](.github/workflows/release.yml) builds a signed APK and attaches it to a GitHub Release. The in-app `data/update/UpdateChecker.kt` polls GitHub Releases on Home launch; the `OWNER`/`REPO` constants there must match the actual GitHub repo or the update banner will be wrong.

## Architecture

**Single-Activity Compose app.** [MainActivity.kt](app/src/main/java/com/lachlan/stitchstash/MainActivity.kt) hosts a single `NavHost` defined in [ui/navigation/AppNavigation.kt](app/src/main/java/com/lachlan/stitchstash/ui/navigation/AppNavigation.kt). Onboarding completion gates which start destination it picks.

**Manual DI via the `Application` class.** [StitchStashApp.kt](app/src/main/java/com/lachlan/stitchstash/StitchStashApp.kt) builds the Room database and a single `StitchRepository` on `onCreate()`. There is no Hilt/Koin. ViewModels are constructed by [ui/AppViewModelFactory.kt](app/src/main/java/com/lachlan/stitchstash/ui/AppViewModelFactory.kt), which pulls the repository off the `Application` via the `APPLICATION_KEY` extra. **When adding a new ViewModel, register an `initializer { … }` here** — composables resolve it via `viewModel(factory = AppViewModelFactory)`.

**Data layer ([data/](app/src/main/java/com/lachlan/stitchstash/data/)).** Room is the single source of truth. 8 entities (`Pattern`, `Colourway`, `Completion`, `Market`, `AppSettings`, `Sticker`, `Scenario`, `FinishCard`) registered in [data/db/StitchStashDatabase.kt](app/src/main/java/com/lachlan/stitchstash/data/db/StitchStashDatabase.kt). All UI reads/writes go through [`StitchRepository`](app/src/main/java/com/lachlan/stitchstash/data/repository/StitchRepository.kt), which exposes `Flow`-based queries. Subpackages:
- `ribblr/` — Jsoup + OkHttp scraper that pulls Open Graph / Twitter card meta from Ribblr pattern URLs.
- `drive/` — `DriveBackupService`, `GoogleSignInHelper` (classic GoogleSignIn, `drive.file` scope only).
- `backup/` — `BackupModel` + `BackupSerializer`, versioned JSON schema (v1) covering all entities.
- `storage/ImageStorage` — copies external `Uri`s into internal app storage so images survive after the source is gone (e.g. Ribblr CDN).
- `update/UpdateChecker` — GitHub Releases poll.

**Domain layer ([domain/](app/src/main/java/com/lachlan/stitchstash/domain/)).** Pure logic, no Android dependencies — safe to extend without touching Room.
- `forecast/ForecastEngine` + `ScenarioSolver` — the Plan playground's lock-two-solve-the-third math.
- `stickers/StickerCatalog` + `StickerEarner` — sticker definitions and the rules that decide which stickers a `Completion` earns.
- `model/` — read-model classes (`PatternWithProgress`, `CompletionWithContext`) joined together by the repository.

**Work layer ([work/BackupWorker.kt](app/src/main/java/com/lachlan/stitchstash/work/BackupWorker.kt)).** Debounced 15-minute Drive backups + manual "Back up now" path; enforces 30-file retention.

**UI layer ([ui/](app/src/main/java/com/lachlan/stitchstash/ui/)).** One folder per screen (`home`, `onboarding`, `patterns`, `log`, `plan`, `stickers`, `markets`, `finishcard`, `settings`), each containing screens + their ViewModel. Shared building blocks live in `components/` (`ProgressRing`, `Confetti`, `UpdateBanner`, `SoftScaffold`). Theme tokens (cream / rose / sage) in `theme/`. The `finishcard/` package renders share cards programmatically to a `Bitmap` (4 border styles + watermark) and exports via FileProvider.

## Stickers & Lottie

Sticker visuals look up `R.raw.sticker_<type>` at runtime in [StickerVisual.kt](app/src/main/java/com/lachlan/stitchstash/ui/stickers/StickerVisual.kt) (loaded via `lottie-compose`). Filename → sticker mapping is fixed by the constants in [StickerCatalog.kt](app/src/main/java/com/lachlan/stitchstash/domain/stickers/StickerCatalog.kt); missing files fall back to the emoji design with no code changes. Reference table for filenames is in [LOTTIE_ASSETS.md](LOTTIE_ASSETS.md).

## Google Drive backup setup

Backup is gated behind Google sign-in + a Drive folder ID entered in Settings. Provisioning steps (OAuth consent screen, Android OAuth client with SHA-1, optional release-key SHA) are in [CLOUD_SETUP.md](CLOUD_SETUP.md). The app itself works fully without any of this — Drive is only Phase 6.
