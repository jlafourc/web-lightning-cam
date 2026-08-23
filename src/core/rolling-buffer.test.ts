import { describe, expect, test } from 'vitest';
import { RollingBuffer } from './rolling-buffer';

describe('RollingBuffer', () => {
  test('returns entries in chronological order', () => {
    const buffer = new RollingBuffer<string>(5000);
    buffer.push('late', 200);
    buffer.push('early', 100);
    expect(buffer.snapshot().map((entry) => entry.value)).toEqual(['early', 'late']);
  });

  test('evicts entries older than its duration relative to newest entry', () => {
    const buffer = new RollingBuffer<string>(1000);
    buffer.push('old', 0);
    buffer.push('edge', 1000);
    buffer.push('new', 1001);
    expect(buffer.snapshot().map((entry) => entry.value)).toEqual(['edge', 'new']);
  });

  test('returns an isolated snapshot', () => {
    const buffer = new RollingBuffer<string>(1000);
    buffer.push('safe', 0);
    const snapshot = buffer.snapshot();
    snapshot.length = 0;
    expect(buffer.snapshot()).toHaveLength(1);
  });

  test('clears all entries', () => {
    const buffer = new RollingBuffer<string>(1000);
    buffer.push('value', 0);
    buffer.clear();
    expect(buffer.snapshot()).toEqual([]);
  });
});
