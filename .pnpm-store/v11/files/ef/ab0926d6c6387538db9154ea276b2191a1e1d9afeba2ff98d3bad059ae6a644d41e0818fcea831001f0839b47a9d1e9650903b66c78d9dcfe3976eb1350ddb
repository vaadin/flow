/**
 * @license
 * Copyright (c) 2021 - 2025 Vaadin Ltd.
 * This program is available under Apache License Version 2.0, available at https://vaadin.com/license/
 */
import type { Constructor } from '@open-wc/dedupe-mixin';

/**
 * A mixin to delegate properties and attributes to a target element.
 */
export declare function DelegateStateMixin<T extends Constructor<HTMLElement>>(
  base: T,
): Constructor<DelegateStateMixinClass> & T;

export declare class DelegateStateMixinClass {
  /**
   * A target element to which attributes and properties are delegated.
   */
  protected stateTarget: HTMLElement | null;
}
