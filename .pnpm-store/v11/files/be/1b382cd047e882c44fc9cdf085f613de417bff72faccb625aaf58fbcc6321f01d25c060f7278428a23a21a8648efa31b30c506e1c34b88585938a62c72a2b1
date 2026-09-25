/**
 * @license
 * Copyright (c) 2021 - 2026 Vaadin Ltd.
 * This program is available under Apache License Version 2.0, available at https://vaadin.com/license/
 */
import type { ReactiveController } from 'lit';

/**
 * A controller which prevents the virtual keyboard from showing up on mobile devices
 * when the field's overlay is closed.
 */
export class VirtualKeyboardController implements ReactiveController {
  constructor(host: HTMLElement & { inputElement?: HTMLElement; opened: boolean });

  /**
   * Phantom optional method to satisfy the `ReactiveController` interface
   * (TS2559: an interface with all-optional members needs at least one
   * declared property in common). Not implemented at runtime; Lit's
   * `addController` invokes it via optional chaining.
   */
  hostConnected?(): void;
}
