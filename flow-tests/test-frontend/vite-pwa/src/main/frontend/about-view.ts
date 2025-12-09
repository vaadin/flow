export class AboutView extends HTMLElement {
  connectedCallback() {
    this.innerHTML = `<h1>About Page</h1>`;
  }
}

customElements.define('about-view', AboutView);
