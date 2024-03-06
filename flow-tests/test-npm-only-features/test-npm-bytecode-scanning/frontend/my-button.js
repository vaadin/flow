/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
import {PolymerElement,html} from '@polymer/polymer/polymer-element.js';

class MyButton extends PolymerElement {

    static get template() {
        return html`<button>Click me</button>`;
    }

    static get is() {
          return 'my-button';
    }
}

customElements.define(MyButton.is, MyButton);
