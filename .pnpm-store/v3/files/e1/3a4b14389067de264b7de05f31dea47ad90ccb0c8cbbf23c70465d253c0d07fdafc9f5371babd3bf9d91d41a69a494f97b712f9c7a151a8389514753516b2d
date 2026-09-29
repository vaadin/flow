/**
 * @license
 * Copyright (c) 2022 - 2024 Vaadin Ltd.
 * This program is available under Apache License Version 2.0, available at https://vaadin.com/license/
 */
import { SlotController } from './slot-controller.js';

/**
 * A controller that manages the slotted tooltip element.
 */
export class TooltipController extends SlotController {
  constructor(host) {
    // Do not provide slot factory to create tooltip lazily.
    super(host, 'tooltip');

    this.setTarget(host);
  }

  /**
   * Override to initialize the newly added custom tooltip.
   *
   * @param {Node} tooltipNode
   * @protected
   * @override
   */
  initCustomNode(tooltipNode) {
    tooltipNode.target = this.target;

    if (this.ariaTarget !== undefined) {
      tooltipNode.ariaTarget = this.ariaTarget;
    }

    if (this.context !== undefined) {
      tooltipNode.context = this.context;
    }

    if (this.manual !== undefined) {
      tooltipNode.manual = this.manual;
    }

    if (this.opened !== undefined) {
      tooltipNode.opened = this.opened;
    }

    if (this.position !== undefined) {
      tooltipNode._position = this.position;
    }

    if (this.shouldShow !== undefined) {
      tooltipNode.shouldShow = this.shouldShow;
    }

    this.__notifyChange();
  }

  /**
   * Override to notify the host when the tooltip is removed.
   *
   * @param {Node} tooltipNode
   * @protected
   * @override
   */
  teardownNode() {
    this.__notifyChange();
  }

  /**
   * Set an HTML element for linking with the tooltip overlay
   * via `aria-describedby` attribute used by screen readers.
   * @param {HTMLElement} ariaTarget
   */
  setAriaTarget(ariaTarget) {
    this.ariaTarget = ariaTarget;

    const tooltipNode = this.node;
    if (tooltipNode) {
      tooltipNode.ariaTarget = ariaTarget;
    }
  }

  /**
   * Set a context object to be used by generator.
   * @param {object} context
   */
  setContext(context) {
    this.context = context;

    const tooltipNode = this.node;
    if (tooltipNode) {
      tooltipNode.context = context;
    }
  }

  /**
   * Toggle manual state on the slotted tooltip.
   * @param {boolean} manual
   */
  setManual(manual) {
    this.manual = manual;

    const tooltipNode = this.node;
    if (tooltipNode) {
      tooltipNode.manual = manual;
    }
  }

  /**
   * Toggle opened state on the slotted tooltip.
   * @param {boolean} opened
   */
  setOpened(opened) {
    this.opened = opened;

    const tooltipNode = this.node;
    if (tooltipNode) {
      tooltipNode.opened = opened;
    }
  }

  /**
   * Set default position for the slotted tooltip.
   * This can be overridden by setting the position
   * using corresponding property or attribute.
   * @param {string} position
   */
  setPosition(position) {
    this.position = position;

    const tooltipNode = this.node;
    if (tooltipNode) {
      tooltipNode._position = position;
    }
  }

  /**
   * Set function used to detect whether to show
   * the tooltip based on a condition.
   * @param {Function} shouldShow
   */
  setShouldShow(shouldShow) {
    this.shouldShow = shouldShow;

    const tooltipNode = this.node;
    if (tooltipNode) {
      tooltipNode.shouldShow = shouldShow;
    }
  }

  /**
   * Set an HTML element to attach the tooltip to.
   * @param {HTMLElement} target
   */
  setTarget(target) {
    this.target = target;

    const tooltipNode = this.node;
    if (tooltipNode) {
      tooltipNode.target = target;
    }
  }

  /** @private */
  __notifyChange() {
    this.dispatchEvent(new CustomEvent('tooltip-changed', { detail: { node: this.node } }));
  }
}
