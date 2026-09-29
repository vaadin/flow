/**
 * @license
 * Copyright (c) 2021 - 2024 Vaadin Ltd.
 * This program is available under Apache License Version 2.0, available at https://vaadin.com/license/
 */
import type { Constructor } from '@open-wc/dedupe-mixin';

/**
 * A controller for saving a focused node and restoring focus to it later.
 */
export declare function FocusRestorationController<T extends Constructor<HTMLElement>>(
  base: T,
): Constructor<FocusRestorationControllerClass> & T;

export declare class FocusRestorationControllerClass {
  /**
   * Saves the given node as a target for restoring focus to
   * when `restoreFocus()` is called. If no node is provided,
   * the currently focused node in the DOM is saved as a target.
   */
  saveFocus(node: Node | null | undefined): void;

  /**
   * Restores focus to the target node that was saved previously with `saveFocus()`.
   */
  restoreFocus(): void;
}
