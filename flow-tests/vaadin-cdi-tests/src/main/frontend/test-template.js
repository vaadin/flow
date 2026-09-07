import {html} from '@polymer/polymer/lib/utils/html-tag.js';
import {PolymerElement} from '@polymer/polymer/polymer-element.js';

class TestTemplate extends PolymerElement {
  static get template() {
    return html`
        <div>
        AAAAAAAAAAAAAAAAAAAAAAAAA
            <input id="input"/>
            <label id="label"/>
        </div>
    `;
  }

  static get is() {
    return 'test-template'
  }
}

window.customElements.define(TestTemplate.is, TestTemplate);
