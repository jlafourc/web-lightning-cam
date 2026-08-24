interface RecorderLike {
  state: string;
  start(timeslice?: number): void;
  stop(): void;
  requestData?(): void;
  addEventListener(type: string, listener: (event: BlobEvent | Event) => void): void;
}

interface RollingRecorderOptions {
  stream: MediaStream;
  preTriggerMs?: number;
  timesliceMs?: number;
  createRecorder?: (stream: MediaStream, options: MediaRecorderOptions) => RecorderLike;
}

interface RecordingSession {
  recorder: RecorderLike;
  chunks: Blob[];
  stopped: Promise<void>;
  resolveStopped: () => void;
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
  private readonly stream: MediaStream;
  private readonly preTriggerMs: number;
  private readonly timesliceMs: number;
  private readonly createRecorder: (stream: MediaStream, options: MediaRecorderOptions) => RecorderLike;
  private session?: RecordingSession;
  private rotationTimer?: number;
  private running = false;
  private capturing = false;
  readonly mimeType: string;

  constructor(options: RollingRecorderOptions) {
    this.stream = options.stream;
    this.preTriggerMs = options.preTriggerMs ?? 4000;
    this.timesliceMs = options.timesliceMs ?? 500;
    const supports = typeof MediaRecorder !== 'undefined' && typeof MediaRecorder.isTypeSupported === 'function'
      ? MediaRecorder.isTypeSupported.bind(MediaRecorder)
      : () => false;
    this.mimeType = selectRecorderMimeType(supports);
    this.createRecorder = options.createRecorder
      ?? ((stream: MediaStream, recorderOptions: MediaRecorderOptions) => new MediaRecorder(stream, recorderOptions));
  }

  start(): void {
    if (this.running) return;
    this.running = true;
    this.startSession();
  }

  async captureClip(postTriggerMs = 3000): Promise<Blob> {
    if (!this.running || !this.session) throw new Error('L’enregistreur vidéo n’est pas démarré.');
    this.capturing = true;
    this.clearRotation();
    await new Promise<void>((resolve) => window.setTimeout(resolve, postTriggerMs));
    const completed = this.session;
    const clip = await this.finalize(completed);
    if (this.running) this.startSession();
    this.capturing = false;
    return clip;
  }

  stop(): void {
    this.running = false;
    this.capturing = false;
    this.clearRotation();
    const active = this.session;
    this.session = undefined;
    if (active && active.recorder.state !== 'inactive') active.recorder.stop();
  }

  private startSession(): void {
    const recorderOptions = this.mimeType ? { mimeType: this.mimeType } : {};
    const recorder = this.createRecorder(this.stream, recorderOptions);
    let resolveStopped: () => void = () => {};
    const stopped = new Promise<void>((resolve) => { resolveStopped = resolve; });
    const session: RecordingSession = { recorder, chunks: [], stopped, resolveStopped };
    recorder.addEventListener('dataavailable', (event) => {
      const data = 'data' in event ? event.data : undefined;
      if (data && data.size > 0) session.chunks.push(data);
    });
    recorder.addEventListener('stop', () => session.resolveStopped());
    this.session = session;
    recorder.start(this.timesliceMs);
    this.scheduleRotation();
  }

  private async rotate(): Promise<void> {
    if (!this.running || this.capturing || !this.session) return;
    const completed = this.session;
    await this.finalize(completed);
    if (this.running && !this.capturing) this.startSession();
  }

  private async finalize(session: RecordingSession): Promise<Blob> {
    this.clearRotation();
    if (session.recorder.state !== 'inactive') {
      session.recorder.requestData?.();
      session.recorder.stop();
    } else {
      session.resolveStopped();
    }
    await session.stopped;
    const type = this.mimeType || session.chunks[0]?.type || 'video/mp4';
    return new Blob(session.chunks, { type });
  }

  private scheduleRotation(): void {
    this.clearRotation();
    this.rotationTimer = window.setTimeout(() => void this.rotate(), this.preTriggerMs);
  }

  private clearRotation(): void {
    if (this.rotationTimer !== undefined) window.clearTimeout(this.rotationTimer);
    this.rotationTimer = undefined;
  }
}
