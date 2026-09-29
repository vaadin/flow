/**
 * @license
 * Copyright (c) 2021 - 2024 Vaadin Ltd.
 * This program is available under Apache License Version 2.0, available at https://vaadin.com/license/
 */
import type { Constructor } from '@open-wc/dedupe-mixin';

/**
 * A mixin to provide disabled property for field components.
 */
export declare function DisabledMixin<T extends Constructor<HTMLElement>>(base: T): Constructor<DisabledMixinClass> & T;

export declare class DisabledMixinClass {
  /**
   * If true, the user cannot interact with this element.
   */
  disabled: boolean;

  protected _disabledChanged(disabled: boolean, oldDisabled: boolean): void;
}
