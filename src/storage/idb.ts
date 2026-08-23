export interface EventBackend<T extends { id: string }> {
  put(value: T): Promise<void>;
  get(id: string): Promise<T | undefined>;
  getAll(): Promise<T[]>;
  delete(id: string): Promise<void>;
  clear(): Promise<void>;
}

export class IndexedDbBackend<T extends { id: string }> implements EventBackend<T> {
  private database?: Promise<IDBDatabase>;

  constructor(
    private readonly databaseName = 'lightning-cam',
    private readonly storeName = 'events',
  ) {}

  async put(value: T): Promise<void> {
    await this.request('readwrite', (store) => store.put(value));
  }

  async get(id: string): Promise<T | undefined> {
    return this.request('readonly', (store) => store.get(id));
  }

  async getAll(): Promise<T[]> {
    return this.request('readonly', (store) => store.getAll());
  }

  async delete(id: string): Promise<void> {
    await this.request('readwrite', (store) => store.delete(id));
  }

  async clear(): Promise<void> {
    await this.request('readwrite', (store) => store.clear());
  }

  private open(): Promise<IDBDatabase> {
    if (this.database) return this.database;
    this.database = new Promise((resolve, reject) => {
      const request = indexedDB.open(this.databaseName, 1);
      request.onupgradeneeded = () => {
        if (!request.result.objectStoreNames.contains(this.storeName)) {
          request.result.createObjectStore(this.storeName, { keyPath: 'id' });
        }
      };
      request.onsuccess = () => resolve(request.result);
      request.onerror = () => reject(request.error ?? new Error('IndexedDB indisponible.'));
    });
    return this.database;
  }

  private async request<R>(
    mode: IDBTransactionMode,
    operation: (store: IDBObjectStore) => IDBRequest<R>,
  ): Promise<R> {
    const database = await this.open();
    return new Promise((resolve, reject) => {
      const transaction = database.transaction(this.storeName, mode);
      const request = operation(transaction.objectStore(this.storeName));
      request.onsuccess = () => resolve(request.result);
      request.onerror = () => reject(request.error ?? new Error('Échec du stockage local.'));
    });
  }
}
