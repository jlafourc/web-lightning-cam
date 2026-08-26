# Selective lightning detector V2

## Context

The field build proved the complete CameraX capture path but intentionally used
permissive thresholds. A small set of bright pixels can therefore trigger on
noise, reflections, vehicle lights, or camera movement.

## Design

The V2 detector keeps the 64x36 luminance analysis grid and replaces raw
thresholding with a temporal-spatial pipeline:

1. Maintain a per-pixel exponential background and noise estimate.
2. Select positive transients only when both absolute delta and normalized
   delta (delta divided by local noise) are significant.
3. Measure negative transients and reject frames dominated by bidirectional
   change, which indicates camera motion rather than scene illumination.
4. Group positive pixels with eight-neighbour connected components.
5. Accept localized events only when a component has enough area and either a
   lightning-like elongated shape or a compact illuminated cloud region.
6. Keep broad, predominantly positive illumination as the global-flash path.
7. Slow background learning while a candidate is present so the transient is
   not immediately absorbed into the background.

Three named profiles expose explicit trade-offs: Strict, Balanced (default),
and Sensitive. The UI cycles profiles while armed and shows the active profile.

## Implementation choice

The analysis stays pure Kotlin. At 2,304 pixels, a bounded flood fill is cheap,
avoids an OpenCV binary dependency, and is straightforward to unit test. The
camera and storage pipeline remains unchanged. A future iOS version can port
this small deterministic core directly or move it to shared C++ once the
algorithm has been validated on a labelled corpus.

## Validation

Unit fixtures cover broad flashes, narrow connected bolts, isolated hot pixels,
scattered reflections, camera motion, noisy scenes, stable highlights,
refractory behavior, and profile ordering. The Android unit suite, lint, and
debug APK build must pass before GitHub deployment.
