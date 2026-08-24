# Native Android Lightning Camera Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Build and publish a sideloadable native Android MVP that detects nighttime lightning quickly on a Pixel 10 Pro XL, captures a sharp still and valid video, and exposes useful diagnostics.

**Architecture:** Add an isolated Gradle project under `android/`, with a pure Kotlin `detector` module and an Android `app` module. CameraX supplies preview, latest-only YUV analysis, still capture, and finalized video; Compose presents the camera, controls, diagnostics, and local events. Platform-neutral detector code and models keep an eventual AVFoundation/iPhone port practical.

**Tech Stack:** Kotlin, Gradle Kotlin DSL, Android Gradle Plugin, CameraX/Camera2 interop, Jetpack Compose, coroutines/Flow, Room, JUnit 5/JUnit 4, AndroidX Test, GitHub Actions.

---

### Task 1: Scaffold the Android build

**Files:**
- Create: `android/settings.gradle.kts`
- Create: `android/build.gradle.kts`
- Create: `android/gradle.properties`
- Create: `android/gradle/libs.versions.toml`
- Create: `android/detector/build.gradle.kts`
- Create: `android/app/build.gradle.kts`
- Create: `android/app/src/main/AndroidManifest.xml`
- Create: `android/app/src/main/java/com/lightningcam/MainActivity.kt`
- Create: `android/app/src/test/java/com/lightningcam/BuildSmokeTest.kt`
- Modify: `.gitignore`

**Step 1:** Write `BuildSmokeTest` asserting the initial app state name is `Lightning Cam`.

**Step 2:** Run `cd android && ./gradlew :app:testDebugUnitTest`; expect failure because the Gradle project/application state does not exist.

**Step 3:** Add the wrapper/build files, SDK levels supported by the installed SDK, application ID `com.lightningcam`, Compose setup, manifest camera/audio permissions, minimal `MainActivity`, and application state.

**Step 4:** Run `cd android && ./gradlew :app:testDebugUnitTest :app:assembleDebug`; expect PASS and `app-debug.apk`.

**Step 5:** Commit with `feat: scaffold native Android application`.

### Task 2: Implement the platform-neutral lightning detector

**Files:**
- Create: `android/detector/src/main/kotlin/com/lightningcam/detector/LuminanceFrame.kt`
- Create: `android/detector/src/main/kotlin/com/lightningcam/detector/DetectionConfig.kt`
- Create: `android/detector/src/main/kotlin/com/lightningcam/detector/DetectionResult.kt`
- Create: `android/detector/src/main/kotlin/com/lightningcam/detector/LightningDetector.kt`
- Create: `android/detector/src/test/kotlin/com/lightningcam/detector/LightningDetectorTest.kt`

**Step 1:** Add failing tests for baseline warmup, broad flash, a narrow 2% bolt, isolated hot-pixel rejection, stable highlight rejection, and refractory timing.

**Step 2:** Run `cd android && ./gradlew :detector:test`; expect failures for missing detector behavior.

**Step 3:** Implement an allocation-conscious temporal luminance detector returning `GLOBAL`, `LOCALIZED`, or no trigger plus scores and changed-pixel ratio.

**Step 4:** Run `cd android && ./gradlew :detector:test`; expect all detector tests PASS.

**Step 5:** Commit with `feat: add native temporal lightning detector`.

### Task 3: Add bounded frame history and best-frame selection

**Files:**
- Create: `android/detector/src/main/kotlin/com/lightningcam/detector/FrameRingBuffer.kt`
- Create: `android/detector/src/main/kotlin/com/lightningcam/detector/BestFrameSelector.kt`
- Create: `android/detector/src/test/kotlin/com/lightningcam/detector/FrameRingBufferTest.kt`
- Create: `android/detector/src/test/kotlin/com/lightningcam/detector/BestFrameSelectorTest.kt`

**Step 1:** Write failing tests for capacity eviction, timestamp ordering, clearing, and selection of the sharp/high-information frame without accepting a saturated frame.

**Step 2:** Run the two targeted test classes; expect FAIL.

**Step 3:** Implement the fixed-capacity buffer and deterministic scoring selector.

**Step 4:** Run `cd android && ./gradlew :detector:test`; expect PASS.

**Step 5:** Commit with `feat: buffer and score lightning frames`.

### Task 4: Convert CameraX YUV frames safely

**Files:**
- Create: `android/app/src/main/java/com/lightningcam/camera/YPlaneSampler.kt`
- Create: `android/app/src/main/java/com/lightningcam/camera/LightningAnalyzer.kt`
- Create: `android/app/src/test/java/com/lightningcam/camera/YPlaneSamplerTest.kt`
- Create: `android/app/src/test/java/com/lightningcam/camera/LightningAnalyzerTest.kt`

**Step 1:** Write failing tests using synthetic planes with row/pixel strides, rotations, and bright narrow regions; test that the proxy-closing callback runs on success and failure.

**Step 2:** Run the targeted app unit tests; expect FAIL.

**Step 3:** Implement direct Y-plane sampling to a fixed grid, monotonic latency measurement, detector invocation, and guaranteed closure in `finally`.

**Step 4:** Run app and detector unit tests; expect PASS.

**Step 5:** Commit with `feat: analyze CameraX luminance frames`.

### Task 5: Model capabilities and camera controls

