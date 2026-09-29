/**
 * @license
 * Copyright (c) 2021 - 2024 Vaadin Ltd.
 * This program is available under Apache License Version 2.0, available at https://vaadin.com/license/
 */
import type { ReactiveController } from 'lit';

/**
 * A controller for trapping focus within a DOM node.
 */
export class FocusTrapController implements ReactiveController {
  /**
   * The controller host element.
   */
  host: HTMLElement;

  constructor(node: HTMLElement);

  hostConnected(): void;

  hostDisconnected(): void;

  /**
   * Activates a focus trap for a DOM node that will prevent focus from escaping the node.
   * The trap can be deactivated with the `.releaseFocus()` method.
   *
   * If focus is initially outside the trap, the method will move focus inside,
   * on the first focusable element of the trap in the tab order.
   * The first focusable element can be the trap node itself if it is focusable
   * and comes first in the tab order.
   */
  trapFocus(trapNode: HTMLElement): void;

  /**
   * Deactivates the focus trap set with the `.trapFocus()` method
   * so that it becomes possible to tab outside the trap node.
   */
  releaseFocus(): void;
}
