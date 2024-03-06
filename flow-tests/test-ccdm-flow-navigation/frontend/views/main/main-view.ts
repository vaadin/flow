/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
import { css, html, LitElement } from 'lit';
import { customElement } from 'lit/decorators.js';

import { router } from 'Frontend/index';

@customElement('main-view')
export class MainView extends LitElement {
    static get styles() {
        return css`
            :host {
              display: block;
            }
        `;
    }

    render() {
        return html`
            <ul>
                <li><a href="${router.urlForPath('about')}" id="menu-about">About</a></li>
                <li><a href="${router.urlForPath('another')}" id="menu-another">Another</a></li>
                <li><a href="deep/another" id="menu-deep-another">Deep another</a></li>
                <li><a href="hello" id="menu-hello">Hello (server side)</a></li>            
            </ul>
            <hr>
            <slot></slot>
        `;
    }
}
