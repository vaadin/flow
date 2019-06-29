import {PolymerElement} from '@polymer/polymer/polymer-element.js';
import {html} from '@polymer/polymer/lib/utils/html-tag.js';

class SubPropertyModel extends PolymerElement {
  static get is() {
    return 'sub-property-model'
  }

  static get template() {
    return html`
       <div id="msg" on-click="click">[[status.message]]</div>
       <button on-click="update" id="button">Update</button>
       <button on-click="sync" id="sync">Update from the client to the server (sync)</button>
       <input id="input" value="{{status.message::input}}" on-input="valueUpdated">
      `;
  }

  sync() {
    this.set("status.message", "Set from the client");
  }
}
customElements.define(SubPropertyModel.is, SubPropertyModel);
