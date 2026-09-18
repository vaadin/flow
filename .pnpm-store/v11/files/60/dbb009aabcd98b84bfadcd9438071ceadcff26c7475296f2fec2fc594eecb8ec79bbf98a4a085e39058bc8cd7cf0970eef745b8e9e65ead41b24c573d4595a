/**
 * @license
 * Copyright (c) 2021 - 2025 Vaadin Ltd.
 * This program is available under Apache License Version 2.0, available at https://vaadin.com/license/
 */
import type { Constructor } from '@open-wc/dedupe-mixin';
import type { DisabledMixinClass } from './disabled-mixin.js';
import type { FocusMixinClass } from './focus-mixin.js';
import type { TabindexMixinClass } from './tabindex-mixin.js';

/**
 * A mixin to forward focus to an element in the light DOM.
 */
export declare function DelegateFocusMixin<T extends Constructor<HTMLElement>>(
  base: T,
): Constructor<DelegateFocusMixinClass> &
  Constructor<DisabledMixinClass> &
  Constructor<FocusMixinClass> &
  Constructor<TabindexMixinClass> &
  T;

export declare class DelegateFocusMixinClass {
  /**
   * Specify that this control should have input focus when the page loads.
   */
  autofocus: boolean;

  /**
   * A reference to the focusable element controlled by the mixin.
   * It can be an input, textarea, button or any element with tabindex > -1.
   *
   * Any component implementing this mixin is expected to provide it
   * by using `this._setFocusElement(input)` Polymer API.
   */
  readonly focusElement: HTMLElement | null | undefined;

  protected _addFocusListeners(element: HTMLElement): void;

  protected _removeFocusListeners(element: HTMLElement): void;

  protected _focusElementChanged(element: HTMLElement, oldElement: HTMLElement): void;

  protected _onBlur(event: FocusEvent): void;

  protected _onFocus(event: FocusEvent): void;

  protected _setFocusElement(element: HTMLElement): void;
}
