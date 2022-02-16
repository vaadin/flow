export class Button extends HTMLElement {
  static get is() {
    return 'bundle-button';
  }

  get isFromBundle() {
    return false;
  }
}

customElements.define('bundle-button', Button);
