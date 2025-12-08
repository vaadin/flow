import { PolymerElement } from '@polymer/polymer/polymer-element.js';
import { html } from '@polymer/polymer/lib/utils/html-tag.js';

class PolymerTemplateWithoutShadowRootView extends PolymerElement {
  _attachDom(dom) {
    // Do not create a shadow root
    this.appendChild(dom);
  }

  static get properties() {
    return {};
  }
  static get is() {
    return 'template-without-shadow-root-view';
  }

  static get template() {
    return html`
      <div real="deal" id="content"></div>
      <div id="special!#id"></div>
      <div id="map"></div>
    `;
  }
}

customElements.define(PolymerTemplateWithoutShadowRootView.is, PolymerTemplateWithoutShadowRootView);
