type CapabilityState = 'available' | 'unsupported';
export type FocusState = 'locked' | 'automatic' | 'unsupported';

export interface CameraReport {
  focus: CapabilityState;
  exposure: CapabilityState;
  iso: CapabilityState;
  zoom: CapabilityState;
  settings: MediaTrackSettings;
}

interface CameraDependencies {
  getUserMedia: (constraints: MediaStreamConstraints) => Promise<MediaStream>;
  createImageCapture?: (track: MediaStreamTrack) => ImageCaptureLike;
}

type ExtendedCapabilities = MediaTrackCapabilities & Record<string, unknown>;

export class CameraController {
  private stream?: MediaStream;
  private track?: MediaStreamTrack;
  private capabilities: ExtendedCapabilities = {};

  constructor(private readonly dependencies: CameraDependencies = browserDependencies()) {}

  async open(): Promise<MediaStream> {
    this.close();
    this.stream = await this.dependencies.getUserMedia({
      audio: false,
      video: {
        facingMode: { ideal: 'environment' },
        width: { ideal: 3840 },
        height: { ideal: 2160 },
        frameRate: { ideal: 30, max: 60 },
      },
    });
    const track = this.stream.getVideoTracks()[0];
    if (!track) throw new Error('Aucune piste vidéo disponible.');
    this.track = track;
    this.capabilities = typeof track.getCapabilities === 'function'
      ? (track.getCapabilities() as ExtendedCapabilities)
      : {};
    return this.stream;
  }

  report(): CameraReport {
    return {
      focus: hasCapability(this.capabilities, 'focusMode'),
      exposure: hasCapability(this.capabilities, 'exposureMode'),
      iso: hasCapability(this.capabilities, 'iso'),
      zoom: hasCapability(this.capabilities, 'zoom'),
      settings: this.track?.getSettings() ?? {},
    };
  }

  async focusAt(x: number, y: number): Promise<FocusState> {
    if (!this.track || !('focusMode' in this.capabilities)) return 'unsupported';
    const advanced: Record<string, unknown> = {};
    const focusModes = this.capabilities.focusMode;
    if (Array.isArray(focusModes)) {
      advanced.focusMode = focusModes.includes('manual') ? 'manual' : 'single-shot';
    }
    if ('pointsOfInterest' in this.capabilities) {
      advanced.pointsOfInterest = [{ x: clamp(x), y: clamp(y) }];
    }
    try {
      await this.track.applyConstraints({ advanced: [advanced as MediaTrackConstraintSet] });
      return advanced.focusMode === 'manual' ? 'locked' : 'automatic';
    } catch {
      return 'automatic';
    }
  }

  async takePhoto(): Promise<Blob> {
    if (!this.track) throw new Error('La caméra n’est pas ouverte.');
    const capture = this.dependencies.createImageCapture?.(this.track);
    if (!capture) throw new Error('La photo haute définition n’est pas disponible dans ce navigateur.');
    return capture.takePhoto();
  }

  getStream(): MediaStream {
    if (!this.stream) throw new Error('La caméra n’est pas ouverte.');
    return this.stream;
  }

  close(): void {
    this.stream?.getTracks().forEach((track) => track.stop());
    this.stream = undefined;
    this.track = undefined;
    this.capabilities = {};
  }
}

function browserDependencies(): CameraDependencies {
  return {
    getUserMedia: (constraints) => navigator.mediaDevices.getUserMedia(constraints),
    createImageCapture: typeof ImageCapture === 'undefined' ? undefined : (track) => new ImageCapture(track),
  };
}

function hasCapability(capabilities: ExtendedCapabilities, name: string): CapabilityState {
  return name in capabilities ? 'available' : 'unsupported';
}

function clamp(value: number): number {
  return Math.max(0, Math.min(1, value));
}
