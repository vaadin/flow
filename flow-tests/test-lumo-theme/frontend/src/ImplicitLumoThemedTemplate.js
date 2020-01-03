import {PolymerElement} from '@polymer/polymer/polymer-element.js';
import {html} from '@polymer/polymer/lib/utils/html-tag.js';

class ImplicitLumoThemedTemplate extends PolymerElement {
  static get is() { return 'implicit-lumo-themed-template' }

  static get template() {
    return html`
      <div id='div'>Template</div>
    `;
  }
}
customElements.define(ImplicitLumoThemedTemplate.is, ImplicitLumoThemedTemplate);
