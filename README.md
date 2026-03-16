# MineBot Android prototype

Native Android prototype for the same `afkbotb.fly.dev` backend as the iOS app.

## Stack

- Kotlin
- Jetpack Compose
- Material 3
- Expressive-styled Material 3 theme
- GitHub Actions APK build

## Build in GitHub Actions

The workflow is in:

```text
.github/workflows/android.yml
```

It builds:

```text
app/build/outputs/apk/debug/app-debug.apk
```

and uploads it as an artifact.

## Backend base URL

```text
https://afkbotb.fly.dev
```

## Notes

- The prototype stores the token with `EncryptedSharedPreferences`.
- Saved servers are stored locally on device.
- The UI mirrors the iOS version:
  - Tutorial
  - Login
  - Bot
  - Status
  - Settings
- Material 3 is used throughout, with expressive styling cues.
