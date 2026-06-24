import {PolymerElement} from '@polymer/polymer/polymer-element.js';
import {html} from '@polymer/polymer/lib/utils/html-tag.js';

class JsSubTemplate extends PolymerElement {
  static get is() {
    return 'js-sub-template'
  }

  static get template() {
    return html`
      <div id="prop">[[foo]]</div>
      <js-injected-grand-child id="js-grand-child"></js-injected-grand-child>
    `;
  }
}
customElements.define(JsSubTemplate.is, JsSubTemplate);
