/**
 * @license
 * Copyright (c) 2021 - 2025 Vaadin Ltd.
 * This program is available under Apache License Version 2.0, available at https://vaadin.com/license/
 */
import type { Icon } from './vaadin-icon.js';
import type { IconSvgLiteral } from './vaadin-icon-svg.js';
import { IconsetMixin } from './vaadin-iconset-mixin.js';

/**
 * `<vaadin-iconset>` is a Web Component for creating SVG icon collections.
 */
declare class Iconset extends IconsetMixin(HTMLElement) {
  /**
   * Set of the `vaadin-icon` instances in the DOM.
   */
  static attachedIcons(): Set<Icon>;

  /**
   * Returns an instance of the iconset by its name.
   */
  static getIconset(name: string): Iconset;

  /**
   * Register an iconset without adding to the DOM.
   */
  static register(name: string, size: number, template: HTMLTemplateElement): void;

  /**
   * Returns SVGTemplateResult for the `icon` ID matching `name` of the
   * iconset, or `nothing` literal if there is no matching icon found.
   */
  static getIconSvg(
    icon: string,
    name?: string,
  ): {
    preserveAspectRatio?: string | null;
    svg: IconSvgLiteral;
    size?: number;
    viewBox?: string | null;
  };
}

declare global {
  interface HTMLElementTagNameMap {
    'vaadin-iconset': Iconset;
  }
}

export { Iconset };
