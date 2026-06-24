import {PolymerElement} from '@polymer/polymer/polymer-element.js';
import {html} from '@polymer/polymer/lib/utils/html-tag.js';
import '@polymer/polymer/lib/elements/dom-repeat.js';

class ClearList extends PolymerElement {
  static get is() {
    return 'clear-list'
  }

  static get template() {
    return html`
        <template is="dom-repeat" items="[[messages]]">
            <div class='msg' on-click="selectItem">[[item.text]]</div>
        </template>
        <button id="clearList" on-click="clearList">clearList</button>
    `;
  }
}
customElements.define(ClearList.is, ClearList);
