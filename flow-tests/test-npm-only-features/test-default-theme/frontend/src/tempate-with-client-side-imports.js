import {PolymerElement} from '@polymer/polymer/polymer-element.js';
import {html} from '@polymer/polymer/lib/utils/html-tag.js';
import '@vaadin/vaadin-button/src/vaadin-button.js';

class TempateWithClientSideImports extends PolymerElement {
  static get template() {
    return html`
       <vaadin-button>Button</vaadin-button>
    `;
  }
  static get is() {
    return 'tempate-with-client-side-imports'
  }
    
}
  
customElements.define(TempateWithClientSideImports.is, TempateWithClientSideImports);