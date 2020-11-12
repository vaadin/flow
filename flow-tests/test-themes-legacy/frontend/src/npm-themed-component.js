import {PolymerElement} from '@polymer/polymer/polymer-element.js';
import {html} from '@polymer/polymer/lib/utils/html-tag.js';
import './client-side-component.js';

class NpmThemedComponent extends PolymerElement {
  static get template() {
    return html`
       <div id="non-themed">Non Themed Component</div>
       <client-side-component></client-side-component>
`;
  }
  
  static get is() {
    return 'npm-themed-component'
  }
  
}
  
customElements.define(NpmThemedComponent.is, NpmThemedComponent);
