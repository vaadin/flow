import {PolymerElement} from '@polymer/polymer/polymer-element.js';
import {html} from '@polymer/polymer/lib/utils/html-tag.js';

class TemplateMappingDetector extends PolymerElement {
  static get is() { return 'template-mapping-detector' }

  static get template() {
    return html`
       <div id="detector1"></div>
       <div id="container"></div>
    `;
  }
}
customElements.define(TemplateMappingDetector.is, TemplateMappingDetector);
