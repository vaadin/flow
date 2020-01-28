import {PolymerElement} from '@polymer/polymer/polymer-element.js';
import {html} from '@polymer/polymer/lib/utils/html-tag.js';

class ImplicitLumoThemedTemplate extends PolymerElement {
  static get is() { return 'implicit-lumo-themed-template' }

  static get template() {
    return html`
      <div id='div'>Lumo themed Template</div>
       <style>
       div {
        color: var(--lumo-error-color); /* color */
        font-size: var(--lumo-font-size-xxxl); /* typography */
        border: var(--lumo-size-m) solid black; /* sizing */
        margin: var(--lumo-space-wide-l); /* spacing */
        border-radius: var(--lumo-border-radius-l); /* style */
        font-family: lumo-icons; /* icons */
       }
       </style>
     `;
  }
}
customElements.define(ImplicitLumoThemedTemplate.is, ImplicitLumoThemedTemplate);
