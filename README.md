# Little Squares

A quiet habit tracker for Android. Tick off a few habits each day and watch your squares fill in. No points, no pressure.

Built with Kotlin, Jetpack Compose (Material 3) and Glance, from the *Little Squares* design.

## What's in it

- **Onboarding**: welcome, how the grid works, and adding your first habit.
- **Today**: progress bar, habit cards with a springy check-off, and a 7-day strip. Tap any of the last 3 days to fill it in.
- **Add / edit habit**: emoji, name, colour (8 pastels), daily reminder time, archive and delete.
- **Habit detail**: current and best streak, this month, all time, and a year grid you can page by year.
- **Calendar**: a week view (habit × day) and a month view (combined intensity plus one mini grid per habit). Swipe to move between weeks or months.
- **Reminders**: one notification per habit with **Mark done** / **Snooze 1 h**. After marking, it flips to "done" with **Undo**. There's also an optional evening nudge at 20:00.
- **Settings**: Light / Dark / System theme, week start, default reminder time, gentle sound, evening nudge, CSV/JSON export, and archived habits.
- **Widgets**:
  - *Today* (resizable): one habit shows a big check, up to 3 show a list, 6+ show an emoji grid, and 4×4 shows this week's grid.
  - *Streak* (pick a habit): a big check, the streak with the last 8 weeks, or the last 4 weeks.
- **Adaptive app icon** ("Checked day"), with a themed monochrome layer for Material You.

The app, notifications and widgets share one state, so checking something off anywhere updates it everywhere.

## Releases

Every push to `main` runs `.github/workflows/android.yml`. It builds a signed release APK and publishes it on the repo's **Releases** page as `v1.0.<run number>`. Pull requests build the APK and attach it as a workflow artifact.

### Signing

Add these repository secrets so every release is signed with the same key and installs over the previous one:

| Secret | Value |
| --- | --- |
| `SIGNING_KEYSTORE_BASE64` | `base64 -w0 your.jks` |
| `SIGNING_STORE_PASSWORD` | keystore password |
| `SIGNING_KEY_ALIAS` | key alias |
| `SIGNING_KEY_PASSWORD` | key password |

To create a keystore:

```bash
keytool -genkeypair -keystore release.jks -alias littlesquares -keyalg RSA -keysize 2048 -validity 10000
```

Without these secrets, CI signs with a throwaway debug key. The APK still installs, but each new release has a different signature, so you'll have to uninstall the old version before installing the new one.

## Building locally

Requires JDK 17 and the Android SDK (API 35).

```bash
./gradlew assembleRelease
```
