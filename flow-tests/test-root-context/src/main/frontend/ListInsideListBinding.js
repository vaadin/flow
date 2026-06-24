import {PolymerElement} from '@polymer/polymer/polymer-element.js';
import {html} from '@polymer/polymer/lib/utils/html-tag.js';
import '@polymer/polymer/lib/elements/dom-repeat.js';
import '@polymer/polymer/lib/elements/dom-if.js';

class ListInsideListBinding extends PolymerElement {
  static get is() {
    return 'list-inside-list-binding'
  }

  static get template() {
    return html`
        <dom-repeat items="[[nestedMessages]]">
        <template>
            <div>
                <template is="dom-repeat" items="[[item]]">
                    <div class='submsg' on-click="removeItem">[[item.text]]</div>
                </template>
            </div>
        </template>
    </dom-repeat>
    <template is="dom-if" if="[[removedMessage]]">
        <div id="removedMessage">Removed message: [[removedMessage.text]]</div>
    </template>
    <button id="reset" on-click="reset">reset</button>
    <button id="updateAllElements" on-click="updateAllElements">updateAllElements</button>
    `;
  }
}
customElements.define(ListInsideListBinding.is, ListInsideListBinding);
