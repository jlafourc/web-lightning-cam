# Lightning Cam Android

Native Android MVP optimized for fast nighttime lightning detection. It analyzes the CameraX Y plane on a dedicated executor using `STRATEGY_KEEP_ONLY_LATEST`, preserves the exact triggering luminance frame as a grayscale JPEG, and stops/finalizes the active MP4 clip after the trigger.

## Build

Requirements: JDK 17 and Android SDK 36.

```bash
cd android
./gradlew test lintDebug assembleDebug
```

The APK is generated at `android/app/build/outputs/apk/debug/app-debug.apk`.

## Install on Pixel

Enable Developer options and USB debugging, connect the phone, approve its RSA prompt, then run:

```bash
adb devices
adb install -r android/app/build/outputs/apk/debug/app-debug.apk
```

Alternatively download the `lightning-cam-android-debug` artifact from the latest **Android APK** GitHub Actions run and open the APK on the Pixel. Android will ask permission to install apps from that source.

Captured media is stored through MediaStore in:

- `Pictures/LightningCam`
- `Movies/LightningCam`

## Current MVP limits

- Detector settings are fixed conservative defaults pending real Pixel measurements.
- Video rotates every four seconds, keeping at most one unused segment, and is finalized 1.2 seconds after a trigger; the next segment starts immediately.
- Trigger photographs use the low-latency analysis resolution (target 640×480) and are grayscale in this first build. They favor capturing the bolt itself over a delayed full-resolution color image.
- Event metadata is kept in memory during the session. Media itself remains in the system gallery.
- Automatic exposure compensation is biased one stop darker when supported to reduce clipping/noise. Manual Camera2 controls are modeled but not yet exposed in the first UI.
- An iPhone client is not included; the detector module has no Android dependency so it can later be moved to Kotlin Multiplatform or ported to Swift.
