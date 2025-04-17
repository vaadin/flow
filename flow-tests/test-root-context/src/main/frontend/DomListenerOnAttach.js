import {PolymerElement} from '@polymer/polymer/polymer-element.js';
import {html} from '@polymer/polymer/lib/utils/html-tag.js';

class EventOnAttach extends PolymerElement {
  static get template() {
    return html`
       Fires an event when attached
    `;
  }
  static get is() {
    return 'event-on-attach'
  }
    
  connectedCallback() {
    super.connectedCallback();

    this.dispatchEvent(new CustomEvent('attach'));
  }
}
  
customElements.define(EventOnAttach.is, EventOnAttach);
