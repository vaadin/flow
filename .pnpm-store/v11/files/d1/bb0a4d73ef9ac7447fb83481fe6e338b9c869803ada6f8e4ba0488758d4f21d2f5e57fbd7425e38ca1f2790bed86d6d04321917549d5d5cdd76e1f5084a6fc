/**
 * @license
 * Copyright (c) 2021 - 2025 Vaadin Ltd.
 * This program is available under Apache License Version 2.0, available at https://vaadin.com/license/
 */
import './vaadin-iconset.js';
import { html, LitElement } from 'lit';
import { ifDefined } from 'lit/directives/if-defined.js';
import { defineCustomElement } from '@vaadin/component-base/src/define.js';
import { ElementMixin } from '@vaadin/component-base/src/element-mixin.js';
import { PolylitMixin } from '@vaadin/component-base/src/polylit-mixin.js';
import { ThemableMixin } from '@vaadin/vaadin-themable-mixin/vaadin-themable-mixin.js';
import { IconMixin } from './vaadin-icon-mixin.js';
import { iconStyles } from './vaadin-icon-styles.js';

/**
 * LitElement based version of `<vaadin-icon>` web component.
 *
 * ## Disclaimer
 *
 * This component is an experiment not intended for publishing to npm.
 * There is no ETA regarding specific Vaadin version where it'll land.
 * Feel free to try this code in your apps as per Apache 2.0 license.
 */
class Icon extends IconMixin(ElementMixin(ThemableMixin(PolylitMixin(LitElement)))) {
  static styles = iconStyles;

  /** @protected */
  render() {
    return html`
      <svg
        version="1.1"
        xmlns="http://www.w3.org/2000/svg"
        xmlns:xlink="http://www.w3.org/1999/xlink"
        viewBox="${this.__computeViewBox(this.size, this.__viewBox)}"
        preserveAspectRatio="${this.__computePAR(this.__defaultPAR, this.__preserveAspectRatio)}"
        fill="${ifDefined(this.__fill)}"
        stroke="${ifDefined(this.__stroke)}"
        stroke-width="${ifDefined(this.__strokeWidth)}"
        stroke-linecap="${ifDefined(this.__strokeLinecap)}"
        stroke-linejoin="${ifDefined(this.__strokeLinejoin)}"
        aria-hidden="true"
      >
        <g id="svg-group"></g>
        <g id="use-group" visibility="${this.__computeVisibility(this.__useRef, this.svg)}">
          <use href="${this.__useRef}" />
        </g>
      </svg>

      <slot name="tooltip"></slot>
    `;
  }

  static get is() {
    return 'vaadin-icon';
  }
}

defineCustomElement(Icon);

export { Icon };
