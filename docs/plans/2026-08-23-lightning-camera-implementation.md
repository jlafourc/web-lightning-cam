# Lightning Camera PWA Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Build and publish an offline-first iPhone-oriented PWA that detects lightning, preserves the brightest frame, records a short clip, requests a still photo, and stores results locally.

**Architecture:** A Vite/TypeScript static application separates camera access, detection, rolling recording, event coordination, IndexedDB persistence, and UI state. Browser-specific APIs are feature-detected and wrapped behind small interfaces so the core detector and buffer logic can be tested without camera hardware.

**Tech Stack:** TypeScript, Vite, Vitest, vanilla DOM/CSS, Media Capture APIs, MediaRecorder, ImageCapture, IndexedDB, service worker, GitHub Actions and GitHub Pages.

---

### Task 1: Project shell and quality gates

**Files:**
- Create: `package.json`
- Create: `tsconfig.json`
- Create: `vite.config.ts`
- Create: `vitest.config.ts`
- Create: `index.html`
- Create: `src/main.ts`
- Create: `src/styles.css`
- Create: `.gitignore`

**Steps:**
1. Add scripts for `dev`, `build`, `test`, and `check` and pin the minimal Vite, TypeScript, and Vitest development dependencies.
2. Create a strict TypeScript configuration with DOM libraries.
3. Create a minimal French app shell and dark mobile-first stylesheet.
4. Install dependencies with `npm install`.
5. Run `npm run check` and `npm run build`; expect both to succeed.
6. Commit with `chore: scaffold lightning camera pwa`.

### Task 2: Lightning detector using TDD

**Files:**
- Create: `src/core/lightning-detector.ts`
- Create: `src/core/lightning-detector.test.ts`

**Steps:**
1. Write failing tests for calibration, stable dark noise, a global flash, a localized flash, sensitivity scaling, cooldown, and brightest-frame retention.
2. Run `npm test -- src/core/lightning-detector.test.ts`; expect failures because the detector does not exist.
3. Implement luminance statistics, an exponential moving baseline, calibrated noise threshold, multi-signal agreement, cooldown, and bounded recent-frame metadata.
4. Run the focused tests; expect all to pass.
5. Commit with `feat: add calibrated lightning detector`.

### Task 3: Rolling media buffer using TDD

**Files:**
- Create: `src/core/rolling-buffer.ts`
- Create: `src/core/rolling-buffer.test.ts`

**Steps:**
1. Write failing tests for chronological ordering, duration-based eviction, snapshot isolation, and clearing.
2. Run the focused test and confirm failure.
3. Implement a generic timestamped bounded buffer usable for MediaRecorder chunks.
4. Run focused and full tests; expect success.
5. Commit with `feat: add bounded rolling media buffer`.

### Task 4: Camera and recording adapters

**Files:**
- Create: `src/camera/camera-controller.ts`
- Create: `src/camera/camera-controller.test.ts`
- Create: `src/camera/rolling-recorder.ts`
- Create: `src/camera/rolling-recorder.test.ts`
- Create: `src/types/image-capture.d.ts`

**Steps:**
1. Write failing tests with mocked media tracks for rear-camera constraints, capability reporting, safe unsupported-constraint fallbacks, still capture fallback, recorder MIME selection, and pre/post-trigger assembly.
2. Run focused tests and confirm failures.
3. Implement `CameraController` with runtime capability checks and transparent status reporting.
4. Implement `RollingRecorder` around MediaRecorder timeslices and the rolling buffer.
5. Run focused and full tests; expect success.
6. Commit with `feat: add camera and rolling recorder adapters`.

### Task 5: Local event gallery

**Files:**
- Create: `src/storage/event-store.ts`
- Create: `src/storage/event-store.test.ts`
- Create: `src/storage/idb.ts`

**Steps:**
1. Write failing tests against a small storage interface for create, list newest-first, retrieve blobs, delete one, clear all, and quota errors.
2. Run focused tests and confirm failure.
3. Implement IndexedDB persistence with an in-memory test adapter and storage-estimate reporting.
4. Run focused and full tests; expect success.
5. Commit with `feat: persist lightning events locally`.

### Task 6: Capture coordinator and application UI

**Files:**
- Create: `src/app/capture-coordinator.ts`
- Create: `src/app/capture-coordinator.test.ts`
- Create: `src/app/app.ts`
- Create: `src/app/dom.ts`
- Modify: `src/main.ts`
- Modify: `src/styles.css`
- Modify: `index.html`

**Steps:**
1. Write failing coordinator tests for arm/calibrate/detect/capture/save/disarm flow and partial API failure recovery.
2. Run focused tests and confirm failure.
3. Implement coordinator dependencies and lifecycle.
4. Build the preparation, monitoring, and results views in French with accessible controls, tap-to-focus, sensitivity, capability badges, live meter, export, and deletion.
5. Use a downscaled canvas analysis loop and preserve the brightest JPEG frame around each detection.
6. Add wake-lock acquisition/release and visibility recovery.
7. Run full tests, type checking, and production build; expect success.
8. Commit with `feat: build lightning capture experience`.

### Task 7: Offline PWA assets and resilience

**Files:**
- Create: `public/manifest.webmanifest`
- Create: `public/sw.js`
- Create: `public/icons/icon.svg`
- Create: `public/icons/maskable.svg`
- Create: `src/pwa/register-service-worker.ts`
- Create: `src/pwa/register-service-worker.test.ts`
- Modify: `index.html`
- Modify: `src/main.ts`

**Steps:**
1. Write failing registration tests for supported, unsupported, and update cases.
2. Implement service-worker registration and update notification.
3. Add a cache-first application shell with network refresh and versioned cache cleanup.
4. Add manifest metadata, standalone display, theme colors, and scalable icons.
5. Run tests and production build; verify all generated shell assets exist.
6. Commit with `feat: add offline pwa support`.

### Task 8: Deployment, documentation, and final verification

**Files:**
- Create: `.github/workflows/deploy-pages.yml`
- Create: `README.md`
- Modify: `vite.config.ts`

**Steps:**
1. Configure Vite for a repository-relative base path.
2. Add a GitHub Pages workflow that installs locked dependencies, tests, builds, uploads `dist`, and deploys it.
3. Document usage, installation on iPhone, privacy, Safari limitations, local development, testing, and field-validation steps.
4. Run `npm ci`, `npm test`, `npm run check`, and `npm run build`; all must pass.
5. Inspect the production bundle and working tree for accidental secrets or server dependencies.
6. Commit with `ci: deploy pwa to github pages`.
7. Create or connect the GitHub repository, push `main`, enable Pages through GitHub Actions, and monitor the deployment to completion.
8. Open the public HTTPS URL and verify the manifest, service worker, camera-permission entry point, and responsive shell.
