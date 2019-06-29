import {PolymerElement} from '@polymer/polymer/polymer-element.js';
import {html} from '@polymer/polymer/lib/utils/html-tag.js';

class JsInjectedTemplate extends PolymerElement {
  static get is() { return 'js-injected-template' }

  clientHandler() {
    this.$server.handleClientCall("bar");
  }

  static get template() {
    return html`
      <label id='foo-prop'>[[foo]]</label>
      <label id='baz-prop'>[[baz]]</label>
    `;
  }
}

customElements.define(JsInjectedTemplate.is, JsInjectedTemplate);
