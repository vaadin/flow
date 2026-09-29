/**
 * @license
 * Copyright (c) 2021 - 2024 Vaadin Ltd.
 * This program is available under Apache License Version 2.0, available at https://vaadin.com/license/
 */
import type { Constructor } from '@open-wc/dedupe-mixin';

/**
 * A mixin that uses a ResizeObserver to listen to host size changes.
 */
export declare function ResizeMixin<T extends Constructor<HTMLElement>>(base: T): Constructor<ResizeMixinClass> & T;

export declare class ResizeMixinClass {
  /**
   * When true, the parent element resize will be also observed.
   * Override this getter and return `true` to enable this.
   */
  protected readonly _observeParent: boolean;

  /**
   * A handler invoked on host resize. By default, it does nothing.
   * Override the method to implement your own behavior.
   */
  protected _onResize(contentRect: DOMRect): void;
}
