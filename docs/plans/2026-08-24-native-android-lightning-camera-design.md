# Native Android Lightning Camera Design

## Objective

Build a native Android MVP for the Pixel 10 Pro XL that detects nighttime lightning with lower latency and more camera control than the existing web application. Preserve the PWA and structure the detection domain so it can later be reused by an iPhone implementation.

## Chosen approach

Use a Kotlin Android application with CameraX for lifecycle, preview, image analysis, still capture, and video capture. Use Camera2 interoperability only for supported manual controls such as focus distance, exposure time, and sensor sensitivity. Keep the detector and its tests in a platform-neutral Kotlin module.

This is preferred over extending the PWA because native YUV analysis avoids browser canvas and media-recorder constraints. It is preferred over Flutter or React Native because the critical camera pipeline would still need a native plugin and would add another latency and lifecycle boundary.

## MVP scope

- Landscape and portrait camera preview using the rear main camera.
- Real-time analysis of luminance frames with `STRATEGY_KEEP_ONLY_LATEST` so stale frames are dropped.
- Detection of both broad flashes and narrow localized bolts.
- A short refractory period to prevent one flash from producing many events.
- Best-frame selection from a small in-memory ring buffer around the trigger.
- Native video capture finalized by CameraX, covering a short interval around the event where supported by the initial pipeline.
- Local event gallery containing the image, video URI when available, timestamp, trigger type, detector score, exposure information, and measured detection latency.
- A diagnostics overlay with frame rate, dropped-frame behavior, thresholds, and camera capabilities.
- Sensible automatic defaults plus supported focus/exposure controls; unsupported manual controls degrade gracefully.

The first version does not include cloud sync, accounts, RAW post-processing, machine-learning detection, or an iOS application.

## Architecture

The repository becomes a small monorepo while retaining the existing web files:

- `android/`: Gradle root for the native application.
- `android/app`: Android UI, permissions, CameraX pipeline, storage, and lifecycle integration.
- `android/detector`: Kotlin/JVM domain module containing frame models, temporal baseline, localized/global detector, ring buffer, and deterministic unit tests.

The UI uses Jetpack Compose and a `PreviewView` hosted through `AndroidView`. A lifecycle-aware camera coordinator binds Preview, ImageAnalysis, ImageCapture, and VideoCapture when the device reports a supported combination. Capabilities are probed at runtime and features are disabled individually when unavailable.

## Data flow

1. CameraX delivers `YUV_420_888` frames to ImageAnalysis on a dedicated executor.
2. The analyzer samples the Y plane into a small luminance grid without allocating a full bitmap.
3. The detector updates its temporal baseline and calculates global change, localized change, and changed-pixel ratio.
4. Each analyzed sample enters a bounded pre-trigger buffer with timestamp and compact luminance data.
5. When thresholds and refractory rules produce a trigger, the coordinator records diagnostics and asks the capture pipeline for a high-quality still.
6. The best temporal sample guides event scoring; the full-resolution CameraX still is the persisted photograph.
7. CameraX finalizes the current video segment before its URI is attached to the event.
8. Room stores event metadata while MediaStore/app storage owns media files.

## Focus, exposure, and grain

The default mode favors short exposure and moderate sensor sensitivity over multi-frame Night mode because a lightning bolt is brief. The app first uses continuous autofocus, then allows infinity focus when the device exposes a valid manual focus range. Exposure compensation is available in automatic mode. A later expert mode can set exposure time and ISO through Camera2 interop when `MANUAL_SENSOR` is supported.

Noise is reduced by avoiding excessive ISO, detecting on downsampled luminance rather than noisy RGB pixels, rejecting isolated hot pixels, and choosing the most informative frame around the trigger. Multi-second night extensions are not part of the detection stream because they increase latency and can prevent concurrent analysis.

## Error handling

- Permission denial leaves the app in an explanatory retry state.
- Camera binding failures show the failed use-case combination and retry with a reduced combination.
- Unsupported manual controls remain disabled and never block automatic capture.
- Analyzer exceptions close every `ImageProxy` and surface a diagnostic counter.
- Media failures preserve the still image and mark video unavailable rather than losing the event.
- Storage failures are reported in the event screen and never crash the camera session.

## Testing

- Pure JVM tests validate global flashes, narrow bolts, hot-pixel rejection, stable highlights, refractory timing, baseline adaptation, ring-buffer bounds, and best-frame selection.
- Android unit tests validate view-model state and capability mapping.
- Instrumented smoke tests validate permission states and app launch when an Android SDK/emulator is available.
- The build produces a debug APK for sideloading on the Pixel. Device acceptance measures detection latency, missed bolts, false positives, focus at distance, visible grain, still validity, and non-zero video duration.

## Portability to iPhone

Detector algorithms, thresholds, calibration models, and event-domain concepts remain free of Android dependencies. A future iOS target will implement the frame source and capture sink with AVFoundation. Kotlin Multiplatform can be introduced after the Android detector has proven useful; the MVP avoids committing to it before real-device measurements validate the algorithm.
