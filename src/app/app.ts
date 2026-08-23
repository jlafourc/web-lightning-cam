import { CameraController, type FocusState } from '../camera/camera-controller';
import { RollingRecorder } from '../camera/rolling-recorder';
import { LightningDetector } from '../core/lightning-detector';
import { EventStore, type LightningEvent } from '../storage/event-store';
import { CaptureCoordinator } from './capture-coordinator';
import { downloadBlob, formatDate, requiredElement } from './dom';

interface WakeLockSentinelLike { release(): Promise<void>; }

export class LightningApp {
  private readonly camera = new CameraController();
  private readonly store = new EventStore();
  private detector = new LightningDetector({ calibrationFrames: 45 });
  private recorder?: RollingRecorder;
  private coordinator?: CaptureCoordinator;
  private focusState: FocusState = 'automatic';
  private animationFrame?: number;
  private lastAnalysis = 0;
  private wakeLock?: WakeLockSentinelLike;
  private objectUrls: string[] = [];
  private eventCount = 0;

  private video!: HTMLVideoElement;
  private analysisCanvas!: HTMLCanvasElement;
  private status!: HTMLElement;
  private meter!: HTMLProgressElement;
  private armButton!: HTMLButtonElement;
  private stopButton!: HTMLButtonElement;
  private sensitivity!: HTMLInputElement;
  private gallery!: HTMLElement;

  constructor(private readonly root: HTMLElement) {}

  mount(): void {
    this.root.innerHTML = template();
    this.video = requiredElement(this.root, '#camera-preview');
    this.analysisCanvas = requiredElement(this.root, '#analysis-canvas');
    this.status = requiredElement(this.root, '#status-text');
    this.meter = requiredElement(this.root, '#light-meter');
    this.armButton = requiredElement(this.root, '#arm-button');
    this.stopButton = requiredElement(this.root, '#stop-button');
    this.sensitivity = requiredElement(this.root, '#sensitivity');
    this.gallery = requiredElement(this.root, '#gallery-list');

    requiredElement<HTMLButtonElement>(this.root, '#camera-button').addEventListener('click', () => void this.startCamera());
    this.armButton.addEventListener('click', () => void this.arm());
    this.stopButton.addEventListener('click', () => this.disarm());
    this.sensitivity.addEventListener('input', () => this.detector.setSensitivity(Number(this.sensitivity.value)));
    this.video.addEventListener('pointerup', (event) => void this.focus(event));
    requiredElement<HTMLButtonElement>(this.root, '#clear-button').addEventListener('click', () => void this.clearGallery());
    document.addEventListener('visibilitychange', () => this.handleVisibility());
    void this.renderGallery();
  }

  private async startCamera(): Promise<void> {
    this.setStatus('Ouverture de la caméra arrière…');
    try {
      const stream = await this.camera.open();
      this.video.srcObject = stream;
      await this.video.play();
      const report = this.camera.report();
      this.renderCapabilities(report);
      this.armButton.disabled = false;
      requiredElement<HTMLElement>(this.root, '#preview-shell').classList.add('is-live');
      this.setStatus('Caméra prête. Touche une zone lointaine pour la mise au point.');
    } catch (error) {
      this.showError(cameraErrorMessage(error));
    }
  }

  private async focus(event: PointerEvent): Promise<void> {
    const bounds = this.video.getBoundingClientRect();
    if (!bounds.width || !bounds.height) return;
    const x = (event.clientX - bounds.left) / bounds.width;
    const y = (event.clientY - bounds.top) / bounds.height;
    this.focusState = await this.camera.focusAt(x, y);
    const marker = requiredElement<HTMLElement>(this.root, '#focus-marker');
    marker.style.left = `${x * 100}%`;
    marker.style.top = `${y * 100}%`;
    marker.classList.add('visible');
    requiredElement(this.root, '#focus-value').textContent = labelCapability(this.focusState);
    this.setStatus(this.focusState === 'locked' ? 'Mise au point verrouillée.' : 'Mise au point laissée en automatique par Safari.');
  }

