/**
 * @license
 * Copyright (c) 2021 - 2026 Vaadin Ltd.
 * This program is available under Apache License Version 2.0, available at https://vaadin.com/license/
 */

/**
 * A controller that detects if content inside the element overflows its scrolling viewport,
 * and sets the `overflow` attribute on the host with a value that indicates the directions
 * where content is overflowing. Supported values are: `top`, `bottom`, `start`, `end`.
 */
export class OverflowController {
  constructor(host, scrollTarget) {
    /**
     * The controller host element.
     *
     * @type {HTMLElement}
     */
    this.host = host;

    /**
     * The element that wraps scrollable content.
     * If not set, the host element is used.
     *
     * @type {HTMLElement}
     */
    this.scrollTarget = scrollTarget || host;

    /** @private */
    this.__boundOnScroll = this.__onScroll.bind(this);
  }

  hostConnected() {
    if (!this.initialized) {
      this.initialized = true;

      this.observe();
    }
  }

  /**
   * Setup scroll listener and observers to update overflow.
   * Also performs one-time update synchronously when called.
   * @protected
   */
  observe() {
    const { host } = this;

    this.__resizeObserver = new ResizeObserver(() => this.__onResize());
    this.__resizeObserver.observe(host);

    // Observe initial children
    [...host.children].forEach((child) => {
      this.__resizeObserver.observe(child);
    });

    this.__childObserver = new MutationObserver((mutations) => {
      mutations.forEach(({ addedNodes, removedNodes }) => {
        addedNodes.forEach((node) => {
          if (node.nodeType === Node.ELEMENT_NODE) {
            this.__resizeObserver.observe(node);
          }
        });

        removedNodes.forEach((node) => {
          if (node.nodeType === Node.ELEMENT_NODE) {
            this.__resizeObserver.unobserve(node);
          }
        });

        if (addedNodes.length === 0 && removedNodes.length > 0) {
          this.__updateState({ sync: true });
        }
      });
    });

    this.__childObserver.observe(host, { childList: true });

    // Update overflow attribute on scroll
    this.scrollTarget.addEventListener('scroll', this.__boundOnScroll);
  }

  /** @private */
  __onResize() {
    this.__updateState({ sync: false });
  }

  /** @private */
  __onScroll() {
    this.__updateState({ sync: true });
  }

  /** @private */
  __updateState({ sync }) {
    cancelAnimationFrame(this.__resizeRaf);

    const state = this.__readState();
    if (sync) {
      this.__writeState(state);
    } else {
      this.__resizeRaf = requestAnimationFrame(() => this.__writeState(state));
    }
  }

  /** @private */
  __readState() {
    const target = this.scrollTarget;

    let overflow = '';

    if (target.scrollTop > 0) {
      overflow += ' top';
    }

    if (Math.ceil(target.scrollTop) < Math.ceil(target.scrollHeight - target.clientHeight)) {
      overflow += ' bottom';
    }

    const scrollLeft = Math.abs(target.scrollLeft);
    if (scrollLeft > 0) {
      overflow += ' start';
    }

    if (Math.ceil(scrollLeft) < Math.ceil(target.scrollWidth - target.clientWidth)) {
      overflow += ' end';
    }

    return { overflow: overflow.trim() };
  }

  /** @private */
  __writeState({ overflow }) {
    if (overflow) {
      this.host.setAttribute('overflow', overflow);
    } else {
      this.host.removeAttribute('overflow');
    }
  }
}
