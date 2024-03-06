/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
import { default as lib } from '@testscope/map/src/lib.js';

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
