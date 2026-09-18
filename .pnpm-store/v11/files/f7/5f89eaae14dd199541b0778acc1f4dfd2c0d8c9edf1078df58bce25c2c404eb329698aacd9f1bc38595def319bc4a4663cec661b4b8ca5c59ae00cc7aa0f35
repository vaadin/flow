/**
 * @license
 * Copyright (c) 2017 - 2025 Vaadin Ltd.
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
   * the sub-component's "theme" attribute to the `theme` property of
   * the host.
   *
   * **NOTE:** Extending the mixin only provides the property for binding,
   * and does not make the propagation alone.
   *
   * See [Styling Components: Sub-components](https://vaadin.com/docs/latest/styling/styling-components/#sub-components).
   * page for more information.
   *
   * @protected
   */
  protected readonly _theme: string | null | undefined;
}
