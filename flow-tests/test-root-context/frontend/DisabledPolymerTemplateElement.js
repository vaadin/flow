import {PolymerElement} from '@polymer/polymer/polymer-element.js';
import {html} from '@polymer/polymer/lib/utils/html-tag.js';

class DisabledPolymerTemplateElement extends PolymerElement {
  static get is() {
    return 'disabled-polymer-element'
  }

  static get template() {
    return html`
        <div id='injected' disabled>Disabled</div>
        <slot></slot>
    `;
  }
}
customElements.define(DisabledPolymerTemplateElement.is, DisabledPolymerTemplateElement);
