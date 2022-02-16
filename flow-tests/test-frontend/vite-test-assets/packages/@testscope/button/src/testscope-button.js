export class Button extends HTMLElement {
  static get is() {
    return 'bundle-button';
  }

  connectedCallback() {
    if (!this.textContent) {
      this.textContent = 'bundle-button NOT from bundle';
    }
  }

  get isFromBundle() {
    return false;
  }
}

customElements.define('bundle-button', Button);
