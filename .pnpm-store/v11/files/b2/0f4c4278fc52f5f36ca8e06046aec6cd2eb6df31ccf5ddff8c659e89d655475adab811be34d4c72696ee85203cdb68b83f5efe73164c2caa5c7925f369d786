/**
 * @license
 * Copyright (c) 2021 - 2025 Vaadin Ltd.
 * This program is available under Apache License Version 2.0, available at https://vaadin.com/license/
 */
import type { Constructor } from '@open-wc/dedupe-mixin';
import type { ReactiveController } from 'lit';

/**
 * A mixin for connecting controllers to the element.
 */
export declare function ControllerMixin<T extends Constructor<HTMLElement>>(
  superclass: T,
): Constructor<ControllerMixinClass> & T;

export declare class ControllerMixinClass {
  /**
   * Registers a controller to participate in the element update cycle.
   */
  protected addController(controller: ReactiveController): void;

  /**
   * Removes a controller from the element.
   */
  protected removeController(controller: ReactiveController): void;
}