  private async arm(): Promise<void> {
    if (this.video.readyState < HTMLMediaElement.HAVE_CURRENT_DATA) return;
    this.detector = new LightningDetector({
      calibrationFrames: 45,
      sensitivity: Number(this.sensitivity.value),
      cooldownMs: 1800,
    });
    try {
      this.recorder = typeof MediaRecorder === 'undefined' ? undefined : new RollingRecorder({ stream: this.camera.getStream() });
      this.recorder?.start();
      this.coordinator = new CaptureCoordinator({
        takePhoto: () => this.camera.takePhoto(),
        captureClip: () => this.recorder ? this.recorder.captureClip(3000) : Promise.resolve(null),
        save: (event) => this.store.save(event),
      });
      this.coordinator.arm();
      this.armButton.hidden = true;
      this.stopButton.hidden = false;
      this.root.classList.add('armed');
      this.eventCount = 0;
      this.updateEventCount();
      await this.acquireWakeLock();
      this.setStatus('Calibration de la nuit… garde l’iPhone immobile.');
      this.analyze();
    } catch (error) {
      this.showError(errorMessage(error));
    }
  }

  private disarm(): void {
    if (this.animationFrame !== undefined) cancelAnimationFrame(this.animationFrame);
    this.animationFrame = undefined;
    this.recorder?.stop();
    this.recorder = undefined;
    this.coordinator?.disarm();
    this.coordinator = undefined;
    void this.wakeLock?.release();
    this.wakeLock = undefined;
    this.armButton.hidden = false;
    this.stopButton.hidden = true;
    this.root.classList.remove('armed');
    this.setStatus('Surveillance arrêtée.');
  }

  private analyze = (timestamp = performance.now()): void => {
    this.animationFrame = requestAnimationFrame(this.analyze);
    if (timestamp - this.lastAnalysis < 66 || this.video.readyState < HTMLMediaElement.HAVE_CURRENT_DATA) return;
    this.lastAnalysis = timestamp;
    const context = this.analysisCanvas.getContext('2d', { willReadFrequently: true });
    if (!context) return;
    context.drawImage(this.video, 0, 0, this.analysisCanvas.width, this.analysisCanvas.height);
    const rgba = context.getImageData(0, 0, this.analysisCanvas.width, this.analysisCanvas.height).data;
    const luminance = new Uint8Array(rgba.length / 4);
    for (let source = 0, target = 0; source < rgba.length; source += 4, target += 1) {
      luminance[target] = Math.round((rgba[source] ?? 0) * 0.2126 + (rgba[source + 1] ?? 0) * 0.7152 + (rgba[source + 2] ?? 0) * 0.0722);
    }
    const result = this.detector.analyze(luminance, timestamp);
    this.meter.value = Math.min(255, result.mean);
    requiredElement(this.root, '#meter-value').textContent = `${Math.round(result.mean)}`;
    if (result.calibrating) {
      this.setStatus('Calibration de la nuit…');
    } else if (!this.coordinator?.isCapturing()) {
      this.setStatus('Armé · en attente d’un éclair');
    }
    if (result.detected) void this.capture(result.mean);
  };

  private async capture(mean: number): Promise<void> {
    if (!this.coordinator) return;
    this.setStatus('Éclair détecté · capture en cours…');
    try {
      const bestFrame = await frameBlob(this.video);
      const event = await this.coordinator.capture(bestFrame, mean, this.focusState);
      if (event) {
        this.eventCount += 1;
        this.updateEventCount();
        await this.renderGallery();
        if ('vibrate' in navigator) navigator.vibrate?.(60);
      }
    } catch (error) {
      this.showError(errorMessage(error));
    }
  }

