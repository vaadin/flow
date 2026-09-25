/**
 * @license
 * Copyright (c) 2017 - 2026 Vaadin Ltd.
 * This program is available under Apache License Version 2.0, available at https://vaadin.com/license/
 */
import type { Constructor } from '@open-wc/dedupe-mixin';
import type { CSSResult, CSSResultGroup } from 'lit';
import type { ThemePropertyMixinClass } from './vaadin-theme-property-mixin.js';

export declare function ThemableMixin<T extends Constructor<HTMLElement>>(
  base: T,
): Constructor<ThemableMixinClass> & Constructor<ThemePropertyMixinClass> & T;

export declare class ThemableMixinClass {
  protected static finalize(): void;

  protected static finalizeStyles(styles?: CSSResultGroup): CSSResult[];
}

// eslint-disable-next-line @typescript-eslint/no-empty-object-type
export declare interface ThemableMixinClass extends ThemePropertyMixinClass {}

/**
 * Registers CSS styles for a component type. Make sure to register the styles before
 * the first instance of a component of the type is attached to DOM.
 */
declare function registerStyles(themeFor: string | null, styles: CSSResultGroup, options?: object | null): void;

export { css, unsafeCSS } from 'lit';

export { registerStyles };
