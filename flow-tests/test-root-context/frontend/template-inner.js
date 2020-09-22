import {PolymerElement} from '@polymer/polymer/polymer-element.js';
import {html} from '@polymer/polymer/lib/utils/html-tag.js';

class TemplateInner extends PolymerElement {

  static get is() { return 'template-inner' }

  static get template() {
    return html`
        <div>Hello template inner</div>
    `;
  }
}

customElements.define(TemplateInner.is, TemplateInner);
