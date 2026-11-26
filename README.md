# DeepBass

Android bass booster prototype written in Kotlin.

## Features
- Bass strength slider and effect toggle
- Deep Bass, Punchy, Balanced, and Soft presets
- Equalizer controls
- Dark mobile-friendly UI

## Compatibility note
This app requests audio effects on Android output session `0` (global/output mix). Modern Android versions and device firmware may reject or ignore global effects. System-wide enhancement across YouTube, Spotify, games, and other apps is **not guaranteed**. No root access is required by this prototype.

## Build
Open the project in Android Studio, allow Gradle sync, then choose **Build > Build Bundle(s) / APK(s) > Build APK(s)**. The debug APK is normally created under `app/build/outputs/apk/debug/`.

## Requirements
- Android SDK 35 for compilation
- Android 6.0 (API 23) or newer
- Audio effect availability varies by device

Package: `com.deepbass.app`


## Dolby Atmos / spatial audio
The app now includes a device capability/status panel and a shortcut to Android **Sound settings**. It checks Android spatializer availability (Android 12+) and common Dolby-capable codec names.

This is a compatibility check, **not a Dolby Atmos activation API**. Dolby Atmos processing is controlled by the phone's OEM audio stack and may require Dolby licensing, supported hardware/firmware, and Atmos content. DeepBass cannot force-enable the system's Dolby Atmos or apply its own controls to every other app. If your phone includes Dolby Atmos, enable it in system Sound settings; DeepBass bass/equalizer controls are separate and may be limited by Android's audio-effect policy.