**Files:**
- Create: `android/app/src/main/java/com/lightningcam/camera/CameraCapabilities.kt`
- Create: `android/app/src/main/java/com/lightningcam/camera/CameraSettings.kt`
- Create: `android/app/src/main/java/com/lightningcam/camera/CameraControlMapper.kt`
- Create: `android/app/src/test/java/com/lightningcam/camera/CameraControlMapperTest.kt`

**Step 1:** Write failing tests for automatic defaults, infinity-focus availability, exposure compensation clamping, manual-sensor support, and unsupported-control fallback.

**Step 2:** Run the mapper tests; expect FAIL.

**Step 3:** Implement immutable capability/settings models and mapping into CameraX controls plus Camera2 interop request options.

**Step 4:** Run app unit tests; expect PASS.

**Step 5:** Commit with `feat: add low-light camera controls`.

### Task 6: Persist event metadata and media references

**Files:**
- Create: `android/app/src/main/java/com/lightningcam/storage/LightningEventEntity.kt`
- Create: `android/app/src/main/java/com/lightningcam/storage/LightningEventDao.kt`
- Create: `android/app/src/main/java/com/lightningcam/storage/LightningDatabase.kt`
- Create: `android/app/src/main/java/com/lightningcam/storage/EventRepository.kt`
- Create: `android/app/src/androidTest/java/com/lightningcam/storage/LightningEventDaoTest.kt`

**Step 1:** Write a failing in-memory Room test for insert, newest-first observation, optional video URI, and delete.

**Step 2:** Run `cd android && ./gradlew :app:connectedDebugAndroidTest` when a device/emulator is available; otherwise compile the test and record device verification as pending.

**Step 3:** Implement Room schema/repository with timestamp, trigger type, scores, latency, photo URI, video URI, ISO, exposure, and focus metadata.

**Step 4:** Run unit tests and `:app:assembleDebug`; expect PASS.

**Step 5:** Commit with `feat: persist native lightning events`.

### Task 7: Bind preview, analysis, still, and finalized video

**Files:**
- Create: `android/app/src/main/java/com/lightningcam/camera/CameraCoordinator.kt`
- Create: `android/app/src/main/java/com/lightningcam/camera/CaptureSink.kt`
- Create: `android/app/src/main/java/com/lightningcam/camera/MediaStoreCaptureSink.kt`
- Create: `android/app/src/test/java/com/lightningcam/camera/CameraCoordinatorTest.kt`

**Step 1:** Write failing coordinator tests with fake ports for lifecycle transitions, one capture per refractory window, still preservation on video failure, and event persistence only after media finalization.

**Step 2:** Run the coordinator tests; expect FAIL.

**Step 3:** Bind CameraX with latest-only ImageAnalysis, robust use-case fallback, prestarted VideoCapture, MediaStore output, and trigger-driven still/final video capture. Restart recording after finalization.

**Step 4:** Run all JVM tests and assemble the debug APK; expect PASS.

**Step 5:** Commit with `feat: capture native lightning photos and video`.

### Task 8: Build the Compose camera experience

**Files:**
- Create: `android/app/src/main/java/com/lightningcam/ui/CameraViewModel.kt`
- Create: `android/app/src/main/java/com/lightningcam/ui/CameraScreen.kt`
- Create: `android/app/src/main/java/com/lightningcam/ui/EventGalleryScreen.kt`
- Create: `android/app/src/main/java/com/lightningcam/ui/LightningCamApp.kt`
- Create: `android/app/src/test/java/com/lightningcam/ui/CameraViewModelTest.kt`
- Modify: `android/app/src/main/java/com/lightningcam/MainActivity.kt`

**Step 1:** Write failing view-model tests for permission states, armed/disarmed state, diagnostics, settings updates, trigger notification, and recoverable errors.

**Step 2:** Run view-model tests; expect FAIL.

**Step 3:** Implement preview, arm button, detection flash, exposure/focus controls, live FPS/latency/scores, capability warnings, gallery, and event detail.

**Step 4:** Run all JVM tests and assemble the APK; expect PASS.

**Step 5:** Commit with `feat: add native lightning camera interface`.

### Task 9: Add CI, installation documentation, and device checklist

**Files:**
- Create: `.github/workflows/android.yml`
- Create: `android/README.md`
- Create: `docs/pixel-device-test-checklist.md`
- Modify: `README.md`

**Step 1:** Add CI configuration for Gradle tests, lint, debug APK build, and artifact upload.

**Step 2:** Run locally: `cd android && ./gradlew test lintDebug assembleDebug`; expect PASS.

**Step 3:** If the Pixel is connected, run `adb devices` then `adb install -r android/app/build/outputs/apk/debug/app-debug.apk`; otherwise document this exact command.

**Step 4:** Verify the repository is clean except intended files and inspect the APK path/hash.

**Step 5:** Commit with `ci: build native Android APK`, push `feature/native-android`, and verify the GitHub Actions run succeeds.

### Task 10: Review, integrate, and publish the MVP

**Files:**
- Modify only files identified by review or verification.

**Step 1:** Invoke `superpowers:requesting-code-review` and address verified findings using `superpowers:receiving-code-review`.

**Step 2:** Invoke `superpowers:verification-before-completion`; freshly run web tests plus Android `test lintDebug assembleDebug`.

**Step 3:** Invoke `superpowers:finishing-a-development-branch`, fast-forward merge the approved branch into `main`, and push.

**Step 4:** Watch the Android GitHub Actions run to completion and provide the APK artifact location plus Pixel installation command.
