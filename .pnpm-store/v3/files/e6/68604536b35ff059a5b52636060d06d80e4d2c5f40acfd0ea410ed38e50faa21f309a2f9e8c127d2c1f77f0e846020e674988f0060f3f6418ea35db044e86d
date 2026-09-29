/**
 * @license
 * Copyright (c) 2021 - 2024 Vaadin Ltd.
 * This program is available under Apache License Version 2.0, available at https://vaadin.com/license/
 */
import type { Constructor } from '@open-wc/dedupe-mixin';
import type { DelegateFocusMixinClass } from '@vaadin/a11y-base/src/delegate-focus-mixin.js';
import type { DisabledMixinClass } from '@vaadin/a11y-base/src/disabled-mixin.js';
import type { FocusMixinClass } from '@vaadin/a11y-base/src/focus-mixin.js';
import type { KeyboardMixinClass } from '@vaadin/a11y-base/src/keyboard-mixin.js';
import type { ControllerMixinClass } from '@vaadin/component-base/src/controller-mixin.js';
import type { DelegateStateMixinClass } from '@vaadin/component-base/src/delegate-state-mixin.js';
import type { SlotStylesMixinClass } from '@vaadin/component-base/src/slot-styles-mixin.js';
import type { ClearButtonMixinClass } from './clear-button-mixin.js';
import type { FieldMixinClass } from './field-mixin.js';
import type { InputConstraintsMixinClass } from './input-constraints-mixin.js';
import type { InputMixinClass } from './input-mixin.js';
import type { LabelMixinClass } from './label-mixin.js';
import type { ValidateMixinClass } from './validate-mixin.js';

/**
 * A mixin to provide shared logic for the editable form input controls.
 */
export declare function InputControlMixin<T extends Constructor<HTMLElement>>(
  base: T,
): Constructor<ClearButtonMixinClass> &
  Constructor<ControllerMixinClass> &
  Constructor<DelegateFocusMixinClass> &
  Constructor<DelegateStateMixinClass> &
  Constructor<DisabledMixinClass> &
  Constructor<FieldMixinClass> &
  Constructor<FocusMixinClass> &
  Constructor<InputConstraintsMixinClass> &
  Constructor<InputControlMixinClass> &
  Constructor<InputMixinClass> &
  Constructor<KeyboardMixinClass> &
  Constructor<LabelMixinClass> &
  Constructor<SlotStylesMixinClass> &
  Constructor<ValidateMixinClass> &
  T;

export declare class InputControlMixinClass {
  /**
   * A pattern matched against individual characters the user inputs.
   *
   * When set, the field will prevent:
   * - `keydown` events if the entered key doesn't match `/^allowedCharPattern$/`
   * - `paste` events if the pasted text doesn't match `/^allowedCharPattern*$/`
   * - `drop` events if the dropped text doesn't match `/^allowedCharPattern*$/`
   *
   * For example, to allow entering only numbers and minus signs, use:
   * `allowedCharPattern = "[\\d-]"`
   * @attr {string} allowed-char-pattern
   */
  allowedCharPattern: string;

  /**
   * If true, the input text gets fully selected when the field is focused using click or touch / tap.
   */
  autoselect: boolean;

  /**
   * The name of this field.
   */
  name: string;

  /**
   * A hint to the user of what can be entered in the field.
   */
  placeholder: string;

  /**
   * When present, it specifies that the field is read-only.
   */
  readonly: boolean;

  /**
   * The text usually displayed in a tooltip popup when the mouse is over the field.
   */
  title: string;
}
