import {PolymerElement} from '@polymer/polymer/polymer-element.js';
import {html} from '@polymer/polymer/lib/utils/html-tag.js';
import '@polymer/polymer/lib/elements/dom-repeat.js';
import '@polymer/polymer/lib/elements/dom-if.js';

class ListBinding extends PolymerElement {
  static get is() {
    return 'list-binding'
  }

  static get template() {
    return html`
    <template is="dom-repeat" items="[[messages]]">
      <div class='msg' on-click="selectItem">[[item.text]]</div>
    </template>
    <button id="reset" on-click="reset">reset</button>
    <button id="addElement" on-click="addElement">addElement</button>
    <button id="addElementByIndex" on-click="addElementByIndex">addElementByIndex</button>
    <button id="addNumerousElements" on-click="addNumerousElements">addNumerousElements</button>
    <button id="addNumerousElementsByIndex" on-click="addNumerousElementsByIndex">addNumerousElementsByIndex
    </button>
    <button id="clearList" on-click="clearList">clearList</button>
    <button id="removeSecondElementByIndex" on-click="removeSecondElementByIndex">removeSecondElementByIndex
    </button>
    <button id="removeFirstElementWithIterator" on-click="removeFirstElementWithIterator">
        removeFirstElementWithIterator
    </button>
    <button id="swapFirstAndSecond" on-click="swapFirstAndSecond">swapFirstAndSecond</button>
    <button id="sortDescending" on-click="sortDescending">sortDescending</button>
    <button id="setInitialStateToEachMessage" on-click="setInitialStateToEachMessage">setInitialStateToEachMessage
    </button>
    <template is="dom-if" if="[[selectedMessage]]">
        <div id="selection">Clicked message: [[selectedMessage.text]]</div>
    </template>
    `;
  }
}
customElements.define(ListBinding.is, ListBinding);
