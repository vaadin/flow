/**
 * @license
 * Copyright (c) 2017 - 2024 Vaadin Ltd.
 * This program is available under Apache License Version 2.0, available at https://vaadin.com/license/
 */
import type { Constructor } from '@open-wc/dedupe-mixin';
import type { KeyboardDirectionMixinClass } from './keyboard-direction-mixin.js';
import type { KeyboardMixinClass } from './keyboard-mixin.js';

/**
 * A mixin for list elements, facilitating navigation and selection of items.
 */
export declare function ListMixin<T extends Constructor<HTMLElement>>(
  base: T,
): Constructor<KeyboardDirectionMixinClass> & Constructor<KeyboardMixinClass> & Constructor<ListMixinClass> & T;

export declare class ListMixinClass {
  /**
   * If true, the user cannot interact with this element.
   * When the element is disabled, the selected item is
   * not updated when `selected` property is changed.
   */
  disabled: boolean;

  /**
   * The index of the item selected in the items array.
   * Note: Not updated when used in `multiple` selection mode.
   */
  selected: number | null | undefined;

  /**
   * Define how items are disposed in the dom.
   * Possible values are: `horizontal|vertical`.
   * It also changes navigation keys from left/right to up/down.
   */
  orientation: 'horizontal' | 'vertical';

  /**
   * The list of items from which a selection can be made.
   * It is populated from the elements passed to the light DOM,
   * and updated dynamically when adding or removing items.
   *
   * The item elements must implement `Vaadin.ItemMixin`.
   *
   * Note: unlike `<vaadin-combo-box>`, this property is read-only,
   * so if you want to provide items by iterating array of data,
   * you have to use `dom-repeat` and place it to the light DOM.
   */
  readonly items: Element[] | undefined;

  protected readonly _scrollerElement: HTMLElement;
}
