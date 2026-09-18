/**
 * @license
 * Copyright (c) 2021 - 2025 Vaadin Ltd.
 * This program is available under Apache License Version 2.0, available at https://vaadin.com/license/
 */
import type { Constructor } from '@open-wc/dedupe-mixin';
import type { ControllerMixinClass } from '@vaadin/component-base/src/controller-mixin.js';
import type { LabelMixinClass } from './label-mixin.js';
import type { ValidateMixinClass } from './validate-mixin.js';

/**
 * A mixin to provide common field logic: label, error message and helper text.
 */
export declare function FieldMixin<T extends Constructor<HTMLElement>>(
  superclass: T,
): Constructor<ControllerMixinClass> &
  Constructor<FieldMixinClass> &
  Constructor<LabelMixinClass> &
  Constructor<ValidateMixinClass> &
  T;

export declare class FieldMixinClass {
  /**
   * String used for the helper text.
   *
   * @attr {string} helper-text
   */
  helperText: string | null | undefined;

  /**
   * Error message to show when the field is invalid.
   *
   * @attr {string} error-message
   */
  errorMessage: string | null | undefined;

  /**
   * String used to label the component to screen reader users.
   * @attr {string} accessible-name
   */
  accessibleName: string | null | undefined;

  /**
   * Id of the element used as label of the component to screen reader users.
   * @attr {string} accessible-name-ref
   */
  accessibleNameRef: string | null | undefined;

  /**
   * A target element to which ARIA attributes are set.
   */
  protected ariaTarget: HTMLElement;

  protected readonly _errorNode: HTMLElement;

  protected readonly _helperNode?: HTMLElement;

  protected _helperTextChanged(helperText: string | null | undefined): void;

  protected _ariaTargetChanged(target: HTMLElement): void;

  protected _requiredChanged(required: boolean): void;

  protected _invalidChanged(invalid: boolean): void;
}
