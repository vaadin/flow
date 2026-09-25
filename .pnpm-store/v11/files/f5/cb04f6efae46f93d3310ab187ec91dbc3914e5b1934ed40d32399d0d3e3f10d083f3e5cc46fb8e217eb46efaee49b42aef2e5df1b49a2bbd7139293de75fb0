/**
 * @license
 * Copyright (c) 2017 - 2026 Vaadin Ltd.
 * This program is available under Apache License Version 2.0, available at https://vaadin.com/license/
 */
import type { Constructor } from '@open-wc/dedupe-mixin';

export declare function ThemePropertyMixin<T extends Constructor<HTMLElement>>(
  base: T,
): Constructor<ThemePropertyMixinClass> & T;

export declare class ThemePropertyMixinClass {
  /**
   * Helper property with theme attribute value facilitating propagation
   * in shadow DOM.
   *
   * Enables the component implementation to propagate the `theme`
   * attribute value to the sub-components in Shadow DOM by binding
   * the sub-component's "theme" attribute using the Lit template:
   *
   * ```html
   * <vaadin-notification-card
   *   theme="${ifDefined(this._theme)}"
   * ></vaadin-notification-card>
   * ```
   *
   * @protected
   */
  protected readonly _theme: string | null | undefined;
}
