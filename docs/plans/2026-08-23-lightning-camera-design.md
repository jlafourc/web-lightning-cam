# Lightning Camera PWA — Design

## Goal

Build an offline-first progressive web app for an iPhone 16 Pro on a tripod that detects nighttime lightning, preserves the best video frame, takes a high-resolution photo when supported, and records a short clip around the event. All processing and media remain on the device.

## Constraints

- The prototype must be buildable and deployable without a Mac or paid Apple developer account.
- Safari controls camera focus, ISO, and exposure inconsistently. The app must detect capabilities at runtime and never imply that an unsupported setting is locked.
- The application must work offline after its first successful load.
- Nighttime processing must minimize heat, battery usage, screen brightness, and false triggers caused by sensor noise or local light sources.
- Browser storage is not permanent. Important captures must be exported to Photos or Files.

## Architecture

The application is a static, client-only PWA.

- `CameraController` opens the rear camera, prefers the main 1× lens and best available resolution, reports actual settings, applies supported constraints, and requests still images.
- `LightningDetector` analyzes a downscaled grayscale stream against a moving baseline.
- `RollingRecorder` records short media chunks and retains a bounded pre-trigger buffer.
- `CaptureCoordinator` reacts to a detection, preserves the brightest analyzed frame, requests a still photo, completes the post-trigger clip, and assembles the event.
- `LocalGallery` stores event metadata and media locally and provides preview, download, sharing, and deletion.
- A service worker caches the application shell for offline operation.

The detection path uses a small analysis canvas while recording uses the best practical camera stream. This separates detection cost from output quality.

## Session Flow

1. The user mounts the iPhone on a tripod, opens the app, and grants camera access.
2. The app requests the rear main camera and reports the camera capabilities exposed by Safari.
3. The user can tap a distant, contrasted area to request focus at that point. The app locks focus and exposure only if the active track reports support.
4. A short calibration measures scene brightness and sensor noise.
5. Arming starts brightness analysis and the bounded rolling recorder.
6. When lightning is detected, the app preserves the brightest recent frame, requests a high-resolution still, records several post-trigger seconds, applies a cooldown, and creates a gallery event.
7. The user compares the still and best frame, reviews the clip and recorded settings, and exports useful files.

The surveillance screen stays very dark and attempts to keep the screen awake while armed. A prominent stop control remains available.

## Detection

The detector combines:

- a sudden increase in mean luminance;
- the proportion of pixels that brighten substantially;
- the frame difference from a moving luminance baseline.

An event requires agreement between signals. Calibration estimates baseline variance and derives an automatic threshold. A sensitivity slider adjusts that threshold. A cooldown prevents one flash sequence from creating an excessive number of events.

## Focus and Noise Strategy

- Prefer the 1× main rear camera because it gathers more light than ultra-wide and telephoto alternatives.
- Avoid digital zoom.
- Offer tap-to-focus and attempt supported point-of-interest, focus, exposure, and ISO constraints.
- Show explicit `locked`, `automatic`, or `unsupported` states based on actual settings and capabilities.
- Ask for the best practical stream and still resolution.
- Preserve original files in the first prototype; do not hide camera limitations behind aggressive synthetic denoising.

## User Interface

The interface has three states:

- **Prepare:** camera preview, focus target, capability report, calibration, and sensitivity.
- **Monitor:** near-black interface, armed state, live brightness signal, sensitivity, and event count.
- **Results:** side-by-side still and best-frame review, clip playback, metadata, export, and deletion.

## Storage and Privacy

All analysis and storage are local. The application has no account, analytics, upload, or server API. IndexedDB stores media blobs and metadata under a configurable quota. The app warns before likely exhaustion and supports deleting individual sessions or all stored data.

Sharing uses the Web Share API with file support when available and falls back to browser downloads. The UI explains that iOS may evict browser data and recommends exporting valuable captures.

## Error Handling

The app provides recovery paths for denied camera permission, unsupported capture or recording APIs, interrupted streams, background suspension, insufficient storage, degraded processing performance, and unavailable manual camera controls. If standalone PWA capture fails, it recommends opening the deployed URL directly in Safari.

## Testing

Automated tests cover detector behavior with synthetic frame sequences, sensitivity and cooldown rules, rolling-buffer bounds, storage behavior, and capability fallbacks. Browser tests cover the principal session flow and offline shell.

Physical validation on the target iPhone begins with controlled artificial flashes, followed by nighttime storms. Measurements include missed events, false triggers, capture latency, sharpness, noise, heat, and battery drain.

## Deployment

The static production build is published on GitHub Pages over HTTPS, which is required for camera access and service workers. The deployment pipeline builds and tests the app before publishing the generated site.