  private renderCapabilities(report: ReturnType<CameraController['report']>): void {
    requiredElement(this.root, '#focus-value').textContent = labelCapability(report.focus);
    requiredElement(this.root, '#exposure-value').textContent = labelCapability(report.exposure);
    requiredElement(this.root, '#iso-value').textContent = labelCapability(report.iso);
    requiredElement(this.root, '#resolution-value').textContent = report.settings.width
      ? `${report.settings.width} × ${report.settings.height}`
      : 'Automatique';
  }

  private async renderGallery(): Promise<void> {
    this.objectUrls.forEach((url) => URL.revokeObjectURL(url));
    this.objectUrls = [];
    const events = await this.store.list();
    this.gallery.replaceChildren();
    requiredElement(this.root, '#empty-gallery').toggleAttribute('hidden', events.length > 0);
    for (const event of events) this.gallery.append(this.eventCard(event));
  }

  private eventCard(event: LightningEvent): HTMLElement {
    const article = document.createElement('article');
    article.className = 'event-card';
    const imageUrl = URL.createObjectURL(event.photo ?? event.bestFrame);
    this.objectUrls.push(imageUrl);
    article.innerHTML = `
      <img src="${imageUrl}" alt="Capture d’éclair du ${formatDate(event.createdAt)}" />
      <div class="event-body">
        <div><strong>${formatDate(event.createdAt)}</strong><small>Lumière ${event.metadata.mean} · focus ${event.metadata.focus}</small></div>
        <div class="event-actions"></div>
      </div>`;
    const actions = requiredElement<HTMLElement>(article, '.event-actions');
    actions.append(
      actionButton('Photo', () => this.exportBlob(event.photo ?? event.bestFrame, `lightning-${event.createdAt}.jpg`)),
      ...(event.clip ? [actionButton('Clip', () => this.exportBlob(event.clip!, `lightning-${event.createdAt}.mp4`))] : []),
      actionButton('Supprimer', () => void this.deleteEvent(event.id), 'danger'),
    );
    return article;
  }

  private async exportBlob(blob: Blob, filename: string): Promise<void> {
    const file = new File([blob], filename, { type: blob.type });
    const shareData = { files: [file], title: 'Lightning Cam' };
    if (navigator.canShare?.(shareData)) {
      try { await navigator.share(shareData); return; } catch { /* User cancelled or Safari refused. */ }
    }
    downloadBlob(blob, filename);
  }

  private async deleteEvent(id: string): Promise<void> {
    await this.store.delete(id);
    await this.renderGallery();
  }

  private async clearGallery(): Promise<void> {
    await this.store.clear();
    await this.renderGallery();
  }

  private async acquireWakeLock(): Promise<void> {
    const wakeLock = (navigator as Navigator & { wakeLock?: { request(type: 'screen'): Promise<WakeLockSentinelLike> } }).wakeLock;
    try { this.wakeLock = await wakeLock?.request('screen'); } catch { this.wakeLock = undefined; }
  }

  private handleVisibility(): void {
    if (document.hidden || !this.root.classList.contains('armed')) return;
    void this.acquireWakeLock();
  }

  private updateEventCount(): void {
    requiredElement(this.root, '#event-count').textContent = `${this.eventCount}`;
  }

  private setStatus(message: string): void {
    this.status.textContent = message;
    this.status.classList.remove('error');
  }

  private showError(message: string): void {
    this.status.textContent = message;
    this.status.classList.add('error');
  }
}

