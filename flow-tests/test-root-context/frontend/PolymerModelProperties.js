import {PolymerElement} from '@polymer/polymer/polymer-element.js';
import {html} from '@polymer/polymer/lib/utils/html-tag.js';

class MyTemplate extends PolymerElement {
  static get is() { return 'model-properties' }

  static get properties() {
    return {
      text :{
        type: String,
        notify: true
      }
    };
  }

  static get template() {
    return html`
      <input id="input" value="{{text::input}}" on-change="valueUpdated">
    `;
  }
}
customElements.define(MyTemplate.is, MyTemplate);
