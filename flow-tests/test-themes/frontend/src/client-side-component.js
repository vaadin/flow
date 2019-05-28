import {PolymerElement} from '@polymer/polymer/polymer-element.js';
import {html} from '@polymer/polymer/lib/utils/html-tag.js';

class ClientSideComponent extends PolymerElement {
  static get template() {
    return html`
       <div id="non-themed">Non Themed Client Side Component</div>
`;
  }
  
  static get is() {
      return 'client-side-component'
  }
  
}
  
customElements.define(ClientSideComponent.is, ClientSideComponent);
