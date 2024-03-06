/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
import {PolymerElement, html} from '@polymer/polymer/polymer-element.js';
import {ThemableMixin} from '@vaadin/vaadin-themable-mixin/vaadin-themable-mixin.js';

class ThemedComponent extends ThemableMixin(PolymerElement) {
  static get template() {
    return html`
        <div part="content">Just a div</div>
    `;
  }

  static get is() { return 'themed-component' }
}

customElements.define(ThemedComponent.is, ThemedComponent);
