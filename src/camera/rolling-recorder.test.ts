import { afterEach, describe, expect, test, vi } from 'vitest';
import { RollingRecorder, selectRecorderMimeType } from './rolling-recorder';

afterEach(() => vi.useRealTimers());

describe('selectRecorderMimeType', () => {
  test('prefers an iPhone-compatible MP4 type', () => {
    expect(selectRecorderMimeType((type) => type === 'video/mp4')).toBe('video/mp4');
  });

  test('falls back to a supported WebM type', () => {
    expect(selectRecorderMimeType((type) => type === 'video/webm')).toBe('video/webm');
  });
});

describe('RollingRecorder', () => {
  test('assembles buffered and post-trigger chunks', async () => {
    vi.useFakeTimers();
    const listeners: { data?: (event: { data: Blob }) => void } = {};
    const recorder = {
      state: 'inactive',
      start: vi.fn(function (this: { state: string }) { this.state = 'recording'; }),
      stop: vi.fn(),
      addEventListener: vi.fn((name: string, callback: (event: { data: Blob }) => void) => {
        if (name === 'dataavailable') listeners.data = callback;
      }),
    };
    const rolling = new RollingRecorder({
      stream: {} as MediaStream,
      preTriggerMs: 5000,
      createRecorder: () => recorder as unknown as MediaRecorder,
      now: () => Date.now(),
    });
    rolling.start();
    listeners.data?.({ data: new Blob(['before'], { type: 'video/mp4' }) });
    const clipPromise = rolling.captureClip(1000);
    vi.advanceTimersByTime(500);
    listeners.data?.({ data: new Blob(['after'], { type: 'video/mp4' }) });
    vi.advanceTimersByTime(500);
    const clip = await clipPromise;
    expect(clip.size).toBe(11);
  });
});
