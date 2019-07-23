import {PolymerElement} from '@polymer/polymer/polymer-element.js';
import {html} from '@polymer/polymer/lib/utils/html-tag.js';
import '@vaadin/vaadin-button/src/vaadin-button.js';

class TemplateWithClientSideImports extends PolymerElement {
  static get template() {
    return html`
       <vaadin-button>Button</vaadin-button>
    `;
  }
  static get is() {
    return 'template-with-client-side-imports'
  }
    
}
  
customElements.define(TemplateWithClientSideImports.is, TemplateWithClientSideImports);
