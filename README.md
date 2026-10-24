# Siprise

Siprise is a small, private-by-default hydration tracker for Android. Log a glass in one tap, follow your daily goal, review the last seven days, and turn on gentle daytime reminders.

## Build

- Android Studio with Android SDK 37
- JDK 17

Open the project in Android Studio or run `gradlew.bat assembleDebug`. The APK is written to `app/build/outputs/apk/debug/app-debug.apk`.

## Privacy

Siprise works offline. Intake history and preferences are stored locally; the app does not request internet access or create an account. Reminders are off until enabled and require notification permission on Android 13 and newer.

The default daily goal is 2,000 ml and can be changed in the app. Siprise is a personal logging tool, not medical advice.
