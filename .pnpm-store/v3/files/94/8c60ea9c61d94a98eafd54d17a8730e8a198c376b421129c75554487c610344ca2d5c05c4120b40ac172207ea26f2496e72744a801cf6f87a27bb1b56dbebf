/**
 * @license
 * Copyright (c) 2021 - 2024 Vaadin Ltd.
 * This program is available under Apache License Version 2.0, available at https://vaadin.com/license/
 */
import { PolymerElement } from '@polymer/polymer/polymer-element.js';
import { defineCustomElement } from '@vaadin/component-base/src/define.js';
import { ElementMixin } from '@vaadin/component-base/src/element-mixin.js';
import { IconsetMixin } from './vaadin-iconset-mixin.js';

/**
 * `<vaadin-iconset>` is a Web Component for creating SVG icon collections.
 *
 * @customElement
 * @extends HTMLElement
 * @mixes IconsetMixin
 * @mixes ElementMixin
 */
class Iconset extends IconsetMixin(ElementMixin(PolymerElement)) {
  static get template() {
    return null;
  }

  static get is() {
    return 'vaadin-iconset';
  }
}

defineCustomElement(Iconset);

export { Iconset };
