# iPad Capture V2 — Corrective Design

## Field observations

An older iPad tested against a YouTube lightning video triggered on broad highlights but missed visually sharp, narrow bolts. Exported clips were reported as zero seconds long.

## Root causes

The detector requires both a whole-frame mean increase and at least eight percent of pixels to brighten. A narrow bolt can be intense while affecting too little of the frame to satisfy either global threshold.

The recorder concatenates live MediaRecorder timeslices without stopping the recorder. Older Safari versions can defer essential MP4 duration/index metadata until the recorder emits its final data and `stop` event, leaving the assembled blob with a reported duration of zero.

## Detection correction

Keep the existing global-flash path for cloud illumination. Add a localized transient path based on the ratio of changed pixels and the mean intensity of those changes. A localized event must affect a small but non-trivial pixel population and have a much stronger temporal delta than calibrated sensor noise. Single hot pixels and stable bright objects must not trigger.

Expose the localized-change intensity in the detection result so tests and future diagnostics can distinguish global and narrow detections.

## Recorder correction

Record short, self-contained sessions rather than building an output blob from unfinalized slices. Rotate the active session periodically so its age remains bounded. On detection, cancel rotation, continue the current session for the post-trigger duration, stop it, wait for the final `dataavailable` and `stop` events, then return that finalized blob and immediately begin a new rolling session.

This retains up to one rotation interval before the event, includes the post-trigger context, bounds memory, and gives Safari a chance to write valid MP4 metadata. Chrome continues to select WebM when MP4 is unavailable.

## Validation

Automated tests reproduce a narrow high-contrast bolt, reject isolated noise, preserve broad flash detection, prove that clip capture waits for `stop`, includes the final chunk, rotates recordings, and restarts after capture. Full tests, strict type checking, production build, GitHub Actions, and public HTTP assets must pass before completion.
