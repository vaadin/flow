import {PolymerElement} from '@polymer/polymer/polymer-element.js';
import {html} from '@polymer/polymer/lib/utils/html-tag.js';

class MyTemplate extends PolymerElement {
  static get is() { return 'client-update-mode' }

  static get template() {
    return html`
       Basic value: <input value="{{value::change}}"><br />
      Two-way denied: <input value="{{twoWayDenied::change}}"><br />
      Indirect value: [[indirect]]<br />
      Indirect allowed value: [[indirectAllowed]]<br />
      <button on-click="updateIndirect">Update indirect properties</button><br />
      `;
  }

  updateIndirect() {
    this.indirect = this.value;
    this.indirectAllowed = this.value;
  }
}
customElements.define(MyTemplate.is, MyTemplate);
