interface ImageCaptureLike {
  takePhoto(): Promise<Blob>;
}

declare class ImageCapture implements ImageCaptureLike {
  constructor(track: MediaStreamTrack);
  takePhoto(): Promise<Blob>;
}
