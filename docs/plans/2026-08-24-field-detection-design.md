# Field detection diagnostics

The native camera pipeline is operational on the Pixel 10 Pro XL, but real,
distant lightning does not cross the current detector thresholds and the UI
provides no evidence that frames are being analyzed.

The field profile keeps the existing global/localized detector and refractory
period, while lowering only the brightness-delta thresholds enough to capture
distant flashes. The analyzer will publish throttled diagnostics containing
frame count, latency, and the strongest recent scores. The UI will display
those diagnostics independently from capture status, so camera activity and
near-threshold flashes are visible without flooding Compose updates.

Detector behavior and diagnostic aggregation are covered by JVM unit tests.
The Android application is then rebuilt, installed over the existing debug
application, launched, and checked on the connected Pixel.
