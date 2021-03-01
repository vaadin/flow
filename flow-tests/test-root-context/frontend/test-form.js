import {PolymerElement} from '@polymer/polymer/polymer-element.js';
import {html} from '@polymer/polymer/lib/utils/html-tag.js';

class TestForm extends PolymerElement {
  static get is() { return 'test-form' }

  static get template() {
    return html`
       <div id="div">Template text</div> 
    `;
  }
}
customElements.define(TestForm.is, TestForm);
