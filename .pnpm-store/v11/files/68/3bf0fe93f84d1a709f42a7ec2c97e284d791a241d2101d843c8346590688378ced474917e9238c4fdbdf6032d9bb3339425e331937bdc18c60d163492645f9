/**
 * @license
 * Copyright (c) 2021 - 2025 Vaadin Ltd.
 * This program is available under Apache License Version 2.0, available at https://vaadin.com/license/
 */
import type { ReactiveController } from 'lit';

/**
 * A controller that detects if content inside the element overflows its scrolling viewport,
 * and sets the `overflow` attribute on the host with a value that indicates the directions
 * where content is overflowing. Supported values are: `top`, `bottom`, `start`, `end`.
 */
export class OverflowController implements ReactiveController {
  /**
   * The controller host element.
   */
  host: HTMLElement;

  /**
   * The element that wraps scrollable content.
   * If not set, the host element is used.
   */
  scrollTarget: HTMLElement;

  constructor(host: HTMLElement, scrollTarget?: HTMLElement);

  hostConnected(): void;

  /**
   * Setup a scroll listener and observers to update overflow.
   * Also performs one-time update synchronously when called.
   */
  protected observe(): void;
}
