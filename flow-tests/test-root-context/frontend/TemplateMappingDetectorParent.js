import {PolymerElement} from '@polymer/polymer/polymer-element.js';
import {html} from '@polymer/polymer/lib/utils/html-tag.js';

class TemplateMappingDetectorParent extends PolymerElement {
  static get is() { return 'template-mapping-detector-parent' }

  static get template() {
    return html`
       <template-mapping-detector id="detector"></template-mapping-detector>
    `;
  }
}
customElements.define(TemplateMappingDetectorParent.is, TemplateMappingDetectorParent);
