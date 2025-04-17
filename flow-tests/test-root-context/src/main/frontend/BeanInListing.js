import {PolymerElement} from '@polymer/polymer/polymer-element.js';
import {html} from '@polymer/polymer/lib/utils/html-tag.js';
import '@polymer/polymer/lib/elements/dom-repeat.js';

class BeanListing extends PolymerElement {
  static get template() {
    return html`
        <template is="dom-repeat" items="[[users]]">
          <div on-click="handleUserClick" class="user-item">[[item.name]]</div>
      </template>

      <template is="dom-repeat" items="[[messages]]">
          <div on-click="handleMsgClick" class="msg-item">[[item]]</div>
      </template>

      <label id="selected" style="display:block;">[[selected]]</label>
    `;
  }
  static get is() {
    return 'listing-bean-view'
  }

  static get properties(){
    return {
      activeUser: {
         type: Object,
         value: {},
         notify: true
      },
      activeMessage: {
         type: String,
         value: null,
         notify: true
      }
    };
  }

  handleUserClick(event){
    this.activeUser = event.model.item;
  }

  handleMsgClick(event){
    this.activeMessage = event.model.item;
  }
}

customElements.define(BeanListing.is, BeanListing);
