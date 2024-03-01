import {PolymerElement} from '@polymer/polymer/polymer-element.js';
import {html} from '@polymer/polymer/lib/utils/html-tag.js';
import '@polymer/polymer/lib/elements/dom-repeat.js';

class TwoWayListBinding extends PolymerElement {
  static get is() {
    return 'two-way-list-binding'
  }

  static get template() {
    return html`
        <div>
    <template is="dom-if" if="[[enable]]">
        <template is="dom-repeat" items="[[messages]]" as="message">
        <input id="input" value="{{message.text::input}}" on-change="valueUpdated">
        </template>
    </template>
    <slot></slot>
    </div>
    `;
  }
}
customElements.define(TwoWayListBinding.is, TwoWayListBinding);
