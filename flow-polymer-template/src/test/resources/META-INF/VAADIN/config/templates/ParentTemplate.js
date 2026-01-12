/*
 * Copyright (C) 2022 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
import { PolymerElement } from '@polymer/polymer/polymer-element.js';
import { html } from '@polymer/polymer/lib/utils/html-tag.js';
import './ChildTemplate.js';

class ParentTemplate extends PolymerElement {
  static get is() {
    return 'parent-template';
  }

  static get template() {
    return html`
      <div>Parent Template</div>
      <div>
        <div>Placeholder</div>

        <child-template id="child"></child-template>
      </div>
      <style>
        parent-template {
          width: 100%;
        }
      </style>
    `;
  }
}

customElements.define(ParentTemplate.is, ParentTemplate);
