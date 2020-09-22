import {PolymerElement} from '@polymer/polymer/polymer-element.js';
import {html} from '@polymer/polymer/lib/utils/html-tag.js';
import './template-inner.js';

class TemplateOuter extends PolymerElement {

  static get is() { return 'template-outer'; }

  static get template() {
    return html`
            <div>Hello template outer</div>
            <template-inner id="inner" style="display: block"></template-inner>
    `;
  }
}

customElements.define(TemplateOuter.is, TemplateOuter);
