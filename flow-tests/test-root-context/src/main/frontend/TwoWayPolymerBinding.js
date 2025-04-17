import {PolymerElement} from '@polymer/polymer/polymer-element.js';
import {html} from '@polymer/polymer/lib/utils/html-tag.js';


class MyTemplate extends PolymerElement {
  static get is() { return 'my-template' }

  static get properties(){
    return {
      value: {
        type: String,
        value: "foo",
        notify: true
      }
    }
  }

  static get template() {
    return html`
        <input id="input" value="{{value::input}}" on-input="valueUpdated">
        <div id="status">[[status]]</div>
        <button id="reset" on-click="resetValue">Reset value</button>
    `;
  }
}
customElements.define(MyTemplate.is, MyTemplate);
