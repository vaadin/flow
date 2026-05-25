/*
 * Copyright 2000-2026 Vaadin Ltd.
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

/*
 * Minimal stand-in for vaadin-text-field used by ThemableTextField.
 * Reproduces the shadow-DOM shape the theme-related ITs walk:
 *   div.vaadin-field-container > vaadin-input-container[part=input-field] > input
 * Defining this as a real custom element is what lets Flow's theme machinery
 * (per-component CSS folder, @CssImport(themeFor=...)) target it.
 */
class ThemableInput extends HTMLElement {
  constructor() {
    super();
    const shadow = this.attachShadow({ mode: 'open' });
    const container = document.createElement('div');
    container.className = 'vaadin-field-container';
    const inputField = document.createElement('vaadin-input-container');
    inputField.setAttribute('part', 'input-field');
    inputField.appendChild(document.createElement('input'));
    container.appendChild(inputField);
    shadow.appendChild(container);
  }
}
customElements.define('themable-input', ThemableInput);
