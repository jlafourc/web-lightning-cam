# Selective Lightning Detector V2 Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Replace the permissive brightness trigger with a selective temporal-spatial lightning detector and user-selectable sensitivity profiles.

**Architecture:** A pure-Kotlin detector maintains per-pixel background/noise state, extracts normalized positive transients, rejects bidirectional camera motion, and validates connected component geometry. CameraX continues feeding 64x36 YUV frames and the UI selects immutable detector profiles.

**Tech Stack:** Kotlin, CameraX, Jetpack Compose, JUnit 4, Gradle.

---

### Task 1: Named sensitivity profiles

**Files:**
- Modify: `android/detector/src/main/kotlin/com/lightningcam/detector/DetectionConfig.kt`
- Test: `android/detector/src/test/kotlin/com/lightningcam/detector/DetectionConfigTest.kt`

1. Write failing tests for Strict, Balanced, and Sensitive ordering and validation.
2. Run `:detector:test` and verify the missing API failure.
3. Implement `DetectionProfile` and profile-specific immutable configurations.
4. Run `:detector:test` and verify green.

### Task 2: Temporal noise and motion rejection

**Files:**
- Modify: `android/detector/src/main/kotlin/com/lightningcam/detector/LightningDetector.kt`
- Modify: `android/detector/src/main/kotlin/com/lightningcam/detector/DetectionResult.kt`
- Test: `android/detector/src/test/kotlin/com/lightningcam/detector/LightningDetectorTest.kt`

1. Add failing fixtures for noisy pixels and bidirectional camera motion.
2. Run focused tests and verify red.
3. Add per-pixel noise normalization and negative-change motion rejection.
4. Run focused tests and verify green.

### Task 3: Spatial component validation

**Files:**
- Create: `android/detector/src/main/kotlin/com/lightningcam/detector/TransientComponents.kt`
- Test: `android/detector/src/test/kotlin/com/lightningcam/detector/TransientComponentsTest.kt`
- Modify: `android/detector/src/main/kotlin/com/lightningcam/detector/LightningDetector.kt`

1. Write failing tests for connected bolts, compact cloud regions, isolated points, and scattered reflections.
2. Run focused tests and verify red.
3. Implement bounded eight-neighbour component extraction and shape statistics.
4. Integrate component acceptance into localized triggering.
5. Run detector tests and verify green.

### Task 4: Runtime profile selection

**Files:**
- Modify: `android/app/src/main/java/com/lightningcam/camera/AndroidCameraSession.kt`
- Modify: `android/app/src/main/java/com/lightningcam/ui/LightningCamApp.kt`
- Test: `android/app/src/test/java/com/lightningcam/camera/DetectionProfileControllerTest.kt`

1. Write a failing controller test for cycling profiles and resetting the detector.
2. Implement the profile controller and session update method.
3. Add a Compose profile button and display the active thresholds/profile.
4. Run app unit tests and verify green.

### Task 5: Full verification and GitHub deployment

1. Run `./gradlew test lintDebug assembleDebug --no-daemon --no-configuration-cache`.
2. Inspect `git diff --check` and repository status.
3. Merge the feature branch into `main`.
4. Repeat the full verification on merged `main`.
5. Push `main` to GitHub and verify the workflow result.
