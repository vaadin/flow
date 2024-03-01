import {PolymerElement} from '@polymer/polymer/polymer-element.js';
import {html} from '@polymer/polymer/lib/utils/html-tag.js';

class TemplateScalabilityView extends PolymerElement {
  static get is() {
    return 'template-scalability-view';
  }

  static get template() {
    return html`
                  <style include="shared-styles">
            :host {
                display: block;
            }
        </style>

        <div id="content">

        </div>
    `;
  }
}

customElements.define(TemplateScalabilityView.is, TemplateScalabilityView);
