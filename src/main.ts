import './styles.css';
import { LightningApp } from './app/app';
import { registerServiceWorker } from './pwa/register-service-worker';

const root = document.querySelector<HTMLElement>('#app');
if (root) {
  new LightningApp(root).mount();
}

if (import.meta.env.PROD) void registerServiceWorker();
