export class Button extends HTMLElement {
  static get is() {
    return 'testscope-button';
  }

  connectedCallback() {
    if (!this.textContent) {
      this.textContent = 'testscope-button NOT from bundle';
    }
  }

  get isFromBundle() {
    return false;
  }
}

customElements.define('testscope-button', Button);
