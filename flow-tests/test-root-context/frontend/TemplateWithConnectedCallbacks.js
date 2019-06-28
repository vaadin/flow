import {PolymerElement} from '@polymer/polymer/polymer-element.js';
import {html} from '@polymer/polymer/lib/utils/html-tag.js';

class TemplateWithConnectedCallbacks extends PolymerElement {
  static get properties() {
    return {
      connected: {
        type: String,
        value: "Not connected",
        notify: true
      }
    }
  }
  static get is() { return 'template-with-connected-callbacks' }

  connectedCallback() {
    super.connectedCallback();
    this.connected = "Connected";
  }

  disconnectedCallback() {
    super.disconnectedCallback();
    this.connected = "Not connected";
  }
  static get template() {
    return html`
       <div id="connectedMessage">{{connected}}</div>
    `;
  }
}
customElements.define(TemplateWithConnectedCallbacks.is, TemplateWithConnectedCallbacks);
