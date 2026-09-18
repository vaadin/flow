/**
 * @license
 * Copyright (c) 2021 - 2025 Vaadin Ltd.
 * This program is available under Apache License Version 2.0, available at https://vaadin.com/license/
 */
import type { Constructor } from '@open-wc/dedupe-mixin';

/**
 * A mixin to handle `focused` and `focus-ring` attributes based on focus.
 */
export declare function FocusMixin<T extends Constructor<HTMLElement>>(base: T): Constructor<FocusMixinClass> & T;

export declare class FocusMixinClass {
  protected readonly _keyboardActive: boolean;

  /**
   * Override to change how focused and focus-ring attributes are set.
   */
  protected _setFocused(focused: boolean): void;

  /**
   * Override to define if the field receives focus based on the event.
   */
  protected _shouldSetFocus(event: FocusEvent): boolean;

  /**
   * Override to define if the field loses focus based on the event.
   */
  protected _shouldRemoveFocus(event: FocusEvent): boolean;
}
