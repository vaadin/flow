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
       <button on-click="clientHandlerError" id="clientError">Delegate via the $server and throw</button>
       <span id="status">[[status]]</span>
    `;
  }

  overriddenClick(event) {
    var event = event || window.event;
    event.result = "ClientSide handler";
  }

  async clientHandler() {
      this.status = "Waiting"
  	
      const msg = "foo";
      const enabled = true;
      let result = await this.$server.handleClientCall(msg, enabled);
      this.status = result;
  }
  
  async clientHandlerError() {
      this.status = "Waiting"
          
      const msg = "foo";
      const enabled = false;
      try {
          await this.$server.handleClientCall(msg, enabled);
      } catch (error) {
      	  this.status = error;
      }
  } 
}
customElements.define(EventHandler.is, EventHandler);
