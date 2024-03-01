import {PolymerElement} from '@polymer/polymer/polymer-element.js';
import {html} from '@polymer/polymer/lib/utils/html-tag.js';
import '@polymer/polymer/lib/elements/dom-repeat.js';

class ModelList extends PolymerElement {
  static get is() {
    return 'model-list';
  }
  ready() {
    super.ready();
    this.$server.add();
  }
  toggle(e) {
    let item = e.model.item;
    this.$server.toggle(item);
  }
  toggleItemWithItem(e) {
    let item = this.itemWithItem.item;
    this.$server.toggle(item);
  }
  setNullTexts(e) {
    this.$server.setNullTexts();
  }
  static get template() {
    return html`
        <div id="repeat-1">
        <template is="dom-repeat" items="{{items}}">
            <div on-click="toggle">{{item.clicked}} {{item.text}}</div>
        </template>
    </div>
    <div id="repeat-2">
        <template is="dom-repeat" items="[[moreItems]]">
            <div on-click="toggle">[[item.clicked]] [[item.text]]</div>
        </template>
    </div>
    <div id="repeat-3">
        <template is="dom-repeat" items="[[lotsOfItems]]" as="outerItem">
            <template is="dom-repeat" items="[[outerItem]]">
                <div on-click="toggle">[[item.clicked]] [[item.text]]</div>
            </template>
        </template>
    </div>
    <div id="repeat-4">
        <template is="dom-repeat" items="[[itemWithItems.items]]">
            <div on-click="toggle">[[item.clicked]] [[item.text]]</div>
        </template>
    </div>
    <div id="item-with-item-div" on-click="toggleItemWithItem">[[itemWithItem.item.clicked]] [[itemWithItem.item.text]]</div>
    <button on-click="setNullTexts" id="set-null">Set null texts</button>
  `;
  }
}
customElements.define(ModelList.is, ModelList);
