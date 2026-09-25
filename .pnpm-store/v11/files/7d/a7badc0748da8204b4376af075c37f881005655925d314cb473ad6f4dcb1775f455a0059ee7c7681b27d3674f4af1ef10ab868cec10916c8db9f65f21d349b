/**
 * @license
 * Copyright (c) 2021 - 2026 Vaadin Ltd.
 * This program is available under Apache License Version 2.0, available at https://vaadin.com/license/
 */
import type { Constructor } from '@open-wc/dedupe-mixin';
import type { KeyboardMixinClass } from '@vaadin/a11y-base/src/keyboard-mixin.js';
import type { InputMixinClass } from './input-mixin.js';

/**
 * A mixin that manages the clear button.
 */
export declare function ClearButtonMixin<T extends Constructor<HTMLElement>>(
  base: T,
): Constructor<ClearButtonMixinClass> & Constructor<InputMixinClass> & Constructor<KeyboardMixinClass> & T;

export declare class ClearButtonMixinClass {
  /**
   * Set to true to display the clear icon which clears the input.
   * This also enables clearing the input when pressing Esc key.
   *
   * @attr {boolean} clear-button-visible
   */
  clearButtonVisible: boolean;

  /**
   * Clears the value and dispatches `input` and `change` events
   * on the input element. This method should be called
   * when the clear action originates from the user.
   */
  protected _onClearAction(): void;
}
