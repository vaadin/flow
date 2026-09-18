/**
 * @license
 * Copyright (c) 2021 - 2025 Vaadin Ltd.
 * This program is available under Apache License Version 2.0, available at https://vaadin.com/license/
 */
import type { Constructor } from '@open-wc/dedupe-mixin';
import type { KeyboardMixinClass } from '@vaadin/a11y-base/src/keyboard-mixin.js';
import type { ControllerMixinClass } from '@vaadin/component-base/src/controller-mixin.js';
import type { InputMixinClass } from './input-mixin.js';

/**
 * A mixin that manages the clear button.
 */
export declare function ClearButtonMixin<T extends Constructor<HTMLElement>>(
  base: T,
): Constructor<ClearButtonMixinClass> &
  Constructor<ControllerMixinClass> &
  Constructor<InputMixinClass> &
  Constructor<KeyboardMixinClass> &
  T;

export declare class ClearButtonMixinClass {
  /**
   * Set to true to display the clear icon which clears the input.
   *
   * It is up to the component to choose where to place the clear icon:
   * in the Shadow DOM or in the light DOM. In any way, a reference to
   * the clear icon element should be provided via the `clearElement` getter.
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
