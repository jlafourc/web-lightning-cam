import { describe, expect, test, vi } from 'vitest';
import { CaptureCoordinator } from './capture-coordinator';

describe('CaptureCoordinator', () => {
  test('arms, captures photo and clip, then saves one event', async () => {
    const save = vi.fn().mockResolvedValue(undefined);
    const coordinator = new CaptureCoordinator({
      takePhoto: vi.fn().mockResolvedValue(new Blob(['photo'])),
      captureClip: vi.fn().mockResolvedValue(new Blob(['clip'])),
      save,
      now: () => 1234,
      createId: () => 'event-1',
    });
    coordinator.arm();
    const captured = await coordinator.capture(new Blob(['frame']), 88, 'locked');
    expect(captured?.id).toBe('event-1');
    expect(save).toHaveBeenCalledWith(expect.objectContaining({
      id: 'event-1',
      photo: expect.any(Blob),
      clip: expect.any(Blob),
    }));
  });

  test('does not capture while disarmed', async () => {
    const save = vi.fn();
    const coordinator = new CaptureCoordinator({
      takePhoto: vi.fn(), captureClip: vi.fn(), save,
    });
    await expect(coordinator.capture(new Blob(), 0, 'automatic')).resolves.toBeUndefined();
    expect(save).not.toHaveBeenCalled();
  });

  test('saves the best frame when still capture is unsupported', async () => {
    const save = vi.fn().mockResolvedValue(undefined);
    const coordinator = new CaptureCoordinator({
      takePhoto: vi.fn().mockRejectedValue(new Error('unsupported')),
      captureClip: vi.fn().mockResolvedValue(new Blob(['clip'])),
      save,
      createId: () => 'fallback',
    });
    coordinator.arm();
    const event = await coordinator.capture(new Blob(['frame']), 50, 'unsupported');
    expect(event?.photo).toBeNull();
    expect(event?.clip).toBeInstanceOf(Blob);
  });

  test('disarm prevents subsequent captures', async () => {
    const save = vi.fn();
    const coordinator = new CaptureCoordinator({ takePhoto: vi.fn(), captureClip: vi.fn(), save });
    coordinator.arm();
    coordinator.disarm();
    await coordinator.capture(new Blob(), 0, 'automatic');
    expect(save).not.toHaveBeenCalled();
  });
});
