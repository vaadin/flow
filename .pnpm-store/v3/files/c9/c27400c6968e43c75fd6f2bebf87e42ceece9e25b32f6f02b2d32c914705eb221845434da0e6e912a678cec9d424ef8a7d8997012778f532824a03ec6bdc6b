/**
 * @license
 * Copyright (c) 2021 - 2024 Vaadin Ltd.
 * This program is available under Apache License Version 2.0, available at https://vaadin.com/license/
 */
import type { ReactiveController } from 'lit';

/**
 * A controller for handling modal state on the elements with `dialog` and `alertdialog` role.
 * See https://developer.mozilla.org/en-US/docs/Web/Accessibility/ARIA/Attributes/aria-modal
 */
export class AriaModalController implements ReactiveController {
  /**
   * The controller host element.
   */
  host: HTMLElement;

  /**
   * The callback used to detect which element
   * to use as a target. Defaults to the host.
   */
  callback: () => HTMLElement | HTMLElement[];

  constructor(node: HTMLElement);

  /**
   * Make the controller host element modal by trapping focus inside it and hiding
   * other elements from screen readers using `aria-hidden="true"` appropriately.
   *
   * The method name is chosen to align with the one provided by native `<dialog>`:
   * https://developer.mozilla.org/en-US/docs/Web/API/HTMLDialogElement/showModal
   */
  showModal(): void;

  /**
   * Exit modal state: release focus and remove `aria-hidden` from other elements
   * unless there are any other underlying elements that are also shown as modal.
   */
  close(): void;
}
