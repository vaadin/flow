import { html, PolymerElement } from "@polymer/polymer/polymer-element.js";

/**
 * Docs docs and more docs
 *
 * @customElement
 * @polymer
 */
class LightDom extends PolymerElement {
  static get template() {
    return html`
          <div class="title">Upgrade to Enterprise</div>
    `;
  }

  static get is() {
    return "light-dom";
  }

  // This allows us to keep the view element in the light DOM.
  _attachDom(dom) {
    this.appendChild(dom);
  }
}

customElements.define(LightDom.is, LightDom);
