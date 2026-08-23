import type { LightningEvent } from '../storage/event-store';

interface CaptureDependencies {
  takePhoto: () => Promise<Blob>;
  captureClip: () => Promise<Blob | null>;
  save: (event: LightningEvent) => Promise<void>;
  now?: () => number;
  createId?: () => string;
}

export class CaptureCoordinator {
  private armed = false;
  private capturing = false;

  constructor(private readonly dependencies: CaptureDependencies) {}

  arm(): void {
    this.armed = true;
  }

  disarm(): void {
    this.armed = false;
  }

  isCapturing(): boolean {
    return this.capturing;
  }

  async capture(bestFrame: Blob, mean: number, focus: string): Promise<LightningEvent | undefined> {
    if (!this.armed || this.capturing) return undefined;
    this.capturing = true;
    try {
      const [photoResult, clipResult] = await Promise.allSettled([
        this.dependencies.takePhoto(),
        this.dependencies.captureClip(),
      ]);
      const createdAt = this.dependencies.now?.() ?? Date.now();
      const event: LightningEvent = {
        id: this.dependencies.createId?.() ?? createEventId(createdAt),
        createdAt,
        bestFrame,
        photo: photoResult.status === 'fulfilled' ? photoResult.value : null,
        clip: clipResult.status === 'fulfilled' ? clipResult.value : null,
        metadata: { mean: Math.round(mean), focus },
      };
      await this.dependencies.save(event);
      return event;
    } finally {
      this.capturing = false;
    }
  }
}

function createEventId(timestamp: number): string {
  return `flash-${timestamp}-${Math.random().toString(36).slice(2, 8)}`;
}
