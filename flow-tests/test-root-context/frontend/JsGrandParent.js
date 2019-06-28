import {PolymerElement} from '@polymer/polymer/polymer-element.js';
import {html} from '@polymer/polymer/lib/utils/html-tag.js';

class JsGrandTemplate extends PolymerElement {
  static get is() {
    return 'js-grand-parent'
  }

  updateSubTempate(){
    this.$['sub-template'].foo='baz';
  }

  static get template() {
    return html`
      <js-sub-template id='sub-template'></js-sub-template>
    `;
  }
}
customElements.define(JsGrandTemplate.is, JsGrandTemplate);
