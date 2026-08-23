export interface TimedEntry<T> {
  value: T;
  timestamp: number;
}

export class RollingBuffer<T> {
  private entries: TimedEntry<T>[] = [];

  constructor(private readonly durationMs: number) {
    if (durationMs < 0) throw new RangeError('durationMs must be positive');
  }

  push(value: T, timestamp: number): void {
    this.entries.push({ value, timestamp });
    this.entries.sort((left, right) => left.timestamp - right.timestamp);
    const newest = this.entries.at(-1)?.timestamp ?? timestamp;
    const oldestAllowed = newest - this.durationMs;
    this.entries = this.entries.filter((entry) => entry.timestamp >= oldestAllowed);
  }

  snapshot(): TimedEntry<T>[] {
    return this.entries.map((entry) => ({ ...entry }));
  }

  clear(): void {
    this.entries = [];
  }
}
