/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
import {PolymerElement} from '@polymer/polymer/polymer-element.js';
import {html} from '@polymer/polymer/lib/utils/html-tag.js';

class TemplateMappingDetectorParent extends PolymerElement {
  static get is() { return 'template-mapping-detector-parent' }

  static get template() {
    return html`
       <template-mapping-detector id="detector"></template-mapping-detector>
    `;
  }
}
customElements.define(TemplateMappingDetectorParent.is, TemplateMappingDetectorParent);
