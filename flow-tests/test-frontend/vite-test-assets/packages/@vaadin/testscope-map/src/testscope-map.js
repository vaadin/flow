import { default as lib } from '@vaadin/testscope-map/src/lib.js';

export class Map extends HTMLElement {
  static get is() {
    return 'testscope-map';
  }

  connectedCallback() {
    if (!this.textContent) {
      this.textContent = `testscope-${lib.MAP} NOT from bundle`;
    }
  }

  get isFromBundle() {
    return false;
  }
}

customElements.define('testscope-map', Map);
