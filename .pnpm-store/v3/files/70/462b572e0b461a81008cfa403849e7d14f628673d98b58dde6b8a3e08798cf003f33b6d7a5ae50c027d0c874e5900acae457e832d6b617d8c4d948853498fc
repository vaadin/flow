/**
 * @license
 * Copyright (c) 2021 - 2024 Vaadin Ltd.
 * This program is available under Apache License Version 2.0, available at https://vaadin.com/license/
 */
import type { Constructor } from '@open-wc/dedupe-mixin';

/**
 * A mixin to store the reference to an input element
 * and add input and change event listeners to it.
 */
export declare function InputMixin<T extends Constructor<HTMLElement>>(base: T): Constructor<InputMixinClass> & T;

export declare class InputMixinClass {
  /**
   * A reference to the input element controlled by the mixin.
   * Any component implementing this mixin is expected to provide it
   * by using `this._setInputElement(input)` Polymer API.
   *
   * A typical case is using `InputController` that does this automatically.
   * However, the input element does not have to always be native <input>:
   * as an example, <vaadin-combo-box-light> accepts other components.
   */
  readonly inputElement: HTMLElement;

  /**
   * The value of the field.
   */
  value: string;

  /**
   * Indicates whether the value is different from the default one.
   * Override if the `value` property has a type other than `string`.
   */
  protected readonly _hasValue: boolean;

  /**
   * A property for accessing the input element's value.
   *
   * Override this getter if the property is different from the default `value` one.
   */
  protected readonly _inputElementValueProperty: string;

  /**
   * The input element's value.
   */
  protected _inputElementValue: string | undefined;

  /**
   * Clear the value of the field.
   */
  clear(): void;

  protected _addInputListeners(input: HTMLElement): void;

  protected _removeInputListeners(input: HTMLElement): void;

  protected _forwardInputValue(input: HTMLElement): void;

  protected _inputElementChanged(input: HTMLElement, oldInput: HTMLElement): void;

  protected _onChange(event: Event): void;

  protected _onInput(event: Event): void;

  protected _setInputElement(input: HTMLElement): void;

  protected _toggleHasValue(hasValue: boolean): void;

  protected _valueChanged(value?: string, oldValue?: string): void;

  protected _setHasInputValue(event: InputEvent): void;
}
