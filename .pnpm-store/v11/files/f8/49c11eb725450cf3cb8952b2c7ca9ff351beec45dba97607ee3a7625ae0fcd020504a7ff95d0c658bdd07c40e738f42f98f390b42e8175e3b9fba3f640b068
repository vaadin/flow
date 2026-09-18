/**
 * @license
 * Copyright (c) 2021 - 2025 Vaadin Ltd.
 * This program is available under Apache License Version 2.0, available at https://vaadin.com/license/
 */
import { SlotChildObserveController } from '@vaadin/component-base/src/slot-child-observe-controller.js';

/**
 * A controller that manages the error message node content.
 */
export class ErrorController extends SlotChildObserveController {
  /**
   * String used for the error message text content.
   */
  protected errorMessage: string | null | undefined;

  /**
   * Set to true when the host element is invalid.
   */
  protected invalid: boolean;

  /**
   * Set the error message element text content.
   */
  setErrorMessage(errorMessage: string | null | undefined): void;

  /**
   * Set invalid state for detecting whether to show error message.
   */
  setInvalid(invalid: boolean): void;
}
