# Field Detection Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Detect distant real-world lightning on the Pixel and expose enough live telemetry to verify the detector is active.

**Architecture:** Keep the existing YUV frame pipeline and detector algorithm. Introduce an explicit field-tuned `DetectionConfig`, and aggregate analyzer results into throttled immutable diagnostics delivered to Compose on the main thread.

**Tech Stack:** Kotlin, CameraX, Jetpack Compose, JUnit 4, Gradle.

---

### Task 1: Field sensitivity profile

**Files:**
- Modify: `android/detector/src/main/kotlin/com/lightningcam/detector/DetectionConfig.kt`
- Test: `android/detector/src/test/kotlin/com/lightningcam/detector/LightningDetectorTest.kt`

1. Write a failing test where a modest localized flash is ignored by defaults but detected by `DetectionConfig.field()`.
2. Run `:detector:test` and confirm the new API is missing.
3. Add the smallest named field profile with lower global, localized, and pixel-delta thresholds.
4. Run `:detector:test` and confirm it passes.

### Task 2: Live analyzer diagnostics

**Files:**
- Modify: `android/app/src/main/java/com/lightningcam/camera/LightningAnalyzer.kt`
- Test: `android/app/src/test/java/com/lightningcam/camera/LightningAnalyzerTest.kt`

1. Write a failing test asserting periodic diagnostics report analyzed frames and peak scores.
2. Run the focused app test and verify the expected failure.
3. Add a throttled diagnostics callback without changing frame ownership or trigger delivery.
4. Run the focused test and verify it passes.

### Task 3: Surface diagnostics in the camera UI

**Files:**
- Modify: `android/app/src/main/java/com/lightningcam/camera/AndroidCameraSession.kt`
- Modify: `android/app/src/main/java/com/lightningcam/ui/LightningCamApp.kt`

1. Wire `DetectionConfig.field()` and analyzer diagnostics through the session.
2. Render frame count, latency, global score, localized score, and changed-pixel percentage beneath status.
3. Keep capture status separate so rotations do not hide diagnostics.

### Task 4: Verify and deploy

1. Run `./gradlew :detector:test :app:testDebugUnitTest :app:assembleDebug --no-configuration-cache --no-daemon`.
2. Install the debug APK with `adb install -r`.
3. Launch `com.lightningcam/.MainActivity` and verify its process is alive.
4. Commit and push the tested change to `main`.
