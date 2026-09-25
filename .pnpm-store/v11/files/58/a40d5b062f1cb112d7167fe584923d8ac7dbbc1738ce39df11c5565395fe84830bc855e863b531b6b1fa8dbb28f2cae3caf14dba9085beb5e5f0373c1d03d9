/**
 * @license
 * Copyright (c) 2021 - 2026 Vaadin Ltd.
 * This program is available under Apache License Version 2.0, available at https://vaadin.com/license/
 */
import type { ReactiveController } from 'lit';
import type { LabelController } from './label-controller.js';

/**
 * A controller for linking a `<label>` element with an `<input>` element.
 */
export class LabelledInputController implements ReactiveController {
  constructor(input: HTMLInputElement, labelController: LabelController);

  /**
   * The input element to link with the label.
   */
  input: HTMLInputElement;

  /**
   * Phantom optional method to satisfy the `ReactiveController` interface
   * (TS2559: an interface with all-optional members needs at least one
   * declared property in common). Not implemented at runtime; Lit's
   * `addController` invokes it via optional chaining.
   */
  hostConnected?(): void;
}
