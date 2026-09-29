/**
 * @license
 * Copyright (c) 2021 - 2024 Vaadin Ltd.
 * This program is available under Apache License Version 2.0, available at https://vaadin.com/license/
 */
import '../custom_typings/vaadin-usage-statistics.js';
import '../custom_typings/vaadin.js';
import type { Constructor } from '@open-wc/dedupe-mixin';
import type { DirMixinClass } from './dir-mixin.js';

/**
 * A mixin providing common logic for Vaadin components.
 */
export declare function ElementMixin<T extends Constructor<HTMLElement>>(
  superclass: T,
): Constructor<DirMixinClass> & Constructor<ElementMixinClass> & T;

export declare class ElementMixinClass {
  static is: string;
  static version: string;

  protected static finalize(): void;
}
