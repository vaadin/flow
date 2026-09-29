/**
 * @license
 * Copyright (c) 2017 - 2024 Vaadin Ltd.
 * This program is available under Apache License Version 2.0, available at https://vaadin.com/license/
 */
import type { Constructor } from '@open-wc/dedupe-mixin';
import type { CSSResult, CSSResultGroup } from 'lit';
import type { ThemePropertyMixinClass } from './vaadin-theme-property-mixin.js';

/**
 * A mixin for `nav` elements, facilitating navigation and selection of childNodes.
 */
export declare function ThemableMixin<T extends Constructor<HTMLElement>>(
  base: T,
): Constructor<ThemableMixinClass> & Constructor<ThemePropertyMixinClass> & T;

export declare class ThemableMixinClass {
  protected static finalize(): void;

  protected static finalizeStyles(styles?: CSSResultGroup): CSSResult[];
}

/**
 * Registers CSS styles for a component type. Make sure to register the styles before
 * the first instance of a component of the type is attached to DOM.
 */
declare function registerStyles(themeFor: string | null, styles: CSSResultGroup, options?: object | null): void;

type Theme = {
  themeFor: string;
  styles: CSSResult[];
  moduleId?: string;
  include?: string[] | string;
};

/**
 * For internal purposes only.
 */
declare const __themeRegistry: Theme[];

export { css, unsafeCSS } from 'lit';

export { registerStyles, __themeRegistry };
