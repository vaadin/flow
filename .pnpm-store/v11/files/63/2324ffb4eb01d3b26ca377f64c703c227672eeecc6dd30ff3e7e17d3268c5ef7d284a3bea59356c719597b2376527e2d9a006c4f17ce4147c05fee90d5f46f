/**
 * @license
 * Copyright (c) 2022 - 2025 Vaadin Ltd.
 * This program is available under Apache License Version 2.0, available at https://vaadin.com/license/
 */
import { SlotController } from './slot-controller.js';

type TooltipPosition =
  | 'bottom-end'
  | 'bottom-start'
  | 'bottom'
  | 'end-bottom'
  | 'end-top'
  | 'end'
  | 'start-bottom'
  | 'start-top'
  | 'start'
  | 'top-end'
  | 'top-start'
  | 'top';

/**
 * A controller that manages the slotted tooltip element.
 */
export class TooltipController extends SlotController {
  /**
   * An HTML element for linking with the tooltip overlay
   * via `aria-describedby` attribute used by screen readers.
   * When not set, defaults to `target`.
   */
  ariaTarget: HTMLElement | HTMLElement[];

  /**
   * Object with properties passed to `generator`
   * function to be used for generating tooltip text.
   */
  context: Record<string, unknown>;

  /**
   * When true, the tooltip is controlled programmatically
   * instead of reacting to focus and mouse events.
   */
  manual: boolean;

  /**
   * When true, the tooltip is opened programmatically.
   * Only works if `manual` is set to `true`.
   */
  opened: boolean;

  /**
   * Position of the tooltip with respect to its target.
   */
  position: TooltipPosition;

  /**
   * An HTML element to attach the tooltip to.
   */
  target: HTMLElement;

  /**
   * Set an HTML element for linking with the tooltip overlay
   * via `aria-describedby` attribute used by screen readers.
   */
  setAriaTarget(ariaTarget: HTMLElement | HTMLElement[]): void;

  /**
   * Set a context object to be used by generator.
   */
  setContext(context: Record<string, unknown>): void;

  /**
   * Toggle manual state on the slotted tooltip.
   */
  setManual(manual: boolean): void;

  /**
   * Toggle opened state on the slotted tooltip.
   */
  setOpened(opened: boolean): void;

  /**
   * Set default position for the slotted tooltip.
   * This can be overridden by setting the position
   * using corresponding property or attribute.
   */
  setPosition(position: TooltipPosition): void;

  /**
   * Set function used to detect whether to show
   * the tooltip based on a condition.
   */
  setShouldShow(shouldShow: (target: HTMLElement) => boolean): void;

  /**
   * Set an HTML element to attach the tooltip to.
   */
  setTarget(target: HTMLElement): void;
}
