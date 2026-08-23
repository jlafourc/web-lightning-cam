import { describe, expect, test } from 'vitest';
import { EventStore, MemoryEventBackend, StorageFullError, type LightningEvent } from './event-store';

const event = (id: string, createdAt: number): LightningEvent => ({
  id,
  createdAt,
  bestFrame: new Blob([id], { type: 'image/jpeg' }),
  photo: null,
  clip: new Blob(['clip'], { type: 'video/mp4' }),
  metadata: { mean: 42, focus: 'automatic' },
});

describe('EventStore', () => {
  test('creates and retrieves an event with its blobs', async () => {
    const store = new EventStore(new MemoryEventBackend());
    await store.save(event('flash-1', 100));
    expect(await store.get('flash-1')).toMatchObject({ id: 'flash-1', createdAt: 100 });
  });

  test('lists newest events first', async () => {
    const store = new EventStore(new MemoryEventBackend());
    await store.save(event('older', 100));
    await store.save(event('newer', 200));
    expect((await store.list()).map((item) => item.id)).toEqual(['newer', 'older']);
  });

  test('deletes one event', async () => {
    const store = new EventStore(new MemoryEventBackend());
    await store.save(event('keep', 100));
    await store.save(event('remove', 200));
    await store.delete('remove');
    expect((await store.list()).map((item) => item.id)).toEqual(['keep']);
  });

  test('clears every event', async () => {
    const store = new EventStore(new MemoryEventBackend());
    await store.save(event('one', 100));
    await store.clear();
    expect(await store.list()).toEqual([]);
  });

  test('converts browser quota failures to a useful storage error', async () => {
    const backend = new MemoryEventBackend();
    backend.failNextWrite = new DOMException('full', 'QuotaExceededError');
    const store = new EventStore(backend);
    await expect(store.save(event('full', 100))).rejects.toBeInstanceOf(StorageFullError);
  });
});