function template(): string {
  return `
    <header class="topbar"><div><span class="eyebrow">NIGHT CAPTURE</span><h1>Lightning Cam</h1></div><span class="privacy-dot">100 % local</span></header>
    <section class="camera-stage" aria-labelledby="status-text">
      <div class="preview-shell" id="preview-shell">
        <video id="camera-preview" autoplay muted playsinline></video>
        <canvas id="analysis-canvas" width="96" height="54" hidden></canvas>
        <span id="focus-marker" class="focus-marker" aria-hidden="true"></span>
        <div class="preview-placeholder"><span class="bolt">ϟ</span><p>Caméra arrière</p></div>
        <div class="hud"><span><i></i> LIVE</span><span>1× · MAIN</span></div>
      </div>
      <div class="status-line"><span id="status-text">Pose l’iPhone sur un trépied, puis démarre la caméra.</span></div>
      <div class="primary-actions">
        <button id="camera-button" class="button secondary">Démarrer la caméra</button>
        <button id="arm-button" class="button primary" disabled>Armer la détection</button>
        <button id="stop-button" class="button stop" hidden>Arrêter</button>
      </div>
    </section>
    <section class="control-grid">
      <article class="panel sensitivity-panel">
        <div class="panel-heading"><div><span class="eyebrow">DÉTECTION</span><h2>Sensibilité</h2></div><output id="meter-value">0</output></div>
        <input id="sensitivity" type="range" min="0" max="1" step="0.05" value="0.65" aria-label="Sensibilité de détection" />
        <div class="range-labels"><span>Moins de faux positifs</span><span>Plus sensible</span></div>
        <progress id="light-meter" max="255" value="0"></progress>
      </article>
      <article class="panel capability-panel">
        <span class="eyebrow">CAMÉRA RÉELLE</span><h2>Contrôles Safari</h2>
        <dl><div><dt>Focus</dt><dd id="focus-value">À tester</dd></div><div><dt>Exposition</dt><dd id="exposure-value">À tester</dd></div><div><dt>ISO</dt><dd id="iso-value">À tester</dd></div><div><dt>Flux</dt><dd id="resolution-value">—</dd></div></dl>
      </article>
      <article class="panel count-panel"><span class="eyebrow">SESSION</span><strong id="event-count">0</strong><span>éclairs capturés</span></article>
    </section>
    <section class="gallery-section">
      <div class="section-heading"><div><span class="eyebrow">SUR CET IPHONE</span><h2>Captures</h2></div><button id="clear-button" class="text-button">Tout effacer</button></div>
      <p id="empty-gallery" class="empty-gallery">Les photos et clips apparaîtront ici. Exporte les meilleurs : iOS peut supprimer les données d’un site.</p>
      <div id="gallery-list" class="gallery-list"></div>
    </section>
    <footer>Hors ligne après la première visite · aucune donnée envoyée</footer>`;
}

function frameBlob(video: HTMLVideoElement): Promise<Blob> {
  const canvas = document.createElement('canvas');
  canvas.width = video.videoWidth || 1920;
  canvas.height = video.videoHeight || 1080;
  canvas.getContext('2d')?.drawImage(video, 0, 0, canvas.width, canvas.height);
  return new Promise((resolve, reject) => canvas.toBlob(
    (blob) => blob ? resolve(blob) : reject(new Error('Impossible de créer la trame vidéo.')),
    'image/jpeg',
    0.95,
  ));
}

function actionButton(label: string, action: () => void, className = ''): HTMLButtonElement {
  const button = document.createElement('button');
  button.className = `chip-button ${className}`;
  button.textContent = label;
  button.addEventListener('click', action);
  return button;
}

function labelCapability(value: string): string {
  if (value === 'available') return 'Disponible';
  if (value === 'locked') return 'Verrouillé';
  if (value === 'automatic') return 'Automatique';
  return 'Non exposé';
}

function cameraErrorMessage(error: unknown): string {
  if (error instanceof DOMException && error.name === 'NotAllowedError') return 'Accès caméra refusé. Autorise la caméra dans les réglages Safari puis réessaie.';
  if (!window.isSecureContext) return 'La caméra exige une page HTTPS. Ouvre la version GitHub Pages.';
  return `Caméra indisponible : ${errorMessage(error)} Essaie aussi directement dans Safari si la PWA installée bloque.`;
}

function errorMessage(error: unknown): string {
  return error instanceof Error ? error.message : 'Une erreur inattendue est survenue.';
}
