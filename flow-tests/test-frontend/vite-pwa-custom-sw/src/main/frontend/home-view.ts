import './home-view.css';

export class HomeView extends HTMLElement {
  #broadcastChannel?: BroadcastChannel;

  connectedCallback() {
    this.innerHTML = `<h1>Home Page</h1><output />`;
    this.#broadcastChannel = new BroadcastChannel('custom-sw');
    this.#broadcastChannel.onmessage = (e: MessageEvent) => {
      const text = String(e.data);
      const p = document.createElement('p');
      p.textContent = text;
      this.querySelector('output')!.appendChild(p);
    };
  }

  disconnectedCallback() {
    if (!this.#broadcastChannel) {
      return;
    }

    this.#broadcastChannel.onmessage = null;
    this.#broadcastChannel.close();
  }
}

customElements.define('home-view', HomeView);
