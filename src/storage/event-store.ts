import { IndexedDbBackend, type EventBackend } from './idb';

export interface LightningMetadata {
  mean: number;
  focus: string;
  [key: string]: string | number | boolean | null;
}

export interface LightningEvent {
  id: string;
  createdAt: number;
  bestFrame: Blob;
  photo: Blob | null;
  clip: Blob | null;
  metadata: LightningMetadata;
}

export class StorageFullError extends Error {
  constructor() {
    super('Le stockage local est plein. Exporte puis supprime d’anciennes captures.');
    this.name = 'StorageFullError';
  }
}

export class EventStore {
  constructor(private readonly backend: EventBackend<LightningEvent> = new IndexedDbBackend()) {}

  async save(event: LightningEvent): Promise<void> {
    try {
      await this.backend.put(event);
    } catch (error) {
      if (error instanceof DOMException && error.name === 'QuotaExceededError') throw new StorageFullError();
      throw error;
    }
  }

  get(id: string): Promise<LightningEvent | undefined> {
    return this.backend.get(id);
  }

  async list(): Promise<LightningEvent[]> {
    const events = await this.backend.getAll();
    return events.sort((left, right) => right.createdAt - left.createdAt);
  }

  delete(id: string): Promise<void> {
    return this.backend.delete(id);
  }

  clear(): Promise<void> {
    return this.backend.clear();
  }

  async estimate(): Promise<StorageEstimate | undefined> {
    return navigator.storage?.estimate?.();
  }
}

export class MemoryEventBackend implements EventBackend<LightningEvent> {
  private values = new Map<string, LightningEvent>();
  failNextWrite?: Error;

  async put(value: LightningEvent): Promise<void> {
    if (this.failNextWrite) {
      const error = this.failNextWrite;
      this.failNextWrite = undefined;
      throw error;
    }
    this.values.set(value.id, value);
  }

  async get(id: string): Promise<LightningEvent | undefined> {
    return this.values.get(id);
  }

  async getAll(): Promise<LightningEvent[]> {
    return [...this.values.values()];
  }

  async delete(id: string): Promise<void> {
    this.values.delete(id);
  }

  async clear(): Promise<void> {
    this.values.clear();
  }
}
