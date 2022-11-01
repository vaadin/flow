import { html, LitElement, css } from "lit";

/**
 * Docs docs and more docs
 *
 * @customElement
 * @polymer
 */
class LightDom extends LitElement {
  render() {
    return html` <div class="title">Upgrade to Enterprise</div> `;
  }

  static get is() {
    return "light-dom";
  }

  // This allows us to keep the view element in the light DOM.
  createRenderRoot() {
    // Do not use a shadow root
    return this;
  }
}

customElements.define(LightDom.is, LightDom);
