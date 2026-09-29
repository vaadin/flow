/**
 * @license
 * Copyright (c) 2021 - 2024 Vaadin Ltd.
 * This program is available under Apache License Version 2.0, available at https://vaadin.com/license/
 */
import { html, PolymerElement } from '@polymer/polymer';
import { defineCustomElement } from '@vaadin/component-base/src/define.js';
import { DirMixin } from '@vaadin/component-base/src/dir-mixin.js';
import { registerStyles, ThemableMixin } from '@vaadin/vaadin-themable-mixin/vaadin-themable-mixin.js';
import { InputContainerMixin } from './vaadin-input-container-mixin.js';
import { inputContainerStyles } from './vaadin-input-container-styles.js';

registerStyles('vaadin-input-container', inputContainerStyles, { moduleId: 'vaadin-input-container-styles' });

/**
 * @customElement
 * @extends HTMLElement
 * @mixes ThemableMixin
 * @mixes DirMixin
 * @mixes InputContainerMixin
 */
export class InputContainer extends InputContainerMixin(ThemableMixin(DirMixin(PolymerElement))) {
  static get is() {
    return 'vaadin-input-container';
  }

  static get template() {
    return html`
      <slot name="prefix"></slot>
      <slot></slot>
      <slot name="suffix"></slot>
    `;
  }
}

defineCustomElement(InputContainer);
