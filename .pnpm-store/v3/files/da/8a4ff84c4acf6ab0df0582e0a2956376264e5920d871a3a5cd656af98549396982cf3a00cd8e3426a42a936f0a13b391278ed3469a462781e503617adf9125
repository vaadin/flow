/**
 * @license
 * Copyright (c) 2021 - 2024 Vaadin Ltd.
 * This program is available under Apache License Version 2.0, available at https://vaadin.com/license/
 */
import type { Constructor } from '@open-wc/dedupe-mixin';
import type { LitElement } from 'lit';

export declare function PolylitMixin<T extends Constructor<LitElement>>(base: T): Constructor<PolylitMixinClass> & T;

export declare class PolylitMixinClass {
  ready(): void;

  /**
   * Reads a value from a path.
   */
  protected _get(root: object, path: string): any;

  /**
   * Sets a value to a path.
   */
  protected _set(root: object, path: string, value: any): void;
}
