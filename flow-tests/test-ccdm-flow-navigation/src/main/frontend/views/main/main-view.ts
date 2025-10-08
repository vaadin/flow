/*
 * Copyright 2000-2025 Vaadin Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
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
      <hr />
      <slot></slot>
    `;
  }
}
