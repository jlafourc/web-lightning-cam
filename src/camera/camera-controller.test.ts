import { describe, expect, test, vi } from 'vitest';
import { CameraController } from './camera-controller';

function cameraFixture(capabilities: Record<string, unknown> = {}) {
  const applyConstraints = vi.fn().mockResolvedValue(undefined);
  const track = {
    getCapabilities: () => capabilities,
    getSettings: () => ({ width: 3840, height: 2160, facingMode: 'environment' }),
    applyConstraints,
    stop: vi.fn(),
  } as unknown as MediaStreamTrack;
  const stream = { getVideoTracks: () => [track], getTracks: () => [track] } as unknown as MediaStream;
  const getUserMedia = vi.fn().mockResolvedValue(stream);
  return { track, stream, getUserMedia, applyConstraints };
}

describe('CameraController', () => {
  test('requests the rear camera at high resolution', async () => {
    const fixture = cameraFixture();
    const camera = new CameraController({ getUserMedia: fixture.getUserMedia });
    await camera.open();
    expect(fixture.getUserMedia).toHaveBeenCalledWith({
      audio: false,
      video: expect.objectContaining({
        facingMode: { ideal: 'environment' },
        width: { ideal: 3840 },
        height: { ideal: 2160 },
      }),
    });
  });

  test('reports only capabilities exposed by the active track', async () => {
    const fixture = cameraFixture({ focusMode: ['continuous', 'manual'], iso: { min: 20, max: 1200 } });
    const camera = new CameraController({ getUserMedia: fixture.getUserMedia });
    await camera.open();
    expect(camera.report().focus).toBe('available');
    expect(camera.report().iso).toBe('available');
    expect(camera.report().exposure).toBe('unsupported');
  });

  test('applies supported focus constraints and ignores unsupported controls', async () => {
    const fixture = cameraFixture({ focusMode: ['manual'], pointsOfInterest: true });
    const camera = new CameraController({ getUserMedia: fixture.getUserMedia });
    await camera.open();
    const result = await camera.focusAt(0.25, 0.75);
    expect(result).toBe('locked');
    expect(fixture.applyConstraints).toHaveBeenCalledOnce();
  });

  test('returns unsupported instead of applying unavailable focus controls', async () => {
    const fixture = cameraFixture();
    const camera = new CameraController({ getUserMedia: fixture.getUserMedia });
    await camera.open();
    expect(await camera.focusAt(0.5, 0.5)).toBe('unsupported');
    expect(fixture.applyConstraints).not.toHaveBeenCalled();
  });

  test('uses ImageCapture when available for a still photo', async () => {
    const fixture = cameraFixture();
    const photo = new Blob(['photo'], { type: 'image/jpeg' });
    const camera = new CameraController({
      getUserMedia: fixture.getUserMedia,
      createImageCapture: () => ({ takePhoto: vi.fn().mockResolvedValue(photo) }),
    });
    await camera.open();
    await expect(camera.takePhoto()).resolves.toBe(photo);
  });
});
