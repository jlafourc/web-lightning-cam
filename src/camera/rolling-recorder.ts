import { RollingBuffer } from '../core/rolling-buffer';

interface RecorderLike {
  state: string;
  start(timeslice?: number): void;
  stop(): void;
  addEventListener(type: 'dataavailable', listener: (event: BlobEvent) => void): void;
}

interface RollingRecorderOptions {
  stream: MediaStream;
  preTriggerMs?: number;
  timesliceMs?: number;
  createRecorder?: (stream: MediaStream, options: MediaRecorderOptions) => RecorderLike;
  now?: () => number;
}

export function selectRecorderMimeType(supports: (type: string) => boolean): string {
  const candidates = [
    'video/mp4;codecs=h264',
    'video/mp4',
    'video/webm;codecs=vp9',
    'video/webm',
  ];
  return candidates.find(supports) ?? '';
}

export class RollingRecorder {
  private readonly recorder: RecorderLike;
  private readonly buffer: RollingBuffer<Blob>;
  private readonly timesliceMs: number;
  private readonly now: () => number;
  private activeCapture?: Blob[];
  readonly mimeType: string;

  constructor(options: RollingRecorderOptions) {
    this.timesliceMs = options.timesliceMs ?? 500;
    this.now = options.now ?? (() => performance.now());
    this.buffer = new RollingBuffer(options.preTriggerMs ?? 4000);
    const supports = typeof MediaRecorder !== 'undefined' && typeof MediaRecorder.isTypeSupported === 'function'
      ? MediaRecorder.isTypeSupported.bind(MediaRecorder)
      : () => false;
    this.mimeType = selectRecorderMimeType(supports);
    const createRecorder = options.createRecorder
      ?? ((stream: MediaStream, recorderOptions: MediaRecorderOptions) => new MediaRecorder(stream, recorderOptions));
    this.recorder = createRecorder(options.stream, this.mimeType ? { mimeType: this.mimeType } : {});
    this.recorder.addEventListener('dataavailable', (event) => {
      if (!event.data || event.data.size === 0) return;
      this.buffer.push(event.data, this.now());
      this.activeCapture?.push(event.data);
    });
  }

  start(): void {
    if (this.recorder.state === 'inactive') this.recorder.start(this.timesliceMs);
  }

  async captureClip(postTriggerMs = 3000): Promise<Blob> {
    const before = this.buffer.snapshot().map((entry) => entry.value);
    this.activeCapture = [];
    await new Promise<void>((resolve) => window.setTimeout(resolve, postTriggerMs));
    const chunks = [...before, ...this.activeCapture];
    this.activeCapture = undefined;
    const type = this.mimeType || chunks[0]?.type || 'video/mp4';
    return new Blob(chunks, { type });
  }

  stop(): void {
    if (this.recorder.state !== 'inactive') this.recorder.stop();
    this.buffer.clear();
    this.activeCapture = undefined;
  }
}
