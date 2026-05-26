# Lottie animations for Stitch Stash stickers

The app's stickers are **already pretty** without any external assets — gradient discs, scalloped trim, emoji centre. Drop a Lottie JSON into `app/src/main/res/raw/` with the right name and the sticker upgrades to a looping animation on top of the gradient.

## How it works

`StickerVisual.kt` looks up `sticker_<TYPE>` as a raw resource at runtime. If found, it loops the Lottie inside the gradient disc. If not found, it falls back to the emoji + decorative dots design.

**No code changes needed when you add a file** — just drop it in and rebuild.

## Filename conventions

The lookup is `R.raw.sticker_<type>` where `<type>` matches the constants in `StickerCatalog.kt`:

| Filename in `res/raw/` | Sticker | Vibe to look for |
|---|---|---|
| `sticker_first_ever.json` | First piece ever | Trophy, star burst, sparkle |
| `sticker_first_of_pattern.json` | First of a new pattern | Sparkle, "new!" badge, glow |
| `sticker_pattern_complete.json` | Both colourways done | Medal, checkmark in sunburst, fireworks |
| `sticker_milestone_fifth.json` | 5 pieces total | Star with "5", high-five hand |
| `sticker_milestone_tenth.json` | 10 pieces total | Number 10, sparkle ring, fireworks |
| `sticker_milestone_25.json` | 25 pieces total | Gold trophy, crown |
| `sticker_weekly_best.json` | Personal weekly best | Upward arrow, rocket, fire |
| `sticker_welcome_back.json` | Returned after a break | Heart, waving hand, hug |
| `sticker_first_ribblr.json` | First Ribblr import | Gift, package opening, link icon |

## Curated LottieFiles search URLs

Tap any of these to browse free animations matching the vibe. **All LottieFiles searches** show free + premium — filter to "Free" in the sidebar before downloading.

### First piece ever
- [Trophy](https://lottiefiles.com/search?q=trophy&type=free)
- [Star sparkle burst](https://lottiefiles.com/search?q=sparkle&type=free)
- [Achievement unlocked badge](https://lottiefiles.com/search?q=achievement+badge&type=free)

### First of a new pattern
- [Sparkle / shine](https://lottiefiles.com/search?q=shine+sparkle&type=free)
- ["New" badge](https://lottiefiles.com/search?q=new+badge&type=free)
- [Magic wand](https://lottiefiles.com/search?q=magic+wand&type=free)

### Pattern complete
- [Medal](https://lottiefiles.com/search?q=medal&type=free)
- [Checkmark celebration](https://lottiefiles.com/search?q=checkmark+success&type=free)
- [Crochet finished](https://lottiefiles.com/search?q=crochet&type=free)

### Milestone 5 / 10 / 25
- [Number badges](https://lottiefiles.com/search?q=number+badge&type=free)
- [Counting](https://lottiefiles.com/search?q=number+counter&type=free)
- [Fireworks](https://lottiefiles.com/search?q=fireworks&type=free) — different intensities for each tier
- [Ranking medals](https://lottiefiles.com/marketplace/ranking-medals) — paid pack but covers 1-10

### Personal weekly best
- [Upward arrow / chart](https://lottiefiles.com/search?q=arrow+up+chart&type=free)
- [Rocket launch](https://lottiefiles.com/search?q=rocket&type=free)
- [Fire flame](https://lottiefiles.com/search?q=fire+flame&type=free)

### Welcome back
- [Heart](https://lottiefiles.com/search?q=heart+beat&type=free)
- [Waving hand](https://lottiefiles.com/search?q=waving+hand&type=free)
- [Cosy](https://lottiefiles.com/search?q=cozy+coffee&type=free)

### First Ribblr import
- [Gift box opening](https://lottiefiles.com/search?q=gift+box&type=free)
- [Download](https://lottiefiles.com/search?q=download+complete&type=free)
- [Link / chain](https://lottiefiles.com/search?q=link+chain&type=free)

## Background decoration

Beyond stickers, you can drop these into `res/raw/` and load them in code where decorative animations help:

| Filename | Where to use | Search |
|---|---|---|
| `decor_yarn_ball.json` | Onboarding step 1, empty pattern list | [yarn ball](https://lottiefiles.com/free-animations/yarn-animation) |
| `decor_calendar.json` | Market countdown card on Home | [calendar flip](https://lottiefiles.com/search?q=calendar&type=free) |
| `decor_hearts.json` | Background of finish-card preview | [hearts floating](https://lottiefiles.com/search?q=hearts&type=free) |
| `decor_confetti.json` | Could replace built-in confetti in celebration dialog | [confetti](https://lottiefiles.com/free-animations/lottie-confetti) |

These aren't auto-loaded by the app — you'd add a `LottieAnimation` call in the relevant screen. I can wire them up if you tell me which spots you want decorated.

## Download flow

1. Open a search URL above
2. Filter to **Free** in left sidebar (LottieFiles tag)
3. Click an animation you like — preview plays
4. Click the download icon → **Lottie JSON**
5. Rename the downloaded file to one of the table names (e.g. `sticker_first_ever.json`)
6. Drop in `app/src/main/res/raw/`
7. Rebuild (`Build → Make Project` in Android Studio)

## Licensing reminder

LottieFiles' free animations have varying licences. Most allow personal + commercial use with optional attribution; a few require it. Click the **Licence** link on each animation's page before shipping. For Stitch Stash (personal use, never going to the Play Store), this is essentially "use whatever you like".

## Quick test

To verify the integration is wired up before hunting down assets, drop *any* Lottie JSON into `res/raw/sticker_first_ever.json`. Open the app, log a piece — the celebration dialog should show that animation looping inside the rose-pink gradient disc instead of the trophy emoji.
