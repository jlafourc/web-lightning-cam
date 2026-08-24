import { describe, expect, test } from 'vitest';
import { LightningDetector } from './lightning-detector';

const frame = (value: number, size = 100): Uint8Array => new Uint8Array(size).fill(value);

describe('LightningDetector', () => {
  test('calibrates before reporting a stable dark scene as ready', () => {
    const detector = new LightningDetector({ calibrationFrames: 3 });
    expect(detector.analyze(frame(12), 0).calibrating).toBe(true);
    expect(detector.analyze(frame(13), 33).calibrating).toBe(true);
    const result = detector.analyze(frame(12), 66);
    expect(result.calibrating).toBe(false);
    expect(result.detected).toBe(false);
  });

  test('ignores normal sensor noise after calibration', () => {
    const detector = new LightningDetector({ calibrationFrames: 2 });
    detector.analyze(frame(10), 0);
    detector.analyze(frame(12), 33);
    expect(detector.analyze(frame(11), 66).detected).toBe(false);
  });

  test('detects a global flash', () => {
    const detector = new LightningDetector({ calibrationFrames: 2 });
    detector.analyze(frame(10), 0);
    detector.analyze(frame(10), 33);
    const result = detector.analyze(frame(120), 66);
    expect(result.detected).toBe(true);
    expect(result.brightenedRatio).toBeGreaterThan(0.9);
  });

  test('detects a localized lightning flash', () => {
    const detector = new LightningDetector({ calibrationFrames: 2 });
    detector.analyze(frame(10), 0);
    detector.analyze(frame(10), 33);
    const localized = frame(10);
    localized.fill(180, 0, 25);
    expect(detector.analyze(localized, 66).detected).toBe(true);
  });

  test('detects a narrow high-contrast bolt without a global brightness jump', () => {
    const detector = new LightningDetector({ calibrationFrames: 2 });
    detector.analyze(frame(10, 1000), 0);
    detector.analyze(frame(10, 1000), 33);
    const narrowBolt = frame(10, 1000);
    narrowBolt.fill(230, 420, 440);
    const result = detector.analyze(narrowBolt, 66);
    expect(result.delta).toBeLessThan(result.threshold);
    expect(result.detected).toBe(true);
    expect(result.trigger).toBe('localized');
  });

  test('ignores an isolated hot pixel', () => {
    const detector = new LightningDetector({ calibrationFrames: 2 });
    detector.analyze(frame(10, 1000), 0);
    detector.analyze(frame(10, 1000), 33);
    const hotPixel = frame(10, 1000);
    hotPixel[500] = 255;
    expect(detector.analyze(hotPixel, 66).detected).toBe(false);
  });

  test('ignores a stable bright object', () => {
    const detector = new LightningDetector({ calibrationFrames: 2 });
    const scene = frame(10, 1000);
    scene.fill(220, 400, 430);
    detector.analyze(scene, 0);
    detector.analyze(scene, 33);
    expect(detector.analyze(scene, 66).detected).toBe(false);
  });

  test('higher sensitivity detects a smaller brightness jump', () => {
    const low = new LightningDetector({ calibrationFrames: 2, sensitivity: 0.1 });
    const high = new LightningDetector({ calibrationFrames: 2, sensitivity: 1 });
    for (const detector of [low, high]) {
      detector.analyze(frame(10), 0);
      detector.analyze(frame(10), 33);
    }
    expect(low.analyze(frame(28), 66).detected).toBe(false);
    expect(high.analyze(frame(28), 66).detected).toBe(true);
  });

  test('applies cooldown after a detection', () => {
    const detector = new LightningDetector({ calibrationFrames: 1, cooldownMs: 1000 });
    detector.analyze(frame(10), 0);
    expect(detector.analyze(frame(150), 100).detected).toBe(true);
    expect(detector.analyze(frame(220), 200).detected).toBe(false);
  });

  test('retains the brightest recent frame metadata', () => {
    const detector = new LightningDetector({ calibrationFrames: 1, recentFrameCount: 3 });
    detector.analyze(frame(10), 0, 'dark');
    detector.analyze(frame(60), 33, 'brightest');
    detector.analyze(frame(20), 66, 'later');
    expect(detector.brightestRecentFrame()?.payload).toBe('brightest');
  });
});
