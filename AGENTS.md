# Project Rules & Custom Instructions

## Mandatory Version Bumping on Every Change
Whenever making any changes, features, bug fixes, or updates to the codebase:
1. **Always increment `versionCode`** by +1 in `app/build.gradle.kts`.
2. **Always bump `versionName`** (e.g., `1.1` -> `1.2` -> `1.3`) in `app/build.gradle.kts`.

### Context & Justification:
- The repository is connected to GitHub (`Moksh-05/JIIM`).
- The GitHub Actions workflow (`.github/workflows/JIIM-apk.yml`) automatically triggers on push, builds `app-debug.apk`, and publishes a GitHub Release tagged as `v${VERSION_NAME}`.
- The phone app's in-app updater (`AppUpdateManager.kt`) checks `https://api.github.com/repos/Moksh-05/JIIM/releases/latest` and compares the latest release tag against `BuildConfig.VERSION_NAME`.
- If `versionName` is not bumped on every update, the user's phone will report "App is up to date" and will not offer the update prompt to download and install the new APK.
