import {PolymerElement} from '@polymer/polymer/polymer-element.js';
import {html} from '@polymer/polymer/lib/utils/html-tag.js';

class ServerRequest extends PolymerElement {
  static get template() {
    return html`
       <div style='height:2000px'></div>
        <button id='send-request' on-click="_requestServer">Send server request</button>
    `;
  }
  static get is() {
    return 'server-request'
  }
    
  _requestServer(){
    this.$server.requestServer();
  }
}
  
customElements.define(ServerRequest.is, ServerRequest);
