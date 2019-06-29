import {PolymerElement} from '@polymer/polymer/polymer-element.js';
import {html} from '@polymer/polymer/lib/utils/html-tag.js';

class TemplateWithInjectedId extends PolymerElement {
  static get is() { return 'template-with-injected-id' }

  static get template() {
    return html`
       <div id="ignore"></div>
       <div id="target"></div>
    `;
  }
}
customElements.define(TemplateWithInjectedId.is, TemplateWithInjectedId);
