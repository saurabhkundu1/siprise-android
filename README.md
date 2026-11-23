# Siprise

[![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)

<p align="center">
  <img src="app/src/main/res/drawable-nodpi/ic_siprise.jpg" alt="Siprise water drop app icon" width="144">
</p>

<p align="center"><strong>A gentle, private water tracker for Android.</strong><br>
Log a sip, see your progress, and keep a simple daily rhythm. Your intake history stays on your device.</p>

## What it does

- Log 150 ml, 250 ml, 500 ml, or a custom amount.
- Check today's progress and log from a home-screen widget; choose its amount in the app from 50–1,000 ml in 50 ml steps (250 ml by default).
- The widget exposes today's total and goal to screen readers, alongside its labeled quick-log button.
- Undo the most recent entry.
- Set a daily goal and review the last seven days.
- See recent sip times in your phone's preferred 12- or 24-hour format.
- Turn on reminders, choose an interval and the hours that fit your day (including overnight), and snooze a nudge for 30 minutes; reminders pause after your daily goal and resume at the next day's chosen start.
- Works offline; no account or internet permission.

## Build

- Android Studio with Android SDK 37
- JDK 17
- Android 13 (API 33) or newer

Open the project in Android Studio or run `gradlew.bat assembleDebug` on Windows (`./gradlew assembleDebug` on macOS/Linux). The debug APK is written to `app/build/outputs/apk/debug/app-debug.apk`.

## Releases and Droidify

The source is `1.5.0` (`versionCode` 6). Pushing the matching `v1.5.0` tag builds and verifies a signed APK, publishes a [GitHub release](https://github.com/saurabhkundu1/siprise-android/releases), and sends it to [Applify](https://saurabhkundu1.github.io/Applify/) for the Droidify-compatible repository.

Before the first release, configure a new Siprise app-signing key and the Applify repository's replacement signing fingerprint. The workflow requires repository secrets `ANDROID_KEYSTORE_BASE64`, `ANDROID_KEYSTORE_PASSWORD`, `ANDROID_KEY_ALIAS`, `ANDROID_KEY_PASSWORD`, and `APPLIFY_DISPATCH_TOKEN`; variables `APPLIFY_RELEASES_ENABLED`, `EXPECTED_WATERTRACKER_SIGNER_SHA256`, and `EXPECTED_REPO_SIGNER_SHA256`; and the matching `EXPECTED_WATERTRACKER_SIGNER_SHA256` variable in Applify. The dispatch token needs Contents: write access to Applify.

## Privacy

Siprise stores intake history and preferences locally. Reminders are off until enabled and require notification permission on Android 13 and newer. The default daily goal is 2,000 ml. Siprise is a personal logging tool, not medical advice.

## License

MIT License. See [LICENSE](LICENSE).
