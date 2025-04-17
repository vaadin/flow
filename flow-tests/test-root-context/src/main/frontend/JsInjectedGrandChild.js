import {PolymerElement} from '@polymer/polymer/polymer-element.js';
import {html} from '@polymer/polymer/lib/utils/html-tag.js';

class JsInjectedGrandChild extends PolymerElement {
  static get is() { return 'js-injected-grand-child' }

  greet() {
    this.$server.handleClientCall("bar");
  }

  static get template() {
    return html`
       <label id='foo-prop'>[[foo]]</label>
       <div id='prop'>[[bar]]</div>
    `;
  }
}

customElements.define(JsInjectedGrandChild.is, JsInjectedGrandChild);
