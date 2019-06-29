import {PolymerElement} from '@polymer/polymer/polymer-element.js';
import {html} from '@polymer/polymer/lib/utils/html-tag.js';

class EventHandler extends PolymerElement {
  static get is() {
    return 'event-handler'
  }

  static get template() {
    return html`
       <button on-click="handleClick" id="handle">Click me</button>
       <button on-click="sendData" id="send">Send event data to the server</button>
       <button on-click="overriddenClick" id="overridden">Client and server event</button>
       <button on-click="clientHandler" id="client">Delegate via the $server</button>
    `;
  }

  overriddenClick(event) {
    var event = event || window.event;
    event.result = "ClientSide handler";
  }

  clientHandler() {
    const msg = "foo";
    const enabled = true;
    this.$server.handleClientCall(msg, enabled);
  }
}
customElements.define(EventHandler.is, EventHandler);
