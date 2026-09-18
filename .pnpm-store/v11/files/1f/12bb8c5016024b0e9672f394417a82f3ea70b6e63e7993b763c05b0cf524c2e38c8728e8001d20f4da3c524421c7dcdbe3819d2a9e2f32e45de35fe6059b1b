/**
 * @license
 * Copyright (c) 2023 - 2025 Vaadin Ltd.
 * This program is available under Apache License Version 2.0, available at https://vaadin.com/license/
 */
import type { Constructor } from '@open-wc/dedupe-mixin';

/**
 * A mixin that forwards CSS class names to the internal overlay element
 * by setting the `overlayClass` property or `overlay-class` attribute.
 */
export declare function OverlayClassMixin<T extends Constructor<HTMLElement>>(
  base: T,
): Constructor<OverlayClassMixinClass> & T;

export declare class OverlayClassMixinClass {
  /**
   * A space-delimited list of CSS class names to set on the overlay element.
   * This property does not affect other CSS class names set manually via JS.
   *
   * Note, if the CSS class name was set with this property, clearing it will
   * remove it from the overlay, even if the same class name was also added
   * manually, e.g. by using `classList.add()` in the `renderer` function.
   *
   * @attr {string} overlay-class
   */
  overlayClass: string;

  /**
   * An overlay element on which CSS class names are set.
   */
  protected _overlayElement: HTMLElement;
}
