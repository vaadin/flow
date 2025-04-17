import {PolymerElement} from '@polymer/polymer/polymer-element.js';
import {html} from '@polymer/polymer/lib/utils/html-tag.js';
import './JsInjectedElement.js';

class InjectsJsTemplate extends PolymerElement {
  static get is() { return 'injects-js-template' }

  static get template() {
    return html`
       <js-injected-template id="injected-template"></js-injected-template>
       <div id="injected-div"></div>
    `;
  }
}

customElements.define(InjectsJsTemplate.is, InjectsJsTemplate);
