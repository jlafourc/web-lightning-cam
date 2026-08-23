import { describe, expect, test, vi } from 'vitest';
import { registerServiceWorker } from './register-service-worker';

describe('registerServiceWorker', () => {
  test('returns unsupported when service workers are unavailable', async () => {
    await expect(registerServiceWorker(undefined)).resolves.toBe('unsupported');
  });

  test('registers the worker relative to the deployed app', async () => {
    const register = vi.fn().mockResolvedValue({ waiting: null });
    await expect(registerServiceWorker({ register } as unknown as ServiceWorkerContainer)).resolves.toBe('ready');
    expect(register).toHaveBeenCalledWith('./sw.js');
  });

  test('reports an update when a worker is already waiting', async () => {
    const register = vi.fn().mockResolvedValue({ waiting: {} });
    await expect(registerServiceWorker({ register } as unknown as ServiceWorkerContainer)).resolves.toBe('update-ready');
  });
});
