/**
 * @license
 * Copyright (c) 2022 - 2024 Vaadin Ltd.
 * This program is available under Apache License Version 2.0, available at https://vaadin.com/license/
 */
import type { Constructor } from '@open-wc/dedupe-mixin';
import type { KeyboardMixinClass } from './keyboard-mixin.js';

/**
 * A mixin for navigating items with keyboard.
 */
export declare function KeyboardDirectionMixin<T extends Constructor<HTMLElement>>(
  base: T,
): Constructor<KeyboardDirectionMixinClass> & Constructor<KeyboardMixinClass> & T;

export declare class KeyboardDirectionMixinClass {
  protected readonly focused: Element | null;

  protected readonly _vertical: boolean;

  /**
   * Returns index of the next item that satisfies the given condition,
   * based on the index of the current item and a numeric increment.
   */
  protected _getAvailableIndex(
    items: Element[],
    index: number,
    increment: number,
    condition: (item: Element) => boolean,
  ): number;

  /**
   * Focus the item at given index. Override this method to add custom logic.
   */
  protected _focus(index: number, navigating: boolean): void;

  /**
   * Focus the given item. Override this method to add custom logic.
   */
  protected _focusItem(item: Element, navigating: boolean): void;
}
