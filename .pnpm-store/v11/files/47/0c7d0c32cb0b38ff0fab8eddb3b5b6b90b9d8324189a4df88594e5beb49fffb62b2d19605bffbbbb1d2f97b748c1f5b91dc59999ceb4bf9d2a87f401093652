/**
 * @license
 * Copyright (c) 2017 - 2025 Vaadin Ltd.
 * This program is available under Apache License Version 2.0, available at https://vaadin.com/license/
 */
import type { Constructor } from '@open-wc/dedupe-mixin';
import type { SlotStylesMixinClass } from '@vaadin/component-base/src/slot-styles-mixin.js';
import type { IconFontSizeMixinClass } from './vaadin-icon-font-size-mixin.js';
import type { IconSvgLiteral } from './vaadin-icon-svg.js';

/**
 * A mixin providing common icon functionality.
 */
export declare function IconMixin<T extends Constructor<HTMLElement>>(
  base: T,
): Constructor<IconFontSizeMixinClass> & Constructor<IconMixinClass> & Constructor<SlotStylesMixinClass> & T;

export declare class IconMixinClass {
  /**
   * The name of the icon to use. The name should be of the form:
   * `iconset_name:icon_name`. When using `vaadin-icons` it is possible
   * to omit the first part and only use `icon_name` as a value.
   *
   * Setting the `icon` property updates the `svg` and `size` based on the
   * values provided by the corresponding `vaadin-iconset` element.
   *
   * See also [`name`](#/elements/vaadin-iconset#property-name) property of `vaadin-iconset`.
   *
   * @attr {string} icon
   */
  icon: string | null;

  /**
   * The SVG icon wrapped in a Lit template literal.
   */
  svg: IconSvgLiteral | null;

  /**
   * The SVG source to be loaded as the icon. It can be:
   * - an URL to a file containing the icon
   * - an URL in the format "/path/to/file.svg#objectID", where the "objectID" refers to an ID attribute contained
   *   inside the SVG referenced by the path. Note that the file needs to follow the same-origin policy.
   * - a string in the format "data:image/svg+xml,<svg>...</svg>". You may need to use the "encodeURIComponent"
   *   function for the SVG content passed
   */
  src: string | null;

  /**
   * The symbol identifier that references an ID of an element contained in the SVG element assigned to the
   * `src` property
   */
  symbol: string | null;

  /**
   * Class names defining an icon font and/or a specific glyph inside an icon font.
   *
   * Example: "fa-solid fa-user"
   *
   * @attr {string} icon-class
   */
  iconClass: string | null;

  /**
   * A hexadecimal code point that specifies a glyph from an icon font.
   *
   * Example: "e001"
   */
  char: string | null;

  /**
   * A ligature name that specifies an icon from an icon font with support for ligatures.
   *
   * Example: "home".
   */
  ligature: string | null;

  /**
   * The font family to use for the font icon.
   *
   * @attr {string} font-family
   */
  fontFamily: string | null;

  /**
   * The size of an icon, used to set the `viewBox` attribute.
   */
  size: number;
}
