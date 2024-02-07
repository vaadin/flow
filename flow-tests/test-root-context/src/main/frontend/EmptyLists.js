import {PolymerElement} from '@polymer/polymer/polymer-element.js';
import {html} from '@polymer/polymer/lib/utils/html-tag.js';

class EmptyList extends PolymerElement {
  static get is() {
    return 'empty-list'
  }

  static get template() {
    return html`
       <ul>
        <template is="dom-repeat" items="[[items]]">
            <li class="item">[[item.label]]</li>
        </template>
    </ul>
    `;
  }
}
customElements.define(EmptyList.is, EmptyList);
