export interface DetectorOptions {
  calibrationFrames?: number;
  sensitivity?: number;
  cooldownMs?: number;
  recentFrameCount?: number;
  baselineAlpha?: number;
}

export interface DetectionResult {
  detected: boolean;
  calibrating: boolean;
  mean: number;
  baseline: number;
  delta: number;
  brightenedRatio: number;
  threshold: number;
}

export interface RecentFrame<T = unknown> {
  timestamp: number;
  luminance: number;
  payload: T;
}

export class LightningDetector<T = unknown> {
  private readonly calibrationFrames: number;
  private readonly cooldownMs: number;
  private readonly recentFrameCount: number;
  private readonly baselineAlpha: number;
  private sensitivity: number;
  private calibration: number[] = [];
  private baseline = 0;
  private noise = 0;
  private previous?: Uint8Array;
  private lastDetection = Number.NEGATIVE_INFINITY;
  private recent: RecentFrame<T>[] = [];

  constructor(options: DetectorOptions = {}) {
    this.calibrationFrames = Math.max(1, options.calibrationFrames ?? 45);
    this.cooldownMs = options.cooldownMs ?? 1500;
    this.recentFrameCount = Math.max(1, options.recentFrameCount ?? 12);
    this.baselineAlpha = options.baselineAlpha ?? 0.04;
    this.sensitivity = clamp(options.sensitivity ?? 0.65, 0, 1);
  }

  setSensitivity(value: number): void {
    this.sensitivity = clamp(value, 0, 1);
  }

  reset(): void {
    this.calibration = [];
    this.baseline = 0;
    this.noise = 0;
    this.previous = undefined;
    this.lastDetection = Number.NEGATIVE_INFINITY;
    this.recent = [];
  }

  analyze(pixels: Uint8Array, timestamp: number, payload?: T): DetectionResult {
    const mean = average(pixels);
    if (payload !== undefined) {
      this.recent.push({ timestamp, luminance: mean, payload });
      if (this.recent.length > this.recentFrameCount) this.recent.shift();
    }

    if (this.calibration.length < this.calibrationFrames) {
      this.calibration.push(mean);
      this.baseline = average(this.calibration);
      this.noise = standardDeviation(this.calibration, this.baseline);
      this.previous = pixels.slice();
      return this.result(false, this.calibration.length < this.calibrationFrames, mean, 0, 0);
    }

    const delta = mean - this.baseline;
    const pixelThreshold = Math.max(12, this.threshold() * 0.65);
    let brightened = 0;
    const previous = this.previous;
    if (previous) {
      const length = Math.min(previous.length, pixels.length);
      for (let index = 0; index < length; index += 1) {
        if ((pixels[index] ?? 0) - (previous[index] ?? 0) >= pixelThreshold) brightened += 1;
      }
    }
    const brightenedRatio = pixels.length > 0 ? brightened / pixels.length : 0;
    const threshold = this.threshold();
    const signalAgreement = delta >= threshold && brightenedRatio >= 0.08;
    const outsideCooldown = timestamp - this.lastDetection >= this.cooldownMs;
    const detected = signalAgreement && outsideCooldown;
    if (detected) this.lastDetection = timestamp;

    if (!detected && Math.abs(delta) < threshold) {
      this.baseline += this.baselineAlpha * delta;
    }
    this.previous = pixels.slice();
    return this.result(detected, false, mean, delta, brightenedRatio);
  }

  brightestRecentFrame(): RecentFrame<T> | undefined {
    return this.recent.reduce<RecentFrame<T> | undefined>(
      (best, item) => (!best || item.luminance > best.luminance ? item : best),
      undefined,
    );
  }

  private threshold(): number {
    return Math.max(8, 30 - this.sensitivity * 20 + this.noise * 3);
  }

  private result(
    detected: boolean,
    calibrating: boolean,
    mean: number,
    delta: number,
    brightenedRatio: number,
  ): DetectionResult {
    return {
      detected,
      calibrating,
      mean,
      baseline: this.baseline,
      delta,
      brightenedRatio,
      threshold: this.threshold(),
    };
  }
}

function average(values: ArrayLike<number>): number {
  if (values.length === 0) return 0;
  let total = 0;
  for (let index = 0; index < values.length; index += 1) total += values[index] ?? 0;
  return total / values.length;
}

function standardDeviation(values: ArrayLike<number>, mean: number): number {
  if (values.length < 2) return 0;
  let sum = 0;
  for (let index = 0; index < values.length; index += 1) {
    const distance = (values[index] ?? 0) - mean;
    sum += distance * distance;
  }
  return Math.sqrt(sum / values.length);
}

function clamp(value: number, min: number, max: number): number {
  return Math.min(max, Math.max(min, value));
}
