export type ServiceWorkerStatus = 'unsupported' | 'ready' | 'update-ready' | 'failed';

export async function registerServiceWorker(
  container: ServiceWorkerContainer | undefined = navigator.serviceWorker,
): Promise<ServiceWorkerStatus> {
  if (!container) return 'unsupported';
  try {
    const registration = await container.register('./sw.js');
    return registration.waiting ? 'update-ready' : 'ready';
  } catch {
    return 'failed';
  }
}
