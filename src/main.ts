import './styles.css';
import { LightningApp } from './app/app';

const root = document.querySelector<HTMLElement>('#app');
if (root) {
  new LightningApp(root).mount();
}
