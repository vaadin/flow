import { PolymerElement } from '@polymer/polymer/polymer-element.js';
import { html } from '@polymer/polymer/lib/utils/html-tag.js';

class TemplateScalabilityPanel extends PolymerElement {
  static get is() {
    return 'template-scalability-panel';
  }

  static get template() {
    return html`
      <style include="shared-styles">
        :host {
          display: block;
        }
      </style>

      <button id="ack-btn"></button>
    `;
  }
}

customElements.define(TemplateScalabilityPanel.is, TemplateScalabilityPanel);
