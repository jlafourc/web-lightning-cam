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
  test('stops the recorder after the post-trigger duration', async () => {
    vi.useFakeTimers();
    const fixture = fakeRecorder('final');
    const rolling = new RollingRecorder({
      stream: {} as MediaStream,
      createRecorder: () => fixture.recorder,
    });
    rolling.start();
    const clipPromise = rolling.captureClip(1000);
    await vi.advanceTimersByTimeAsync(1000);
    expect(fixture.stop).toHaveBeenCalledOnce();
    await clipPromise;
  });

  test('waits for stop and includes Safari finalization data in the clip', async () => {
    vi.useFakeTimers();
    const fixture = fakeRecorder('final');
    const rolling = new RollingRecorder({
      stream: {} as MediaStream,
      createRecorder: () => fixture.recorder,
    });
    rolling.start();
    const clipPromise = rolling.captureClip(1000);
    await vi.advanceTimersByTimeAsync(1000);
    const clip = await clipPromise;
    expect(clip.size).toBe(5);
  });

  test('starts a fresh rolling session after finalizing a capture', async () => {
    vi.useFakeTimers();
    const fixtures = [fakeRecorder('first'), fakeRecorder('second')];
    const createRecorder = vi.fn(() => fixtures.shift()!.recorder);
    const rolling = new RollingRecorder({ stream: {} as MediaStream, createRecorder });
    rolling.start();
    const clipPromise = rolling.captureClip(1000);
    await vi.advanceTimersByTimeAsync(1000);
    await clipPromise;
    expect(createRecorder).toHaveBeenCalledTimes(2);
  });

  test('rotates and finalizes an old session to bound pre-trigger duration', async () => {
    vi.useFakeTimers();
    const first = fakeRecorder('old');
    const second = fakeRecorder('new');
    const fixtures = [first, second];
    const rolling = new RollingRecorder({
      stream: {} as MediaStream,
      preTriggerMs: 1000,
      createRecorder: () => fixtures.shift()!.recorder,
    });
    rolling.start();
    await vi.advanceTimersByTimeAsync(1000);
    expect(first.stop).toHaveBeenCalledOnce();
    expect(second.start).toHaveBeenCalledOnce();
    rolling.stop();
  });
});

function fakeRecorder(finalData: string) {
  const listeners: Record<string, Array<(event: { data?: Blob }) => void>> = {};
  const recorder = {
    state: 'inactive',
    start: vi.fn(function (this: { state: string }) { this.state = 'recording'; }),
    stop: vi.fn(function (this: { state: string }) {
      this.state = 'inactive';
      for (const listener of listeners.dataavailable ?? []) {
        listener({ data: new Blob([finalData], { type: 'video/mp4' }) });
      }
      for (const listener of listeners.stop ?? []) listener({});
    }),
    addEventListener: vi.fn((name: string, callback: (event: { data?: Blob }) => void) => {
      (listeners[name] ??= []).push(callback);
    }),
  };
  return { recorder: recorder as unknown as MediaRecorder, start: recorder.start, stop: recorder.stop };
}
